package de.mickare.xserver.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.SocketFactory;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.XType;
import de.mickare.xserver.events.XServerConnectionDenied;
import de.mickare.xserver.events.XServerDisconnectEvent;
import de.mickare.xserver.events.XServerLoggedInEvent;
import de.mickare.xserver.util.MyStringUtils;

public class ConnectionObj implements Connection {
	
	private final static int SOCKET_TIMEOUT = 5000;
	
	private static final void sendOwnLogin( final AbstractXServerManager manager, final Socket socket,
			final DataInputStream inputStream, final DataOutputStream outputStream, final XServerObj other,
			PacketType type ) throws IOException {
		/*
		 * Send own Login
		 */
		outputStream.writeInt( type.packetID );
		outputStream.writeUTF( manager.getHomeServer().getName() );
		outputStream.writeUTF( manager.getHomeServer().getPassword() );
		outputStream.writeInt( manager.getPlugin().getHomeType().getNumber() );
		
		if ( inputStream.readInt() != PacketType.LOGIN_ACCEPTED.packetID ) {
			String msg = "Own Login denied from " + other.getName() + " (" + socket.getInetAddress().getHostAddress()
					+ ":" + socket.getPort() + ")";
			manager.getLogger().info( msg );
			socket.close();
			throw new IOException( msg );
		}
		manager.getLogger().info( "Own Login from " + other.getName() + " accepted! ("
				+ socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ")" );
	}
	
	private static final Object[] receiveOtherLogin( final AbstractXServerManager manager, final Socket socket,
			final DataInputStream inputStream, final DataOutputStream outputStream, PacketType type )
			throws IOException {
		
		/*
		 * Check other Login
		 */
		if ( inputStream.readInt() != type.packetID ) {
			// Packet Error
			String msg = "Other has wrong Login (Packet Error) (" + socket.getInetAddress().getHostAddress() + ":"
					+ socket.getPort() + ")";
			manager.getLogger().info( msg );
			outputStream.writeInt( PacketType.Error.packetID );
			socket.close();
			throw new IOException( msg );
		}
		final String name = inputStream.readUTF();
		final String password = inputStream.readUTF();
		final XType otherType = XType.getByNumber( inputStream.readInt() );
		final XServerObj other = manager.getServer( name );
		
		if ( other == null || !other.getPassword().equals( password ) ) {
			// Wrong Login
			outputStream.writeInt( PacketType.LOGIN_DENIED.packetID );
			String msg = "Other Login from " + name + " denied! (" + socket.getInetAddress().getHostAddress() + ":"
					+ socket.getPort() + ")";
			if ( other == null ) {
				msg += " - XServer \"" + name + "\" not found";
			} else {
				msg += " - wrong password";
			}
			manager.getLogger().info( msg );
			manager.getEventHandler().callEvent( new XServerConnectionDenied( name, password, socket.getInetAddress()
					.getHostAddress(), socket.getPort() ) );
			socket.close();
			throw new IOException( msg );
		}
		
		// Accept
		outputStream.writeInt( PacketType.LOGIN_ACCEPTED.packetID );
		manager.getLogger().info( "Other Login from " + name + " accepted! ("
				+ socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ")" );
		
		return new Object[] { other, otherType };
	}
	
	public static ConnectionObj endHandshake( final AbstractXServerManager manager, final Socket socket,
			final DataInputStream inputStream, final DataOutputStream outputStream, final XServerObj other,
			final XType otherType ) throws IOException, InterruptedException {
		/*
		 * End Handshake
		 */
		outputStream.writeInt( PacketType.LOGIN_END.packetID );
		
		if ( inputStream.readInt() != PacketType.LOGIN_END.packetID ) {
			manager.getLogger().info( "Handshake failed with " + socket.getInetAddress().getHostAddress() + ":"
					+ socket.getPort() );
			socket.close();
			throw new IOException( "Handshake failed" );
		}
		
		other.setType( otherType );
		ConnectionObj con = new ConnectionObj( manager, socket, inputStream, outputStream, other );
		manager.getExecutorService().submit( new Runnable() {
			@Override
			public void run() {
				manager.getEventHandler().callEvent( new XServerLoggedInEvent( other ) );
			}
		} );
		
		return con;
	}
	
	public static final ConnectionObj connectToServer( final AbstractXServerManager manager, final SocketFactory sf,
			final XServerObj other ) throws UnknownHostException, IOException, InterruptedException {
		
		// manager.getLogger().info( "connectToServer" );
		
		Socket socket = sf.createSocket( other.getHost(), other.getPort() );
		socket.setSoTimeout( SOCKET_TIMEOUT );
		
		final DataInputStream inputStream = new DataInputStream( socket.getInputStream() );
		final DataOutputStream outputStream = new DataOutputStream( socket.getOutputStream() );
		
		/*
		 * Send own Login
		 */
		sendOwnLogin( manager, socket, inputStream, outputStream, other, PacketType.LOGIN_CLIENT );
		
		/*
		 * Check other Login
		 */
		Object[] o = receiveOtherLogin( manager, socket, inputStream, outputStream, PacketType.LOGIN_SERVER );
		XServerObj other2 = ( XServerObj ) o[0];
		XType otherType = ( XType ) o[1];
		
		/*
		 * End Handshake
		 */
		return endHandshake( manager, socket, inputStream, outputStream, other2, otherType );
	}
	
