package com.mickare.xserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mickare.xserver.net.XServer;

public class Message {

	private final XServer<? extends Object> sender;
	private final String subChannel;
	private final byte[] content;

	
	
	protected Message(XServer<? extends Object> sender, String subChannel, byte[] content) {
		this.sender = sender;
		this.subChannel = subChannel;
		this.content = content;
	}

	protected Message(XServer<? extends Object> sender, byte[] data) throws IOException {
		this.sender = sender;
		DataInputStream in = null;
		try {
			in = new DataInputStream(new ByteArrayInputStream(data));
			subChannel = in.readUTF();
			int contentLength = in.readInt();
			byte[] contentData = new byte[contentLength];
			in.readFully(contentData);
			content = contentData;
		} finally {
			if(in != null) {
				in.close();
			}
		}
		
	}

	/**
	 * Get Sender Server Name
	 * @return sender name
	 */
	public XServer<? extends Object> getSender() {
		return sender;
	}

	/**
	 * SubChannel
	 * @return subchannel name
	 */
	public String getSubChannel() {
		return subChannel;
	}

	/**
	 * Get the content of this message...
	 * @return byte array
	 */
	public byte[] getContent() {
		return content;
	}
	
	/**
	 * Get the compiled byte array of this message...
	 * @return byte array
	 * @throws IOException
	 */
	public byte[] getData() throws IOException {
		// Write Data
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(b);
			out.writeUTF(subChannel);
			out.writeInt(content.length);
			out.write(content);
		} finally {
			if(out != null) {
				out.close();
			}
		}
		
		return b.toByteArray();
	}

}
