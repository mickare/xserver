package de.mickare.xserver;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.net.SocketFactory;

import de.mickare.xserver.exceptions.InvalidConfigurationException;
import de.mickare.xserver.exceptions.NotInitializedException;
import de.mickare.xserver.net.XServer;
import de.mickare.xserver.net.XServerObj;
import de.mickare.xserver.util.Consumer;
import de.mickare.xserver.util.MySQL;
import de.mickare.xserver.util.TableInstall;
import de.mickare.xserver.util.concurrent.CloseableLock;
import de.mickare.xserver.util.concurrent.CloseableReadWriteLock;
import de.mickare.xserver.util.concurrent.CloseableReentrantReadWriteLock;

public abstract class AbstractXServerManagerObj implements AbstractXServerManager {

  public static enum State {
    NEW, RUNNING, STOPPED;
  }
  
  private volatile State state = State.NEW;
  
  private boolean debug = false;

  private final String sql_table_xservers, sql_table_xgroups, sql_table_xserversxgroups;

  private final XServerPlugin plugin;
  private ServerThreadPoolExecutor stpool;
  private SocketFactory sf;
  private MainServer mainserver;

  private final MySQL connection;
  private final String homeServerName;
  private CloseableReadWriteLock homeLock = new CloseableReentrantReadWriteLock(true);
  private XServerObj homeServer;
  private CloseableReadWriteLock serversLock = new CloseableReentrantReadWriteLock(true);

  private final Map<String, XServerObj> servers = new ConcurrentHashMap<String, XServerObj>();
  private final Map<String, XGroup> groups = new ConcurrentHashMap<String, XGroup>();

  private final Map<XServer, AtomicInteger> notConnectedServers = new ConcurrentHashMap<XServer, AtomicInteger>();

  protected AbstractXServerManagerObj(String servername, XServerPlugin plugin, MySQL connection, String sql_table_xservers,
      String sql_table_xgroups, String sql_table_xserversxgroups, ServerThreadPoolExecutor stpool)
          throws InvalidConfigurationException, IOException {
    this.plugin = plugin;
    this.stpool = stpool;
    // this.stpool = new ServerThreadPoolExecutorObj();
    this.sf = SocketFactory.getDefault();
    this.connection = connection;
    this.sql_table_xservers = sql_table_xservers;
    this.sql_table_xgroups = sql_table_xgroups;
    this.sql_table_xserversxgroups = sql_table_xserversxgroups;
    this.homeServerName = servername;

    // Installation

    TableInstall ti = new TableInstall(plugin, connection, sql_table_xservers, sql_table_xgroups, sql_table_xserversxgroups);
    ti.install();

    // Loading

    this.reload();
    if (homeServer == null) {
      throw new InvalidConfigurationException("Server information for \"" + servername + "\" was not found!");
    }

  }

  public boolean isRunning() {
    return this.state == State.RUNNING;
  }

  public void debugInfo(String msg) {
    if (debug) {
      this.getLogger().info(msg);
    }
  }

  public boolean isDebugging() {
    return this.debug;
  }

