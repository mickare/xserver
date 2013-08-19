package com.mickare.xserver;

import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;

import com.mickare.xserver.exceptions.InvalidConfigurationException;
import com.mickare.xserver.util.MySQL;

public class ProxyXServerManager extends XServerManager {

	protected ProxyXServerManager(String servername, Logger logger,
			MySQL connection) throws InvalidConfigurationException {
		
		super(servername, logger, connection, new EventHandler());
				
	}

	@Override
	public void stopServer() {
		ProxyServer.getInstance().stop();
	}

}
