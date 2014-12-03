package de.mickare.xserver.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.Message;
import de.mickare.xserver.XGroup;
import de.mickare.xserver.XServerManager;
import de.mickare.xserver.XType;
import de.mickare.xserver.events.XServerMessageOutgoingEvent;
import de.mickare.xserver.net.protocol.DataPacket;
import de.mickare.xserver.net.protocol.PingPacket;
import de.mickare.xserver.util.Encryption;
import de.mickare.xserver.util.MyStringUtils;
import de.mickare.xserver.util.concurrent.CloseableLock;
import de.mickare.xserver.util.concurrent.CloseableReadWriteLock;
import de.mickare.xserver.util.concurrent.CloseableReentrantReadWriteLock;

public class XServerObj implements XServer {
	
	private final static int CAPACITY = 16384;
	
	private final static int BORDER_INCREASE = 500;
	private final static int BORDER_DECREASE = 200;
	
	private final static int MAX_CONNECTIONS = 3;
	private final static long CONNECTION_CHANGE_DELAY = 10 * 1000;
	// private final static int MESSAGE_CACHE_SIZE = 8192;
	
	private final String name;
	private final String host;
	private final int port;
	private final String password;
	
	private volatile boolean deprecated = false;
	
	private final CloseableReadWriteLock connectionLock = new CloseableReentrantReadWriteLock();
	private final Set<ConnectionObj> connectionOpened = Collections
			.newSetFromMap( new ConcurrentHashMap<ConnectionObj, Boolean>() );
	private final LinkedBlockingDeque<ConnectionObj> connectionPool = new LinkedBlockingDeque<ConnectionObj>();
	
	private volatile XType type = XType.Other;
	
	private final Set<XGroup> groups = new HashSet<XGroup>();
	
	// private final CacheList<Packet> pendingPackets = new CacheList<Packet>( MESSAGE_CACHE_SIZE );
	
	private final AbstractXServerManager manager;
	
	private final ArrayBlockingQueue<Packet> pendingSendingPackets = new ArrayBlockingQueue<Packet>( CAPACITY, true );
	
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
	
	/*
	 * private void log( String msg ) { manager.getLogger().info( Thread.currentThread().getName() + " - " + msg + "\n"
	 * + MyStringUtils.stackTraceToString( Thread.currentThread().getStackTrace() ) ); }
	 */
	
	protected void addConnection( ConnectionObj con ) throws InterruptedException {
		// log( "A1" );
		if ( con != null && !con.isClosed() && !deprecated ) {
			// log( "A2" );
			this.connectionOpened.add( con );
			this.connectionPool.putLast( con );
		}
	}
	
	/*
	 * private ConnectionObj createConnection() throws UnknownHostException, IOException, InterruptedException { // log(
	 * "B1" );
	 * 
	 * if ( this.deprecated ) { return null; }
	 * 
	 * ConnectionObj con;
	 * 
	 * do { con = null;
	 * 
	 * // try ( CloseableLock c = this.connectionLock.writeLock().open() ) { // log( "B2.1" ); if (
	 * connectionOpened.size() < MAX_CONNECTIONS ) { try ( CloseableLock c = createConnectionLock.open() ) { if (
	 * connectionOpened.size() < MAX_CONNECTIONS ) { con = ConnectionObj.connectToServer( manager,
	 * manager.getSocketFactory(), this ); connectionOpened.add( con ); } } }
	 * 
	 * // log( "B3.1" ); if ( con == null ) { con = connectionPool.pollLast( 1, TimeUnit.MILLISECONDS ); } // }
	 * 
	 * if ( con != null && con.isClosed() ) { this.connectionOpened.remove( con ); this.connectionPool.remove( con );
	 * con = null; } } while ( con == null || con.isClosed() ); con.setLastUseNow(); // log( "B3.2" ); return con;
	 * 
	 * }
	 */
	
