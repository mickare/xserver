package de.mickare.xserver;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.SocketFactory;

import de.mickare.xserver.exceptions.InvalidConfigurationException;
import de.mickare.xserver.net.XServer;

public interface AbstractXServerManager {

  public abstract void start() throws IOException;

  public abstract void start_async();

  public abstract void reconnectAll_soft();

  public abstract void reconnectAll_forced();

  public abstract void stop() throws IOException;

  /**
   * Reload configuration
   * 
   * @throws IOException
   * 
   * @throws InvalidConfigurationException
   */
  public abstract void reload() throws IOException;

  public abstract XServer getHomeServer();

  public abstract XServer getServer(String servername);

  public abstract XServerPlugin getPlugin();

  public abstract Logger getLogger();

  public abstract ServerThreadPoolExecutor getThreadPool();

  public abstract SocketFactory getSocketFactory();

  public abstract XServer getXServer(String name);

  /**
   * Get the list of all available servers
   * 
   * @return servers
   */
  public abstract Set<XServer> getServers();

  /**
   * Get a string list of all servernames
   * 
   * @return
   */
  public abstract String[] getServernames();

  /**
   * Get the XServer Object with the servername name
   * 
   * @param name servername
   * @return XServer with that name
   */
  public abstract XServer getServerIgnoreCase(String name);

  /**
   * Get the XServer Object via host and port
   * 
   * @param host
   * @param port
   * @return XServer
   */
  public abstract XServer getServer(String host, int port);

  public abstract String getHomeServerName();

  public abstract Message createMessage(String subChannel, byte[] content);

  public abstract Message readMessage(XServer sender, byte[] data) throws IOException;

  public abstract EventHandler<?> getEventHandler();

  public abstract void registerOwnListeners();

  Set<XServer> getServers(XGroup group);

  Set<? extends XGroup> getGroups();

  XGroup getGroupByName(String name);

}
