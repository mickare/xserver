package de.mickare.xserver.stresstest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.Message;
import de.mickare.xserver.net.XServer;
import de.mickare.xserver.user.ComSender;
import de.mickare.xserver.util.CacheMap;
import de.mickare.xserver.util.ChatColor;
import de.mickare.xserver.util.Encryption;

public class StressTest {

	private static final long TIMEOUT = 4000;
	
	public static final String STRESSTEST_CHANNEL_PING_SYNC = "STRESSTEST_CHANNEL_PING_SYNC";
	public static final String STRESSTEST_CHANNEL_PONG_SYNC = "STRESSTEST_CHANNEL_PONG_SYNC";

	public static final String STRESSTEST_CHANNEL_PING_ASYNC = "STRESSTEST_CHANNEL_PING_ASYNC";
	public static final String STRESSTEST_CHANNEL_PONG_ASYNC = "STRESSTEST_CHANNEL_PONG_ASYNC";

	private static final AtomicInteger rollingnumber = new AtomicInteger(0);

	private static int getNextRollingNumber() {
		int r = rollingnumber.getAndIncrement();
		if (r == Integer.MAX_VALUE) {
			rollingnumber.set(Integer.MIN_VALUE);
		}
		return r;
	}

	private static final CacheMap<String, StressTest> pending = new CacheMap<String, StressTest>(
			40);

	public static void addPendingPing(StressTest st) {
		if (st.started.get() == -1) {
			synchronized (pending) {
				pending.put(st.key, st);
			}
		}
	}

	public static void receive(String key, long created, XServer server) {
		StressTest st = null;
		synchronized (pending) {
			st = pending.get(key);
		}
		if (st != null) {
			st.receive(server, created);
		}
	}

