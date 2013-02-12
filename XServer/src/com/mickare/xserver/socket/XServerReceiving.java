package com.mickare.xserver.socket;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import com.mickare.xserver.ConfigServers;
import com.mickare.xserver.EventHandler;
import com.mickare.xserver.Message;
import com.mickare.xserver.MessageFactory;
import com.mickare.xserver.XServer;
import com.mickare.xserver.Exception.Message_SenderUnknownException;
import com.mickare.xserver.Exception.NotInitializedException;
import com.mickare.xserver.Exception.NotPluginMessage;
import com.mickare.xserver.events.XServerDataDeniedEvent;

public class XServerReceiving implements Runnable {

	private boolean closed = false;
	private final Socket socket;
	private DataInputStream streamIn = null;
	
	private byte[] data = null;
	
	public XServerReceiving(Socket socket) {
		this.socket = socket;
	}
	
	public synchronized void close() throws IOException {
		closed = true;
		if(socket != null) {
			socket.close();
		}
		if(streamIn != null) {
			streamIn.close();
		}
	}
	
	public synchronized boolean isClosed() {
		return closed;
	}
	
	@Override
	public void run() {
		Message m = null;
		XServer s = null;
		try {
			if(!isClosed()) {
				streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				
				int len = streamIn.readInt();
				data = new byte[len];
				streamIn.readFully(data);
				
				m = MessageFactory.getInstance().recreateMessage(data);
						
				s = ConfigServers.getInstance().getServer(m.getSender_Name());
				s.receive(m);
		
			}
		} catch (IOException e) { 
			
		} catch (NotInitializedException e) { 
			
		} catch (NotPluginMessage e) { 
			try {
				EventHandler.getInstance().callEvent(new XServerDataDeniedEvent(data, "not plugin message"));
			} catch (NotInitializedException e1) {
			}
		} catch (Message_SenderUnknownException e) {
			try {
				EventHandler.getInstance().callEvent(new XServerDataDeniedEvent(data, "unknown sender"));
			} catch (NotInitializedException e1) {
			}
		}
		try {
			close();
		} catch (IOException e) {
		}
	}

}
