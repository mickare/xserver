package de.mickare.xserver.net.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.mickare.xserver.net.Packet;
import de.mickare.xserver.net.PacketType;
import de.mickare.xserver.net.SocketPacketHandler;

public class DataPacket implements Packet {
	
	private final static PacketType type = PacketType.DATA;
	private byte[] data;
	
	public static DataPacket readFrom( DataInputStream input ) throws IOException {
		// int packetID = input.readInt();
		int length = input.readInt();
		byte[] data = new byte[length];
		input.readFully( data );
		
		return new DataPacket( data );
	}
	
	public DataPacket( byte[] data ) {
		this.data = data;
	}
	
	public void writeTo( DataOutputStream output ) throws IOException {
		output.writeInt( data.length );
		output.write( data );
	}
	
	public int getPacketID() {
		return type.getPacketID();
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void destroy() {
		try {
			this.finalize();
		} catch ( Throwable e ) {
			
		}
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
