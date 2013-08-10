package com.mickare.xserver.net;

import java.io.DataOutputStream;
import java.io.IOException;

public class Packet {
		private Types type;
		private byte[] data;

		public static enum Types {

			KeepAlive(100), Disconnect(200), Error(400), LoginDenied(401), LoginRequest(500), LoginAccepted(501), PingRequest(
					600), PingAnswer(601), Message(800);

			public final int packetID;

			private Types(int packetID) {
				this.packetID = packetID;
			}

			public int getPacketId() {
				return packetID;
			}

		}
		
		public Packet(Types type, byte[] data) {
			this.type = type;
			this.data = data;
		}

		public void writeToStream(DataOutputStream output) throws IOException {
			output.writeInt(type.packetID);
			output.writeInt(data.length);
			output.write(data);
			output.flush();
		}

		public Types getType() {
			return type;
		}

		public byte[] getData() {
			return data;
		}
}
