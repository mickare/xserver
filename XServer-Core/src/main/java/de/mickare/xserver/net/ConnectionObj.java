package de.mickare.xserver.net;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.SocketFactory;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.events.XServerDisconnectEvent;
import de.mickare.xserver.events.XServerLoggedInEvent;
import de.mickare.xserver.net.protocol.HandshakeAcceptPacket;
import de.mickare.xserver.net.protocol.HandshakeAuthentificationPacket;
import de.mickare.xserver.net.protocol.KeepAlivePacket;

public class ConnectionObj implements Connection {
	
	private final static int KEEP_ALIVE_PACKETS = 1000;
	
	public enum TYPE {
		CLIENT,
		SERVER;
	}
	
	public static final ConnectionObj connectToServer( final AbstractXServerManager manager, final SocketFactory sf,
			final XServerObj other ) throws UnknownHostException, IOException, InterruptedException {
		
		// manager.getLogger().info( "connectToServer" );
		
		final Socket socket = sf.createSocket( other.getHost(), other.getPort() );
		socket.setSoTimeout( AbstractXServerManager.SOCKET_TIMEOUT );
		
		try {
			
			Thread.sleep( 10 );
			
			final NetPacketHandler handler = new NetPacketHandler( socket, manager );
			
			/*
			 * Authentification of client side
			 */
			handler.write( new HandshakeAuthentificationPacket( manager.getHomeServer().getName(), manager
					.getHomeServer().getPassword(), manager.getPlugin().getHomeType() ) );
			
			/*
			 * Authentification of server side
			 */
			handler.read();
			if ( handler.getMyStatus() != NetPacketHandler.State.EXPECTING_HANDSHAKE_ACCEPT
					|| handler.getXserverIfPresent() == null ) {
				String msg = "Other has not accepted handshake (" + socket.getInetAddress().getHostAddress() + ":"
						+ socket.getPort() + ")";
				manager.getLogger().info( msg );
				socket.close();
				throw new IOException( msg );
			}
			
			/*
			 * Finish handshake on server side
			 */
			handler.write( new HandshakeAcceptPacket() );
			
			/*
			 * Finish the handshake on client side
			 */
			handler.read();
			if ( handler.getMyStatus() != NetPacketHandler.State.NORMAL ) {
				String msg = "Other has not finished handshake (" + socket.getInetAddress().getHostAddress() + ":"
						+ socket.getPort() + ")";
				manager.getLogger().info( msg );
				socket.close();
				throw new IOException( msg );
			}
			
			if ( other != handler.getXserverIfPresent() ) {
				String msg = "Connected to wrong server " + other.getName() + " <-> " + handler.getXserverIfPresent();
				manager.getLogger().info( msg );
				socket.close();
				throw new IOException( msg );
			}
			
			/*
			 * Create valid connection
			 */
			ConnectionObj con = new ConnectionObj( manager, socket, other, handler, TYPE.CLIENT );
			handler.setConnection( con ); // Important to reference the connection with the handler
			manager.getThreadPool().runTask( new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep( 10 );
					} catch ( InterruptedException e ) {
					}
					manager.getEventHandler().callEvent( new XServerLoggedInEvent( other ) );
				}
			} );
			return con;
		} catch ( Exception e ) {
			socket.close();
			throw e;
		}
	}
	
	public static final void handleClient( final AbstractXServerManager manager, final Socket socket ) {
		
		manager.getThreadPool().runTask( new Runnable() {
			@Override
			public void run() {
				
				try {
					final NetPacketHandler handler = new NetPacketHandler( socket, manager );
					
					/*
					 * Authentification of client side
					 */
					handler.read();
					if ( handler.getMyStatus() != NetPacketHandler.State.EXPECTING_HANDSHAKE_ACCEPT
							|| handler.getXserverIfPresent() == null ) {
						String msg = "Other has not accepted handshake (" + socket.getInetAddress().getHostAddress()
								+ ":" + socket.getPort() + ")";
						manager.getLogger().info( msg );
						socket.close();
						throw new IOException( msg );
					}
					
					final XServerObj other = handler.getXserverIfPresent();
					
					/*
					 * Authentification of server side
					 */
					handler.write( new HandshakeAuthentificationPacket( manager.getHomeServer().getName(), manager
							.getHomeServer().getPassword(), manager.getPlugin().getHomeType() ) );
					
					/*
					 * Finish handshake on server side
					 */
					handler.read();
					if ( handler.getMyStatus() != NetPacketHandler.State.NORMAL ) {
						String msg = "Other has not finished handshake (" + socket.getInetAddress().getHostAddress()
								+ ":" + socket.getPort() + ")";
						manager.getLogger().info( msg );
						socket.close();
						throw new IOException( msg );
					}
					
					/*
					 * Finish the handshake on client side
					 */
					handler.write( new HandshakeAcceptPacket() );
					
					/*
					 * Create valid connection
					 */
					ConnectionObj con = new ConnectionObj( manager, socket, other, handler, TYPE.SERVER );
					handler.setConnection( con ); // Important to reference the connection with the handler
					other.addConnection( con );
					manager.getThreadPool().runTask( new Runnable() {
						@Override
						public void run() {
							try {
								Thread.sleep( 10 );
							} catch ( InterruptedException e ) {
							}
							manager.getEventHandler().callEvent( new XServerLoggedInEvent( other ) );
						}
					} );
					
				} catch ( Exception e ) {
					try {
						socket.close();
					} catch ( IOException e1 ) {
					}
				}
			}
		} );
		
	}
	
	private volatile boolean closed = false;
	
	private final AbstractXServerManager manager;
	private final XServerObj xserver;
	private final TYPE connectionType;
	private final String host;
	private final int port;
	
	private final Socket socket;
	
	private final Receiving receiving;
	private final Sending sending;
	private final NetPacketHandler packetHandler;
	
	private final AtomicLong lastUse = new AtomicLong( System.currentTimeMillis() );
	
	public ConnectionObj( AbstractXServerManager manager, Socket socket, XServerObj xserver, NetPacketHandler handler,
			TYPE connectionType ) {
		
		this.manager = manager;
		this.xserver = xserver;
		this.connectionType = connectionType;
		
		this.socket = socket;
		this.host = socket.getInetAddress().getHostAddress();
		this.port = socket.getPort();
		
		this.packetHandler = handler;
		
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
			manager.getThreadPool().runTask( this );
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
					
					Packet p = ConnectionObj.this.xserver.getPendingSendingPackets()
							.poll( KEEP_ALIVE_PACKETS, TimeUnit.MILLISECONDS );
					
					if ( isStopped() ) {
						return;
					}
					
					if ( p == null ) {
						if ( !isClosed() ) {
							ConnectionObj.this.packetHandler.write( new KeepAlivePacket() );
							tickPacket();
						} else {
							ConnectionObj.this.closeForReal();
							this.stopWorker();
						}
					} else {
						ConnectionObj.this.packetHandler.write( p );
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
					ConnectionObj.this.packetHandler.read();
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
	
	protected Socket getSocket() {
		return this.socket;
	}
	
	public TYPE getConnectionType() {
		return connectionType;
	}
	
}
