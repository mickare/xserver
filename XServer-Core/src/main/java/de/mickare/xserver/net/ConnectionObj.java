package de.mickare.xserver.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.SocketFactory;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.XType;
import de.mickare.xserver.events.XServerConnectionDenied;
import de.mickare.xserver.events.XServerDisconnectEvent;
import de.mickare.xserver.events.XServerLoggedInEvent;

public class ConnectionObj implements Connection {
	
	private final static int CAPACITY = 16384;
	private final static int SOCKET_TIMEOUT = 5000;
	
	public static final ConnectionObj connectToServer( final AbstractXServerManager manager, final SocketFactory sf,
			final XServerObj xserver ) throws UnknownHostException, IOException {
		
		Socket socket = sf.createSocket( xserver.getHost(), xserver.getPort() );
		socket.setSoTimeout( SOCKET_TIMEOUT );
		
		final DataInputStream inputStream = new DataInputStream( socket.getInputStream() );
		final DataOutputStream outputStream = new DataOutputStream( socket.getOutputStream() );
		
		/*
		 * Send own Login
		 */
		
		outputStream.writeInt( PacketType.LOGIN_CLIENT.packetID );
		outputStream.writeUTF( manager.getHomeServer().getName() );
		outputStream.writeUTF( manager.getHomeServer().getPassword() );
		outputStream.writeInt( manager.getPlugin().getHomeType().getNumber() );
		
		if ( inputStream.readInt() != PacketType.LOGIN_ACCEPTED.packetID ) {
			manager.getLogger().info( "Login denied from " + socket.getInetAddress().getHostAddress() + ":"
					+ socket.getPort() );
			socket.close();
			throw new IOException( "Self wrong Login" );
		}
		
		/*
		 * Check other Login
		 */
		
		if ( inputStream.readInt() != PacketType.LOGIN_SERVER.packetID ) {
			// Packet Error
			outputStream.writeInt( PacketType.Error.packetID );
			socket.close();
			throw new IOException( "Other wrong Login (Packet Error)" );
		}
		final String name = inputStream.readUTF();
		final String password = inputStream.readUTF();
		final XType xtype = XType.getByNumber( inputStream.readInt() );
		final XServerObj other = manager.getServer( name );
		
		if ( other == null || other.getPassword().equals( password ) ) {
			// Wrong Login
			outputStream.writeInt( PacketType.LOGIN_DENIED.packetID );
			manager.getLogger().info( "Client Login from " + name + " denied! ("
					+ socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ")" );
			manager.getEventHandler().callEvent( new XServerConnectionDenied( name, password, socket.getInetAddress()
					.getHostAddress(), socket.getPort() ) );
			socket.close();
			throw new IOException( "Other wrong Login" );
		}
		
		// Accept
		outputStream.writeInt( PacketType.LOGIN_ACCEPTED.packetID );
		manager.getLogger().info( "Server Login from " + name + " accepted!" );
		
		// End Handshake
		outputStream.writeInt( PacketType.LOGIN_END.packetID );
		
		if ( inputStream.readInt() != PacketType.LOGIN_END.packetID ) {
			manager.getLogger().info( "Handshake failed with " + socket.getInetAddress().getHostAddress() + ":"
					+ socket.getPort() );
			socket.close();
			throw new IOException( "Handshake failed" );
		}
		
		xserver.setType( xtype );
		ConnectionObj con = new ConnectionObj( manager, socket, inputStream, outputStream, xserver );
		xserver.addConnection( con );
		manager.getExecutorService().submit( new Runnable() {
			@Override
			public void run() {
				manager.getEventHandler().callEvent( new XServerLoggedInEvent( other ) );
			}
		} );
		
		return con;
	}
	
