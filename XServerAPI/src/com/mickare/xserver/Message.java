package com.mickare.xserver;

import java.io.IOException;

import com.mickare.xserver.net.XServer;

public interface Message {

	/**
	 * Get Sender Server Name
	 * @return sender name
	 */
	public abstract XServer getSender();

	/**
	 * SubChannel
	 * @return subchannel name
	 */
	public abstract String getSubChannel();

	/**
	 * Get the content of this message...
	 * @return byte array
	 */
	public abstract byte[] getContent();

	/**
	 * Get the compiled byte array of this message...
	 * @return byte array
	 * @throws IOException
	 */
	public abstract byte[] getData() throws IOException;

}