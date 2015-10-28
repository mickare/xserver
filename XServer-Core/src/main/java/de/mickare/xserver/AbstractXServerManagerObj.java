package de.mickare.xserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import de.mickare.xserver.exceptions.InvalidConfigurationException;
import de.mickare.xserver.exceptions.NotInitializedException;
import de.mickare.xserver.net.XServer;
import de.mickare.xserver.net.XServerObj;
import de.mickare.xserver.util.Consumer;
import de.mickare.xserver.util.MySQL;
import de.mickare.xserver.util.MyStringUtils;
import de.mickare.xserver.util.TableInstall;
import de.mickare.xserver.util.concurrent.CloseableLock;
import de.mickare.xserver.util.concurrent.CloseableReadWriteLock;
import de.mickare.xserver.util.concurrent.CloseableReentrantReadWriteLock;

public abstract class AbstractXServerManagerObj implements AbstractXServerManager {
	
	public final static int SOCKET_TIMEOUT = 3000;
	
	private final String sql_table_xservers, sql_table_xgroups, sql_table_xserversxgroups;
	
	private final XServerPlugin plugin;
	private ServerThreadPoolExecutor stpool;
	private SocketFactory sf;
	private MainServer mainserver;
	
	private final MySQL connection;
	private final String homeServerName;
	private CloseableReadWriteLock homeLock = new CloseableReentrantReadWriteLock();
	private XServerObj homeServer;
	private CloseableReadWriteLock serversLock = new CloseableReentrantReadWriteLock();
	
	private final Map<String, XServerObj> servers = new HashMap<String, XServerObj>();
	private final Map<String, XGroup> groups = new HashMap<String, XGroup>();
	
	private final Map<XServer, Integer> notConnectedServers = Collections
			.synchronizedMap( new HashMap<XServer, Integer>() );
	
	private boolean reconnectClockRunning = false;
	
	protected AbstractXServerManagerObj( String servername, XServerPlugin plugin, MySQL connection,
			String sql_table_xservers, String sql_table_xgroups, String sql_table_xserversxgroups,
			ServerThreadPoolExecutor stpool ) throws InvalidConfigurationException, IOException {
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
		
		TableInstall ti = new TableInstall( plugin, connection, sql_table_xservers, sql_table_xgroups,
				sql_table_xserversxgroups );
		ti.install();
		
		// Loading
		
		this.reload();
		if ( homeServer == null ) {
			throw new InvalidConfigurationException( "Server information for \"" + servername + "\" was not found!" );
		}
		
	}
	
