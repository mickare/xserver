package de.mickare.xserver.net;

public enum PacketType {
	
	BAD_PACKET( -1 ),
	DISCONNECT( 2 ),
	Error( 3 ),

	KEEP_ALIVE( 100 ),
	
	HANDSHAKE_AUTHENTIFICATION( 500 ),
	HANDSHAKE_ACCEPT(501),
	
	PING( 600 ),
	DATA( 800 );
	
	public final int packetID;
	
	private PacketType( int packetID ) {
		this.packetID = packetID;
	}
	
	public int getPacketID() {
		return packetID;
	}
	
	public static PacketType getPacket( int packetID ) {
		for ( PacketType t : PacketType.values() ) {
			if ( t.getPacketID() == packetID ) {
				return t;
			}
		}
		return BAD_PACKET;
	}
	
}
