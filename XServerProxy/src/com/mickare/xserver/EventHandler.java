package com.mickare.xserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.md_5.bungee.api.plugin.Plugin;

import com.mickare.xserver.events.XServerEvent;
import com.mickare.xserver.exceptions.NotInitializedException;

public class EventHandler {

	private final HashMap<XServerListener, Plugin> listeners = new HashMap<XServerListener, Plugin>();

	protected EventHandler() {
	}

	/**
	 * Get all Listeners...
	 * 
	 * @return new Map
	 */
	public Map<XServerListener, Plugin> getListeners() {
		return new HashMap<XServerListener, Plugin>(listeners);
	}

	/**
	 * Register a new listener...
	 * 
	 * @param plugin
	 * @param lis
	 */
	public synchronized void registerListener(Plugin plugin, XServerListener lis) {
		listeners.put(lis, plugin);
	}

	/**
	 * Unregister a old listener...
	 * 
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
	 * 
	 * @param plugin
	 */
	public synchronized void unregisterAll(Plugin plugin) {
		for (XServerListener lis : getListeners().keySet()) {
			if (listeners.get(lis).equals(plugin)) {
				listeners.remove(lis);
			}
		}
	}

	private boolean isInStringHashSet(HashSet<String> list, String search) {
		for (String text : list) {
			if (text.equals(search)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Call an Event...
	 * 
	 * @param event
	 * @throws NotInitializedException
	 */
	public synchronized void callEvent(final XServerEvent event)
			throws NotInitializedException {
		for (XServerListener lis : listeners.keySet()) {

			HashSet<String> usedMethods = new HashSet<String>();

			for (Method m : lis.getClass().getMethods()) {

				if (!isInStringHashSet(usedMethods, m.getName())) {

					Method sm = null;
					try {
						sm = lis.getClass().getMethod(m.getName(),
								event.getClass());
						usedMethods.add(m.getName());
					} catch (NoSuchMethodException nsme) {
						sm = null;
					} catch (SecurityException se) {
						sm = null;
					}

					if (sm != null) {

						// XEventHandler a =
						// sm.getAnnotation(XEventHandler.class);

						XServerManager.getInstance().getThreadPool()
								.runTask(new runEventWrapper(lis, event, sm));

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
