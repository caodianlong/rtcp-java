# rtcp-java
使用java实现的反向tcp链接，可实现端口转发、内网穿透。
使用java标准库，不依赖第三方库。只需编译rtcp.java即可。

参考项目https://github.com/knownsec/rtcp，命令行格式与其一致。但改进了程序在等待对端链接容易引起死锁的问题、重连间隔改为10秒。


# Usage: 
	 java -jar rtcp.jar stream1 stream2
	 stream为：l:port或c:host:port 
	 l:port表示监听指定的本地端口 
	 c:host:port表示监听远程指定的端口 
   
