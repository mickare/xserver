package com.mickare.xserver.exceptions;

@SuppressWarnings("serial")
public class InvalidConfigurationException extends Exception {

	public InvalidConfigurationException() {
	}

	public InvalidConfigurationException(String reason) {
		super(reason);
	}

	public InvalidConfigurationException(Throwable cause) {
		super(cause);
	}

	public InvalidConfigurationException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
