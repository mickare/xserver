package de.mickare.xserver.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.Message;
import de.mickare.xserver.XGroup;
import de.mickare.xserver.XServerManager;
import de.mickare.xserver.XType;
import de.mickare.xserver.events.XServerMessageOutgoingEvent;
import de.mickare.xserver.exceptions.NotInitializedException;
import de.mickare.xserver.util.Encryption;
import de.mickare.xserver.util.MyStringUtils;
import de.mickare.xserver.util.concurrent.CloseableLock;
import de.mickare.xserver.util.concurrent.CloseableReadWriteLock;
import de.mickare.xserver.util.concurrent.CloseableReentrantReadWriteLock;

public class XServerObj implements XServer {
	
	private final static int MAX_CONNECTIONS = 4;
	private final static long MAX_LAST_USE = 120 * 1000;
	// private final static int MESSAGE_CACHE_SIZE = 8192;
	
	private final String name;
	private final String host;
	private final int port;
	private final String password;
	
	private volatile boolean deprecated = false;
	
	private CloseableReadWriteLock connectionLock = new CloseableReentrantReadWriteLock();
	private final Set<ConnectionObj> connectionOpened = Collections
			.newSetFromMap( new ConcurrentHashMap<ConnectionObj, Boolean>() );
	private final LinkedBlockingDeque<ConnectionObj> connectionPool = new LinkedBlockingDeque<ConnectionObj>();
	
	private volatile XType type = XType.Other;
	
	private final Set<XGroup> groups = new HashSet<XGroup>();
	
	// private final CacheList<Packet> pendingPackets = new CacheList<Packet>( MESSAGE_CACHE_SIZE );
	
	private final AbstractXServerManager manager;
	