	public static final void handleClient( final AbstractXServerManager manager, final Socket socket ) {
		
		// manager.getLogger().info( "handleClient" );
		
		manager.getExecutorService().submit( new Runnable() {
			@Override
			public void run() {
				try {
					socket.setSoTimeout( SOCKET_TIMEOUT );
					
					final DataInputStream inputStream = new DataInputStream( socket.getInputStream() );
					final DataOutputStream outputStream = new DataOutputStream( socket.getOutputStream() );
					
					/*
					 * Check other Login
					 */
					Object[] o = receiveOtherLogin( manager, socket, inputStream, outputStream, PacketType.LOGIN_CLIENT );
					XServerObj other = ( XServerObj ) o[0];
					XType otherType = ( XType ) o[1];
					
					/*
					 * Send own Login
					 */
					sendOwnLogin( manager, socket, inputStream, outputStream, other, PacketType.LOGIN_SERVER );
					
					/*
					 * End Handshake
					 */
					ConnectionObj con = endHandshake( manager, socket, inputStream, outputStream, other, otherType );
					other.addConnection( con );
				} catch ( IOException | InterruptedException e ) {
					manager.getLogger().warning( "Exception while connecting: " + e.getMessage() + "\n"
							+ MyStringUtils.stackTraceToString( e ) );
				}
			}
		} );
		
	}
	
	private volatile boolean closed = false;
	
	private final AbstractXServerManager manager;
	private final XServerObj xserver;
	private final String host;
	private final int port;
	
	private final Socket socket;
	private final DataInputStream input;
	private final DataOutputStream output;
	
	private final Receiving receiving;
	private final Sending sending;
	private final NetPacketHandler packetHandler;
	
	private final AtomicLong lastUse = new AtomicLong( System.currentTimeMillis() );
	
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
	
	public synchronized long getLastUse() {
		return this.lastUse.get();
	}
	
	public synchronized void setLastUse( long time ) {
		this.lastUse.set( time );
	}
	
	public synchronized void setLastUseNow() {
		this.setLastUse( System.currentTimeMillis() );
	}
	
	@Override
	public void close() throws Exception {
		if ( !closed ) {
			this.xserver.addConnection( this );
		}
	}
	
	protected void closeForReal() throws IOException {
		if ( !closed ) {
			closed = true;
			try {
				this.xserver.closeConnection( this );
				
			} catch ( Exception e ) {
				e.printStackTrace();
			} finally {
				disconnect();
			}
		}
	}
	
	@Override
	public boolean isClosed() {
		return closed ? closed : socket.isClosed();
	}
	
	public void disconnect() throws IOException {
		closed = true;
		sending.stopWorker();
		receiving.stopWorker();
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
	
	private abstract class InterruptableRunnable implements Runnable {
		private final AtomicBoolean stopped = new AtomicBoolean( false );
		
		private InterruptableRunnable( String name ) {
		}
		
		public void start( AbstractXServerManager manager ) {
			manager.getExecutorService().submit( this );
		}
		
		public boolean isStopped() {
			return stopped.get();
		}
		
		public void stopWorker() {
			this.stopped.set( true );
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
				while ( !isStopped() && !isClosed() ) {
					
					Packet p = ConnectionObj.this.xserver.getPendingSendingPackets().poll( 1000, TimeUnit.MILLISECONDS );
					
					if ( isStopped() ) {
						return;
					}
					
					if ( p == null ) {
						if ( !isClosed() ) {
							new Packet( PacketType.KEEP_ALIVE, new byte[0] ).writeToStream( output ).destroy();
							;
							tickPacket();
						} else {
							ConnectionObj.this.closeForReal();
							this.stopWorker();
						}
					} else {
						p.writeToStream( output );
						tickPacket();
					}
					p = null;
					
				}
			} catch ( IOException | InterruptedException e ) {
				this.stopWorker();
				try {
					ConnectionObj.this.closeForReal();
				} catch ( IOException e1 ) {
				}
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
				while ( !isStopped() && !isClosed() ) {
					packetHandler.handle( Packet.readFromSteam( input ) );
					tickPacket();
				}
			} catch ( IOException e ) {
				try {
					ConnectionObj.this.closeForReal();
				} catch ( IOException e1 ) {
				}
				this.stopWorker();
			}
		}
	}
	
	@Override
	public XServerObj getXServer() {
		return this.xserver;
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
