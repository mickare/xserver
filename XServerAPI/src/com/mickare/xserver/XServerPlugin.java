package com.mickare.xserver;

import java.util.logging.Logger;

public interface XServerPlugin<T> {
	
	public Logger getLogger();
	
	public void shutdownServer();

	public AbstractXServerManager<T> getManager();
	
}
