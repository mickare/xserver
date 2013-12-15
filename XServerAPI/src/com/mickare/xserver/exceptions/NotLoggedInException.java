package com.mickare.xserver.exceptions;

@SuppressWarnings("serial")
public class NotLoggedInException extends Exception {

	public NotLoggedInException() {
	}

	public NotLoggedInException(String reason) {
		super(reason);
	}

	public NotLoggedInException(Throwable cause) {
		super(cause);
	}

	public NotLoggedInException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
