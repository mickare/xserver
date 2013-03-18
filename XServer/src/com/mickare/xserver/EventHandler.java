package com.mickare.xserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;

import com.mickare.xserver.Exception.NotInitializedException;
import com.mickare.xserver.annotations.XEventHandler;

import com.mickare.xserver.events.XServerEvent;

public class EventHandler {

	private static EventHandler instance = null;
	
	public static EventHandler getInstance() throws NotInitializedException {
		if(instance == null) {
			throw new NotInitializedException("EventHandler not initialized!");
		}
		return instance;
	}
	
	protected static void initialize(JavaPlugin plugin) {
		instance = new EventHandler(plugin);		
	}
	
	
	private final HashMap<XServerListener, JavaPlugin> listeners = new HashMap<XServerListener, JavaPlugin>();

	private final JavaPlugin plugin;
	
	private EventHandler(JavaPlugin plugin) {
		this.plugin = plugin;
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
	public synchronized void registerListener(JavaPlugin plugin,
			XServerListener lis) {
		listeners.put(lis, plugin);
	}

	/**
	 * Unregister a old listener...
	 * @param lis
	 */
	public synchronized void unregisterListener(XServerListener lis) {
		listeners.remove(lis);
	}

	/**
	 * Unregister all listeners...
	 */
	public synchronized void unregisterAll() {
		listeners.clear();
	}

	/**
	 * Unregister all listeners of a plugin...
	 * @param plugin
	 */
	public synchronized void unregisterAll(JavaPlugin plugin) {
		for (XServerListener lis : getListeners().keySet()) {
			if (listeners.get(lis).equals(plugin)) {
				listeners.remove(lis);
			}
		}
	}

	private boolean isInStringHashSet(HashSet<String> list, String search) {
		for(String text : list) {
			if(text.equals(search)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Call an Event...
	 * @param event
	 */
	public synchronized void callEvent(final XServerEvent event) {
		for (XServerListener lis : listeners.keySet()) {
			
			HashSet<String> usedMethods = new HashSet<String>();

			for (Method m : lis.getClass().getMethods()) {

				if(!isInStringHashSet(usedMethods, m.getName())) {
					
				
					Method sm = null;
					try {
						sm = lis.getClass()
								.getMethod(m.getName(), event.getClass());
						usedMethods.add(m.getName());
					} catch (NoSuchMethodException nsme) {
						sm = null;
					} catch (SecurityException se) {
						sm = null;
					}
	
					if (sm != null) {
	
						boolean sync = true;
	
						XEventHandler a = sm.getAnnotation(XEventHandler.class);
	
						if (a != null) {
							sync = a.sync();
						}
	
						runEventWrapper rew = new runEventWrapper(lis, event, sm);
	
						if (sync) {
							plugin.getServer().getScheduler()
									.runTask(listeners.get(lis), rew);
						} else {
							plugin.getServer().getScheduler()
									.runTaskAsynchronously(listeners.get(lis), rew);
						}
					}
				}
			}
		}
	}

	private static class runEventWrapper implements Runnable {

		private final XServerEvent event;
		private final XServerListener lis;
		private final Method m;

		public runEventWrapper(final XServerListener lis,
				final XServerEvent event, final Method m) {
			this.lis = lis;
			this.event = event;
			this.m = m;
		}

		@Override
		public void run() {
			try {
				m.invoke(lis, event);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.getCause().printStackTrace();
			}
		}

	}

}
