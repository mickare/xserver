package com.mickare.xserver.net;

public enum PacketType
{

	BadPacket(1), KeepAlive(100), Disconnect(200), Error(400), LoginDenied(401), LoginRequest(500), LoginAccepted(501), PingRequest(
			600), PingAnswer(601), Message(800);

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
		return BadPacket;
	}
	
}