	private synchronized boolean isReconnectClockRunning() {
		return reconnectClockRunning;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#start()
	 */
	@Override
	public void start() throws IOException {
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			reconnectAll_soft();
			if ( !isReconnectClockRunning() ) {
				reconnectClockRunning = true;
				stpool.runTask( new Runnable() {
					@Override
					public void run() {
						try {
							while ( isReconnectClockRunning() ) {
								reconnectAll_soft();
								Thread.sleep( plugin.getAutoReconnectTime() );
							}
						} catch ( InterruptedException e ) {
						}
					}
				} );
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#start_async()
	 */
	@Override
	public void start_async() {
		stpool.runTask( new Runnable() {
			public void run() {
				try {
					start();
				} catch ( Exception e ) {
					plugin.getLogger().severe( "XServer not started correctly!" + e.getMessage() + "\n"
							+ MyStringUtils.stackTraceToString( e ) );
					plugin.shutdownServer();
				}
			}
		} );
	}
	
	private synchronized void notifyNotConnected( XServer s, Exception e ) {
		synchronized ( notConnectedServers ) {
			int n = 0;
			if ( notConnectedServers.containsKey( s ) ) {
				n = notConnectedServers.get( s ).intValue();
			}
			
			if ( n++ % 100 == 0 ) {
				plugin.getLogger().info( "Connection to " + s.getName() + " failed! {Cause: " + e.getMessage() + "}" );
				// plugin.getLogger().info( "Connection to " + s.getName() + " failed! {Cause: " + e.getMessage() + "}"
				// + "\n" + MyStringUtils.stackTraceToString( e ) );
			}
			
			// logger.warning("Connection to " + s.getName() + " failed!\n" +
			// e.getMessage() + "\n" + MyStringUtils.stackTraceToString(e));
			notConnectedServers.put( s, new Integer( n ) );
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#reconnectAll_soft()
	 */
	@Override
	public void reconnectAll_soft() {
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			for ( final XServerObj s : servers.values() ) {
				stpool.runTask( new Runnable() {
					public void run() {
						if ( !s.isConnected() ) {
							try {
								s.connect();
								synchronized ( notConnectedServers ) {
									notConnectedServers.remove( s );
								}
							} catch ( IOException | InterruptedException | NotInitializedException e ) {
								notifyNotConnected( s, e );
							}
						}
					}
				} );
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
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			for ( final XServerObj s : servers.values() ) {
				stpool.runTask( new Runnable() {
					public void run() {
						try {
							s.connect();
							synchronized ( notConnectedServers ) {
								notConnectedServers.remove( s );
							}
						} catch ( IOException | InterruptedException | NotInitializedException e ) {
							notifyNotConnected( s, e );
						}
					}
				} );
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#stop()
	 */
	@Override
	public void stop() {
		reconnectClockRunning = false;
		try {
			mainserver.close();
		} catch ( IOException e ) {
			this.getLogger().warning( "An exception occured while stopping xserver server!\n" + e.getMessage() );
		} finally {
			try ( CloseableLock cs = serversLock.writeLock().open() ) {
				// executorService.shutDown();
				
				for ( XServerObj s : servers.values() ) {
					s.disconnect();
					s.setDeprecated();
				}
				
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#reload()
	 */
	@Override
	public void reload() throws IOException {
		try ( CloseableLock ch = homeLock.writeLock().open() ) {
			try ( CloseableLock cs = serversLock.writeLock().open() ) {
				
				if ( mainserver != null ) {
					try {
						mainserver.close();
					} catch ( IOException e ) {
						// this.plugin.getLogger().info( e.getClass().getName() + ": " + e.getMessage() + "\n" +
						// MyStringUtils.stackTraceToString( e ) );
					}
				}
				for ( XServerObj s : servers.values() ) {
					s.setDeprecated();
					s.disconnect();
				}
				servers.clear();
				
				// Reestablish connection
				connection.reconnect();
				;
				
				// Get all servers
				
				final Map<Integer, XServerObj> idMap = new HashMap<Integer, XServerObj>();
				
				connection.query( new Consumer<ResultSet>() {
					@Override
					public void accept( ResultSet rs ) {
						try {
							while ( rs.next() ) {
								int id = rs.getInt( "ID" );
								String servername = rs.getString( "NAME" );
								String[] hostip = rs.getString( "ADRESS" ).split( ":" );
								String pw = rs.getString( "PW" );
								
								if ( hostip.length < 2 ) {
									plugin.getLogger().warning( "XServer \"" + servername
											+ "\" has an invalid address! (host:port)" );
									continue;
								}
								
								String host = hostip[0];
								if ( hostip.length > 2 ) {
									for ( int i = 1; i < hostip.length - 1; i++ ) {
										host += ":" + hostip[i];
									}
								}
								int ip = 20000;
								try {
									ip = Integer.valueOf( hostip[hostip.length - 1] );
								} catch ( NumberFormatException nfe ) {
									plugin.getLogger().warning( "XServer \"" + servername
											+ "\" has an invalid address! (host:port)" );
									continue;
								}
								XServerObj result = new XServerObj( servername, host, ip, pw,
										AbstractXServerManagerObj.this );
								servers.put( servername, result );
								idMap.put( id, result );
							}
						} catch ( Exception e ) {
							plugin.getLogger().severe( e.getMessage() );
							throw new RuntimeException( "Couldn't load XServer List form Database!", e );
						}
					}
				}, "SELECT * FROM " + sql_table_xservers );
				
				homeServer = getServer( this.homeServerName );
				
				if ( homeServer == null ) {
					throw new IllegalArgumentException( "The home server \"" + this.homeServerName + "\" wasn't found!" );
				}
				
				// Groups
				
				this.groups.clear();
				
				final Map<Integer, XGroup> tempgroups = new HashMap<Integer, XGroup>();
				
				connection.query( new Consumer<ResultSet>() {
					@Override
					public void accept( ResultSet rs ) {
						try {
							while ( rs.next() ) {
								int groupId = rs.getInt( "groupID" );
								String name = rs.getString( "name" );
								XGroupObj o = new XGroupObj( groupId, name );
								AbstractXServerManagerObj.this.groups.put( name, o );
								tempgroups.put( groupId, o );
							}
							
						} catch ( Exception e ) {
							plugin.getLogger().severe( e.getMessage() );
							throw new RuntimeException( "Couldn't load XServer Groups form Database!", e );
						}
					}
				}, "SELECT * FROM " + sql_table_xgroups );
				
				// Relations to groups
				
				connection.query( new Consumer<ResultSet>() {
					@Override
					public void accept( ResultSet rs ) {
						try {
							while ( rs.next() ) {
								int serverId = rs.getInt( "serverID" );
								int groupId = rs.getInt( "groupId" );
								
								XServerObj x = idMap.get( serverId );
								XGroup g = tempgroups.get( groupId );
								if ( x != null && g != null ) {
									x.addGroup( g );
								}
								
							}
						} catch ( Exception e ) {
							plugin.getLogger().severe( e.getMessage() );
							throw new RuntimeException( "Couldn't load XServer Group-Relations form Database!", e );
						}
					}
				}, "SELECT * FROM " + sql_table_xserversxgroups );
				
				connection.disconnect();
				
				ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket();
				ss.setReuseAddress( true );
				ss.setPerformancePreferences( 0, 1, 1 );
				ss.setSoTimeout( SOCKET_TIMEOUT );
				ss.bind( new InetSocketAddress( homeServer.getPort() ), 500 );
				
				mainserver = new MainServer( ss, this );
				mainserver.start( this.getThreadPool() );
				
				start_async();
				
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#getHomeServer()
	 */
	@Override
	public XServerObj getHomeServer() {
		try ( CloseableLock ch = homeLock.readLock().open() ) {
			return homeServer;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#getServer(java.lang.String)
	 */
	@Override
	public XServerObj getServer( String servername ) {
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			return servers.get( servername );
		}
	}
	
	@Override
	public Set<XServer> getServers( XGroup group ) {
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			Set<XServer> result = new HashSet<XServer>();
			for ( XServerObj x : this.servers.values() ) {
				if ( x.hasGroup( group ) ) {
					result.add( x );
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
	public XServerObj getXServer( String name ) {
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			return servers.get( name );
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#getServers()
	 */
	@Override
	public Set<XServer> getServers() {
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			return new HashSet<XServer>( servers.values() );
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#getServernames()
	 */
	@Override
	public String[] getServernames() {
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			return servers.values().toArray( new String[servers.size()] );
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#getServerIgnoreCase(java.lang.String)
	 */
	@Override
	public XServerObj getServerIgnoreCase( String name ) {
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			for ( XServerObj s : servers.values() ) {
				if ( s.getName().equalsIgnoreCase( name ) ) {
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
	public XServerObj getServer( String host, int port ) {
		try ( CloseableLock cs = serversLock.readLock().open() ) {
			for ( XServerObj s : servers.values() ) {
				if ( s.getHost().equalsIgnoreCase( host ) && s.getPort() == port ) {
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
	public Message createMessage( String subChannel, byte[] content ) {
		return new MessageObj( getHomeServer(), subChannel, content );
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.AbstractXServerManager#readMessage(de.mickare.xserver.net.XServer, byte[])
	 */
	@Override
	public Message readMessage( XServer sender, byte[] data ) throws IOException {
		return new MessageObj( sender, data );
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
		try ( CloseableLock cs = this.serversLock.readLock().open() ) {
			return Collections.unmodifiableSet( new HashSet<XGroup>( this.groups.values() ) );
		}
	}
	
	@Override
	public XGroup getGroupByName( String name ) {
		return this.groups.get( name );
	}
	
}
