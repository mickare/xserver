package com.mickare.xserver.net;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.CacheMap;
import com.mickare.xserver.util.Encryption;

public abstract class Ping {
	
	
	private static CacheMap<String, Ping> pending = new CacheMap<String, Ping>(40);

	private static int rollingnumber = 0;
	
	private static int getNextRollingNumber() {
		if(rollingnumber == Integer.MAX_VALUE) {
			rollingnumber = Integer.MIN_VALUE;
		}
		return rollingnumber++;
	}
	
	public static void addPendingPing(Ping ping) {
		synchronized(pending) {
			if(ping.getStarted() == -1) {
				pending.put(ping.key, ping);
			}
		}
	}
	
	public static void receive(String key, XServer server) {
		Ping p = null;
		synchronized(pending) {
			p = pending.get(key);
		}
		if(p != null) {
			p.receive(server);
		}
	}

	
	private final String key;
	
	private boolean resultprinted = false;
	
	private long started = -1;
	private final long timeout = 2000;
	private final Map<XServer, Long> responses = Collections.synchronizedMap(new HashMap<XServer, Long>());
	private final Set<XServer> waiting = Collections.synchronizedSet(new HashSet<XServer>());
	
	public Ping() {
		this("Ping");
	}
	
	public Ping(String salt) {
		this.key = Encryption.MD5(String.valueOf(Math.random()) + salt + getNextRollingNumber());
	}
	
	public boolean start() throws NotInitializedException {
		if(getStarted() == -1) {
			addPendingPing(this);
				for(XServer s : getWaiting().toArray(new XServer[getWaiting().size()])) {
					if(!s.isConnected()) {
						getWaiting().remove(s);
						getResponses().put(s, (long) -1);
					}
				}
			setStarted(System.currentTimeMillis());
			for(XServer s : getWaiting().toArray(new XServer[getWaiting().size()])) {
				try {
					s.ping(this);
				} catch (InterruptedException | IOException e) {
					getWaiting().remove(s);
					getResponses().put(s, Long.MAX_VALUE);
				}
			}
			check();
			XServerManager.getInstance().getThreadPool().runTask(new Runnable() {
				public void run() {
					try {
						Thread.sleep(timeout);
						check();
					} catch (InterruptedException e) {
					}
				}});
			return true;
		}
		return false;
	}
		
	public void add(XServer server) {
		getResponses().put(server, Long.MAX_VALUE);
		getWaiting().add(server);
	}
	
	public void addAll(Collection<XServer> servers) {
		for(XServer s : servers) {
			add(s);
		}
	}
	
	public void receive(XServer server) {
		long t = System.currentTimeMillis();
			if(getWaiting().contains(server)) {
				getWaiting().remove(server);
				synchronized(getResponses()) {
					getResponses().put(server, t);
				}
				check();
			}
	}
	
	private boolean check() {
		if(!isPending() && !resultprinted) {
			resultprinted = true;
			getWaiting().clear();
			sendMessageToCommandSender(getFormatedString());
			return true;
		}
		return false;
	}
	
	public boolean isPending() {
		return ((getWaiting().size() > 0) ? (System.currentTimeMillis() - getStarted() < timeout) : false); 
	}
	
	protected abstract void sendMessageToCommandSender(String message);
	
	public abstract String getFormatedString();

	public String getKey() {
		return key;
	}

	public Set<XServer> getWaiting() {
		return waiting;
	}

	public Map<XServer, Long> getResponses() {
		return responses;
	}

	public long getStarted() {
		return started;
	}

	public void setStarted(long started) {
		this.started = started;
	}

	
}
