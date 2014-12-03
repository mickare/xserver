package de.mickare.xserver.net.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.mickare.xserver.XType;
import de.mickare.xserver.net.Packet;
import de.mickare.xserver.net.PacketType;
import de.mickare.xserver.net.SocketPacketHandler;

public class HandshakeAuthentificationPacket implements Packet {
	
	private final static PacketType type = PacketType.HANDSHAKE_AUTHENTIFICATION;
	
	public static HandshakeAuthentificationPacket readFrom( DataInputStream input ) throws IOException {
		
		final String name = input.readUTF();
		final String password = input.readUTF();
		final XType xtype = XType.getByNumber( input.readInt() );
		
		return new HandshakeAuthentificationPacket( name, password, xtype );
	}
	
	private final String name, password;
	private final XType xtype;
	
	public HandshakeAuthentificationPacket( String name, String password, XType xtype ) {
		this.name = name;
		this.password = password;
		this.xtype = xtype;
	}
	
	public void writeTo( DataOutputStream output ) throws IOException {
		output.writeUTF( name );
		output.writeUTF( password );
		output.writeInt( xtype.getNumber() );
	}
	
	public void destroy() {
		try {
			this.finalize();
		} catch ( Throwable e ) {
			
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getPassword() {
		return password;
	}
	
	public XType getXType() {
		return xtype;
	}
	
	@Override
	public PacketType getPacketType() {
		return type;
	}
	
	@Override
	public void handle( SocketPacketHandler handler ) {
		handler.handlePacket( this );
	}
	
}
