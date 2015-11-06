package de.mickare.xserver3.exception;

import de.mickare.xserver.protocol.Error.ErrorMessage;

public class ProtocolException extends ConnectionException {

  /**
   * 
   */
  private static final long serialVersionUID = 1386498623631144842L;

  public ProtocolException() {
    super();
  }


  public ProtocolException(String msg) {
    super(msg);
  }


  @Override
  public ErrorMessage.Type type() {
    return ErrorMessage.Type.PROTOCOL_ERROR;
  }


  @Override
  public boolean isFatal() {
    return true;
  }


}
