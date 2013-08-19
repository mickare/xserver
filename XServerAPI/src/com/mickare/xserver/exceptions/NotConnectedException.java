package com.mickare.xserver.exceptions;

@SuppressWarnings("serial")
public class NotConnectedException extends Exception {

	public NotConnectedException() {
	}

	public NotConnectedException(String reason) {
		super(reason);
	}

	public NotConnectedException(Throwable cause) {
		super(cause);
	}

	public NotConnectedException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
