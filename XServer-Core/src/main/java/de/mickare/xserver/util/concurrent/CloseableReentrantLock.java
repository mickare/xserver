package de.mickare.xserver.util.concurrent;

import java.util.concurrent.locks.ReentrantLock;

public class CloseableReentrantLock extends ReentrantLock implements AutoCloseable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4139432594795219762L;

	public CloseableReentrantLock open() {
		this.lock();
		return this;
	}
	
	public void close() {
		this.unlock();
	}

}
