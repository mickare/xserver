package de.mickare.xserver.util.concurrent;

import java.util.concurrent.locks.Lock;

public interface CloseableLock extends Lock, AutoCloseable {

	public CloseableLock open();
	
	public void close();
	
}
