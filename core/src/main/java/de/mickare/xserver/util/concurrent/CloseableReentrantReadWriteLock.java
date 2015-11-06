package de.mickare.xserver.util.concurrent;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CloseableReentrantReadWriteLock implements CloseableReadWriteLock {

	private final CloseableLock readLock;
	private final CloseableLock writeLock;

	private final ReentrantReadWriteLock original;
	
	private CloseableReentrantReadWriteLock(ReentrantReadWriteLock original) {
		this.original = original;
		this.readLock = new WrapperLock(this.original.readLock());
		this.writeLock = new WrapperLock(this.original.writeLock());
	}
	
	public CloseableReentrantReadWriteLock() {
		this(new ReentrantReadWriteLock());
	}
	
	public CloseableReentrantReadWriteLock(boolean fair) {
		this(new ReentrantReadWriteLock(fair));
	}
	
	public CloseableLock readLock() {
		return this.readLock;
	}

	public CloseableLock writeLock() {
		return this.writeLock;
	}
	
}
