package de.mickare.xserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.mickare.xserver.net.XServer;

public class MessageObj implements Message {

	private final XServer sender;
	private final String subChannel;
	private final byte[] content;

	
	
	protected MessageObj(XServer sender, String subChannel, byte[] content) {
		this.sender = sender;
		this.subChannel = subChannel;
		this.content = content;
	}

	protected MessageObj(XServer sender, byte[] data) throws IOException {
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.Message#getSender()
	 */
	@Override
	public XServer getSender() {
		return sender;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.Message#getSubChannel()
	 */
	@Override
	public String getSubChannel() {
		return subChannel;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.Message#getContent()
	 */
	@Override
	public byte[] getContent() {
		return content;
	}
	
	/* (non-Javadoc)
	 * @see de.mickare.xserver.Message#getData()
	 */
	@Override
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
