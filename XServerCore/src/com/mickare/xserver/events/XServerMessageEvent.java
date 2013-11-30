package com.mickare.xserver.events;

import com.mickare.xserver.Message;
import com.mickare.xserver.net.XServer;

public class XServerMessageEvent extends XServerEvent {

	private final XServer server;
	private final Message message;

	public XServerMessageEvent(final XServer server, final Message message) {
		super();
		this.server = server;
		this.message = message;
	}
	
	public XServerMessageEvent(final XServer server, final Message message, String text) {
		super(text);
		this.server = server;
		this.message = message;
	}

	public Message getMessage() {
		return message;
	}

	public XServer getServer() {
		return server;
	}

	@Override
	public void postCall() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getChannel() {
		return message.getSubChannel();
	}

	
}
