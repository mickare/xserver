package de.mickare.xserver.net;

import java.util.Collection;

import de.mickare.xserver.user.ComSender;

public interface Ping {

  public abstract boolean start();

  public abstract void add(XServer server);

  public abstract void addAll(Collection<XServer> servers);

  public abstract void receive(XServer server);

  public abstract boolean isPending();

  public abstract String getFormatedString();

  public abstract ComSender getSender();

  public abstract String getKey();

  public abstract long getStarted();

}
