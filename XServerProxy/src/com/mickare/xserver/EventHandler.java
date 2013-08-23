package com.mickare.xserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.md_5.bungee.api.plugin.Plugin;

import com.mickare.xserver.events.XServerEvent;

public class EventHandler {

	private final HashMap<XServerListener, Plugin> listeners = new HashMap<XServerListener, Plugin>();

	private final XServerManager manager;
	private final EventBus bus;
	
	protected EventHandler(XServerManager manager) {
		this.manager = manager;
		this.bus = new EventBus(this, manager.getLogger());
	}

	/**
	 * Get all Listeners...
	 * @return new Map
	 */
	public Map<XServerListener, Plugin> getListeners() {
		return new HashMap<XServerListener, Plugin>(listeners);
	}

	/**
	 * Register a new listener...
	 * @param plugin
	 * @param lis
	 */
	public synchronized void registerListener(Plugin plugin, XServerListener lis) {
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
	public synchronized void unregisterAll(Plugin plugin) {
		for(Entry<XServerListener, Plugin> e : new HashSet<Entry<XServerListener, Plugin>>(listeners.entrySet())) {
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
            manager.getLogger().log( Level.WARNING, "Event {0} took more {1}ns to process!", new Object[]
            {
                event, elapsed
            } );
        }
        return event;
	}
	
	
	protected void runTask(Boolean sync, Plugin plugin, Runnable run) {
		/*
		boolean s = true;
		if(sync != null) {
			s = sync.booleanValue();
		}
		*/

		manager.getThreadPool().runTask(run);
		
	}
}
