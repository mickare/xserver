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
import de.mickare.xserver.util.InterruptableRunnable;
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
  private InterruptableRunnable reconnectTask = null;

  private final String sql_table_xservers, sql_table_xgroups, sql_table_xserversxgroups;

  private final XServerPlugin plugin;
  private ServerThreadPoolExecutor stpool;
  private SocketFactory sf;
  private MainServer mainserver;

  private final MySQL connection;
  private final String homeServerName;
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
    this.start();
  }

  public boolean isRunning() {
    return this.state == State.RUNNING;
  }

  public void debugInfo(String msg) {
    if (plugin.isDebugging()) {
      this.getLogger().info(msg);
    }
  }

  public boolean isDebugging() {
    return plugin.isDebugging();
  }

  @Override
  public synchronized void start() throws IOException {
    if (this.state == State.RUNNING) {
      return;
    }
    try {
      if (this.homeServer == null) {
        throw new IllegalStateException("Home Server is null!");
      }
      this.debugInfo("Starting XServerManager...");
      this.state = State.RUNNING;
      
      this.mainserver = new MainServer(this.homeServer.getPort(), this).start(this.stpool);

      if (this.reconnectTask != null) {
        this.reconnectTask.interrupt();
      }
      this.reconnectTask = new InterruptableRunnable("Reconnect Task") {
        @Override
        public void run() {
          try {
            while (!this.isInterrupted() && isRunning()) {
              reconnectAll_soft();
              Thread.sleep(plugin.getAutoReconnectTime());
            }
          } catch (InterruptedException e) {
          }
        }
      };
      this.reconnectTask.start(this.getThreadPool());
      this.debugInfo("XServerManager started");
    } catch (Exception e) {
      this.state = State.STOPPED;
      throw e;
    }
  }

  @Override
  public void start_async() {
    try {
      this.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void notifyNotConnected(XServer s, Exception e) {
    AtomicInteger n;
    synchronized (notConnectedServers) {
      n = notConnectedServers.get(s);
      if (n == null) {
        n = new AtomicInteger(0);
        notConnectedServers.put(s, n);
      }
    }
    if (n.incrementAndGet() % 100 == 0) {
      plugin.getLogger().info("Connection to " + s.getName() + " failed! {Cause: " + e.getMessage() + "}");
    }
  }

  @Override
  public void reconnectAll_soft() {
    if (!isRunning()) {
      return;
    }
    stpool.runTask(new Runnable() {
      public void run() {
        debugInfo("Reconnecting softly...");
        Set<XServerObj> temp;
        try (CloseableLock cs = serversLock.readLock().open()) {
          temp = new HashSet<>(servers.values());
        }
        for (final XServerObj s : temp) {
          try {
            s.connectSoft();
            notConnectedServers.remove(s);
          } catch (NotInitializedException | IOException | InterruptedException e) {
            notifyNotConnected(s, e);
          }
        }
      }
    });
  }

  @Override
  public void reconnectAll_forced() {
    if (!isRunning()) {
      return;
    }
    stpool.runTask(new Runnable() {
      public void run() {
        debugInfo("Reconnecting forced...");
        Set<XServerObj> temp;
        try (CloseableLock cs = serversLock.readLock().open()) {
          temp = new HashSet<>(servers.values());
        }
        for (final XServerObj s : temp) {
          try {
            s.connect();
            notConnectedServers.remove(s);
          } catch (NotInitializedException | IOException | InterruptedException e) {
            notifyNotConnected(s, e);
          }
        }
      }
    });
  }

  @Override
  public synchronized void stop() {
    if (this.state == State.STOPPED) {
      return;
    }
    this.debugInfo("Stopping XServerManager...");
    State oldState = this.state;
    this.state = State.STOPPED;


    if (oldState == State.NEW) {
      this.state = State.STOPPED;
      // nothing
    } else if (oldState == State.RUNNING) {
      this.state = State.STOPPED;

      if (this.reconnectTask != null) {
        this.reconnectTask.interrupt();
      }

      if (mainserver != null) {
        try {
          mainserver.stop();
        } catch (IOException e) {
          this.getLogger().warning("An exception occured while stopping xserver server!\n" + e.getMessage());
        }
      }

      try (CloseableLock cs = serversLock.readLock().open()) {
        this.debugInfo("Deprecating servers...");
        for (XServerObj s : servers.values()) {
          s.setDeprecated();
          if (this.isDebugging())
            this.debugInfo(s.getName() + " deprecated");
        }

        this.debugInfo("Disconnecting servers...");
        for (XServerObj s : servers.values()) {
          s.disconnect();
        }
      }

      notConnectedServers.clear();

    }
    this.debugInfo("XServerManager stopped");
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.AbstractXServerManager#reload()
   */
  @Override
  public synchronized void reload() throws IOException {
    this.debugInfo("XServerManager reloading...");
    final State oldState = this.state;
    try (CloseableLock cs = serversLock.writeLock().open()) {
      this.stop();

      // Reestablish connection
      connection.reconnect();;

      // Get all servers
      final Map<Integer, XServerObj> idMap = new HashMap<Integer, XServerObj>();

      this.debugInfo("Loading XServers...");
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
      if (this.isDebugging()) {
        this.debugInfo(this.servers.size() + " XServers loaded");
      }

      // home server

      this.homeServer = getServer(this.homeServerName);
      if (this.homeServer == null) {
        throw new IllegalStateException("The home server \"" + this.homeServerName + "\" wasn't found!");
      }
      this.debugInfo("Home Server found: " + this.homeServer.getName());

      // Groups
      this.debugInfo("Loading Groups...");
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
      if (this.isDebugging()) {
        this.debugInfo(tempgroups.size() + " Groups loaded");
      }

      // Relations to groups
      this.debugInfo("Loading Group-Server relations...");
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

      if (oldState == State.RUNNING) {
        this.debugInfo("Restarting XServerManager...");
        this.start();
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
    try (CloseableLock ch = serversLock.readLock().open()) {
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
