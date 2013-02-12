package com.mickare.xserver.socket;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.net.SocketFactory;

import com.mickare.xserver.EventHandler;
import com.mickare.xserver.Message;
import com.mickare.xserver.XServer;
import com.mickare.xserver.Exception.NotInitializedException;
import com.mickare.xserver.events.XServerErrorSendingEvent;

public class XServerSending implements Runnable {

	private final XServer server;
	
	private SocketFactory sf;
	
	
	private boolean closed = false;
	private Socket socket = null;
	private DataOutputStream streamOut = null;
	
	private final Message m;
	
	private byte[] data = new byte[0];
	
	
	public XServerSending(XServer server, Message m) throws IOException {
		this.server = server;
		this.data = m.getData();
		this.m = m;
		sf = SocketFactory.getDefault();
	}
	
	public synchronized void close() throws IOException {
		closed = true;
		if(socket != null) {
			socket.close();
		}
		if(streamOut != null) {
			streamOut.close();
		}
	}
	
	public synchronized boolean isClosed() {
		return closed;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(!isClosed()) {
			try {		
				socket = sf.createSocket(server.getHost(), server.getPort());			
				streamOut = new DataOutputStream(socket.getOutputStream());
				streamOut.writeInt(data.length);
				streamOut.write(data);
				streamOut.flush();		
			} catch (IOException e) {
				try {
					EventHandler.getInstance().callEvent(new XServerErrorSendingEvent(m));
				} catch (NotInitializedException e1) {
				}
			}
		}
			try {
				close();
			} catch (IOException e1) {
			}
		
	}
	
}
