package de.mickare.xserver.events;

import de.mickare.xserver.Message;
import de.mickare.xserver.net.XServer;

public class XServerMessageOutgoingEvent extends XServerMessageEvent {

  public XServerMessageOutgoingEvent(final XServer server, Message message) {
    super(server, message);
  }

  public XServerMessageOutgoingEvent(final XServer server, Message message, String text) {
    super(server, message, text);
  }


}
