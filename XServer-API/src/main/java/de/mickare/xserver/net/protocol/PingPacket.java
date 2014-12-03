package de.mickare.xserver.net.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.mickare.xserver.net.Packet;
import de.mickare.xserver.net.PacketType;
import de.mickare.xserver.net.SocketPacketHandler;

public class PingPacket implements Packet {
	
	public enum Direction {
		PING( false ),
		PONG( true );
		
		private final boolean returned;
		
		Direction( boolean returned ) {
			this.returned = returned;
		}
		
		public boolean isReturned() {
			return returned;
		}
		
		public static Direction get( boolean returned ) {
			if ( returned ) {
				return PONG;
			}
			return PING;
		}
	}
	
	private final static PacketType type = PacketType.HANDSHAKE_ACCEPT;
	
	private final Direction direction;
	private final byte[] data;
	
	public static PingPacket readFrom( DataInputStream input ) throws IOException {
		Direction direction = Direction.get( input.readBoolean() );
		byte[] data = new byte[input.readInt()];
		input.readFully( data );
		return new PingPacket( direction, data );
	}
	
	public PingPacket( Direction direction, byte[] data ) {
		this.direction = direction;
		this.data = data;
	}
	
	public void writeTo( DataOutputStream output ) throws IOException {
		output.writeBoolean( direction.isReturned() );
		output.writeInt( getData().length );
		output.write( getData() );
	}
	
	public int getPacketID() {
		return type.getPacketID();
	}
	
	@Override
	public PacketType getPacketType() {
		return type;
	}
	
	@Override
	public void handle( SocketPacketHandler handler ) {
		handler.handlePacket( this );
	}
	
	public Direction getDirection() {
		return direction;
	}

	public byte[] getData() {
		return data;
	}
	
}
