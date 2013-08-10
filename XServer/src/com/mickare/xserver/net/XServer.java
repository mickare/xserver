package com.mickare.xserver.net;

import java.io.IOException;
import java.net.UnknownHostException;

import com.mickare.xserver.Message;
import com.mickare.xserver.XServerManager;
import com.mickare.xserver.exceptions.NotConnectedException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.Encryption;

public class XServer {

	private final String name;
	private final String host;
	private final int port;
	private final String password;
	
	private Connection connection = null;
	

	public XServer(String name, String host, int port, String password) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
	}
	
	public void connect() throws UnknownHostException, IOException, InterruptedException, NotInitializedException {
		synchronized(connection) {
			if(isConnected()) {
				connection.disconnect();
			}
			connection = new Connection(XServerManager.getInstance().getSocketFactory(), host, port);	
		}
	}
	
	protected void setConnection(Connection con) {
		synchronized(connection) {
			if(this.connection != con && isConnected()) {
				this.connection.disconnect();
			} 
			
			this.connection = con;
		}
	}
	
	public boolean isConnected() {
		synchronized(connection) {
			return connection != null ? !connection.isConnected() : false;
		}
	}
	
	public void disconnect() {
		synchronized(connection) {
			connection.disconnect();
		}
	}
	
	
	
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
	
	public void sendMessage(Message message) throws NotConnectedException, InterruptedException, IOException {
		if(!isConnected()) {
			throw new NotConnectedException();
		} 
		connection.send(new Packet(Packet.Types.Message, message.getData()));
	}
	
}