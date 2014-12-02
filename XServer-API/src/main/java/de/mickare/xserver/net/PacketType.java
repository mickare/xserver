package de.mickare.xserver.net;

public enum PacketType
{

	BAD_PACKET(1), KEEP_ALIVE(100), DISCONNECT(200), 
	Error(400), 
	LOGIN_CLIENT(500), LOGIN_SERVER(501), LOGIN_DENIED(502), LOGIN_ACCEPTED(503), LOGIN_END(504),
	
	PING_REQUEST(600), PING_ANSWER(601), MESSAGE(800);

	public final int packetID;

	private PacketType(int packetID) {
		this.packetID = packetID;
	}

	public int getPacketID() {
		return packetID;
	}
	
	public static PacketType getPacket(int packetID) {
		for(PacketType t :  PacketType.values()) {
			if(t.getPacketID() == packetID) {
				return t;
			}
		}
		return BAD_PACKET;
	}
	
}
