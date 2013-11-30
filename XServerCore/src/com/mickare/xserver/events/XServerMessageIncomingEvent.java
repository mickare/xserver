package com.mickare.xserver.events;

import com.mickare.xserver.Message;
import com.mickare.xserver.net.XServer;

public class XServerMessageIncomingEvent extends XServerMessageEvent {

	public XServerMessageIncomingEvent(final XServer server, final Message message) {
		super(server, message);
	}

	public XServerMessageIncomingEvent(final XServer server, final Message message, String text) {
		super(server, message, text);
	}
	
}
