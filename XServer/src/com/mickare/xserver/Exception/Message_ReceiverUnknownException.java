package com.mickare.xserver.Exception;

@SuppressWarnings("serial")
public class Message_ReceiverUnknownException extends Message_Exception {

	public Message_ReceiverUnknownException() {
	}

	public Message_ReceiverUnknownException(String reason) {
		super(reason);
	}

	public Message_ReceiverUnknownException(Throwable cause) {
		super(cause);
	}

	public Message_ReceiverUnknownException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
