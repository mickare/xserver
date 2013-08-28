package com.mickare.xserver;

import java.io.IOException;
import java.net.ServerSocket;

import com.mickare.xserver.net.Connection;

public class MainServer extends Thread
{

	private final ServerSocket server;
	
	protected MainServer(ServerSocket server)
	{
		super("XServer Main Server Thread");
		this.server = server;
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
				new Connection(server.accept());
			} catch (IOException e)
			{

			}
		}
	}

}
