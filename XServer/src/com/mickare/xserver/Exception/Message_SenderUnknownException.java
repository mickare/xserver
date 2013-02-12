package com.mickare.xserver.Exception;

@SuppressWarnings("serial")
public class Message_SenderUnknownException extends Message_Exception {

	public Message_SenderUnknownException() {
	}

	public Message_SenderUnknownException(String reason) {
		super(reason);
	}

	public Message_SenderUnknownException(Throwable cause) {
		super(cause);
	}

	public Message_SenderUnknownException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
