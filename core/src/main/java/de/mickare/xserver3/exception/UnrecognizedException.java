package de.mickare.xserver3.exception;

import de.mickare.xserver.protocol.Error.ErrorMessage;

public class UnrecognizedException extends ConnectionException {

  /**
   * 
   */
  private static final long serialVersionUID = -7035665479020126147L;


  @Override
  public ErrorMessage.Type type() {
    return ErrorMessage.Type.UNRECOGNIZED;
  }


  @Override
  public boolean isFatal() {
    return true;
  }

}
