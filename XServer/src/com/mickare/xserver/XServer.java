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
	public XServer(String name, String host, int port, String password) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
	}

	// Info Getters
	public String getName() {
		return name;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getPassword() {
		return password;
	}
	
	// Methods
	
	public void send(Message m) throws IOException, NotInitializedException {		
		ServerMain.getInstance().sendMessage(this, m);
		EventHandler.getInstance().callEvent(new XServerMessageOutgoingEvent(this, m));
	}
						
	public void receive(Message m) throws NotInitializedException {
		EventHandler.getInstance().callEvent(new XServerMessageIncomingEvent(this, m));
	}
	
}
