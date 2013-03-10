package com.mickare.xserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mickare.xserver.Exception.Message_SenderUnknownException;
import com.mickare.xserver.Exception.NotPluginMessage;

public class Message {

	protected static final String PLUGIN_MESSAGE_CODE = "XServerMessage";

	private final String typename;
	
	private final String senderName;
	private final String senderPassword;
	private final String receiverName;
	private final String receiverPassword;
	private final String subChannel;
	private final byte[] content;

	protected Message(ConfigServers cs, String typename, XServer receiver,
			String subChannel, byte[] content) {
		this.typename = typename;
		
		this.senderName = cs.getHomeServer().getName();
		this.senderPassword = cs.getHomeServer().getPassword();
		this.receiverName = receiver.getName();
		this.receiverPassword = receiver.getPassword();
		this.subChannel = subChannel;
		this.content = content;
	}

	protected Message(ConfigServers cs, byte[] data) throws IOException,
			NotPluginMessage, Message_SenderUnknownException {

		// Read header
		
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		String plugin_message_code = in.readUTF();
		if (!plugin_message_code.equals(PLUGIN_MESSAGE_CODE)) {
			throw new NotPluginMessage();
		}
		typename = in.readUTF();
		senderName = in.readUTF();
		XServer senderServer = cs.getServer(senderName);

		if (senderServer == null) {
			throw new Message_SenderUnknownException(senderName);
		}
		senderPassword = senderServer.getPassword();

		
		// Decription
		int encDataLen = in.readInt();
		byte[] encData = new byte[encDataLen];
		in.readFully(encData);
		byte[] decData = de_encrypt(senderPassword, encData);

		DataInputStream inC = new DataInputStream(new ByteArrayInputStream(
				decData));

		// Read decrypted content
		
		receiverName = inC.readUTF();
		receiverPassword = inC.readUTF();
		subChannel = inC.readUTF();
		int contentLength = inC.readInt();
		byte[] contentData = new byte[contentLength];
		inC.readFully(contentData);
		content = contentData;

		in.close();
		inC.close();
	}

	/**
	 * Get Sender Server Name
	 * @return sender name
	 */
	public String getSender_Name() {
		return senderName;
	}

	/**
	 * Get Receiver Server Name
	 * @return receiver name
	 */
	public String getReceiver_Name() {
		return receiverName;
	}

	/**
	 * Get Receiver Server password
	 * @return password
	 */
	public String getReceiver_Password() {
		return receiverPassword;
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
	 * Get the Messagetype
	 * @return type name
	 */
	protected String getTypeName() {
		return typename;
	}
	
	/**
	 * Get the compiled byte array of this message...
	 * @return byte array
	 * @throws IOException
	 */
	public byte[] getData() throws IOException {

		// Content Encyption
		// ENCRYPTION ROCKS!!!
		ByteArrayOutputStream enb = new ByteArrayOutputStream();
		DataOutputStream enOut = new DataOutputStream(enb);
		enOut.writeUTF(receiverName);
		enOut.writeUTF(receiverPassword);
		enOut.writeUTF(subChannel);
		enOut.writeInt(content.length);
		enOut.write(content);
		enOut.close();
		byte[] encData = de_encrypt(senderPassword, enb.toByteArray());

		// Write Data
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		out.writeUTF(PLUGIN_MESSAGE_CODE);
		out.writeUTF(typename);
		out.writeUTF(senderName);
		out.writeInt(encData.length);
		out.write(encData);
		out.close();
		return b.toByteArray();
	}

	private byte[] de_encrypt(String password, byte[] data) {
		byte[] result = new byte[data.length];
		byte[] pB = password.getBytes();
		for (int i = 0; i < data.length; i++) {
			result[i] = (byte) (data[i] ^ pB[i % pB.length]);
		}
		return result;
	}

}
