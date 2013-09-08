package com.mickare.xserver.events;

import com.mickare.xserver.Message;
import com.mickare.xserver.net.XServer;

public class XServerMessageOutgoingEvent extends XServerMessageEvent {

	public XServerMessageOutgoingEvent(final XServer<? extends Object> server, Message message) {
		super(server, message);
	}
	
	public XServerMessageOutgoingEvent(final XServer<? extends Object> server, Message message, String text) {
		super(server, message, text);
	}

}
