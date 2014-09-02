package de.mickare.xserver.net;

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

import de.mickare.xserver.AbstractXServerManagerObj;
import de.mickare.xserver.events.XServerDisconnectEvent;
import de.mickare.xserver.exceptions.NotInitializedException;

public class ConnectionObj implements Connection
{

	private final static int CAPACITY = 8192;
	private final static int SOCKET_TIMEOUT = 5000;

	private ReentrantReadWriteLock statusLock = new ReentrantReadWriteLock();
	private stats status = stats.connecting;

	private final String host;
	private final int port;

	private ReentrantReadWriteLock xserverLock = new ReentrantReadWriteLock();
	private XServer xserver;

	private final Socket socket;
	private final DataInputStream input;
	private final DataOutputStream output;

	private final ArrayBlockingQueue<Packet> pendingSendingPackets = new ArrayBlockingQueue<Packet>(CAPACITY, true);

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
	public ConnectionObj(SocketFactory sf, String host, int port, AbstractXServerManagerObj manager) throws UnknownHostException, IOException, InterruptedException,
			NotInitializedException
	{

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

		this.receiving.start(manager);
		this.sending.start(manager);
		//this.packetHandler.start();
	}

	/**
	 * Receive a new Connection from another Server (response to a Login
	 * Request)
	 * 
	 * @param socket
	 * @throws IOException
	 * @throws NotInitializedException
	 */
	public ConnectionObj(Socket socket, AbstractXServerManagerObj manager) throws IOException
	{


		this.host = socket.getInetAddress().getHostAddress();
		this.port = socket.getPort();
		this.socket = socket;
		this.socket.setSoTimeout(SOCKET_TIMEOUT);

		this.input = new DataInputStream(socket.getInputStream());
		this.output = new DataOutputStream(socket.getOutputStream());

		this.packetHandler = new NetPacketHandler(this, manager);
		this.receiving = new Receiving();
		this.sending = new Sending();

		this.receiving.start(manager);
		this.sending.start(manager);
		//this.packetHandler.start();
		
		
		//manager.getLogger().info("New Connection from: " + host + ":" + port);
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#ping(de.mickare.xserver.net.Ping)
	 */
	@Override
	public void ping(Ping ping) throws InterruptedException, IOException
	{
		ByteArrayOutputStream b = null;
		DataOutputStream out = null;
		try
		{
			b = new ByteArrayOutputStream();
			out = new DataOutputStream(b);
			out.writeUTF(ping.getKey());
			pendingSendingPackets.put(new Packet(PacketType.PingRequest, b.toByteArray()));
		} finally
		{
			if (out != null)
			{
				out.close();
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#isConnected()
	 */
	@Override
	public boolean isConnected()
	{
		return socket != null ? !socket.isClosed() : false;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#disconnect()
	 */
	@Override
	public void disconnect()
	{
		setStatus(stats.disconnected);
		sending.interrupt();
		receiving.interrupt();
		//packetHandler.interrupt();

		try
		{
			socket.close();
			input.close();
			output.close();
		} catch (IOException e)
		{

		} finally
		{
			if (this.xserver != null)
			{
				this.xserver.getManager().getEventHandler().callEvent(new XServerDisconnectEvent(xserver));
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#errorDisconnect()
	 */
	@Override
	public void errorDisconnect()
	{
		setStatus(stats.error);
		sending.interrupt();
		receiving.interrupt();
		//packetHandler.interrupt();

		try
		{
			socket.close();
			input.close();
			output.close();
		} catch (IOException e)
		{

		} finally
		{
			if (this.xserver != null)
			{
				this.xserver.getManager().getEventHandler().callEvent(new XServerDisconnectEvent(xserver));
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#getHost()
	 */
	@Override
	public String getHost()
	{
		return host;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#getPort()
	 */
	@Override
	public int getPort()
	{
		return port;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#send(de.mickare.xserver.net.Packet)
	 */
	@Override
	public boolean send(Packet packet)
	{
		return pendingSendingPackets.offer(packet);
	}
	
	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#sendAll(java.util.Collection)
	 */
	@Override
	public boolean sendAll(Collection<Packet> packets)
	{
		boolean result = true;
		for (Packet p : packets)
		{
			result &= send(p);
		}
		return result;
	}

	private abstract class InterruptableRunnable implements Runnable {
		private final AtomicBoolean interrupted = new AtomicBoolean(false);
		private final String name;
		
		private InterruptableRunnable(String name) {
			this.name = name;
		}

		public void start(AbstractXServerManagerObj manager) {
			manager.getThreadPool().runTask( this );
		}
		
		public boolean isInterrupted() {
			return interrupted.get();
		}
		
		public void interrupt() {
			this.interrupted.set( true );
		}

		public String getName() {
			return name;
		}
		
	}
	
	private class Sending extends InterruptableRunnable
	{

		private final AtomicLong recordSecondPackageCount = new AtomicLong(0);
		private final AtomicLong lastSecondPackageCount = new AtomicLong(0);
		
		private long lastSecond = 0;
		private long packageCount = 0;
		
		public Sending()
		{
			super("Sending Thread to (" + host + ":" + port + ")");
		}

		private void tickPacket() {
			if(System.currentTimeMillis() - lastSecond > 1000) {
				lastSecondPackageCount.set(packageCount);
				if(packageCount > recordSecondPackageCount.get()) {
					recordSecondPackageCount.set(packageCount);
				}
				packageCount = 0;
				lastSecond = System.currentTimeMillis();
			}
			packageCount++;
		}
		
		@Override
		public void run()
		{
			try
			{
				while (!isInterrupted() && isConnected())
				{

					Packet p = pendingSendingPackets.poll(1000, TimeUnit.MILLISECONDS);

					if (isInterrupted())
					{
						return;
					}

					if (p == null)
					{
						if (isLoggedIn())
						{
							new Packet(PacketType.KeepAlive, new byte[0]).writeToStream(output);
							tickPacket();
						} else
						{
							errorDisconnect();
						}
					} else
					{
						p.writeToStream(output);
						tickPacket();
					}
					p = null;

				}
			} catch (IOException | InterruptedException e)
			{
				//TODO
				//manager.getLogger().warning("Error Disconnect (" + host + ":" + port + "): " + e.getMessage() + "\n" +  MyStringUtils.stackTraceToString(e));
				errorDisconnect();
			}
			this.interrupt();
		}

	}

	private class Receiving extends InterruptableRunnable
	{

		private final AtomicLong recordSecondPackageCount = new AtomicLong(0);
		private final AtomicLong lastSecondPackageCount = new AtomicLong(0);
		
		private long lastSecond = 0;
		private long packageCount = 0;
		
		public Receiving()
		{
			super("Receiving Thread to (" + host + ":" + port + ")");
		}

		private void tickPacket() {
			if(System.currentTimeMillis() - lastSecond > 1000) {
				lastSecondPackageCount.set(packageCount);
				if(packageCount > recordSecondPackageCount.get()) {
					recordSecondPackageCount.set(packageCount);
				}
				packageCount = 0;
				lastSecond = System.currentTimeMillis();
			}
			packageCount++;
		}
		
		@Override
		public void run()
		{
			try
			{
				while (!isInterrupted() && isConnected())
				{
					packetHandler.handle(Packet.readFromSteam(input));
					tickPacket();
				}
			} catch (IOException e)
			{
				//TODO
				//manager.getLogger().warning("Error Disconnect (" + host + ":" + port + "): " + e.getMessage() + "\n" +  MyStringUtils.stackTraceToString(e));
				errorDisconnect();
			}
			this.interrupt();
		}
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#getStatus()
	 */
	@Override
	public stats getStatus()
	{
		statusLock.readLock().lock();
		try
		{
			return status;
		} finally
		{
			statusLock.readLock().unlock();
		}
	}

	protected void setStatus(stats status)
	{
		statusLock.writeLock().lock();
		try
		{
			this.status = status;
		} finally
		{
			statusLock.writeLock().unlock();
		}
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#getXserver()
	 */
	@Override
	public XServer getXserver()
	{
		xserverLock.readLock().lock();
		try
		{
			return xserver;
		} finally
		{
			xserverLock.readLock().unlock();
		}
	}

	protected void setXserver(XServer xserver)
	{
		xserverLock.writeLock().lock();
		try
		{
			this.xserver = xserver;
			xserver.setConnection(this);
		} finally
		{
			xserverLock.writeLock().unlock();
		}
	}

	protected void setReloginXserver(XServer xserver)
	{
		xserverLock.writeLock().lock();
		try
		{
			this.xserver = xserver;
			xserver.setReloginConnection(this);
		} finally
		{
			xserverLock.writeLock().unlock();
		}
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#getPendingPackets()
	 */
	@Override
	public Queue<Packet> getPendingPackets()
	{
		return new ArrayBlockingQueue<Packet>(CAPACITY, false, pendingSendingPackets);
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#isLoggedIn()
	 */
	@Override
	public boolean isLoggedIn()
	{
		return isConnected() ? stats.connected.equals(getStatus()) : false;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#isLoggingIn()
	 */
	@Override
	public boolean isLoggingIn()
	{
		return isConnected() ? stats.connecting.equals(getStatus()) : false;
	}

	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#toString()
	 */
	@Override
	public String toString() {
		return host + ":" + port;
	}
	
	//recordSecondPackageCount
	//lastSecondPackageCount
	
	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#getSendingRecordSecondPackageCount()
	 */
	@Override
	public long getSendingRecordSecondPackageCount() {
		return this.sending.recordSecondPackageCount.get();
	}
	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#getSendinglastSecondPackageCount()
	 */
	@Override
	public long getSendinglastSecondPackageCount() {
		return this.sending.lastSecondPackageCount.get();
	}
	
	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#getReceivingRecordSecondPackageCount()
	 */
	@Override
	public long getReceivingRecordSecondPackageCount() {
		return this.receiving.recordSecondPackageCount.get();
	}
	
	/* (non-Javadoc)
	 * @see de.mickare.xserver.net.Connection#getReceivinglastSecondPackageCount()
	 */
	@Override
	public long getReceivinglastSecondPackageCount() {
		return this.receiving.lastSecondPackageCount.get();
	}
	
}
