package com.mickare.xserver.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.SocketFactory;

import com.mickare.xserver.AbstractXServerManagerObj;
import com.mickare.xserver.events.XServerDisconnectEvent;
import com.mickare.xserver.exceptions.NotConnectedException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.exceptions.NotLoggedInException;
import com.mickare.xserver.util.MyStringUtils;

public class ConnectionObj implements Connection {

	private final static int CAPACITY = 8192;
	private final static int SOCKET_TIMEOUT = 5000;

	private ReentrantReadWriteLock statusLock = new ReentrantReadWriteLock();
	private STATS status = STATS.connecting;

	private final String host;
	private final int port;

	private ReentrantReadWriteLock xserverLock = new ReentrantReadWriteLock();
	private XServer xserver;

	private final Socket socket;
	private final DataInputStream input;
	private final DataOutputStream output;

	private final ArrayBlockingQueue<Packet> pendingSendingPackets = new ArrayBlockingQueue<Packet>(CAPACITY, false);

	private Receiving receiving;
	private Sending sending;
	private final NetPacketHandler packetHandler;

	/**
	 * Create a new Connection to another Server (sends a Login Request)
	 * 
	 * @param sf
	 * @param host
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotInitializedException
	 */
	public ConnectionObj(SocketFactory sf, String host, int port, AbstractXServerManagerObj manager)
			throws UnknownHostException, IOException, InterruptedException, NotInitializedException {
		
		this.host = host;
		this.port = port;
		this.socket = sf.createSocket(host, port);
		this.socket.setSoTimeout(SOCKET_TIMEOUT);

		this.input = new DataInputStream(socket.getInputStream());
		this.output = new DataOutputStream(socket.getOutputStream());

		this.packetHandler = new NetPacketHandler(this, manager);
		this.receiving = new Receiving();
		this.sending = new Sending();

		this.packetHandler.sendFirstLoginRequest();

		//manager.getThreadPool().runTask(this.receiving);
		//manager.getThreadPool().runTask(this.sending);
		/*
		if (!manager.getThreadPool().runTask(this.receiving) || !manager.getThreadPool().runTask(this.sending)) {
			this.errorDisconnect();
		}
		*/
		
		this.receiving.start();
		this.sending.start();
		// this.packetHandler.start();
	}

