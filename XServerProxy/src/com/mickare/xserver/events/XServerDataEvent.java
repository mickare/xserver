package com.mickare.xserver.events;

public class XServerDataEvent extends XServerEvent {

	private final byte[] data;

	public XServerDataEvent(final byte[] data) {
		super();
		this.data = data;
	}
	
	public XServerDataEvent(final byte[] data, String text) {
		super(text);
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

}