	public static void receive(Message m) throws IOException {
		DataInputStream in = null;
		try {
			in = new DataInputStream(new ByteArrayInputStream(m.getContent()));
			
			receive(in.readUTF(), in.readLong(), m.getSender());
		
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
	
	private static Message createMessage(AbstractXServerManager manager, StressTest st) throws IOException {
		
		DataOutputStream out = null;
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			out = new DataOutputStream(b);
			out.writeUTF(st.key);
			out.writeLong(System.currentTimeMillis());

			return manager.createMessage(st.getChannelPing(), b.toByteArray());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	private final AbstractXServerManager manager;
	private final ComSender sender;
	private final int times;
	private final String key;
	private final Map<XServer, AtomicInteger> targetResponses = new HashMap<XServer, AtomicInteger>();
	private final Map<XServer, AtomicLong> targetPingSum = new HashMap<XServer, AtomicLong>();
	private final boolean sync;
	
	private int expectedResponses = 0;

	private final AtomicBoolean stopped = new AtomicBoolean(false);
	private final AtomicBoolean timedOut = new AtomicBoolean(false);
	private final AtomicBoolean resultprinted = new AtomicBoolean(false);
	
	private final AtomicInteger responsesTotal = new AtomicInteger(0);

	private final AtomicLong started = new AtomicLong(-1);
	

	public StressTest(AbstractXServerManager manager, ComSender sender,
			int times, boolean sync) {
		this(manager, sender, times, "StressTest", sync);
	}

	public StressTest(AbstractXServerManager manager, ComSender sender,
			int times, String salt, boolean sync) {
		this.manager = manager;
		this.sender = sender;
		this.times = times > 0 ? times : 1;
		this.key = Encryption.MD5(String.valueOf(Math.random()) + salt
				+ getNextRollingNumber());
		this.sync = sync;
	}

	private String getChannelPing() {
		return this.sync ? STRESSTEST_CHANNEL_PING_SYNC
				: STRESSTEST_CHANNEL_PING_ASYNC;
	}

	private Message createMessage() throws IOException {

		return createMessage(manager, this);

	}

	public void add(XServer s) {
		if (started.get() == -1) {
			targetResponses.put(s, new AtomicInteger(0));
			targetPingSum.put(s, new AtomicLong(0));
		}
	}
	
	public void addAll(Collection<XServer> servers) {
		for(XServer s : servers) {
			add(s);
		}
	}

	public boolean start() {
		if (started.get() == -1) {
			
			final StressTest self = this;
			
			Runnable startRun = new Runnable() {
				@Override
				public void run() {
					
					addPendingPing(self);
					started.set(System.currentTimeMillis());

					List<XServer> servers = new LinkedList<XServer>();
					for(XServer s : targetResponses.keySet()) {
						if(s.isConnected()) {
							expectedResponses += times;
							servers.add(s);
						} else {
							targetResponses.get(s).set(-1);
						}
					}
					
					
					for (int t = 0; t < times; t++) {
						for (XServer s : servers) {
							try {
								s.sendMessage(createMessage());
							} catch (IOException e) {
								
							}
						}
					}
					
					check();
					
					manager.getThreadPool().runTask(new Runnable() {
						public void run() {
							try {
								Thread.sleep(TIMEOUT + 10);
								stopped.set(true);
								timedOut.set(true);
								check();
							} catch (InterruptedException e) {
							}
						}
					});
				}
			};
			
			if(sync) {
				startRun.run();
			} else {
				manager.getThreadPool().runTask(startRun);
			}
			
		
		
		}
		return false;
	}

	public boolean hasTarget(XServer s) {
		return targetResponses.containsKey(s);
	}
	
	public void receive(XServer server, long created) {
		long tDiff = System.currentTimeMillis() - created;
		if (!stopped.get() && targetResponses.containsKey(server)) {
			responsesTotal.incrementAndGet();
			targetResponses.get(server).incrementAndGet();
			targetPingSum.get(server).addAndGet(tDiff);
			check();
		}
	}

	public boolean isPending() {
		return stopped.get() ? false : responsesTotal.get() < expectedResponses ? (System.currentTimeMillis() - started.get() <= TIMEOUT) : false;
	}

	private boolean check() {
		if (!isPending() && !resultprinted.get()) {
			stopped.set(true);
			printResult();
			return true;
		}
		return false;
	}

	private void printResult() {
		resultprinted.set(true);
		sender.sendMessage(getFormatedString());
	}

	public String getFormatedString() {
		if (isPending()) {
			return "Still Pending...";
		} else {
			StringBuilder sb = new StringBuilder();

			LinkedList<XServer> servers = new LinkedList<XServer>(targetResponses.keySet());

			Collections.sort(servers, new Comparator<XServer>() {
				@Override
				public int compare(XServer o1, XServer o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});

			for (XServer s : servers) {
				int responses = targetResponses.get(s).get();
				long avg_ping = -1;
				
				//manager.getLogger().info(s.getName() + " - " + targetPingSum.get(s).get());
				
				if(responses > 0) {
					avg_ping = targetPingSum.get(s).get() / responses;
				}
				
				sb.append(ChatColor.GOLD).append(s.getName())
						.append(ChatColor.GRAY).append(" - ");
				
				if (responses < 0) {
					sb.append(ChatColor.RED).append("Not connected!");
				} else if (responses == 0 || avg_ping < 0) {
					if (s.isConnected()) {
						sb.append(ChatColor.RED).append("Timeout!");
					} else {
						sb.append(ChatColor.RED).append(
								"Timeout! Connection lost!");
					}
				} else {
					
					if (responses == times) {
						sb.append(ChatColor.GREEN);
					} else if(responses == times - 1) {
						sb.append(ChatColor.YELLOW);
					} else {
						sb.append(ChatColor.RED);
					}
					
					sb.append(responses).append(ChatColor.GRAY).append("/").append(times);
					
					sb.append(ChatColor.GRAY).append(" (");
					if (avg_ping < 10) {
						sb.append(ChatColor.GREEN);
					} else if (avg_ping < 30) {
						sb.append(ChatColor.YELLOW);
					} else if (avg_ping < 100) {
						sb.append(ChatColor.GOLD);
					} else {
						sb.append(ChatColor.RED);
					}
					sb.append(avg_ping).append("ms").append(ChatColor.GRAY).append(")");
				}
				sb.append("\n");
			}
			
			if(timedOut.get()) {
				sb.append(ChatColor.RED).append(sync ? "Sync" : "Async")
				.append("StressTest(").append(times).append(") timed out after ").append(TIMEOUT).append("ms!")
				.append("\n").append(ChatColor.GRAY).append("(Total: ").append(responsesTotal.get()).append("/").append(expectedResponses).append(")");
			} else {
				sb.append(ChatColor.GRAY).append(sync ? "Sync" : "Async")
				.append(" StressTest(").append(times).append(")")
				.append("\n").append(ChatColor.GRAY).append("(Total: ").append(responsesTotal.get()).append("/").append(expectedResponses).append(")");
			}
			
			return sb.toString();
		}
	}
	
}