	/**
	 * Receive a new Connection from another Server (response to a Login
	 * Request)
	 * 
	 * @param socket
	 * @throws IOException
	 * @throws NotInitializedException
	 */
	public ConnectionObj(Socket socket, AbstractXServerManagerObj manager) throws IOException {

		if(socket == null) {
			throw new IllegalArgumentException("socket is null!");
		}
		
		this.host = socket.getInetAddress().getHostAddress();
		this.port = socket.getPort();
		this.socket = socket;
		this.socket.setSoTimeout(SOCKET_TIMEOUT);

		this.input = new DataInputStream(socket.getInputStream());
		this.output = new DataOutputStream(socket.getOutputStream());

		this.packetHandler = new NetPacketHandler(this, manager);
		this.receiving = new Receiving();
		this.sending = new Sending();

		//if (!manager.getThreadPool().runTask(this.receiving) || !manager.getThreadPool().runTask(this.sending)) {
		//	this.errorDisconnect();
		//}

		this.receiving.start();
		this.sending.start();
		// this.packetHandler.start();

		// manager.getLogger().info("New Connection from: " + host + ":" +
		// port);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mickare.xserver.net.Connection#ping(com.mickare.xserver.net.Ping)
	 */
	@Override
	public void ping(Ping ping) throws InterruptedException, IOException {
		ByteArrayOutputStream b = null;
		DataOutputStream out = null;
		try {
			b = new ByteArrayOutputStream();
			out = new DataOutputStream(b);
			out.writeUTF(ping.getKey());
			pendingSendingPackets.put(new Packet(PacketType.PingRequest, b.toByteArray()));
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return socket != null ? !socket.isClosed() : false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#disconnect()
	 */
	@Override
	public void disconnect() {
		setStatus(STATS.disconnected);
		
		try {
			socket.close();
			input.close();
			output.close();
		} catch (IOException e) {

		} finally {
			xserverLock.readLock().lock();
			try {
				if (this.xserver != null) {
					this.xserver.getManager().getEventHandler().callEvent(new XServerDisconnectEvent(getXserver()));
					this.xserver = null;
				}
			} finally {
				xserverLock.readLock().unlock();
			}
			
			if(!sending.isDisconnectHalt()) {
				sending.disconnectHalt();
				sending.interrupt();
			}
			if(!receiving.isDisconnectHalt()) {
				receiving.disconnectHalt();
				receiving.interrupt();
			}
			
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#errorDisconnect()
	 */
	@Override
	public void errorDisconnect() {
		errorDisconnect(STATS.error);
	}

	@Override
	public void errorDisconnect(STATS errorstatus) {
		setStatus(errorstatus);
		
		try {
			socket.close();
			input.close();
			output.close();
		} catch (IOException e) {

		} finally {
			xserverLock.readLock().lock();
			try {
				if (this.xserver != null) {
					this.xserver.getManager().getEventHandler().callEvent(new XServerDisconnectEvent(this.xserver));
					this.xserver = null;
				}
			} finally {
				xserverLock.readLock().unlock();
			}
			
			if(!sending.isDisconnectHalt()) {
				sending.disconnectHalt();
				sending.interrupt();
			}
			if(!receiving.isDisconnectHalt()) {
				receiving.disconnectHalt();
				receiving.interrupt();
			}
		
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#getPort()
	 */
	@Override
	public int getPort() {
		return port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mickare.xserver.net.Connection#send(com.mickare.xserver.net.Packet)
	 */
	@Override
	public boolean send(Packet packet) {
		return pendingSendingPackets.offer(packet);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#sendAll(java.util.Collection)
	 */
	@Override
	public boolean sendAll(Collection<Packet> packets) {
		boolean result = true;
		for (Packet p : packets) {
			result &= send(p);
		}
		return result;
	}

	private final class Sending extends Thread {
		
		private final AtomicBoolean disconnectHalted = new AtomicBoolean(false);
		
		private final AtomicLong recordSecondPackageCount = new AtomicLong(0);
		private final AtomicLong lastSecondPackageCount = new AtomicLong(0);

		private long lastSecond = 0;
		private long packageCount = 0;

		public Sending() {
			 super("Sending Thread to (" + host + ":" + port + ")");
		}

		public void disconnectHalt() {
			disconnectHalted.set(true);
		}
		
		public boolean isDisconnectHalt() {
			return this.disconnectHalted.get();
		}

		private void tickPacket() {
			if (System.currentTimeMillis() - lastSecond > 1000) {
				lastSecondPackageCount.set(packageCount);
				if (packageCount > recordSecondPackageCount.get()) {
					recordSecondPackageCount.set(packageCount);
				}
				packageCount = 0;
				lastSecond = System.currentTimeMillis();
			}
			packageCount++;
		}

		@Override
		public void run() {
			try {
				while (!isInterrupted() && isConnected() && !disconnectHalted.get()) {

					Packet p = pendingSendingPackets.poll(1000, TimeUnit.MILLISECONDS);

				  if (isInterrupted() || disconnectHalted.get()) { return; }
					

					if (p == null) {
						if (isLoggedIn()) {
							new Packet(PacketType.KeepAlive, new byte[0]).writeToStream(output);
							tickPacket();
						} else {
							errorDisconnect();
						}
					} else {
						p.writeToStream(output);
						tickPacket();
					}
					p = null;

				}
			} catch (IOException | InterruptedException e){
				errorDisconnect();
			} catch (Exception e) {
				packetHandler.getLogger().warning("Error Disconnect (" + host + ":"
						+ port + "): " + e.getMessage() + "\n" +
						MyStringUtils.stackTraceToString(e));
				errorDisconnect();
			}
			// this.interrupt();
		}

	}

	private final class Receiving extends Thread {

		private final AtomicBoolean disconnectHalted = new AtomicBoolean(false);
		
		private final AtomicLong recordSecondPackageCount = new AtomicLong(0);
		private final AtomicLong lastSecondPackageCount = new AtomicLong(0);

		private long lastSecond = 0;
		private long packageCount = 0;

		public Receiving() {
			 super("Receiving Thread to (" + host + ":" + port + ")");
		}


		public void disconnectHalt() {
			this.disconnectHalted.set(true);
		}

		public boolean isDisconnectHalt() {
			return this.disconnectHalted.get();
		}

		private void tickPacket() {
			if (System.currentTimeMillis() - lastSecond > 1000) {
				lastSecondPackageCount.set(packageCount);
				if (packageCount > recordSecondPackageCount.get()) {
					recordSecondPackageCount.set(packageCount);
				}
				packageCount = 0;
				lastSecond = System.currentTimeMillis();
			}
			packageCount++;
		}

		@Override
		public void run() {
			try {
				while (!isInterrupted() && isConnected() && !disconnectHalted.get()) {
					packetHandler.handle(Packet.readFromSteam(input));
					tickPacket();
				}
			} catch (NotConnectedException | NotLoggedInException | IOException e){
				errorDisconnect();
			} catch (Exception e) {
				packetHandler.getLogger().warning("Error Disconnect (" + host + ":"
						+ port + "): " + e.getMessage() + "\n" +
						MyStringUtils.stackTraceToString(e));
				errorDisconnect();
			}
			// this.interrupt();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#getStatus()
	 */
	@Override
	public STATS getStatus() {
		statusLock.readLock().lock();
		try {
			return status;
		} finally {
			statusLock.readLock().unlock();
		}
	}

	protected void setStatus(STATS status) {
		statusLock.writeLock().lock();
		try {
			this.status = status;
		} finally {
			statusLock.writeLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#getXserver()
	 */
	@Override
	public XServer getXserver() {
		xserverLock.readLock().lock();
		try {
			return xserver;
		} finally {
			xserverLock.readLock().unlock();
		}
	}

	protected void setXserver(XServer xserver) {
		xserverLock.writeLock().lock();
		try {
			this.xserver = xserver;
			xserver.setConnection(this);
		} finally {
			xserverLock.writeLock().unlock();
		}
	}

	protected void setReloginXserver(XServer xserver) {
		xserverLock.writeLock().lock();
		try {
			this.xserver = xserver;
			xserver.setReloginConnection(this);
		} finally {
			xserverLock.writeLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#getPendingPackets()
	 */
	@Override
	public Queue<Packet> getPendingPackets() {
		return new ArrayBlockingQueue<Packet>(CAPACITY, false, pendingSendingPackets);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#isLoggedIn()
	 */
	@Override
	public boolean isLoggedIn() {
		return isConnected() ? STATS.connected.equals(getStatus()) : false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#isLoggingIn()
	 */
	@Override
	public boolean isLoggingIn() {
		return isConnected() ? STATS.connecting.equals(getStatus()) : false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mickare.xserver.net.Connection#toString()
	 */
	@Override
	public String toString() {
		return host + ":" + port;
	}

	// recordSecondPackageCount
	// lastSecondPackageCount

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mickare.xserver.net.Connection#getSendingRecordSecondPackageCount()
	 */
	@Override
	public long getSendingRecordSecondPackageCount() {
		return this.sending.recordSecondPackageCount.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mickare.xserver.net.Connection#getSendinglastSecondPackageCount()
	 */
	@Override
	public long getSendinglastSecondPackageCount() {
		return this.sending.lastSecondPackageCount.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mickare.xserver.net.Connection#getReceivingRecordSecondPackageCount()
	 */
	@Override
	public long getReceivingRecordSecondPackageCount() {
		return this.receiving.recordSecondPackageCount.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mickare.xserver.net.Connection#getReceivinglastSecondPackageCount()
	 */
	@Override
	public long getReceivinglastSecondPackageCount() {
		return this.receiving.lastSecondPackageCount.get();
	}

}
