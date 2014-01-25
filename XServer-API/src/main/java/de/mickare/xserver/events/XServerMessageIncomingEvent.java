package de.mickare.xserver.events;

import de.mickare.xserver.Message;
import de.mickare.xserver.net.XServer;

public class XServerMessageIncomingEvent extends XServerMessageEvent {

	public XServerMessageIncomingEvent(final XServer server, final Message message) {
		super(server, message);
	}

	public XServerMessageIncomingEvent(final XServer server, final Message message, String text) {
		super(server, message, text);
	}
	
}