	public static final ConnectionObj handleClient( final AbstractXServerManager manager, final Socket socket )
			throws IOException {
		
		socket.setSoTimeout( SOCKET_TIMEOUT );
		
		final DataInputStream inputStream = new DataInputStream( socket.getInputStream() );
		final DataOutputStream outputStream = new DataOutputStream( socket.getOutputStream() );
		
		// Check other Login
		
		if ( inputStream.readInt() != PacketType.LOGIN_CLIENT.packetID ) {
			// Packet Error
			outputStream.writeInt( PacketType.Error.packetID );
			socket.close();
			throw new IOException( "Other wrong Login (Packet Error)" );
		}
		final String name = inputStream.readUTF();
		final String password = inputStream.readUTF();
		final XType xtype = XType.getByNumber( inputStream.readInt() );
		final XServerObj other = manager.getServer( name );
		
		if ( other == null || other.getPassword().equals( password ) ) {
			// Wrong Login
			outputStream.writeInt( PacketType.LOGIN_DENIED.packetID );
			manager.getLogger().info( "Client Login from " + name + " denied! ("
					+ socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ")" );
			manager.getEventHandler().callEvent( new XServerConnectionDenied( name, password, socket.getInetAddress()
					.getHostAddress(), socket.getPort() ) );
			socket.close();
			throw new IOException( "Other wrong Login" );
		}
		
		// Accept
		outputStream.writeInt( PacketType.LOGIN_ACCEPTED.packetID );
		manager.getLogger().info( "Client Login from " + name + " accepted!" );
		
		// Send own Login
		outputStream.writeInt( PacketType.LOGIN_SERVER.packetID );
		outputStream.writeUTF( manager.getHomeServer().getName() );
		outputStream.writeUTF( manager.getHomeServer().getPassword() );
		outputStream.writeInt( manager.getPlugin().getHomeType().getNumber() );
		
		if ( inputStream.readInt() != PacketType.LOGIN_ACCEPTED.packetID ) {
			manager.getLogger().info( "Login denied from " + socket.getInetAddress().getHostAddress() + ":"
					+ socket.getPort() );
			socket.close();
			throw new IOException( "Self wrong Login" );
		}
		
		// End Handshake
		outputStream.writeInt( PacketType.LOGIN_END.packetID );
		
		if ( inputStream.readInt() != PacketType.LOGIN_END.packetID ) {
			manager.getLogger().info( "Handshake failed with " + socket.getInetAddress().getHostAddress() + ":"
					+ socket.getPort() );
			socket.close();
			throw new IOException( "Handshake failed" );
		}
		
		other.setType( xtype );
		ConnectionObj con = new ConnectionObj( manager, socket, inputStream, outputStream, other );
		other.addConnection( con );
		manager.getExecutorService().submit( new Runnable() {
			@Override
			public void run() {
				manager.getEventHandler().callEvent( new XServerLoggedInEvent( other ) );
			}
		} );
		
		return con;
	}
	
	private volatile boolean closed = false;
	
	private final AbstractXServerManager manager;
	private final XServerObj xserver;
	private final String host;
	private final int port;
	
	private final Socket socket;
	private final DataInputStream input;
	private final DataOutputStream output;
	
	private final ArrayBlockingQueue<Packet> pendingSendingPackets = new ArrayBlockingQueue<Packet>( CAPACITY, true );
	
	private final Receiving receiving;
	private final Sending sending;
	private final NetPacketHandler packetHandler;
	
