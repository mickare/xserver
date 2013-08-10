package com.mickare.xserver.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;

import javax.net.SocketFactory;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.CacheMap;
import com.mickare.xserver.util.Encryption;

public class Connection {

	private stats status = stats.connecting;
	
	private final String host;
	private final int port;
	
	private XServer xserver;
	
	private final Socket socket;
	private final DataInputStream input;
	private final DataOutputStream output;
	
	private final ArrayBlockingQueue<Packet> sendingMessages = new ArrayBlockingQueue<Packet>(256);
	
	private Receiving receiving;
	private Sending sending;
	
	// Response Time in miliseconds.
	private int lastping = Integer.MAX_VALUE;
	
	private final CacheMap<String, Long> pings = new CacheMap<String, Long>(10);
	
	public enum stats {
		disconnected, connecting, connected, error
	}
	
	/**
	 * Create a new Connection to another Server (sends a Login Request)
	 * @param sf
	 * @param host
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NotInitializedException 
	 */
	public Connection(SocketFactory sf, String host, int port) throws UnknownHostException, IOException, InterruptedException, NotInitializedException {
		this.host = host;
		this.port = port;
		socket = sf.createSocket(host, port);
		input = new DataInputStream(socket.getInputStream());
		output = new DataOutputStream(socket.getOutputStream());
		receiving = new Receiving(this);
		sending = new Sending(this);
		XServerManager.getInstance().getThreadPool().runTask(receiving);
		XServerManager.getInstance().getThreadPool().runTask(sending);
		sendLoginRequest();
	}
	
	/**
	 * Receive a new Connection from another Server (response to a Login Request)
	 * @param socket
	 * @throws IOException
	 * @throws NotInitializedException 
	 */
	public Connection(Socket socket) throws IOException, NotInitializedException {
		this.host = ((InetSocketAddress)socket.getRemoteSocketAddress()).getHostName();
		this.port = ((InetSocketAddress)socket.getRemoteSocketAddress()).getPort();
		this.socket = socket;
		input = new DataInputStream(socket.getInputStream());
		output = new DataOutputStream(socket.getOutputStream());
		receiving = new Receiving(this);
		sending = new Sending(this);
		XServerManager.getInstance().getThreadPool().runTask(receiving);
		XServerManager.getInstance().getThreadPool().runTask(sending);
	}

	private void sendLoginRequest() throws IOException, InterruptedException, NotInitializedException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		out.writeUTF(XServerManager.getInstance().getHomeServer().getName());
		out.writeUTF(XServerManager.getInstance().getHomeServer().getPassword());
		out.close();
		sendingMessages.put(new Packet(Packet.Types.LoginRequest, b.toByteArray()));
		sendingMessages.notifyAll();
	}
	
	public boolean isConnected() {
		return socket != null ? !socket.isClosed() : false;
	}
	
	public void disconnect() {
		setStatus(stats.disconnected);
		receiving.stop();
		sending.stop();
		try {
			socket.close();
			input.close();
			output.close();
		} catch (IOException e) {
			
		}
	}
	
	public void errorDisconnect() {
		setStatus(stats.error);
		receiving.stop();
		sending.stop();
		try {
			socket.close();
			input.close();
			output.close();
		} catch (IOException e) {
			
		}
	}
	
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
	
	public void send(Packet packet) throws InterruptedException {
		sendingMessages.put(packet);
		sendingMessages.notifyAll();
	}
	
	private static class Sending implements Runnable {

		private final Connection con;
		private volatile boolean running = true;
		
		public Sending(Connection con) {
			this.con = con;
		}
		
		public void stop() {
			running = false;
		}
		
		@Override
		public void run() {
			while(running && con.isConnected()) {
				try {
					con.sendingMessages.wait(1000);
					
					if(con.sendingMessages.isEmpty()) {
						new Packet(Packet.Types.KeepAlive, new byte[0]).writeToStream(con.output);
					} else {
						for(Packet p : con.sendingMessages) {
							p.writeToStream(con.output);
						}
					}
					
				} catch (IOException | InterruptedException e) {
					running = false;
					con.errorDisconnect();
				}
			}
		}

	}
	
	private static class Receiving implements Runnable {

		private final Connection con;
		private volatile boolean running = true;
		
		public Receiving(Connection con) {
			this.con = con;
		}
		
		public void stop() {
			running = false;
		}
		
		@Override
		public void run() {
			while(running && con.isConnected()) {
				try {
					NetPacketHandler.handle(con, con.input);
				} catch (IOException e) {
					running = false;
					con.errorDisconnect();
				}
			}
		}
	}
	
	/**
	 * Creates a new Ping Timestamp
	 * @return HashKey for Ping
	 */
	public String createPing() {
		synchronized(pings) {
			String key = Encryption.MD5(String.valueOf(Math.random()));
			pings.put(key, System.currentTimeMillis());
			return key;
		}
	} 
	
	public void returningPing(String key) {
		synchronized(pings) {
			if(pings.containsKey(key)) {
				setLastping((int) (System.currentTimeMillis() - pings.get(key)));
			}
		}
	}

	public synchronized int getLastping() {
		return lastping;
	}

	private synchronized void setLastping(int lastping) {
		this.lastping = lastping;
	}

	public stats getStatus() {
		return status;
	}

	protected void setStatus(stats status) {
		this.status = status;
	}

	public XServer getXserver() {
		return xserver;
	}

	protected void setXserver(XServer xserver) {
		this.xserver = xserver;
		xserver.setConnection(this);
	}
	
}
