package com.mickare.xserver.net;

import java.io.IOException;
import java.net.UnknownHostException;

import com.mickare.xserver.AbstractXServerManager;
import com.mickare.xserver.Message;
import com.mickare.xserver.XType;
import com.mickare.xserver.exceptions.NotInitializedException;

public interface XServer {

	public abstract void connect() throws UnknownHostException, IOException,
	InterruptedException, NotInitializedException;

	public abstract void setConnection(Connection con);
	
	public abstract void setReloginConnection(Connection con);
	
	public abstract boolean isConnected();
	
	public abstract void disconnect();
	
	public abstract String getName();
	
	public abstract String getHost();
	
	public abstract int getPort();
	
	public abstract String getPassword();
	
	public abstract boolean sendMessage(Message message) throws IOException;
	
	public abstract void ping(Ping ping) throws InterruptedException,
		IOException;
	
	public abstract void flushCache();
	
	public abstract XType getType();
	
	public abstract AbstractXServerManager getManager();
	
	public abstract long getSendingRecordSecondPackageCount();
	
	public abstract long getSendinglastSecondPackageCount();
	
	public abstract long getReceivingRecordSecondPackageCount();
	
	public abstract long getReceivinglastSecondPackageCount();
	
}