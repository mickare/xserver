package com.mickare.xserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.net.Connection;

public class MainServer implements Runnable {
	
	private final static int BACKLOG = 100;
	
	private final XServerManager manager;
	
	private final ServerSocketFactory ssf;
	
	private ServerSocket server;
	private Thread serverThread;
	
	protected MainServer(XServerManager manager) {	
		this.manager = manager;
		ssf = ServerSocketFactory.getDefault();
	}
	
	protected synchronized boolean start() throws IOException {
			if (serverThread == null) {
				server = ssf.createServerSocket(manager.getHomeServer().getPort(), BACKLOG);
				serverThread = new Thread(this);
				serverThread.start();
				return true;
			}
			return false;
	}

	protected synchronized boolean stop() throws IOException {
			if (serverThread != null) {			
				serverThread.interrupt();
				serverThread = null;
				return true;
			}
			if(server != null) {
				server.close();
				server = null;
			}
			return false;
	}

	@Override
	public void run() {
		while (serverThread != null) {
			try {
				final Socket s = server.accept();
				manager.getThreadPool().runTask(new Runnable(){
					@Override
					public void run()
					{
						try
						{
							new Connection(s);
						} catch (IOException | NotInitializedException e)
						{

						}
					}
				});
			} catch (IOException e) {
				
			}
		}
	}	
		
}
