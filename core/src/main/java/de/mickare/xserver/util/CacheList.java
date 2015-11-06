package de.mickare.xserver.util;

import java.util.Collection;
import java.util.LinkedList;

public class CacheList<V> extends LinkedList<V> {

	private static final long serialVersionUID = -3646681278070557865L;
	
	private final int maxCapacity;

    public CacheList(int maxCapacity) {
    	super();
        if (maxCapacity < 1) {
            throw new IllegalArgumentException(
                    "Capacity must be greater than 0");
        }
        this.maxCapacity = maxCapacity;
        
    }
    
	@Override
	public boolean add(V e) {
		boolean result = super.add(e);
		if(size() > maxCapacity) {
			super.pollLast();
		}
		return result;
	}

	@Override
	public boolean addAll(Collection<? extends V> c) {
		boolean result = super.addAll(c);
		while(size() > maxCapacity) {
			super.pollLast();
		}
		return result;
	}

	@Override
	public boolean addAll(int index, Collection<? extends V> c) {
		boolean result = super.addAll(index, c);
		while(size() > maxCapacity) {
			super.pollLast();
		}
		return result;
	}

	@Override
	public V set(int index, V element) {
		V result = super.set(index, element);
		while(size() > maxCapacity) {
			super.pollLast();
		}
		return result;
	}

	@Override
	public void add(int index, V element) {
		super.set(index, element);
		while(size() > maxCapacity) {
			super.pollLast();
		}
	}
	
	@Override
	public void push(V e) {
		super.push(e);
		while(size() > maxCapacity) {
			super.pollLast();
		}
	}
	
	@Override
	public void addFirst(V e) {
		super.addFirst(e);
		while(size() > maxCapacity) {
			super.pollLast();
		}
	}
	
	@Override
	public void addLast(V e) {
		super.addFirst(e);
		while(size() > maxCapacity) {
			super.pollLast();
		}
	}
	
	@Override
	public boolean offer(V e) {
		boolean result = super.offer(e);
		while(size() > maxCapacity) {
			super.pollLast();
		}
		return result;
	}

	
	@Override
	public boolean offerLast(V e) {
		boolean result = super.offerLast(e);
		while(size() > maxCapacity) {
			super.pollLast();
		}
		return result;
	}

	
	@Override
	public boolean offerFirst(V e) {
		boolean result = super.offerFirst(e);
		while(size() > maxCapacity) {
			super.pollLast();
		}
		return result;
	}


	
}
