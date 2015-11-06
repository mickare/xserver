package de.mickare.xserver.net.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.mickare.xserver.net.Packet;
import de.mickare.xserver.net.PacketType;
import de.mickare.xserver.net.SocketPacketHandler;

public class KeepAlivePacket implements Packet {

	private final static PacketType type = PacketType.KEEP_ALIVE;
	
	public static HandshakeAcceptPacket readFrom( DataInputStream input ) throws IOException {
		return new HandshakeAcceptPacket();
	}
	
	public KeepAlivePacket() {
		
	}
	
	public void writeTo( DataOutputStream output ) throws IOException {
		
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
