package de.mickare.xserver3.exception;

import de.mickare.xserver.protocol.ErrorProto.ErrorMessage.Type;

public class ProtocolException extends ConnectionException {

  /**
   * 
   */
  private static final long serialVersionUID = -5871338075781356108L;

  public ProtocolException() {
    super(Type.PROTOCOL_ERROR, true);
    // TODO Auto-generated constructor stub
  }

  public ProtocolException(String message) {
    super(Type.PROTOCOL_ERROR, true, message);
    // TODO Auto-generated constructor stub
  }

  public ProtocolException(Throwable cause) {
    super(Type.PROTOCOL_ERROR, true, cause);
    // TODO Auto-generated constructor stub
  }

  public ProtocolException(String message, Throwable cause) {
    super(Type.PROTOCOL_ERROR, true, message, cause);
    // TODO Auto-generated constructor stub
  }

  public ProtocolException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(Type.PROTOCOL_ERROR, true, message, cause, enableSuppression, writableStackTrace);
    // TODO Auto-generated constructor stub
  }

}
