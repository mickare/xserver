package de.mickare.xserver.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.AbstractXServerManagerObj;
import de.mickare.xserver.Message;
import de.mickare.xserver.XType;
import de.mickare.xserver.events.XServerMessageOutgoingEvent;
import de.mickare.xserver.exceptions.NotInitializedException;
import de.mickare.xserver.util.CacheList;
import de.mickare.xserver.util.Encryption;

public class XServerObj implements XServer {

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

	private final AbstractXServerManagerObj manager;

	public XServerObj(String name, String host, int port, String password, AbstractXServerManagerObj manager) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
		this.manager = manager;
	}

	public XServerObj(String name, String host, int port, String password, XType type, AbstractXServerManagerObj manager) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
		this.type = type;
		this.manager = manager;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#connect()
	 */
	@Override
	public void connect() throws UnknownHostException, IOException, InterruptedException, NotInitializedException {

		if (isConnected()) {
			this.disconnect();
		}

		new ConnectionObj(manager.getSocketFactory(), host, port, manager);
	}

	public void setConnection(Connection con) {
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#setReloginConnection(de.mickare.xserver.net.Connection)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#isConnected()
	 */
	@Override
	public boolean isConnected() {
		conLock.readLock().lock();
		try {
			return connection != null ? connection.isLoggedIn() : false;
		} finally {
			conLock.readLock().unlock();
		}
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#disconnect()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getPort()
	 */
	@Override
	public int getPort() {
		return port;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getPassword()
	 */
	@Override
	public String getPassword() {
		return password;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#sendMessage(de.mickare.xserver.Message)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#ping(de.mickare.xserver.net.Ping)
	 */
	@Override
	public void ping(Ping ping) throws InterruptedException, IOException {
		conLock.readLock().lock();
		try {
			if (isConnected()) {
				connection.ping(ping);
			}
		} finally {
			conLock.readLock().unlock();
		}
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#flushCache()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getType()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getManager()
	 */
	@Override
	public AbstractXServerManager getManager() {
		return manager;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getSendingRecordSecondPackageCount()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getSendinglastSecondPackageCount()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getReceivingRecordSecondPackageCount()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.XServer#getReceivinglastSecondPackageCount()
	 */
	@Override
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