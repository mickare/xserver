package de.mickare.xserver3.exception;

import com.google.common.base.Preconditions;

import de.mickare.xserver.protocol.ErrorProto.ErrorMessage;
import lombok.Getter;

@SuppressWarnings("serial")
public class ConnectionException extends Exception {

  @Getter
  private final ErrorMessage.Type type;
  @Getter
  private final boolean fatal;

  @Getter
  private boolean doLog = false;

  public ConnectionException(ErrorMessage.Type type, boolean fatal) {
    Preconditions.checkNotNull(type);
    this.type = type;
    this.fatal = fatal;
  }

  public ConnectionException(ErrorMessage.Type type, boolean fatal, String message) {
    super(message);
    Preconditions.checkNotNull(type);
    this.type = type;
    this.fatal = fatal;
  }

  public ConnectionException(ErrorMessage.Type type, boolean fatal, Throwable cause) {
    super(cause);
    Preconditions.checkNotNull(type);
    this.type = type;
    this.fatal = fatal;
  }

  public ConnectionException(ErrorMessage.Type type, boolean fatal, String message,
      Throwable cause) {
    super(message, cause);
    Preconditions.checkNotNull(type);
    this.type = type;
    this.fatal = fatal;
  }

  public ConnectionException(ErrorMessage.Type type, boolean fatal, String message, Throwable cause,
      boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    Preconditions.checkNotNull(type);
    this.type = type;
    this.fatal = fatal;
  }


  public ConnectionException doLog() {
    this.doLog = true;
    return this;
  }

}
