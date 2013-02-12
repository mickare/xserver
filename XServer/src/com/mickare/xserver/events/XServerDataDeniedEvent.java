package com.mickare.xserver.events;

public class XServerDataDeniedEvent extends XServerDataEvent {

	public XServerDataDeniedEvent(byte[] data) {
		super(data);
	}
	
	public XServerDataDeniedEvent(byte[] data, String text) {
		super(data, text);
	}

}
