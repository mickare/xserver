package de.mickare.xserver.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;

import de.mickare.xserver.XServerManager;
import de.mickare.xserver.Message;
import de.mickare.xserver.XGroup;
import de.mickare.xserver.XType;
import de.mickare.xserver.exceptions.NotInitializedException;

public interface XServer {
	
	/**
	 * Returns the connection status
	 * @return true if connected, otherwise false
	 */
	public abstract boolean isConnected();
	
	/**
	 * Disconnect from this server
	 */
	public abstract void disconnect();
	
	/**
	 * Get the Server Name
	 * @return name
	 */
	public abstract String getName();
	
	/**
	 * Get the host/ip address
	 * @return host
	 */
	public abstract String getHost();
	
	/**
	 * Get the port
	 * @return port
	 */
	public abstract int getPort();
	
	/**
	 * Get the md5-encrypted password that is needed to login to this server
	 * @return
	 */
	public abstract String getPassword();
	
	/**
	 * Send a data Message to this server
	 * @param message
	 * @return true, if queued for sending into connection (= did send); otherwise false (= message cached)
	 * @throws IOException
	 */
	public abstract boolean sendMessage(Message message) throws IOException;
	
	/**
	 * Ping this server with a new Ping
	 * @param ping that is used to verify this ping
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public abstract void ping(Ping ping) throws InterruptedException,
		IOException;
	
	/**
	 * Send cached packets = queue packets into connection
	 */
	public abstract void flushCache();
	
	/**
	 * Get the type of this server
	 * @return Server Type
	 */
	public abstract XType getType();
	
	/**
	 * Get the Current Server Manager Object
	 * @return
	 */
	public abstract XServerManager getManager();
	
	/**
	 * Get the record number of packages sended in one second
	 * @return number of packages
	 */
	public abstract long getSendingRecordSecondPackageCount();
	
	/**
	 * Get the number of packages sended in the last one second
	 * @return number of packages
	 */
	public abstract long getSendinglastSecondPackageCount();
	
	/**
	 * Get the record number of packages received in one second
	 * @return number of packages
	 */
	public abstract long getReceivingRecordSecondPackageCount();
	
	/**
	 * Get the number of packages received in the last one second
	 * @return number of packages
	 */
	public abstract long getReceivinglastSecondPackageCount();
	
	public abstract Set<XGroup> getGroups();

	boolean hasGroup( XGroup group );
	
	boolean isDeprecated();
	
	XServer getCurrentXServer();
	
}