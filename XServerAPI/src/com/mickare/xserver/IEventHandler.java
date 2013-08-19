package com.mickare.xserver;

import com.mickare.xserver.events.XServerEvent;
import com.mickare.xserver.exceptions.NotInitializedException;

public interface IEventHandler {
	
	/**
	 * Unregister a old listener...
	 * @param lis
	 */
	public abstract void unregisterListener(XServerListener lis);

	/**
	 * Unregister all listeners...
	 */
	public abstract void unregisterAll();
	
	/**
	 * Call an Event...
	 * @param event
	 */
	public abstract void callEvent(final XServerEvent event) throws NotInitializedException;


}
