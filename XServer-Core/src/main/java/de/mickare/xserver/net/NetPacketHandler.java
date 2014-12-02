package de.mickare.xserver.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.events.XServerMessageIncomingEvent;
import de.mickare.xserver.exceptions.NotInitializedException;

public class NetPacketHandler // extends Thread
{
	
	private final ConnectionObj con;
	private final AbstractXServerManager manager;
	
	public NetPacketHandler( ConnectionObj con, AbstractXServerManager manager ) {
		this.con = con;
		this.manager = manager;
	}
	
	public void handle( Packet p ) throws IOException {
		
		try {
			
			if ( p.getPacketID() == PacketType.KEEP_ALIVE.packetID ) // Keep Alive
			{
				
			} else if ( p.getPacketID() == PacketType.DISCONNECT.packetID ) // Disconnect
			{
				manager.getLogger().info( "Disconnecting from " + con.getHost() + ":" + con.getPort() );
				con.disconnect();
			} else if ( p.getPacketID() == PacketType.PING_REQUEST.packetID ) // PingRequest
			{
				con.send( new Packet( PacketType.PING_ANSWER, p.getData() ) );
				
			} else if ( p.getPacketID() == PacketType.PING_ANSWER.packetID ) // PingAnswer
			{
				try ( DataInputStream is = new DataInputStream( new ByteArrayInputStream( p.getData() ) ) ) {
					PingObj.receive( is.readUTF(), con.getXServer() );
				}
			} else if ( p.getPacketID() == PacketType.MESSAGE.packetID ) // Message
			{
				// manager.getThreadPool().runTask(new
				// Runnable() {
				// public void run() {
				
				try {
					if ( con.getXServer() != null && !con.isClosed() ) {
						manager.getEventHandler().callEvent( new XServerMessageIncomingEvent( con.getXServer(),
								manager.readMessage( con.getXServer(), p.getData() ) ) );
					}
				} catch ( IOException e ) {
					
				}
				// }
				// });
				
			} else if ( p.getPacketID() == PacketType.Error.packetID ) // Error
			{
				manager.getLogger().info( "Connection Error with " + con.getHost() + ":" + con.getPort() );
				con.disconnect();
				
			} else {
				manager.getLogger().info( "Connection Packet Error with " + con.getHost() + ":" + con.getPort() );
				con.disconnect();
				
			}
		} catch ( IOException | NotInitializedException e ) {
			
			manager.getLogger().severe( e.getMessage() );
			
			con.disconnect();
		}
		p.destroy();
	}
	
	protected void sendFirstLoginRequest() throws IOException, InterruptedException, NotInitializedException {
		sendLoginRequest( PacketType.LOGIN_CLIENT );
	}
	
	protected void sendAcceptedLoginRequest() throws IOException, InterruptedException, NotInitializedException {
		sendLoginRequest( PacketType.LOGIN_SERVER );
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
