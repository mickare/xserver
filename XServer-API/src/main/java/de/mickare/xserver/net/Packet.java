package de.mickare.xserver.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Packet {
		private int packetID;
		private byte[] data;
		
		public static Packet readFromSteam(DataInputStream input) throws IOException {
			int packetID = input.readInt();
			int length = input.readInt();
			byte[] data = new byte[length];
			input.readFully(data);

			return new Packet(packetID, data);
		}
		
		public Packet(int packetID, byte[] data) {
			this.packetID = packetID;
			this.data = data;
		}
		
		public Packet(PacketType type, byte[] data) {
			this.packetID = type.packetID;
			this.data = data;
		}

		public Packet writeToStream(DataOutputStream output) throws IOException {
			output.writeInt(packetID);
			output.writeInt(data.length);
			output.write(data);
			output.flush();
			return this;
		}

		public int getPacketID() {
			return packetID;
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
		
}
