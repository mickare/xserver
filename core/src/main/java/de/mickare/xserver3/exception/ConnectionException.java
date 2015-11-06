package de.mickare.xserver3.exception;

import de.mickare.xserver.protocol.Error.ErrorMessage;

@SuppressWarnings("serial")
public abstract class ConnectionException extends Exception {

  public ConnectionException() {
    // TODO Auto-generated constructor stub
  }

  public ConnectionException(String message) {
    super(message);
    // TODO Auto-generated constructor stub
  }

  public ConnectionException(Throwable cause) {
    super(cause);
    // TODO Auto-generated constructor stub
  }

  public ConnectionException(String message, Throwable cause) {
    super(message, cause);
    // TODO Auto-generated constructor stub
  }

  public ConnectionException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    // TODO Auto-generated constructor stub
  }
  
  public abstract ErrorMessage.Type type();
  
  public abstract boolean isFatal();

}
