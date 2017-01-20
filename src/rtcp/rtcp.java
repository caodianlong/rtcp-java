package rtcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class rtcp {

	private static rtcp instance = new rtcp();
	private Socket[] sockets = {null, null}; //存放需要数据交换的两个socket对象
	private Thread[] threads = {null, null}; //存放两个工作线程 
	
	public rtcp() {
		
	}

	public static void main(String[] args) throws InterruptedException {
		if (args.length == 0){
			help();
			return;
		}
		
		int i = 0;
		for (i = 0; i < 2 && i < args.length; i++){
			String[] s = args[i].split(":");
			//listen
			if (s.length == 2 && s[0].toLowerCase().equals("l")){
				int num = i;
				int port = Integer.parseInt(s[1]);
				instance.threads[i] = new Thread(new Runnable(){
					@Override
					public void run() {
						try{
							instance.listen(num, port);
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				});				
			}
			
			//connect
			if (s.length == 3 && s[0].toLowerCase().equals("c")){
				int num = i;
				String host = s[1];
				int port = Integer.parseInt(s[2]);
				instance.threads[i] = new Thread(new Runnable(){
					@Override
					public void run() {
						try{
							instance.connect(num, host, port);
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				});
			}
		}
		
		for(Thread t : instance.threads){
			if (t != null) t.start();
		}
		
		for(Thread t : instance.threads){
			if (t != null) t.join();
		}
		
	}
	
	private static void help(){		
		System.out.println("# Usage: \n"
			+ "\t java -jar rtcp.jar stream1 stream2 \n"
			+ "\t stream为：l:port或c:host:port \n"
			+ "\t l:port表示监听指定的本地端口 \n"
			+ "\t c:host:port表示监听远程指定的端口 \n"
		);
	}
	
	private Socket getAnother(int num){
		int another = 0;
		if (num == 0) another = 1;
//		while(true){
//			//对当前进行检查，避免在等待另一个连接时，当前连接已关闭造成死锁
//			if (sockets[num] == null || sockets[num].isClosed()){
//				return null;
//			}
//			if (sockets[another] == null){
//				try {
//					Thread.sleep(1);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				continue;
//			}else if (sockets[another].isClosed()){
//				sockets[another] = null;
//			}else{
//				break;
//			}
//		}
		
		return sockets[another];
		
	}
	
	@SuppressWarnings("resource")
	private void listen(int num, int port){
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(port);
			System.out.println(String.format("[%d] listen on %d", num, port));
		} catch (IOException e) {
			System.out.println(String.format("[%d] can not listen on %d", num, port));
			return;
		} 
    	while(true){
    		try {
				Socket socket = serverSocket.accept();
				synchronized(sockets){
					sockets[num] = socket;
				}
				System.out.println(String.format("[%d] connect from: %s:%d", num, socket.getInetAddress().getHostAddress(), socket.getPort()));
				exchange(num, socket);
				if (!socket.isClosed()) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
	}
	
	private void connect(int num, String host, int port){
		while(true){
			Socket socket = null;
			try {
				socket = new Socket(host, port);
			} catch (UnknownHostException e) {
				System.out.println(String.format("[%d] unknow host: %s", num, host));
				return;
			} catch (IOException e) {
				e.printStackTrace();
				try {
					System.out.println(String.format("[%d] can not connect to %s:%d, retry after 10s", num, host, port));
					Thread.sleep(10000); //retry after 30s
					continue;
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			
			if (socket == null) continue;
			synchronized(sockets){
				sockets[num] = socket;
			}
			System.out.println(String.format("[%d] connect to %s:%d", num, host, port));
			exchange(num, socket);
			if (!socket.isClosed()) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void exchange(int num, Socket s1){
//		System.out.println(String.format("[%d] ready for exchange", num));
		Socket s2 = null;
		try {
			InputStream ins1 = s1.getInputStream();
	        byte[] buf = new byte[10 * 1024];
	        while(true){
	        	int rlen = ins1.read(buf);
	        	if (rlen <= 0){
	        		break;
	        	}
	        	
	        	s2 = getAnother(num);
	        	if (s2 != null && s2.isConnected()){
		        	OutputStream ops2 = s2.getOutputStream();
		        	ops2.write(buf, 0, rlen);
		        	System.out.println(String.format("[%d] exchange %d bytes", num, rlen));
	        	}else{
	        		System.out.println(String.format("[%d] discard %d bytes", num, rlen));
	        	}
	        }
		}catch (SocketException e){
			//e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		synchronized(sockets){
			try {
				if (s1 != null && !s1.isClosed()) {
					s1.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				if (s2 != null && !s2.isClosed()) {
					s2.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			sockets[num] = null;
		}
		
//		waitAnother(num);

//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		System.out.println(String.format("[%d] connect closed", num));
	}
	
	private void waitAnother(int num){
		int another = 0;
		if (num == 0) another = 1;
		while(sockets[another] != null){
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
