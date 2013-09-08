package com.mickare.xserver;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import com.mickare.xserver.exceptions.InvalidConfigurationException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.net.XServer;
import com.mickare.xserver.util.MySQL;
import com.mickare.xserver.util.MyStringUtils;

public abstract class AbstractXServerManager<T> {

	public abstract long getAutoReconnectTime();
	public abstract XType getHomeType();
	
	private final XServerPlugin<T> plugin;
	private EventHandler<T> eventhandler;
	private ServerThreadPoolExecutor stpool;
	private SocketFactory sf;
	private MainServer<T> mainserver;

	private final MySQL connection;
	private final String homeServerName;
	private Lock homeLock = new ReentrantLock();
	public XServer<T> homeServer;
	private ReentrantReadWriteLock serversLock = new ReentrantReadWriteLock();

	private final HashMap<String, XServer<T>> servers = new HashMap<String, XServer<T>>();

	private final Map<XServer<T>, Integer> notConnectedServers = Collections
			.synchronizedMap(new HashMap<XServer<T>, Integer>());

	private boolean reconnectClockRunning = false;

	protected AbstractXServerManager(String servername, XServerPlugin<T> plugin, MySQL connection, EventHandler<T> eventhandler)
			throws InvalidConfigurationException, IOException {
		this.plugin = plugin;
		stpool = new ServerThreadPoolExecutor();
		sf = SocketFactory.getDefault();
		this.connection = connection;
		this.homeServerName = servername;
		this.reload();
		if (homeServer == null) {
			throw new InvalidConfigurationException("Server information for \""
					+ servername + "\" was not found!");
		}

		this.eventhandler = eventhandler;
	}

	private synchronized boolean isReconnectClockRunning() {
		return reconnectClockRunning;
	}

	public void start() throws IOException {
		serversLock.readLock().lock();
		try {
			reconnectAll_soft();
			if (!isReconnectClockRunning()) {
				reconnectClockRunning = true;
				stpool.runTask(new Runnable() {
					@Override
					public void run() {
						try {
							while (isReconnectClockRunning()) {
								reconnectAll_soft();
								Thread.sleep(getAutoReconnectTime());
							}
						} catch (InterruptedException e) {
						}
					}
				});
			}
		} finally {
			serversLock.readLock().unlock();
		}
	}

	public void start_async() {
		stpool.runTask(new Runnable() {
			public void run() {
				try {
					start();
				} catch (Exception e) {
					plugin.getLogger().severe("XServer not started correctly!"
							+ e.getMessage() + "\n"
							+ MyStringUtils.stackTraceToString(e));
					plugin.shutdownServer();
				}
			}
		});
	}

	private synchronized void notifyNotConnected(XServer<T> s, Exception e) {
		synchronized (notConnectedServers) {
			int n = 0;
			if (notConnectedServers.containsKey(s)) {
				n = notConnectedServers.get(s).intValue();
			}

			if (n++ % 100 == 0) {
				plugin.getLogger().info("Connection to " + s.getName() + " failed! {Cause: "
						+ e.getMessage() + "}");
			}

			// logger.warning("Connection to " + s.getName() + " failed!\n" +
			// e.getMessage() + "\n" + MyStringUtils.stackTraceToString(e));
			notConnectedServers.put(s, new Integer(n));
		}
	}

	public void reconnectAll_soft() {
		serversLock.readLock().lock();
		try {
			for (final XServer<T> s : servers.values()) {
				stpool.runTask(new Runnable() {
					public void run() {
						if (!s.isConnected()) {
							try {
								s.connect();
								synchronized (notConnectedServers) {
									notConnectedServers.remove(s);
								}
							} catch (IOException | InterruptedException
									| NotInitializedException e) {
								notifyNotConnected(s, e);
							}
						}
					}
				});
			}
		} finally {
			serversLock.readLock().unlock();
		}
	}

	public void reconnectAll_forced() {
		serversLock.readLock().lock();
		try {
			for (final XServer<T> s : servers.values()) {
				stpool.runTask(new Runnable() {
					public void run() {
						try {
							s.connect();
							synchronized (notConnectedServers) {
								notConnectedServers.remove(s);
							}
						} catch (IOException | InterruptedException
								| NotInitializedException e) {
							notifyNotConnected(s, e);
						}
					}
				});
			}
		} finally {
			serversLock.readLock().unlock();
		}
	}

	public void stop() throws IOException {
		serversLock.readLock().lock();
		try {
			mainserver.close();
			stpool.shutDown();
			reconnectClockRunning = false;

			for (XServer<T> s : this.getServers()) {
				s.disconnect();
			}

		} finally {
			serversLock.readLock().unlock();
		}
	}

