package com.mickare.xserver.events;

import com.mickare.xserver.net.XServer;

public class XServerDisconnectEvent extends XServerEvent
{

	private final XServer<? extends Object> server;
	
	public XServerDisconnectEvent(XServer<? extends Object> server) {
		super("Disconnected from server " + server.getName());
		this.server = server;
	}

	public XServer<? extends Object> getServer()
	{
		return server;
	}
	
}