	protected void closeConnection( final ConnectionObj con ) {
		// log( "C1" );
		try {
			this.connectionPool.remove( con );
			this.connectionOpened.remove( con );
			con.closeForReal();
			// log( "C2" );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			this.connectionOpened.remove( con );
			if ( !deprecated ) {
				try {
					connect();
				} catch ( Exception e ) {
				}
			}
		}
	}
	
	private final AtomicLong lastChange = new AtomicLong( System.currentTimeMillis() );
	
	private synchronized void changeConnections() throws IOException, InterruptedException {
		if ( System.currentTimeMillis() - lastChange.get() <= CONNECTION_CHANGE_DELAY ) {
			return;
		}
		int size = this.pendingSendingPackets.size();
		if ( size > BORDER_INCREASE ) {
			this.increaseConnections();
		} else if ( size <= BORDER_DECREASE ) {
			this.decreaseConnections();
		}
	}
	
	private synchronized void decreaseConnections() throws IOException {
		ConnectionObj con = this.connectionPool.pollFirst();
		if ( this.connectionOpened.size() <= 1 || con == null
				|| System.currentTimeMillis() - lastChange.get() <= CONNECTION_CHANGE_DELAY ) {
			return;
		}
		lastChange.set( System.currentTimeMillis() );
		try {
			con.closeForReal();
			this.manager.getLogger().info( this.name + " - Decreased connections to: " + this.connectionOpened.size() );
		} catch ( IOException e ) {
			this.connectionOpened.remove( con );
			throw e;
		}
	}
	
	private synchronized void increaseConnections() throws IOException, InterruptedException {
		
		if ( this.connectionOpened.size() >= MAX_CONNECTIONS
				|| System.currentTimeMillis() - lastChange.get() <= CONNECTION_CHANGE_DELAY ) {
			return;
		}
		
		ConnectionObj con = null;
		try {
			con = ConnectionObj.connectToServer( manager, manager.getSocketFactory(), this );
			this.connectionOpened.add( con );
			this.connectionPool.putLast( con );
			this.manager.getLogger().info( this.name + " - Increased connections to: " + this.connectionOpened.size() );
		} catch ( IOException | InterruptedException e ) {
			if ( con != null ) {
				this.connectionOpened.remove( con );
				this.connectionPool.remove( con );
				try {
					con.closeForReal();
				} catch ( IOException e1 ) {
				}
			}
			throw e;
		}
		
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
		if ( !valid() ) {
			return;
		}
		try ( CloseableLock c = connectionLock.writeLock().open() ) {
			if ( !hasOpenedConnections() ) {
				this.increaseConnections();
			}
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
		if ( message == null ) {
			throw new NullPointerException( "parameter is null" );
		}
		boolean result = false;
		if ( !valid() ) {
			return false;
		}
		
		send( new DataPacket( message.getData() ) );
		
		manager.getEventHandler().callEvent( new XServerMessageOutgoingEvent( this, message ) );
		return result;
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.XServer#ping(de.mickare.xserver.net.Ping)
	 */
	@Override
	public void ping( Ping ping ) throws IOException {
		if ( !valid() ) {
			return;
		}
		
		ByteArrayOutputStream b = null;
		DataOutputStream out = null;
		try {
			b = new ByteArrayOutputStream();
			out = new DataOutputStream( b );
			out.writeUTF( ping.getKey() );
			send( new PingPacket( PingPacket.Direction.PING, b.toByteArray() ) );
		} finally {
			if ( out != null ) {
				out.close();
			}
		}
		
	}
	
	public void send( Packet packet ) {
		getPendingSendingPackets().offer( packet );
		try {
			this.changeConnections();
		} catch ( InterruptedException e ) {
			throw new RuntimeException( e );
		} catch ( IOException e ) {
			if ( !this.hasOpenedConnections() ) {
				manager.notifyNotConnected( this, e );
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
	
	protected ArrayBlockingQueue<Packet> getPendingSendingPackets() {
		return pendingSendingPackets;
	}
	
}