	public XServerObj( String name, String host, int port, String password, AbstractXServerManager manager ) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5( password );
		this.manager = manager;
	}
	
	public XServerObj( String name, String host, int port, String password, XType type, AbstractXServerManager manager ) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5( password );
		this.type = type;
		this.manager = manager;
	}
	
	private void log( String msg ) {
		manager.getLogger().info( msg );
	}
	
	protected void addConnection( ConnectionObj con ) throws InterruptedException {
		// log( "A1" );
		if ( con != null && !con.isClosed() && !deprecated ) {
			// log( "A2" );
			this.connectionOpened.add( con );
			this.connectionPool.putLast( con );
		}
	}
	
	private ConnectionObj createConnection() throws UnknownHostException, IOException, InterruptedException {
		// log( "B1" );
		ConnectionObj con;
		if ( connectionOpened.size() >= MAX_CONNECTIONS ) {
			// log( "B2.1" );
			con = connectionPool.takeLast();
			// log( "B2.2" );
			con.setLastUseNow();
			return con;
		}
		// log( "B3.1" );
		ConnectionObj.connectToServer( manager, manager.getSocketFactory(), this );
		con = connectionPool.takeLast();
		con.setLastUseNow();
		// log( "B3.2" );
		return con;
		
	}
	
	protected void closeConnection( final ConnectionObj con ) throws Exception {
		// log( "C1" );
		try {
			this.connectionPool.remove( con );
			con.closeForReal();
			// log( "C2" );
		} catch ( Exception e ) {
			throw e;
		} finally {
			this.connectionOpened.remove( con );
			if ( !deprecated ) {
				connect();
			}
		}
	}
	
	private final AtomicLong lastClean = new AtomicLong( System.currentTimeMillis() );
	
	private void cleanConnections() {
		if ( System.currentTimeMillis() - lastClean.get() <= MAX_LAST_USE ) {
			return;
		}
		// log( "D1" );
		lastClean.set( System.currentTimeMillis() );
		for ( ConnectionObj o : new HashSet<ConnectionObj>( this.connectionOpened ) ) {
			if ( System.currentTimeMillis() - o.getLastUse() > MAX_LAST_USE ) {
				try {
					o.closeForReal();
				} catch ( Exception e ) {
				}
			}
		}
	}
	
	private ConnectionObj getConnection() {
		// log( "E1" );
		try {
			ConnectionObj con = connectionPool.pollLast();
			if ( con != null && !con.isClosed() ) {
				// log( "E2" );
				con.setLastUseNow();
				return con;
			} else {
				// log( "E3" );
				return createConnection();
			}
		} catch ( NotInitializedException | IOException | InterruptedException e ) {
			manager.getLogger().info( "No Connection! - " + e.getMessage() );
		}
		return null;
	}
	
	private boolean hasOpenedConnections() {
		return this.connectionOpened.size() > 0;
	}
	
	@Override
	public int countConnections() {
		return this.connectionOpened.size();
	}
	
	@Override
	public boolean isConnected() {
		return this.deprecated ? false : hasOpenedConnections();
	}
	
	public void connect() throws Exception {
		try ( CloseableLock c = connectionLock.readLock().open() ) {
			if ( !valid() ) {
				return;
			}
			if ( !hasOpenedConnections() ) {
				ConnectionObj con = this.createConnection();
				if ( con != null ) {
					this.connectionPool.put( this.createConnection() );
				}
			}
			cleanConnections();
		}
	}
	
	@Override
	public void disconnect() {
		try ( CloseableLock c = this.connectionLock.writeLock().open() ) {
			for ( ConnectionObj o : new HashSet<ConnectionObj>( this.connectionOpened ) ) {
				try {
					o.closeForReal();
				} catch ( Exception e ) {
				}
			}
			this.connectionOpened.clear();
			this.connectionPool.clear();
			/*
			 * if ( !deprecated ) { try { connect(); } catch ( NotInitializedException | IOException |
			 * InterruptedException e ) { } }
			 */
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#getPort()
	 */
	@Override
	public int getPort() {
		return port;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#getPassword()
	 */
	@Override
	public String getPassword() {
		return password;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#sendMessage(de.mickare.xserver.Message)
	 */
	@Override
	public boolean sendMessage( Message message ) throws IOException {
		boolean result = false;
		if ( !valid() ) {
			return false;
		}
		if ( isConnected() && hasOpenedConnections() ) {
			try ( ConnectionObj con = this.getConnection() ) {
				if ( con.send( new Packet( PacketType.MESSAGE, message.getData() ) ) ) {
					result = true;
				}
			} catch ( InterruptedException e ) {
				return false;
			} catch ( Exception e ) {
				throw new RuntimeException( e );
			}
		}
		
		manager.getEventHandler().callEvent( new XServerMessageOutgoingEvent( this, message ) );
		return result;
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#ping(de.mickare.xserver.net.Ping)
	 */
	@Override
	public void ping( Ping ping ) throws InterruptedException, IOException {
		if ( !valid() ) {
			return;
		}
		if ( isConnected() && hasOpenedConnections() ) {
			try ( ConnectionObj con = this.getConnection() ) {
				con.ping( ping );
			} catch ( InterruptedException e ) {
				throw e;
			} catch ( Exception e ) {
				throw new RuntimeException( e );
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#getType()
	 */
	@Override
	public XType getType() {
		return type;
	}
	
	protected void setType( XType type ) {
		this.type = type;
	}
	
	public XServerManager getManager() {
		return manager;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#getSendingRecordSecondPackageCount()
	 */
	@Override
	public long getSendingRecordSecondPackageCount() {
		try ( CloseableLock c = this.connectionLock.readLock().open() ) {
			long result = 0;
			for ( ConnectionObj o : this.connectionOpened ) {
				result += o.getSendingRecordSecondPackageCount();
			}
			return result;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#getSendinglastSecondPackageCount()
	 */
	@Override
	public long getSendinglastSecondPackageCount() {
		try ( CloseableLock c = this.connectionLock.readLock().open() ) {
			long result = 0;
			for ( ConnectionObj o : this.connectionOpened ) {
				result += o.getSendinglastSecondPackageCount();
			}
			return result;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#getReceivingRecordSecondPackageCount()
	 */
	@Override
	public long getReceivingRecordSecondPackageCount() {
		try ( CloseableLock c = this.connectionLock.readLock().open() ) {
			long result = 0;
			for ( ConnectionObj o : this.connectionOpened ) {
				result += o.getReceivingRecordSecondPackageCount();
			}
			return result;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#getReceivinglastSecondPackageCount()
	 */
	@Override
	public long getReceivinglastSecondPackageCount() {
		try ( CloseableLock c = this.connectionLock.readLock().open() ) {
			long result = 0;
			for ( ConnectionObj o : this.connectionOpened ) {
				result += o.getReceivinglastSecondPackageCount();
			}
			return result;
		}
	}
	
	public void addGroup( XGroup g ) {
		this.groups.add( g );
	}
	
	@Override
	public Set<XGroup> getGroups() {
		return Collections.unmodifiableSet( this.groups );
	}
	
	@Override
	public boolean hasGroup( XGroup group ) {
		return this.groups.contains( group );
	}
	
	private boolean valid() {
		if ( deprecated ) {
			this.manager.getLogger().warning( "This XServer Object \"" + this.name + "\" is deprecated!\n"
					+ MyStringUtils.stackTraceToString( Thread.currentThread().getStackTrace() ) );
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean isDeprecated() {
		return this.deprecated;
	}
	
	public void setDeprecated() {
		this.deprecated = true;
	}
	
	@Override
	public XServer getCurrentXServer() {
		return this.manager.getXServer( name );
	}
	
}