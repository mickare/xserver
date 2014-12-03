package de.mickare.xserver.net;

import de.mickare.xserver.net.protocol.*;

public interface SocketPacketHandler {
	
	void handlePacket( DataPacket p );
	
	void handlePacket( HandshakeAcceptPacket p );
	
	void handlePacket( HandshakeAuthentificationPacket p );
	
	void handlePacket( KeepAlivePacket p );
	
	void handlePacket( PingPacket p );
	
}
