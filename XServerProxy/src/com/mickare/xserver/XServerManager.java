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

public class XServerManager
{

	// In Milliseconds
	private static final long AUTORECONNECT = 10 * 1000;
	public static final XType HOMETYPE = XType.BungeeCord;

	private static XServerManager instance = null;

	public static XServerManager getInstance() throws NotInitializedException
	{
		if (instance == null)
		{
			throw new NotInitializedException();
		}
		return instance;
	}

	private Logger logger;
	private EventHandler eventhandler;
	private ServerThreadPoolExecutor stpool;
	private SocketFactory sf;
	private MainServer mainserver = null;

	private final MySQL connection;
	private final String homeServerName;

	private Lock homeLock = new ReentrantLock();
	public XServer homeServer;

	private ReentrantReadWriteLock serversLock = new ReentrantReadWriteLock();
	private final HashMap<String, XServer> servers = new HashMap<String, XServer>();

	private final Map<XServer, Integer> notConnectedServers = Collections.synchronizedMap(new HashMap<XServer, Integer>());

	private boolean reconnectClockRunning = false;

	protected XServerManager(String servername, Logger logger, MySQL connection) throws InvalidConfigurationException, IOException
	{
		instance = this;
		this.logger = logger;
		stpool = new ServerThreadPoolExecutor();
		sf = SocketFactory.getDefault();
		this.connection = connection;
		this.homeServerName = servername;
		this.reload();
		if (homeServer == null)
		{
			throw new InvalidConfigurationException("Server information for \"" + servername + "\" was not found!");
		}

		this.eventhandler = new EventHandler(this);

	}

	private synchronized boolean isReconnectClockRunning()
	{
		return reconnectClockRunning;
	}

	public void start() throws IOException
	{
		serversLock.readLock().lock();
		try
		{
			reconnectAll_soft();
			if (!isReconnectClockRunning())
			{
				reconnectClockRunning = true;
				stpool.runTask(new Runnable() {
					@Override
					public void run()
					{
						try
						{
							while (isReconnectClockRunning())
							{
								reconnectAll_soft();
								Thread.sleep(AUTORECONNECT);
							}
						} catch (InterruptedException e)
						{
						}
					}
				});
			}
		} finally
		{
			serversLock.readLock().unlock();
		}
	}

	public void start_async()
	{
		stpool.runTask(new Runnable() {
			public void run()
			{
				try
				{
					start();
				} catch (Exception e)
				{
					logger.severe("XServer not started correctly!");
					logger.severe(e.getMessage());
					// Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
					// "stop");;
				}
			}
		});
	}

	private synchronized void notifyNotConnected(XServer s, Exception e)
	{
		synchronized (notConnectedServers)
		{
			int n = 0;
			if (notConnectedServers.containsKey(s))
			{
				n = notConnectedServers.get(s).intValue();
			}
			if (n++ % 200 == 0)
			{
				logger.info("Connection to " + s.getName() + " failed!\n" + e.getMessage());
			}
			notConnectedServers.put(s, new Integer(n));
		}
	}

	public void reconnectAll_soft()
	{
		serversLock.readLock().lock();
		try
		{
			for (final XServer s : servers.values())
			{
				stpool.runTask(new Runnable() {
					public void run()
					{
						if (!s.isConnected())
						{
							try
							{
								s.connect();
								synchronized (notConnectedServers)
								{
									notConnectedServers.remove(s);
								}
							} catch (IOException | InterruptedException | NotInitializedException e)
							{
								notifyNotConnected(s, e);
							}
						}
					}
				});
			}
		} finally
		{
			serversLock.readLock().unlock();
		}
	}

	public void reconnectAll_forced()
	{
		serversLock.readLock().lock();
		try
		{
			for (final XServer s : servers.values())
			{
				stpool.runTask(new Runnable() {
					public void run()
					{
						try
						{
							s.connect();
							synchronized (notConnectedServers)
							{
								notConnectedServers.remove(s);
							}
						} catch (IOException | InterruptedException | NotInitializedException e)
						{
							notifyNotConnected(s, e);
						}
					}
				});
			}
		} finally
		{
			serversLock.readLock().unlock();
		}
	}

