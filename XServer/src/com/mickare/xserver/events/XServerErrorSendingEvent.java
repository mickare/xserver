package com.mickare.xserver.events;

import com.mickare.xserver.Message;

public class XServerErrorSendingEvent extends XServerEvent {

	private final Message m;
	
	public XServerErrorSendingEvent(Message m) {
		this.m = m;
	}
	
	public XServerErrorSendingEvent(Message m, String text) {
		super(text);
		this.m = m;
	}

	public Message getMessage() {
		return m;
	}

	@Override
	public void postCall() {
		// TODO Auto-generated method stub
		
	}
	
}
