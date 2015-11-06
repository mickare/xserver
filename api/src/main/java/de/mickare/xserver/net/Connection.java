package de.mickare.xserver.net;

public interface Connection extends AutoCloseable {
	
	public abstract boolean isClosed();
	
	public abstract String getHost();
	
	public abstract int getPort();
	
	public abstract XServer getXServer();
	
	public abstract String toString();
	
	public abstract long getSendingRecordSecondPackageCount();
	
	public abstract long getSendinglastSecondPackageCount();
	
	public abstract long getReceivingRecordSecondPackageCount();
	
	public abstract long getReceivinglastSecondPackageCount();
	
}