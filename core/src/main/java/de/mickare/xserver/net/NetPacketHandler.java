package de.mickare.xserver.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.events.XServerMessageIncomingEvent;
import de.mickare.xserver.net.protocol.DataPacket;
import de.mickare.xserver.net.protocol.HandshakeAcceptPacket;
import de.mickare.xserver.net.protocol.HandshakeAuthentificationPacket;
import de.mickare.xserver.net.protocol.KeepAlivePacket;
import de.mickare.xserver.net.protocol.PingPacket;
import de.mickare.xserver.util.concurrent.CloseableLock;
import de.mickare.xserver.util.concurrent.CloseableReentrantLock;

public class NetPacketHandler implements SocketPacketHandler // extends Thread
{
		
	public enum State {
		EXPECTING_HANDSHAKE_AUTHENTIFICATION,
		EXPECTING_HANDSHAKE_ACCEPT,
		NORMAL;
	}
	
	private ConnectionObj con = null;
	private XServerObj xserver = null;
	private final Socket socket;
	private final AbstractXServerManager manager;
	
	private volatile State status = State.EXPECTING_HANDSHAKE_AUTHENTIFICATION;
	
	private final DataInputStream dataIn;
	private final DataOutputStream dataOut;
	
	private final CloseableReentrantLock inLock = new CloseableReentrantLock();
	private final CloseableReentrantLock outLock = new CloseableReentrantLock();
	
	public NetPacketHandler( Socket socket, AbstractXServerManager manager ) throws IOException {
		this.socket = socket;
		this.manager = manager;
		
		dataIn = new DataInputStream( socket.getInputStream() );
		dataOut = new DataOutputStream( socket.getOutputStream() );
	}
	
	private void disconnect() throws IOException {
		try {
			if ( con != null ) {
				con.disconnect();
			}
			socket.close();
		} finally {
			this.notifyAll();
		}
	}
	
	public void read() throws IOException {
		
		if ( socket.isClosed() ) {
			throw new IOException( "Socket is closed!" );
		}
		// InputStream in = socket.getInputStream();
		try ( CloseableLock c = inLock.open() ) {
			/*
			 * PacketType type = PacketType.getPacket( in.read() ); if ( type == PacketType.BAD_PACKET ) { throw new
			 * IOException( "Bad packet!" ); } int size = in.read(); if ( size < 0 ) { throw new IOException(
			 * "Bad packet!" ); } byte[] buf = new byte[size]; in.read( buf );
			 */
			
			// try ( DataInputStream dataIn = new DataInputStream(in ) ) {
			int packetID = dataIn.readInt();
			PacketType type = PacketType.getPacket( packetID );
			if ( type == PacketType.BAD_PACKET ) {
				throw new IOException( "Bad packet! (" + packetID + ")" );
			}
			
			Packet p = null;
			switch ( type ) {
				case DATA:
					p = DataPacket.readFrom( dataIn );
					break;
				case KEEP_ALIVE:
					p = KeepAlivePacket.readFrom( dataIn );
					break;
				case PING:
					p = PingPacket.readFrom( dataIn );
					break;
				case HANDSHAKE_AUTHENTIFICATION:
					p = HandshakeAuthentificationPacket.readFrom( dataIn );
					break;
				case HANDSHAKE_ACCEPT:
					p = HandshakeAcceptPacket.readFrom( dataIn );
					break;
				default:
					disconnect();
			}
			
			if ( p == null ) {
				disconnect();
			} else {
				p.handle( this );
			}
		}
	}
	
	public void write( Packet p ) throws IOException {
		if ( socket.isClosed() ) {
			throw new IOException( "Socket is closed!" );
		}
		try ( CloseableLock c = outLock.open() ) {
			dataOut.writeInt( p.getPacketType().getPacketID() );
			p.writeTo( dataOut );
			dataOut.flush();
		}
	}
	
	@Override
	public void handlePacket( DataPacket p ) {
		if ( status != State.NORMAL || con == null ) {
			try {
				this.disconnect();
			} catch ( IOException e ) {
			}
			return;
		}
		
		if ( !con.isClosed() ) {
			try {
				manager.getEventHandler().callEvent( new XServerMessageIncomingEvent( con.getXServer(),
						manager.readMessage( con.getXServer(), p.getData() ) ) );
				
			} catch ( IOException e ) {
				try {
					this.con.disconnect();
				} catch ( IOException e1 ) {
				}
			}
		}
	}
	
	@Override
	public void handlePacket( HandshakeAcceptPacket p ) {
		
		if ( status != State.EXPECTING_HANDSHAKE_ACCEPT ) {
			try {
				this.disconnect();
			} catch ( IOException e ) {
			}
			return;
		}
		status = State.NORMAL;
		
	}
	
	@Override
	public void handlePacket( HandshakeAuthentificationPacket p ) {
		if ( status != State.EXPECTING_HANDSHAKE_AUTHENTIFICATION ) {
			try {
				this.disconnect();
			} catch ( IOException e ) {
			}
			return;
		}
		
		final XServerObj other = manager.getServer( p.getName() );
		
		if ( other == null || !other.getPassword().equals( p.getPassword() ) ) {
			
			String msg = "Other has not accepted handshake (" + socket.getInetAddress().getHostAddress() + ":"
					+ socket.getPort() + ")";
			if ( other == null ) {
				msg += " - XServer \"" + p.getName() + "\" not found";
			} else {
				msg += " - wrong password";
			}
			manager.getLogger().info( msg );
			try {
				this.disconnect();
			} catch ( IOException e ) {
			}
			return;
		}
		
		other.setType( p.getXType() );
		
		this.xserver = other;
		
		status = State.EXPECTING_HANDSHAKE_ACCEPT;
	}
	
	@Override
	public void handlePacket( KeepAlivePacket p ) {
		if ( status != State.NORMAL || con == null ) {
			try {
				this.disconnect();
			} catch ( IOException e ) {
			}
			return;
		}
		
	}
	
	@Override
	public void handlePacket( PingPacket p ) {
		if ( status != State.NORMAL || con == null ) {
			try {
				this.disconnect();
			} catch ( IOException e ) {
			}
			return;
		}
		
		if ( p.getDirection() == PingPacket.Direction.PING ) {
			con.getXServer().send( new PingPacket( PingPacket.Direction.PONG, p.getData() ) );
		} else {
			PingObj.receive( p, con.getXServer() );
		}
	}
	
	public ConnectionObj getConnectionIfPresent() {
		return con;
	}
	
	protected void setConnection( ConnectionObj con ) {
		this.con = con;
	}
	
	public State getMyStatus() {
		return status;
	}
	
	public XServerObj getXserverIfPresent() {
		return xserver;
	}
	
}
