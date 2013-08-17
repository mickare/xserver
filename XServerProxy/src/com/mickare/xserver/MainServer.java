package com.mickare.xserver;

import java.io.IOException;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.net.Connection;
import com.mickare.xserver.net.XServer;

public class MainServer implements Runnable {

	private final static int BACKLOG = 10;

	private final ServerSocketFactory ssf;
	private final XServer home;

	private ServerSocket server;
	private Thread serverThread;

	protected MainServer(XServerManager manager) {
		this.home = manager.getHomeServer();
		ssf = ServerSocketFactory.getDefault();
	}

	protected synchronized boolean start() throws IOException {
		if (serverThread == null) {
			server = ssf.createServerSocket(home.getPort(), BACKLOG);
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
		if (server != null) {
			server.close();
			server = null;
		}
		return false;
	}

	@Override
	public void run() {
		while (serverThread != null) {
			try {
				new Connection(server.accept());
			} catch (IOException | NotInitializedException e) {

			}
		}
	}

}
