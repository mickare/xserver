package com.mickare.xserver.events;

import com.mickare.xserver.net.XServer;

public class XServerLoggedInEvent extends XServerEvent
{

	private final XServer<? extends Object> server;
	
	public XServerLoggedInEvent(XServer<? extends Object> server)
	{
		super("Server " + server.getName() + " logged inLogged");
		this.server = server;
	}

	public XServer<? extends Object> getServer()
	{
		return server;
	}

}