	public ConnectionObj( AbstractXServerManager manager, Socket socket, DataInputStream inputStream,
			DataOutputStream outputStream, XServerObj xserver ) {
		
		this.manager = manager;
		this.xserver = xserver;
		
		this.socket = socket;
		this.host = socket.getInetAddress().getHostAddress();
		this.port = socket.getPort();
		
		this.input = inputStream;
		this.output = outputStream;
		
		this.packetHandler = new NetPacketHandler( this, manager );
		
		this.receiving = new Receiving();
		this.receiving.start( manager );
		
		this.sending = new Sending();
		this.sending.start( manager );
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.Connection#ping(de.mickare.xserver.net.Ping)
	 */
	@Override
	public void ping( Ping ping ) throws InterruptedException, IOException {
		ByteArrayOutputStream b = null;
		DataOutputStream out = null;
		try {
			b = new ByteArrayOutputStream();
			out = new DataOutputStream( b );
			out.writeUTF( ping.getKey() );
			pendingSendingPackets.put( new Packet( PacketType.PING_REQUEST, b.toByteArray() ) );
		} finally {
			if ( out != null ) {
				out.close();
			}
		}
	}
	
	@Override
	public void close() throws Exception {
		this.xserver.addConnection( this );
	}
	
	protected void closeForReal() throws Exception {
		disconnect();
	}
	
	@Override
	public boolean isClosed() {
		return closed ? closed : ( socket != null ? !socket.isClosed() : false );
	}
	
	public void disconnect() throws IOException {
		closed = true;
		sending.interrupt();
		receiving.interrupt();
		try {
			socket.close();
			input.close();
			output.close();
		} finally {
			if ( this.xserver != null ) {
				this.manager.getEventHandler().callEvent( new XServerDisconnectEvent( xserver ) );
			}
		}
	}
	
	@Override
	public String getHost() {
		return host;
	}
	
	@Override
	public int getPort() {
		return port;
	}
	
	@Override
	public boolean send( Packet packet ) {
		return pendingSendingPackets.offer( packet );
	}
	
	@Override
	public boolean sendAll( Collection<Packet> packets ) {
		boolean result = true;
		for ( Packet p : packets ) {
			result &= send( p );
		}
		return result;
	}
	
	private abstract class InterruptableRunnable implements Runnable {
		private final AtomicBoolean interrupted = new AtomicBoolean( false );
		private final String name;
		
		private InterruptableRunnable( String name ) {
			this.name = name;
		}
		
		public void start( AbstractXServerManager manager ) {
			manager.getExecutorService().submit( this );
		}
		
		public boolean isInterrupted() {
			return interrupted.get();
		}
		
		public void interrupt() {
			this.interrupted.set( true );
		}
		
		public String getName() {
			return name;
		}
		
	}
	
	private class Sending extends InterruptableRunnable {
		
		private final AtomicLong recordSecondPackageCount = new AtomicLong( 0 );
		private final AtomicLong lastSecondPackageCount = new AtomicLong( 0 );
		
		private long lastSecond = 0;
		private long packageCount = 0;
		
		public Sending() {
			super( "Sending Thread to (" + host + ":" + port + ")" );
		}
		
		private void tickPacket() {
			if ( System.currentTimeMillis() - lastSecond > 1000 ) {
				lastSecondPackageCount.set( packageCount );
				if ( packageCount > recordSecondPackageCount.get() ) {
					recordSecondPackageCount.set( packageCount );
				}
				packageCount = 0;
				lastSecond = System.currentTimeMillis();
			}
			packageCount++;
		}
		
		@Override
		public void run() {
			try {
				while ( !isInterrupted() && !isClosed() ) {
					
					Packet p = pendingSendingPackets.poll( 1000, TimeUnit.MILLISECONDS );
					
					if ( isInterrupted() ) {
						return;
					}
					
					if ( p == null ) {
						if ( !isClosed() ) {
							new Packet( PacketType.KEEP_ALIVE, new byte[0] ).writeToStream( output ).destroy();
							;
							tickPacket();
						} else {
							disconnect();
							this.interrupt();
						}
					} else {
						p.writeToStream( output );
						tickPacket();
					}
					p = null;
					
				}
			} catch ( IOException | InterruptedException e ) {
				try {
					disconnect();
				} catch ( IOException e1 ) {
				}
				this.interrupt();
			}
		}
		
	}
	
	private class Receiving extends InterruptableRunnable {
		
		private final AtomicLong recordSecondPackageCount = new AtomicLong( 0 );
		private final AtomicLong lastSecondPackageCount = new AtomicLong( 0 );
		
		private long lastSecond = 0;
		private long packageCount = 0;
		
		public Receiving() {
			super( "Receiving Thread to (" + host + ":" + port + ")" );
		}
		
		private void tickPacket() {
			if ( System.currentTimeMillis() - lastSecond > 1000 ) {
				lastSecondPackageCount.set( packageCount );
				if ( packageCount > recordSecondPackageCount.get() ) {
					recordSecondPackageCount.set( packageCount );
				}
				packageCount = 0;
				lastSecond = System.currentTimeMillis();
			}
			packageCount++;
		}
		
		@Override
		public void run() {
			try {
				while ( !isInterrupted() && !isClosed() ) {
					packetHandler.handle( Packet.readFromSteam( input ) );
					tickPacket();
				}
			} catch ( IOException e ) {
				try {
					disconnect();
				} catch ( IOException e1 ) {
				}
				this.interrupt();
			}
		}
	}
	
	@Override
	public XServerObj getXServer() {
		return this.xserver;
	}
	
	@Override
	public Queue<Packet> getPendingPackets() {
		return new ArrayBlockingQueue<Packet>( CAPACITY, false, pendingSendingPackets );
	}
	
	@Override
	public String toString() {
		return host + ":" + port;
	}
	
	@Override
	public long getSendingRecordSecondPackageCount() {
		return this.sending.recordSecondPackageCount.get();
	}
	
	@Override
	public long getSendinglastSecondPackageCount() {
		return this.sending.lastSecondPackageCount.get();
	}
	
	@Override
	public long getReceivingRecordSecondPackageCount() {
		return this.receiving.recordSecondPackageCount.get();
	}
	
	@Override
	public long getReceivinglastSecondPackageCount() {
		return this.receiving.lastSecondPackageCount.get();
	}
	
}
