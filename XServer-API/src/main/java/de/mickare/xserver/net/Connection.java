package de.mickare.xserver.net;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Queue;

public interface Connection extends AutoCloseable, Closeable {
	
	public enum STATE {
		DISCONNECTED,
		CONNECTING_CLIENT,
		CONNECTION_SERVER,
		CONNECTED,
		ERROR		
	}
	
	public abstract void ping( Ping ping ) throws InterruptedException, IOException;
	
	public abstract boolean isOpened();
	
	public abstract String getHost();
	
	public abstract int getPort();
	
	public abstract boolean send( Packet packet );
	
	public abstract boolean sendAll( Collection<Packet> packets );
	
	public abstract STATE getStatus();
	
	public abstract XServer getXServer();
	
	public abstract Queue<Packet> getPendingPackets();
		
	public abstract String toString();
	
	public abstract long getSendingRecordSecondPackageCount();
	
	public abstract long getSendinglastSecondPackageCount();
	
	public abstract long getReceivingRecordSecondPackageCount();
	
	public abstract long getReceivinglastSecondPackageCount();
	
}