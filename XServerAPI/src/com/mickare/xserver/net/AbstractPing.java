package com.mickare.xserver.net;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mickare.xserver.AbstractXServerManager;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.CacheMap;
import com.mickare.xserver.util.Encryption;

public abstract class AbstractPing {
	
	
	private static CacheMap<String, AbstractPing> pending = new CacheMap<String, AbstractPing>(40);

	private static int rollingnumber = 0;
	
	private static int getNextRollingNumber() {
		if(rollingnumber == Integer.MAX_VALUE) {
			rollingnumber = Integer.MIN_VALUE;
		}
		return rollingnumber++;
	}
	
	public static void addPendingPing(AbstractPing ping) {
		synchronized(pending) {
			if(ping.started == -1) {
				pending.put(ping.key, ping);
			}
		}
	}
	
	public static void receive(String key, XServer server) {
		AbstractPing p = null;
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
	
	public AbstractPing() {
		this("Ping");
	}
	
	public AbstractPing(String salt) {
		this.key = Encryption.MD5(String.valueOf(Math.random()) + salt + getNextRollingNumber());
	}
	
	public boolean start() throws NotInitializedException {
		if(started == -1) {
			addPendingPing(this);
				for(XServer s : waiting.toArray(new XServer[waiting.size()])) {
					if(!s.isConnected()) {
						waiting.remove(s);
						responses.put(s, (long) -1);
					}
				}
			started = System.currentTimeMillis();
			for(XServer s : waiting.toArray(new XServer[waiting.size()])) {
				try {
					s.ping(this);
				} catch (InterruptedException | IOException e) {
					waiting.remove(s);
					responses.put(s, Long.MAX_VALUE);
				}
			}
			check();
			AbstractXServerManager.getInstance().getThreadPool().runTask(new Runnable() {
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
		responses.put(server, Long.MAX_VALUE);
		waiting.add(server);
	}
	
	public void addAll(Collection<XServer> servers) {
		for(XServer s : servers) {
			add(s);
		}
	}
	
	public void receive(XServer server) {
		long t = System.currentTimeMillis();
			if(waiting.contains(server)) {
				waiting.remove(server);
				synchronized(responses) {
					responses.put(server, t);
				}
				check();
			}
	}
	
	private boolean check() {
		if(!isPending() && !resultprinted) {
			resultprinted = true;
			waiting.clear();
			sendMessageToCommandSender(getFormatedString());
			return true;
		}
		return false;
	}
	
	public boolean isPending() {
		return ((waiting.size() > 0) ? (System.currentTimeMillis() - started < timeout) : false); 
	}
	
	protected abstract void sendMessageToCommandSender(String message);
	
	public abstract String getFormatedString();

	public String getKey() {
		return key;
	}

	
}
