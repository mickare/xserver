package de.mickare.xserver.net;

import java.io.DataOutputStream;
import java.io.IOException;

public interface Packet {
	
	PacketType getPacketType();
	
	int getPacketID();
	
	void handle( SocketPacketHandler handler );
	
	void writeTo( DataOutputStream out ) throws IOException;
	
}
