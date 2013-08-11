package com.mickare.xserver.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	private Lock conLock = new ReentrantLock();

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
			}
			this.connection = con;
		} finally {
			conLock.unlock();
		}
	}

	public boolean isConnected() {
		conLock.lock();
		try {
			return connection != null ? connection.isConnected() : false;
		} finally {
			conLock.unlock();
		}
	}

	public void disconnect() {
		conLock.lock();
		try {
			connection.disconnect();
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

	public void sendMessage(Message message) throws NotConnectedException,
			InterruptedException, IOException {
		conLock.lock();
		try {
			if (!isConnected()) {
				throw new NotConnectedException("Not Connected to this server!");
			}
			connection
					.send(new Packet(Packet.Types.Message, message.getData()));
		} finally {
			conLock.unlock();
		}
	}

}