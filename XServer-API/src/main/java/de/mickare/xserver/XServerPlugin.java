package de.mickare.xserver;

import java.util.logging.Logger;

public interface XServerPlugin {
	
	public Logger getLogger();
	
	public void shutdownServer();

	public AbstractXServerManager getManager();
	
	public long getAutoReconnectTime();
	public XType getHomeType();
	
}
