package de.mickare.xserver.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.mickare.xserver.AbstractXServerManagerObj;
import de.mickare.xserver.XType;
import de.mickare.xserver.events.XServerConnectionDenied;
import de.mickare.xserver.events.XServerLoggedInEvent;
import de.mickare.xserver.events.XServerMessageIncomingEvent;
import de.mickare.xserver.exceptions.NotInitializedException;

public class NetPacketHandler // extends Thread
{
	
	// private final static int CAPACITY = 2048;
	
	private final ConnectionObj con;
	private final AbstractXServerManagerObj manager;
	
	// private final ArrayBlockingQueue<Packet> pendingReceivingPackets = new ArrayBlockingQueue<Packet>(CAPACITY,
	// true);
	
	public NetPacketHandler( ConnectionObj con, AbstractXServerManagerObj manager ) {
		this.con = con;
		this.manager = manager;
	}
	
	/*
	 * @Override public void run() { try { while (!isInterrupted() && con.isConnected()) {
	 * doHandle(pendingReceivingPackets.take()); } } catch (IOException | InterruptedException e) {
	 * con.errorDisconnect(); } this.interrupt(); }
	 * 
	 * public void handle(Packet p) throws InterruptedException { pendingReceivingPackets.put(p);
	 * //pendingReceivingPackets.offer(p); }
	 * 
	 * private void doHandle(Packet p) throws IOException {
	 */
	
	public void handle( Packet p ) throws IOException {
		
		// manager.getLogger().info("Packet: " + p.getPacketID() + " - L" + p.getData().length);
		
		try {
			
			if ( p.getPacketID() == PacketType.KeepAlive.packetID ) // Keep Alive
			{
				
			} else if ( p.getPacketID() == PacketType.Disconnect.packetID ) // Disconnect
			{
				manager.getLogger().info( "Disconnecting from " + con.getHost() + ":" + con.getPort() );
				con.disconnect();
				
			} else if ( p.getPacketID() == PacketType.Error.packetID ) // Error
			{
				manager.getLogger().info( "Connection Error with " + con.getHost() + ":" + con.getPort() );
				con.errorDisconnect();
				
			} else if ( p.getPacketID() == PacketType.LoginDenied.packetID ) // LoginDenied
			{
				manager.getLogger().info( "Login denied from " + con.getHost() + ":" + con.getPort() );
				con.errorDisconnect();
				
			} else if ( p.getPacketID() == PacketType.LoginRequest.packetID ) // LoginRequest
			{
				try ( DataInputStream is = new DataInputStream( new ByteArrayInputStream( p.getData() ) ) ) {
					String name = is.readUTF();
					String password = is.readUTF();
					XType xtype = XType.getByNumber( is.readInt() );
					XServerObj s = manager.getServer( name );
					
					// Debugging...
					/*
					 * manager.getLogger().info ("Debugging!\n" + name + " - " + password + "\n" + "Serverfound:" +
					 * String.valueOf(s != null) + "\n" + ((s != null) ? s.getName() + " - " + s.getPassword() : ""));
					 */
					if ( s != null && s.getPassword().equals( password ) ) {
						s.setType( xtype );
						sendAcceptedLoginRequest();
									
						con.setLoginXserver( s );                        
						con.setStatus( Connection.Status.connected );
						
						s.getManager().getLogger().info( "Login Request from " + name + " accepted!" );
						s.flushCache();
						s.getManager().getEventHandler().callEvent( new XServerLoggedInEvent( con.getXserver() ) );
					} else {
						con.send( new Packet( PacketType.LoginDenied, new byte[0] ) );
						manager.getLogger().info( "Login Request from " + name + " denied! (" + con.getHost() + ":"
								+ con.getPort() + ")" );
						con.errorDisconnect();
						manager.getEventHandler().callEvent( new XServerConnectionDenied( name, password,
								con.getHost(), con.getPort() ) );
					}
				}
				
			} else if ( p.getPacketID() == PacketType.LoginAccepted.packetID ) // LoginAccepted
			{
				try ( DataInputStream is = new DataInputStream( new ByteArrayInputStream( p.getData() ) ) ) {
					String name = is.readUTF();
					String password = is.readUTF();
					XType xtype = XType.getByNumber( is.readInt() );
					XServerObj s = manager.getServer( name );
					
					// Debugging...
					
					/*
					 * XServerManager .getInstance() .getLogger() .info("Debugging!\n" + name + " - " + password + "\n"
					 * + "Serverfound:" + String.valueOf(s != null) + "\n" + ((s != null) ? s.getName() + " - " +
					 * s.getPassword() : ""));
					 */
					
					if ( s != null && s.getPassword().equals( password ) ) {
						s.setType( xtype );
						
						con.setXserver( s );						
						con.setStatus( Connection.Status.connected );
						
						s.getManager().getLogger().info( "Login Reply accepted from " + s.getName() );
						s.flushCache();
						s.getManager().getEventHandler().callEvent( new XServerLoggedInEvent( s ) );
					} else {
						con.send( new Packet( PacketType.LoginDenied, new byte[0] ) );
						manager.getLogger().info( "Login Reply from " + name + " denied! (" + con.getHost() + ":"
								+ con.getPort() + ")" );
						con.errorDisconnect();
						manager.getEventHandler().callEvent( new XServerConnectionDenied( name, password,
								con.getHost(), con.getPort() ) );
					}
				}
			} else if ( p.getPacketID() == PacketType.PingRequest.packetID ) // PingRequest
			{
				con.send( new Packet( PacketType.PingAnswer, p.getData() ) );
				
			} else if ( p.getPacketID() == PacketType.PingAnswer.packetID ) // PingAnswer
			{
				try ( DataInputStream is = new DataInputStream( new ByteArrayInputStream( p.getData() ) ) ) {
					PingObj.receive( is.readUTF(), con.getXserver() );
				}
			} else if ( p.getPacketID() == PacketType.Message.packetID ) // Message
			{
				// manager.getThreadPool().runTask(new
				// Runnable() {
				// public void run() {
				
				try {
					if ( con.getXserver() != null && con.isSocketOpen() && con.isLoggedIn() ) {
						manager.getEventHandler().callEvent( new XServerMessageIncomingEvent( con.getXserver(),
								manager.readMessage( con.getXserver(), p.getData() ) ) );
					}
				} catch ( IOException e ) {
					
				}
				// }
				// });
				
			} else {
				con.errorDisconnect();
				
			}
		} catch ( InterruptedException | IOException | NotInitializedException e ) {
			
			manager.getLogger().severe( e.getMessage() );
			
			con.errorDisconnect();
		}
		p.destroy();
	}
	
	protected void sendFirstLoginRequest() throws IOException, InterruptedException, NotInitializedException {
		sendLoginRequest( PacketType.LoginRequest );
	}
	
	protected void sendAcceptedLoginRequest() throws IOException, InterruptedException, NotInitializedException {
		sendLoginRequest( PacketType.LoginAccepted );
	}
	
	private void sendLoginRequest( PacketType type ) throws IOException, InterruptedException, NotInitializedException {
		try ( ByteArrayOutputStream b = new ByteArrayOutputStream() ) {
			try ( DataOutputStream out = new DataOutputStream( b ) ) {
				out.writeUTF( manager.getHomeServer().getName() );
				out.writeUTF( manager.getHomeServer().getPassword() );
				out.writeInt( manager.getPlugin().getHomeType().getNumber() );
				con.send( new Packet( type, b.toByteArray() ) );
			}
		}
	}
	
}
