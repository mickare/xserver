package de.mickare.xserver.exceptions;

@SuppressWarnings("serial")
public class NotInitializedException extends RuntimeException {

  public NotInitializedException() {}

  public NotInitializedException(String reason) {
    super(reason);
  }

  public NotInitializedException(Throwable cause) {
    super(cause);
  }

  public NotInitializedException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
