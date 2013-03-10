package com.mickare.xserver;

import java.io.IOException;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;


import com.mickare.xserver.Exception.NotInitializedException;
import com.mickare.xserver.socket.ServerThreadPoolExecutor;
import com.mickare.xserver.socket.XServerReceiving;
import com.mickare.xserver.socket.XServerSending;

public class ServerMain implements Runnable {
	
	private static ServerMain instance = null;
	
	public static ServerMain getInstance() throws NotInitializedException {
		if(instance == null) {
			throw new NotInitializedException("ServerMain not initialized!");
		}
		return instance;
	}
	
	protected static void initialize(XServer meServer) {
		instance = new ServerMain(meServer);		
	}
	
	private final static int BACKLOG = 10;
	
	private final ServerSocketFactory ssf;
	private final XServer meServer;
	
	
	private ServerThreadPoolExecutor stpool;

	private ServerSocket server;
	private Thread serverThread;
	
	private ServerMain(XServer meServer) {	
		this.meServer = meServer;
		ssf = ServerSocketFactory.getDefault();
	}
	
	protected synchronized boolean start() throws IOException {
			if (serverThread == null) {
				server = ssf.createServerSocket(meServer.getPort(), BACKLOG);
				serverThread = new Thread(this);
				stpool = new ServerThreadPoolExecutor();
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
			if(stpool != null) {
				stpool.shutDown();
				stpool = null;
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
				stpool.runTask(new XServerReceiving(server.accept()));
			} catch (IOException e) {
				
			}
		}
	}
	
	protected void sendMessage(XServer target, Message m) throws IOException {
		stpool.runTask(new XServerSending(target, m));
	}
	
	
	
}
