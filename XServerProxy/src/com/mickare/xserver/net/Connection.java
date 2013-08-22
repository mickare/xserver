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

import javax.net.SocketFactory;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.exceptions.NotInitializedException;

public class Connection {

	private final static int CAPACITY = 512;
	
	private stats status = stats.connecting;
	
	private final String host;
	private final int port;
	
	private XServer xserver;
	
	private final Socket socket;
	private final DataInputStream input;
	private final DataOutputStream output;
	
	
	private final ArrayBlockingQueue<Packet> pendingPackets = new ArrayBlockingQueue<Packet>(CAPACITY);
	
	private Receiving receiving;
	private Sending sending;
	
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
		sendFirstLoginRequest();
	}
	
	/**
	 * Receive a new Connection from another Server (response to a Login Request)
	 * @param socket
	 * @throws IOException
	 * @throws NotInitializedException 
	 */
	public Connection(Socket socket) throws IOException, NotInitializedException {
		
		
		this.host = socket.getInetAddress().getHostAddress();
		this.port = socket.getPort();
		this.socket = socket;
		input = new DataInputStream(socket.getInputStream());
		output = new DataOutputStream(socket.getOutputStream());
		receiving = new Receiving(this);
		sending = new Sending(this);
		XServerManager.getInstance().getThreadPool().runTask(receiving);
		XServerManager.getInstance().getThreadPool().runTask(sending);
	}

	private void sendFirstLoginRequest() throws IOException, InterruptedException, NotInitializedException {
		sendLoginRequest(Packet.Types.LoginRequest);
	}
	
	protected void sendAcceptedLoginRequest() throws IOException, InterruptedException, NotInitializedException {
		sendLoginRequest(Packet.Types.LoginAccepted);
	}
	
	private void sendLoginRequest(Packet.Types type) throws IOException, InterruptedException, NotInitializedException {
		ByteArrayOutputStream b = null; 
		DataOutputStream out = null;
		try {
			b = new ByteArrayOutputStream();
			out = new DataOutputStream(b);
			out.writeUTF(XServerManager.getInstance().getHomeServer().getName());
			out.writeUTF(XServerManager.getInstance().getHomeServer().getPassword());
		pendingPackets.put(new Packet(type, b.toByteArray()));
		} finally {
			if(out != null) {
				out.close();
			}
		}
	}
	
	public void ping(Ping ping) throws InterruptedException, IOException {
		ByteArrayOutputStream b = null; 
		DataOutputStream out = null;
		try {
			b = new ByteArrayOutputStream();
			out = new DataOutputStream(b);
			out.writeUTF(ping.getKey());
			pendingPackets.put(new Packet(Packet.Types.PingRequest, b.toByteArray()));
		} finally {
			if(out != null) {
				out.close();
			}
		}
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
	
	public boolean send(Packet packet) {
		return pendingPackets.offer(packet);
	}
	
	public boolean sendAll(Collection<Packet> packets) {
		boolean result = true;
		for(Packet p : packets) {
			result &= pendingPackets.offer(p);
		}
		return result;
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
			Packet p = null;
			while(running && con.isConnected()) {
				try {
					p = con.pendingPackets.poll(1000, TimeUnit.MILLISECONDS);

					if(!running) {
						return;
					}
					
					if(p == null) {
						new Packet(Packet.Types.KeepAlive, new byte[0]).writeToStream(con.output);
					} else {
						p.writeToStream(con.output);
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
	
	protected void setReloginXserver(XServer xserver) throws NotInitializedException {
		this.xserver = xserver;
		xserver.setReloginConnection(this);
	}
		
	public Queue<Packet> getPendingPackets() {
		return new ArrayBlockingQueue<Packet>(CAPACITY, false, pendingPackets);
	}
		
	
	public boolean isLoggedIn() {
		return this.status.equals(stats.connected);
	}
	
}
