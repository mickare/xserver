package de.mickare.xserver.util.concurrent;

import java.util.concurrent.locks.ReadWriteLock;

public interface CloseableReadWriteLock extends ReadWriteLock {

	public CloseableLock readLock();

	public CloseableLock writeLock();

}