	/**
	 * Reload configuration
	 * 
	 * @throws IOException
	 * 
	 * @throws InvalidConfigurationException
	 */
	public void reload() throws IOException {
		homeLock.lock();
		serversLock.writeLock().lock();
		try {

			if (mainserver != null) {
				try {
					mainserver.close();
				} catch (IOException e) {

				}
			}
			for (XServer<T> s : servers.values()) {
				s.disconnect();
			}
			servers.clear();

			ResultSet rs = null;

			synchronized (connection) {
				rs = connection.query("SELECT * FROM xserver");
			}

			if (rs != null) {
				try {
					while (rs.next()) {
						String servername = rs.getString("NAME");
						String[] hostip = rs.getString("ADRESS").split(":");

						if (hostip.length < 2) {
							plugin.getLogger().warning("XServer \"" + servername
									+ "\" has an invalid address! (host:port)");
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
							plugin.getLogger().warning("XServer \"" + servername
									+ "\" has an invalid address! (host:port)");
							continue;
						}
						String pw = rs.getString("PW");
						servers.put(servername, new XServer<T>(servername, host,
								ip, pw, this));
					}
				} catch (SQLException e) {
					plugin.getLogger().severe(e.getMessage());
				}
			} else {
				plugin.getLogger().severe("Couldn't load XServer List form Database!");
			}
			homeServer = getServer(this.homeServerName);

			if(homeServer == null) {
				throw new IllegalArgumentException("The home server \"" + this.homeServerName + "\" wasn't found!");				
			}
			
			mainserver = new MainServer<T>(ServerSocketFactory.getDefault()
					.createServerSocket(homeServer.getPort(), 100), this);
			mainserver.start();

			start_async();

		} finally {
			homeLock.unlock();
			serversLock.writeLock().unlock();
		}
	}

	public XServer<T> getHomeServer() {
		homeLock.lock();
		try {
			return homeServer;
		} finally {
			homeLock.unlock();
		}
	}

	public XServer<T> getServer(String servername) {
		serversLock.readLock();
		try {
			return servers.get(servername);
		} finally {
			serversLock.readLock();
		}
	}

	public XServerPlugin<T> getPlugin() {
		return plugin;
	}
	
	public Logger getLogger() {
		return plugin.getLogger();
	}

	public ServerThreadPoolExecutor getThreadPool() {
		return stpool;
	}

	public SocketFactory getSocketFactory() {
		return sf;
	}

	public XServer<T> getXServer(String name) {
		serversLock.readLock().lock();
		try {
			return servers.get(name);
		} finally {
			serversLock.readLock().unlock();
		}
	}

	/**
	 * Get the list of all available servers
	 * 
	 * @return servers
	 */
	public Set<XServer<T>> getServers() {
		serversLock.readLock().lock();
		try {
			return new HashSet<XServer<T>>(servers.values());
		} finally {
			serversLock.readLock().unlock();
		}
	}

	/**
	 * Get a string list of all servernames
	 * 
	 * @return
	 */
	public String[] getServernames() {
		serversLock.readLock().lock();
		try {
			return servers.values().toArray(new String[servers.size()]);
		} finally {
			serversLock.readLock().unlock();
		}
	}

	/**
	 * Get the XServer Object with the servername name
	 * 
	 * @param name
	 *            servername
	 * @return XServer with that name
	 */
	public XServer<T> getServerIgnoreCase(String name) {
		serversLock.readLock().lock();
		try {
			for (XServer<T> s : servers.values()) {
				if (s.getName().equalsIgnoreCase(name)) {
					return s;
				}
			}
			return null;
		} finally {
			serversLock.readLock().unlock();
		}
	}

	/**
	 * Get the XServer Object via host and port
	 * 
	 * @param host
	 * @param port
	 * @return XServer
	 */
	public XServer<T> getServer(String host, int port) {
		serversLock.readLock().lock();
		try {
			for (XServer<T> s : servers.values()) {
				if (s.getHost().equalsIgnoreCase(host) && s.getPort() == port) {
					return s;
				}
			}
			return null;
		} finally {
			serversLock.readLock().unlock();
		}
	}

	public EventHandler<T> getEventHandler() {
		return eventhandler;
	}

	public String getHomeServerName() {
		return homeServerName;
	}
	
	public Message createMessage(String subChannel, byte[] content) throws NotInitializedException {
		return new Message(getHomeServer(), subChannel, content);
	}
	
	public Message readMessage(XServer<T> sender, byte[] data) throws IOException {
		return new Message(sender, data);
	}

}
