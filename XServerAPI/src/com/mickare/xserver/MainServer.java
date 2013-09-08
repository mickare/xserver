package com.mickare.xserver;

import java.io.IOException;
import java.net.ServerSocket;

import com.mickare.xserver.net.Connection;

public class MainServer<T> extends Thread
{

	private final ServerSocket server;
	private final AbstractXServerManager<T> manager;
	
	protected MainServer(ServerSocket server, AbstractXServerManager<T> manager)
	{
		super("XServer Main Server Thread");
		this.server = server;
		this.manager = manager;
	}

	public void close() throws IOException {
		this.interrupt();
		this.server.close();
	}

	public boolean isClosed() {
		return this.server.isClosed();
	}
	
	@Override
	public void run()
	{
		while (!this.isInterrupted() && !isClosed())
		{
			try
			{
				new Connection<T>(server.accept(), manager);
			} catch (IOException e)
			{
				
			}
		}
	}

}
