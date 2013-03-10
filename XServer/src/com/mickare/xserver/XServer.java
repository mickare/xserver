package com.mickare.xserver;

import java.io.IOException;

import com.mickare.xserver.Exception.NotInitializedException;
import com.mickare.xserver.events.*;
import com.mickare.xserver.util.Encryption;

public class XServer {
	
	// Infos
	private final String name;
	private final String host;
	private final int port;
	private final String password;
			
	// Constructor
	protected XServer(String name, String host, int port, String password) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
	}

	// Info Getters
	/**
	 * Get server name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get server host
	 * @return host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Get server port
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Get server password
	 * @return
	 */
	public String getPassword() {
		return password;
	}
	
	// Methods
	/**
	 * Send a message over to this xserver... 
	 * @param m Message
	 * @throws IOException
	 * @throws NotInitializedException
	 */
	public void send(Message m) throws IOException, NotInitializedException {		
		ServerMain.getInstance().sendMessage(this, m);
		EventHandler.getInstance().callEvent(new XServerMessageOutgoingEvent(this, m));
	}
						
	/**
	 * Normally you don't use this!
	 * Server receives a message...
	 * @param m message
	 * @throws NotInitializedException
	 */
	public void receive(Message m) throws NotInitializedException {
		EventHandler.getInstance().callEvent(new XServerMessageIncomingEvent(this, m));
	}
	
}