	public void stop() throws IOException
	{
		serversLock.readLock().lock();
		try
		{
			mainserver.close();
			stpool.shutDown();
			reconnectClockRunning = false;

			for (XServer s : this.getServers())
			{
				s.disconnect();
			}

		} finally
		{
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
	public void reload() throws IOException
	{
		homeLock.lock();
		serversLock.writeLock().lock();
		try
		{
			if (mainserver != null)
			{
				try
				{
					mainserver.close();
				} catch (IOException e)
				{

				}
			}
			for (XServer s : servers.values())
			{
				s.disconnect();
			}
			servers.clear();

			ResultSet rs = null;
			synchronized (connection)
			{
				rs = connection.query("SELECT * FROM xserver");
			}

			if (rs != null)
			{
				try
				{
					while (rs.next())
					{
						String servername = rs.getString("NAME");
						String[] hostip = rs.getString("ADRESS").split(":");

						if (hostip.length < 2)
						{
							logger.warning("XServer \"" + servername + "\" has an invalid address! (host:port)");
							continue;
						}

						String host = hostip[0];
						if (hostip.length > 2)
						{
							for (int i = 1; i < hostip.length - 1; i++)
							{
								host += ":" + hostip[i];
							}
						}
						int ip = 20000;
						try
						{
							ip = Integer.valueOf(hostip[hostip.length - 1]);
						} catch (NumberFormatException nfe)
						{
							logger.warning("XServer \"" + servername + "\" has an invalid address! (host:port)");
							continue;
						}
						String pw = rs.getString("PW");
						servers.put(servername, new XServer(servername, host, ip, pw, this));
					}
				} catch (NumberFormatException | SQLException e)
				{
					logger.severe(e.getMessage());
				}
			} else
			{
				logger.severe("Couldn't load XServer List form Database!");
			}

			homeServer = getServer(this.homeServerName);

			mainserver = new MainServer(ServerSocketFactory.getDefault().createServerSocket(homeServer.getPort(), 100));
			mainserver.start();

			start_async();

		} finally
		{
			homeLock.unlock();
			serversLock.writeLock().lock();
		}
	}

	public XServer getHomeServer()
	{
		homeLock.lock();
		try
		{
			return homeServer;
		} finally
		{
			homeLock.unlock();
		}
	}

	public XServer getServer(String servername)
	{
		serversLock.readLock();
		try
		{
			return servers.get(servername);
		} finally
		{
			serversLock.readLock();
		}
	}

	public Logger getLogger()
	{
		return logger;
	}

	public ServerThreadPoolExecutor getThreadPool()
	{
		return stpool;
	}

	public SocketFactory getSocketFactory()
	{
		return sf;
	}

	public XServer getXServer(String name)
	{
		serversLock.readLock().lock();
		try
		{
			return servers.get(name);
		} finally
		{
			serversLock.readLock().unlock();
		}
	}

	/**
	 * Get the list of all available servers
	 * 
	 * @return servers
	 */
	public synchronized Set<XServer> getServers()
	{
		serversLock.readLock().lock();
		try
		{
			return new HashSet<XServer>(servers.values());
		} finally
		{
			serversLock.readLock().unlock();
		}
	}

	/**
	 * Get a string list of all servernames
	 * 
	 * @return
	 */
	public synchronized String[] getServernames()
	{
		serversLock.readLock().lock();
		try
		{
			return servers.values().toArray(new String[servers.size()]);
		} finally
		{
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
	public synchronized XServer getServerIgnoreCase(String name)
	{
		serversLock.readLock().lock();
		try
		{
			for (XServer s : servers.values())
			{
				if (s.getName().equalsIgnoreCase(name))
				{
					return s;
				}
			}
			return null;
		} finally
		{
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
	public synchronized XServer getServer(String host, int port)
	{
		serversLock.readLock().lock();
		try
		{
			for (XServer s : servers.values())
			{
				if (s.getHost().equalsIgnoreCase(host) && s.getPort() == port)
				{
					return s;
				}
			}
			return null;
		} finally
		{
			serversLock.readLock().unlock();
		}
	}

	public EventHandler getEventHandler()
	{
		return eventhandler;
	}

}
