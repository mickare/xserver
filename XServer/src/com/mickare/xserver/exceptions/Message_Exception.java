package com.mickare.xserver.exceptions;

@SuppressWarnings("serial")
public class Message_Exception extends Exception {

	public Message_Exception() {
	}

	public Message_Exception(String reason) {
		super(reason);
	}

	public Message_Exception(Throwable cause) {
		super(cause);
	}

	public Message_Exception(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	
}
