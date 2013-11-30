package com.mickare.xserver.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mickare.xserver.AbstractXServerManager;
import com.mickare.xserver.Message;
import com.mickare.xserver.XType;
import com.mickare.xserver.events.XServerMessageOutgoingEvent;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.CacheList;
import com.mickare.xserver.util.Encryption;

public class XServer {

	private final static int MESSAGE_CACHE_SIZE = 8192;

	private final String name;
	private final String host;
	private final int port;
	private final String password;

	private Connection connection = null;
	private Connection connection2 = null; // Fix for HomeServer that is not
											// connectable.
	private ReadWriteLock conLock = new ReentrantReadWriteLock();

	private ReadWriteLock typeLock = new ReentrantReadWriteLock();
	private XType type = XType.Other;

	private final CacheList<Packet> pendingPackets = new CacheList<Packet>(MESSAGE_CACHE_SIZE);

	private final AbstractXServerManager manager;

	public XServer(String name, String host, int port, String password, AbstractXServerManager manager) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
		this.manager = manager;
	}

	public XServer(String name, String host, int port, String password, XType type, AbstractXServerManager manager) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
		this.type = type;
		this.manager = manager;
	}

	public void connect() throws UnknownHostException, IOException, InterruptedException, NotInitializedException {

		if (isConnected()) {
			this.disconnect();
		}

		new Connection(manager.getSocketFactory(), host, port, manager);
	}

	protected void setConnection(Connection con) {
		conLock.writeLock().lock();
		try {
			if (this.connection != con) {
				this.disconnect();
			}
			this.connection = con;
		} finally {
			conLock.writeLock().unlock();
		}
	}

	public void setReloginConnection(Connection con) {
		if (manager.getHomeServer() == this) {
			conLock.writeLock().lock();
			try {
				if (this.connection2 != con && (this.connection2 != null ? this.connection2.isConnected() : false)) {
					this.disconnect();
				}
				this.connection2 = con;
			} finally {
				conLock.writeLock().unlock();
			}
		} else {
			setConnection(con);
		}
	}

	public boolean isConnected() {
		conLock.readLock().lock();
		try {
			return connection != null ? connection.isLoggedIn() : false;
		} finally {
			conLock.readLock().unlock();
		}
	}

	public void disconnect() {
		conLock.writeLock().lock();
		try {
			if (connection != null) {
				connection.disconnect();
				synchronized (pendingPackets) {
					for (Packet p : this.connection.getPendingPackets()) {
						if (p.getPacketID() == PacketType.Message.packetID) {
							this.pendingPackets.push(p);
						}
					}
				}
				connection = null;
				connection2 = null;
			}
		} finally {
			conLock.writeLock().unlock();
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

	public boolean sendMessage(Message message) throws IOException {
		boolean result = false;

		if (!isConnected()) {
			synchronized (pendingPackets) {
				pendingPackets.push(new Packet(PacketType.Message, message.getData()));
			}
			// throw new NotConnectedException("Not Connected to this server!");
		} else {
			conLock.readLock().lock();
			try {
				if (connection.send(new Packet(PacketType.Message, message.getData()))) {
					result = true;
				}
			} finally {
				conLock.readLock().unlock();
			}
		}

		manager.getEventHandler().callEvent(new XServerMessageOutgoingEvent(this, message));
		return result;

	}

	public void ping(Ping ping) throws InterruptedException, IOException {
		conLock.readLock().lock();
		if (isConnected()) {
			connection.ping(ping);
		}
		conLock.readLock().unlock();
	}

	public void flushCache() {
		conLock.readLock().lock();
		try {
			if (isConnected()) {
				synchronized (pendingPackets) {
					Packet p = pendingPackets.pollLast();
					while (p != null) {
						connection.send(p);
						p = pendingPackets.pollLast();
					}
				}
			}
		} finally {
			conLock.readLock().unlock();
		}
	}

	public XType getType() {
		typeLock.readLock().lock();
		try {
			return type;
		} finally {
			typeLock.readLock().unlock();
		}
	}

	protected void setType(XType type) {
		typeLock.writeLock().lock();
		try {
			this.type = type;
		} finally {
			typeLock.writeLock().unlock();
		}
	}

	public AbstractXServerManager getManager() {
		return manager;
	}

	public long getSendingRecordSecondPackageCount() {
		conLock.readLock().lock();
		try {
			if (isConnected()) {
				if (this.connection2 != null) {
					return this.connection.getSendingRecordSecondPackageCount()
							+ this.connection2.getSendingRecordSecondPackageCount();
				}
				return this.connection.getSendingRecordSecondPackageCount();
			}
		} finally {
			conLock.readLock().unlock();
		}
		return 0;
	}

	public long getSendinglastSecondPackageCount() {
		conLock.readLock().lock();
		try {
			if (isConnected()) {
				if (this.connection2 != null) {
					return this.connection.getSendinglastSecondPackageCount()
							+ this.connection2.getSendinglastSecondPackageCount();
				}
				return this.connection.getSendinglastSecondPackageCount();
			}
		} finally {
			conLock.readLock().unlock();
		}
		return 0;
	}

	public long getReceivingRecordSecondPackageCount() {
		conLock.readLock().lock();
		try {
			if (isConnected()) {
				if (this.connection2 != null) {
					return this.connection.getReceivingRecordSecondPackageCount()
							+ this.connection2.getReceivingRecordSecondPackageCount();
				}
				return this.connection.getReceivingRecordSecondPackageCount();
			}
		} finally {
			conLock.readLock().unlock();
		}
		return 0;
	}

	public long getReceivinglastSecondPackageCount() {
		conLock.readLock().lock();
		try {
			if (isConnected()) {
				if (this.connection2 != null) {
					return this.connection.getReceivinglastSecondPackageCount()
							+ this.connection2.getReceivinglastSecondPackageCount();
				}
				return this.connection.getReceivinglastSecondPackageCount();
			}
		} finally {
			conLock.readLock().unlock();
		}
		return 0;
	}

}