package com.mickare.xserver.events;

import com.mickare.xserver.net.XServer;

public class XServerLoggedInEvent extends XServerEvent
{

	private final XServer server;
	
	public XServerLoggedInEvent(XServer server)
	{
		super("Server " + server.getName() + " logged inLogged");
		this.server = server;
	}

	public XServer getServer()
	{
		return server;
	}

}
