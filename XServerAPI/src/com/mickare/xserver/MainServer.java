package com.mickare.xserver;

import java.io.IOException;
import java.net.ServerSocket;

import com.mickare.xserver.net.Connection;
import com.mickare.xserver.util.MyStringUtils;

public class MainServer extends Thread
{

	private final ServerSocket server;
	private final AbstractXServerManager manager;
	
	protected MainServer(ServerSocket server, AbstractXServerManager manager)
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
				new Connection(server.accept(), manager);
			} catch (IOException e)
			{
				manager.getLogger().warning("Exception while connecting: " + e.getMessage() + "\n" + MyStringUtils.stackTraceToString(e));
			}
		}
	}

}
