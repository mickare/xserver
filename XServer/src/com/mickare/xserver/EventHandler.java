package com.mickare.xserver;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.mickare.xserver.events.XServerEvent;

public class EventHandler {
	
	private final HashMap<XServerListener, JavaPlugin> listeners = new HashMap<XServerListener, JavaPlugin>();

	private final XServerManager manager;
	private final EventBus bus;
	
	protected EventHandler(XServerManager manager) {
		this.manager = manager;
		bus = new EventBus(this, manager.getLogger());
	}

	/**
	 * Get all Listeners...
	 * @return new Map
	 */
	public Map<XServerListener, JavaPlugin> getListeners() {
		return new HashMap<XServerListener, JavaPlugin>(listeners);
	}

	/**
	 * Register a new listener...
	 * @param plugin
	 * @param lis
	 */
	public synchronized void registerListener(JavaPlugin plugin, XServerListener lis) {
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
	
	
	protected void runTask(Boolean sync, JavaPlugin plugin, Runnable run) {
		boolean s = true;
		if(sync != null) {
			s = sync.booleanValue();
		}
		if (s) {
            Bukkit.getScheduler().runTask(plugin, run);
	    } else {
	    	Bukkit.getScheduler().runTaskAsynchronously(plugin, run);
	    }
	}
	
}
