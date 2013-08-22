package com.mickare.xserver.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.mickare.xserver.Message;
import com.mickare.xserver.XServerManager;
import com.mickare.xserver.exceptions.NotConnectedException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.CacheList;
import com.mickare.xserver.util.Encryption;

public class XServer {

	private final static int MESSAGE_CACHE_SIZE = 256;
	
	private final String name;
	private final String host;
	private final int port;
	private final String password;

	private Connection connection = null;
	private Connection connection2 = null;	// Fix for HomeServer that is not connectable.
	private Lock conLock = new ReentrantLock();

	private CacheList<Packet> pendingPackets = new CacheList<Packet>(MESSAGE_CACHE_SIZE);
	
	public XServer(String name, String host, int port, String password) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
	}

	public void connect() throws UnknownHostException, IOException,
			InterruptedException, NotInitializedException {
		conLock.lock();
		try {
			if (isConnected()) {
				this.disconnect();
			}
			connection = new Connection(XServerManager.getInstance()
					.getSocketFactory(), host, port);
		} finally {
			conLock.unlock();
		}
	}

	protected void setConnection(Connection con) {
		conLock.lock();
		try {
			if (this.connection != con && isConnected()) {
				this.disconnect();
				for(Packet p : this.connection.getPendingPackets()) {
					if(p.getType().equals(Packet.Types.Message)) {
						this.pendingPackets.push(p);
					}
				}
			}
			this.connection = con;
		} finally {
			conLock.unlock();
		}
	}

	public void setReloginConnection(Connection con) throws NotInitializedException {
		conLock.lock();
		try {
			if(XServerManager.getInstance().getHomeServer() == this) {
				if (this.connection2 != con && (this.connection2 != null ? this.connection2.isConnected() : false)) {
					this.disconnect();
				}
				this.connection2 = con;
			} else {
				setConnection(con);
			}
		} finally {
			conLock.unlock();
		}
	}
	
	public boolean isConnected() {
		conLock.lock();
		try {
			return connection != null ? connection.isConnected() && (connection.isLoggedIn() || connection.isLoggingIn()) : false;
		} finally {
			conLock.unlock();
		}
	}

	public void disconnect() {
		conLock.lock();
		try {
			if(connection != null) {
				connection.disconnect();
			}
		} finally {
			conLock.unlock();
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

	public void sendMessage(Message message) throws NotConnectedException, IOException {
		conLock.lock();
		try {
			if (!isConnected() || (connection != null ? !connection.isLoggedIn() : false)) {
				pendingPackets.push(new Packet(Packet.Types.Message, message.getData()));
				throw new NotConnectedException("Not Connected to this server!");
			}
			connection
					.send(new Packet(Packet.Types.Message, message.getData()));
		} finally {
			conLock.unlock();
		}
	}
	
	public void ping(Ping ping) throws InterruptedException, IOException {
		conLock.lock();
		if(isConnected()) {
			connection.ping(ping);
		}
		conLock.unlock();
	}
	
	public void flushCache() {
		conLock.lock();
		try {
			Packet p = pendingPackets.pollLast();
			while(p != null) {
				connection.send(p);
				p = pendingPackets.pollLast();
			}
		} finally {
			conLock.unlock();
		}
	}

}