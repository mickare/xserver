package com.mickare.xserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mickare.xserver.events.XServerEvent;

public abstract class EventHandler<T> {
	
	private final HashMap<XServerListener, T> listeners = new HashMap<XServerListener, T>();

	private final Logger logger;
	private final EventBus<T> bus;
	
	protected EventHandler(Logger logger) {
		this.logger = logger;
		bus = new EventBus<T>(this, logger);
	}

	/**
	 * Get all Listeners...
	 * @return new Map
	 */
	public Map<XServerListener, T> getListeners() {
		return new HashMap<XServerListener, T>(listeners);
	}

	/**
	 * Register a new listener...
	 * @param plugin
	 * @param lis
	 */
	public synchronized void registerListener(T plugin, XServerListener lis) {
		listeners.put(lis, plugin);
		bus.register(lis, plugin);
	}

	/**
	 * Unregister a old listener...
	 * @param lis
	 */
	public synchronized void unregisterListener(XServerListener lis) {
		bus.unregister(lis);
		listeners.remove(lis);
	}

	/**
	 * Unregister all for a plugin listeners...
	 */
	public synchronized void unregisterAll(T plugin) {
		for(Entry<XServerListener, T> e : new HashSet<Entry<XServerListener, T>>(listeners.entrySet())) {
			if(e.getValue() == plugin) {
				bus.unregister(e.getKey());
				listeners.remove(e.getKey());
			}
		}
	}
	
	/**
	 * Unregister all listeners...
	 */
	public synchronized void unregisterAll() {
		for(XServerListener lis : listeners.keySet()) {
			bus.unregister(lis);
		}
		listeners.clear();
	}
	
	/**
	 * Call an Event...
	 * @param event
	 */
	public synchronized XServerEvent callEvent(final XServerEvent event) {
		
		if(event == null) {
			throw new IllegalArgumentException("event can't be null" );
		}

        long start = System.nanoTime();
        bus.post( event );
        event.postCall();

        long elapsed = start - System.nanoTime();
        if ( elapsed > 250000 )
        {
        	this.logger.log( Level.WARNING, "Event {0} took more {1}ns to process!", new Object[]
            {
                event, elapsed
            } );
        }
        return event;
	}
	
	
	protected abstract void runTask(Boolean sync, T plugin, Runnable run);
	
}
