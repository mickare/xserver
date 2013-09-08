package com.mickare.xserver.events;

public class XServerEvent {

	private final String text;
	
	public XServerEvent() {
		text = "";
	}
	
	public XServerEvent(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void postCall() {
		
	}
	
}
