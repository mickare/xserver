package com.mickare.xserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mickare.xserver.events.XServerEvent;

public abstract class EventHandlerObj<T> implements EventHandler<T> {
	
	private final HashMap<XServerListener, XServerListenerPlugin<T>> listeners = new HashMap<XServerListener, XServerListenerPlugin<T>>();

	private final Logger logger;
	private final EventBus<T> bus;
	
	protected EventHandlerObj(Logger logger) {
		this.logger = logger;
		bus = new EventBus<T>(this, logger);
	}
	
	protected XServerListenerPlugin<T> getListPlugin(T original) {
		for(XServerListenerPlugin<T> lp : listeners.values()) {
			if(lp.getPlugin() == original) {
				return lp;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.mickare.xserver.EventHandler#getListeners()
	 */
	@Override
	public Map<XServerListener, XServerListenerPlugin<T>> getListeners() {
		return new HashMap<XServerListener, XServerListenerPlugin<T>>(listeners);
	}

	/* (non-Javadoc)
	 * @see com.mickare.xserver.EventHandler#registerListener(T, com.mickare.xserver.XServerListener)
	 */
	@Override
	public abstract void registerListener(T plugin, XServerListener lis);
	
	protected synchronized void registerListener(XServerListenerPlugin<T> plugin, XServerListener lis) {
		listeners.put(lis, plugin);
		bus.register(lis, plugin);
	}

	/* (non-Javadoc)
	 * @see com.mickare.xserver.EventHandler#unregisterListener(com.mickare.xserver.XServerListener)
	 */
	@Override
	public synchronized void unregisterListener(XServerListener lis) {
		bus.unregister(lis);
		listeners.remove(lis);
	}

	/* (non-Javadoc)
	 * @see com.mickare.xserver.EventHandler#unregisterAll(T)
	 */
	@Override
	public synchronized void unregisterAll(T plugin) {
		XServerListenerPlugin<T> lp = getListPlugin(plugin);
		if(lp != null) {
			unregisterAll(lp);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.mickare.xserver.EventHandler#unregisterAll(com.mickare.xserver.XServerListenerPlugin)
	 */
	@Override
	public synchronized void unregisterAll(XServerListenerPlugin<T> plugin) {
		for(Entry<XServerListener, XServerListenerPlugin<T>> e : new HashSet<Entry<XServerListener, XServerListenerPlugin<T>>>(listeners.entrySet())) {
			if(e.getValue() == plugin) {
				bus.unregister(e.getKey());
				listeners.remove(e.getKey());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.mickare.xserver.EventHandler#unregisterAll()
	 */
	@Override
	public synchronized void unregisterAll() {
		for(XServerListener lis : listeners.keySet()) {
			bus.unregister(lis);
		}
		listeners.clear();
	}
	
	/* (non-Javadoc)
	 * @see com.mickare.xserver.EventHandler#callEvent(com.mickare.xserver.events.XServerEvent)
	 */
	@Override
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
	
	
	public abstract void runTask(Boolean sync, XServerListenerPlugin<T> plugin, Runnable run);
	
}
