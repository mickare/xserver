package com.mickare.xserver.events;

import com.mickare.xserver.net.XServer;

public class XServerDisconnectEvent extends XServerEvent
{

	private final XServer server;
	
	public XServerDisconnectEvent(XServer server) {
		super("Disconnected from server " + server.getName());
		this.server = server;
	}

	public XServer getServer()
	{
		return server;
	}
	
}
