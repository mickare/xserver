package com.mickare.xserver;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.mickare.xserver.Exception.Message_SenderUnknownException;
import com.mickare.xserver.Exception.NotInitializedException;
import com.mickare.xserver.Exception.NotPluginMessage;

public class MessageFactory {

	
	// Static
	
	private static MessageFactory instance = null;
	
	/**
	 * Get instance of MessageFactory
	 * @return
	 * @throws NotInitializedException
	 */
	public static MessageFactory getInstance() throws NotInitializedException {
		if(instance == null) {
			throw new NotInitializedException("MessageFactory not initialized!");
		}
		return instance;
	}
	
	protected static void initialize(ConfigServers cs) {
		instance = new MessageFactory(cs);
	}
	
	// Normal
	
	private final ConfigServers cs;
	
	private MessageFactory(ConfigServers cs) {
		this.cs = cs;
	}
	

	/**
	 * Create a new Message that will be send to another server...
	 * @param server target name
	 * @param subChannel name
	 * @param data message content
	 * @return Message
	 */
	public Message createMessage(XServer server, String subChannel, byte[] data) {
		return new Message(cs, "DATA", server, subChannel, data);
	}
	
	/**
	 * Read a message byte array...
	 * @param data
	 * @return message from data
	 * @throws IOException
	 * @throws NotPluginMessage
	 * @throws Message_SenderUnknownException
	 */
	public Message recreateMessage(byte[] data) throws IOException, NotPluginMessage, Message_SenderUnknownException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		String plugin_message_code = in.readUTF();
		if (!plugin_message_code.equals(Message.PLUGIN_MESSAGE_CODE)) {
			throw new NotPluginMessage();
		}
		String typename = in.readUTF();
		
		if(typename.equals("DATA")) {
			return new Message(cs, data);
		}
		return null;
	}
		
}
