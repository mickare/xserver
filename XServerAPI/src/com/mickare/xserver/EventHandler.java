package com.mickare.xserver;

import java.util.Map;

import com.mickare.xserver.events.XServerEvent;

public interface EventHandler<T> {

	/**
	 * Get all Listeners...
	 * @return new Map
	 */
	public abstract Map<XServerListener, XServerListenerPlugin<T>> getListeners();

	/**
	 * Register a new listener...
	 * @param plugin
	 * @param lis
	 */
	public abstract void registerListener(T plugin, XServerListener lis);

	/**
	 * Unregister a old listener...
	 * @param lis
	 */
	public abstract void unregisterListener(XServerListener lis);

	/**
	 * Unregister all for a plugin listeners...
	 */
	public abstract void unregisterAll(T plugin);

	public abstract void unregisterAll(XServerListenerPlugin<T> plugin);

	/**
	 * Unregister all listeners...
	 */
	public abstract void unregisterAll();

	/**
	 * Call an Event...
	 * @param event
	 */
	public abstract XServerEvent callEvent(XServerEvent event);

	public abstract void runTask(Boolean sync, XServerListenerPlugin<T> plugin, Runnable run);
}