  public void setDebugging(boolean debug) {
    this.debug = debug;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#start()
   */
  @Override
  public synchronized void start() {
    if (this.state != State.NEW) {
      throw new IllegalStateException("Not NEW!");
    }
    this.state = State.RUNNING;
    stpool.runTask(new Runnable() {
      @Override
      public void run() {
        try {
          while (isRunning()) {
            reconnectAll_soft();
            Thread.sleep(plugin.getAutoReconnectTime());
          }
        } catch (InterruptedException e) {
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#start_async()
   */
  @Override
  public void start_async() {
    this.start();
  }

  private void notifyNotConnected(XServer s, Exception e) {
      AtomicInteger n;
      synchronized(notConnectedServers) {
          n = notConnectedServers.get(s);
          if(n == null) {
              n = new AtomicInteger(0);
              notConnectedServers.put(s, n);
          }
      }
      if (n.incrementAndGet() % 100 == 0) {
        plugin.getLogger().info("Connection to " + s.getName() + " failed! {Cause: " + e.getMessage() + "}");
      }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#reconnectAll_soft()
   */
  @Override
  public void reconnectAll_soft() {
    if (!isRunning()) {
      return;
    }
    try (CloseableLock cs = serversLock.readLock().open()) {
      for (final XServerObj s : servers.values()) {
        stpool.runTask(new Runnable() {
          public void run() {
              try {
                s.connectSoft();
                notConnectedServers.remove(s);
              } catch (IOException | InterruptedException | NotInitializedException e) {
                notifyNotConnected(s, e);
              }
          }
        });
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#reconnectAll_forced()
   */
  @Override
  public void reconnectAll_forced() {
    if (!isRunning()) {
      return;
    }
    try (CloseableLock cs = serversLock.readLock().open()) {
      for (final XServerObj s : servers.values()) {
        stpool.runTask(new Runnable() {
          public void run() {
            try {
              s.connect();
              notConnectedServers.remove(s);
            } catch (IOException | InterruptedException | NotInitializedException e) {
              notifyNotConnected(s, e);
            }
          }
        });
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#stop()
   */
  @Override
  public synchronized void stop() {
    if(this.state == State.STOPPED) {
      return;
    }
    this.debugInfo("Stopping XServerManager...");
    this.state = State.STOPPED;

    try {
      mainserver.stop();
    } catch (IOException e) {
      this.getLogger().warning("An exception occured while stopping xserver server!\n" + e.getMessage());
    }
    // try ( CloseableLock cs = serversLock.writeLock().open() ) {
    // executorService.shutDown();

    try (CloseableLock cs = serversLock.readLock().open()) {
        this.debugInfo("Deprecating servers...");
        for (XServerObj s : servers.values()) {
          s.setDeprecated();
          if(debug)
            this.debugInfo(s.getName() + " deprecated");
        }

        this.debugInfo("Disconnecting servers...");
        for (XServerObj s : servers.values()) {
          s.disconnect();
        }
    }
    this.debugInfo("XServerManager stopped");
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#reload()
   */
  @Override
  public void reload() throws IOException {
    if (this.state == State.STOPPED) {
      return;
    }
    this.debugInfo("XServerManager reloading...");
    try (CloseableLock ch = homeLock.writeLock().open()) {
      try (CloseableLock cs = serversLock.writeLock().open()) {
        notConnectedServers.clear();

        if (this.state == State.STOPPED) {
          return;
        }

        if (mainserver != null) {
          try {
            mainserver.stop();
          } catch (IOException e) {
            // this.plugin.getLogger().info( e.getClass().getName() + ": " + e.getMessage() + "\n" +
            // MyStringUtils.stackTraceToString( e ) );
          }
        }
        for (XServerObj s : servers.values()) {
          s.setDeprecated();
          s.disconnect();
        }
        servers.clear();

        // Reestablish connection
        connection.reconnect();;

        // Get all servers

        final Map<Integer, XServerObj> idMap = new HashMap<Integer, XServerObj>();

        connection.query(new Consumer<ResultSet>() {
          @Override
          public void accept(ResultSet rs) {
            try {
              while (rs.next()) {
                int id = rs.getInt("ID");
                String servername = rs.getString("NAME");
                String[] hostip = rs.getString("ADRESS").split(":");
                String pw = rs.getString("PW");

                if (hostip.length < 2) {
                  plugin.getLogger().warning("XServer \"" + servername + "\" has an invalid address! (host:port)");
                  continue;
                }

                String host = hostip[0];
                if (hostip.length > 2) {
                  for (int i = 1; i < hostip.length - 1; i++) {
                    host += ":" + hostip[i];
                  }
                }
                int ip = 20000;
                try {
                  ip = Integer.valueOf(hostip[hostip.length - 1]);
                } catch (NumberFormatException nfe) {
                  plugin.getLogger().warning("XServer \"" + servername + "\" has an invalid address! (host:port)");
                  continue;
                }
                XServerObj result = new XServerObj(servername, host, ip, pw, AbstractXServerManagerObj.this);
                servers.put(servername, result);
                idMap.put(id, result);
              }
            } catch (Exception e) {
              plugin.getLogger().severe(e.getMessage());
              throw new RuntimeException("Couldn't load XServer List form Database!", e);
            }
          }
        }, "SELECT * FROM " + sql_table_xservers);

        homeServer = getServer(this.homeServerName);

        if (homeServer == null) {
          throw new IllegalArgumentException("The home server \"" + this.homeServerName + "\" wasn't found!");
        }

        // Groups

        this.groups.clear();

        final Map<Integer, XGroup> tempgroups = new HashMap<Integer, XGroup>();

        connection.query(new Consumer<ResultSet>() {
          @Override
          public void accept(ResultSet rs) {
            try {
              while (rs.next()) {
                int groupId = rs.getInt("groupID");
                String name = rs.getString("name");
                XGroupObj o = new XGroupObj(groupId, name);
                AbstractXServerManagerObj.this.groups.put(name, o);
                tempgroups.put(groupId, o);
              }

            } catch (Exception e) {
              plugin.getLogger().severe(e.getMessage());
              throw new RuntimeException("Couldn't load XServer Groups form Database!", e);
            }
          }
        }, "SELECT * FROM " + sql_table_xgroups);

        // Relations to groups

        connection.query(new Consumer<ResultSet>() {
          @Override
          public void accept(ResultSet rs) {
            try {
              while (rs.next()) {
                int serverId = rs.getInt("serverID");
                int groupId = rs.getInt("groupId");

                XServerObj x = idMap.get(serverId);
                XGroup g = tempgroups.get(groupId);
                if (x != null && g != null) {
                  x.addGroup(g);
                }

              }
            } catch (Exception e) {
              plugin.getLogger().severe(e.getMessage());
              throw new RuntimeException("Couldn't load XServer Group-Relations form Database!", e);
            }
          }
        }, "SELECT * FROM " + sql_table_xserversxgroups);

        // End of queries
        connection.disconnect();


        if (this.state == State.STOPPED) {
          return;
        }

        // Start MainServer
        mainserver = new MainServer(homeServer.getPort(), this);
        mainserver.start(this.getThreadPool());

        // Connect the xservers

        reconnectAll_soft();

      }
    }
    this.debugInfo("XServerManager reloaded");
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getHomeServer()
   */
  @Override
  public XServerObj getHomeServer() {
    try (CloseableLock ch = homeLock.readLock().open()) {
      return homeServer;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getServer(java.lang.String)
   */
  @Override
  public XServerObj getServer(String servername) {
    // try (CloseableLock cs = serversLock.readLock().open()) {
    return servers.get(servername);
    // }
  }

  @Override
  public Set<XServer> getServers(XGroup group) {
    try (CloseableLock cs = serversLock.readLock().open()) {
      Set<XServer> result = new HashSet<XServer>();
      for (XServerObj x : this.servers.values()) {
        if (x.hasGroup(group)) {
          result.add(x);
        }
      }
      return result;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getPlugin()
   */
  @Override
  public XServerPlugin getPlugin() {
    return plugin;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getLogger()
   */
  @Override
  public Logger getLogger() {
    return plugin.getLogger();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getExecutorService()
   */
  @Override
  public ServerThreadPoolExecutor getThreadPool() {
    return stpool;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getSocketFactory()
   */
  @Override
  public SocketFactory getSocketFactory() {
    return sf;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getXServer(java.lang.String)
   */
  @Override
  public XServerObj getXServer(String name) {
    // try (CloseableLock cs = serversLock.readLock().open()) {
    return servers.get(name);
    // }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getServers()
   */
  @Override
  public Set<XServer> getServers() {
    try (CloseableLock cs = serversLock.readLock().open()) {
      return new HashSet<XServer>(servers.values());
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getServernames()
   */
  @Override
  public String[] getServernames() {
    try (CloseableLock cs = serversLock.readLock().open()) {
      return servers.values().toArray(new String[servers.size()]);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getServerIgnoreCase(java.lang.String)
   */
  @Override
  public XServerObj getServerIgnoreCase(String name) {
    try (CloseableLock cs = serversLock.readLock().open()) {
      for (XServerObj s : servers.values()) {
        if (s.getName().equalsIgnoreCase(name)) {
          return s;
        }
      }
      return null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getServer(java.lang.String, int)
   */
  @Override
  public XServerObj getServer(String host, int port) {
    try (CloseableLock cs = serversLock.readLock().open()) {
      for (XServerObj s : servers.values()) {
        if (s.getHost().equalsIgnoreCase(host) && s.getPort() == port) {
          return s;
        }
      }
      return null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getHomeServerName()
   */
  @Override
  public String getHomeServerName() {
    return homeServerName;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#createMessage(java.lang.String, byte[])
   */
  @Override
  public Message createMessage(String subChannel, byte[] content) {
    return new MessageObj(getHomeServer(), subChannel, content);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#readMessage(de.mickare.xserver.net.XServer,
   * byte[])
   */
  @Override
  public Message readMessage(XServer sender, byte[] data) throws IOException {
    return new MessageObj(sender, data);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#getEventHandler()
   */
  @Override
  public abstract EventHandler<?> getEventHandler();

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#registerOwnListeners()
   */
  @Override
  public abstract void registerOwnListeners();

  @Override
  public Set<XGroup> getGroups() {
    try (CloseableLock cs = this.serversLock.readLock().open()) {
      return Collections.unmodifiableSet(new HashSet<XGroup>(this.groups.values()));
    }
  }

  @Override
  public XGroup getGroupByName(String name) {
    return this.groups.get(name);
  }

}
