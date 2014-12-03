package de.mickare.xserver.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import de.mickare.xserver.XServerManager;
import de.mickare.xserver.net.protocol.PingPacket;
import de.mickare.xserver.user.ComSender;
import de.mickare.xserver.util.CacheMap;
import de.mickare.xserver.util.ChatColor;
import de.mickare.xserver.util.Encryption;

public class PingObj implements Ping {
	
	private static final long TIMEOUT = 2000;
	
	private static final CacheMap<String, Ping> pending = new CacheMap<String, Ping>( 40 );
	
	private static final AtomicInteger rollingnumber = new AtomicInteger( 0 );
	
	private static int getNextRollingNumber() {
		int r = rollingnumber.getAndIncrement();
		if ( r == Integer.MAX_VALUE ) {
			rollingnumber.set( Integer.MIN_VALUE );
		}
		return r;
	}
	
	public static void addPendingPing( Ping ping ) {
		if ( ping.getStarted() == -1 ) {
			synchronized ( pending ) {
				pending.put( ping.getKey(), ping );
			}
		}
	}
	
	public static void receive( PingPacket packet, XServer server ) {
		
		try ( DataInputStream is = new DataInputStream( new ByteArrayInputStream( packet.getData() ) ) ) {
			String key = is.readUTF();
			
			Ping p = null;
			synchronized ( pending ) {
				p = pending.get( key );
			}
			if ( p != null ) {
				p.receive( server );
			}
			
		} catch ( IOException e ) {
		}
		
	}
	
	private final ComSender sender;
	private final String key;
	
	private boolean resultprinted = false;
	
	private long started = -1;
	private final Map<XServer, Long> responses = Collections.synchronizedMap( new HashMap<XServer, Long>() );
	private final Set<XServer> waiting = Collections.synchronizedSet( new HashSet<XServer>() );
	
	private final XServerManager manager;
	
	public PingObj( XServerManager manager, ComSender sender ) {
		this( manager, sender, "Ping" );
	}
	
	public PingObj( XServerManager manager, ComSender sender, String salt ) {
		this.manager = manager;
		this.sender = sender;
		this.key = Encryption.MD5( String.valueOf( Math.random() ) + salt + getNextRollingNumber() );
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.Ping#start()
	 */
	@Override
	public boolean start() {
		if ( started == -1 ) {
			addPendingPing( this );
			for ( XServer s : waiting.toArray( new XServer[waiting.size()] ) ) {
				if ( !s.isConnected() ) {
					waiting.remove( s );
					responses.put( s, ( long ) -1 );
				}
			}
			started = System.currentTimeMillis();
			for ( XServer s : waiting.toArray( new XServer[waiting.size()] ) ) {
				try {
					s.ping( this );
				} catch ( InterruptedException | IOException e ) {
					waiting.remove( s );
					responses.put( s, Long.MAX_VALUE );
				}
			}
			check();
			manager.getThreadPool().runTask( new Runnable() {
				public void run() {
					try {
						Thread.sleep( TIMEOUT + 10 );
						check();
					} catch ( InterruptedException e ) {
					}
				}
			} );
			return true;
		}
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.Ping#add(de.mickare.xserver.net.XServer)
	 */
	@Override
	public void add( XServer server ) {
		responses.put( server, Long.MAX_VALUE );
		waiting.add( server );
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.Ping#addAll(java.util.Collection)
	 */
	@Override
	public void addAll( Collection<XServer> servers ) {
		for ( XServer s : servers ) {
			add( s );
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.Ping#receive(de.mickare.xserver.net.XServer)
	 */
	@Override
	public void receive( XServer server ) {
		long t = System.currentTimeMillis();
		if ( waiting.contains( server ) ) {
			waiting.remove( server );
			synchronized ( responses ) {
				responses.put( server, t );
			}
			check();
		}
	}
	
	private boolean check() {
		if ( !isPending() && !resultprinted ) {
			resultprinted = true;
			waiting.clear();
			sender.sendMessage( getFormatedString() );
			return true;
		}
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.Ping#isPending()
	 */
	@Override
	public boolean isPending() {
		return ( ( waiting.size() > 0 ) ? ( System.currentTimeMillis() - started <= TIMEOUT ) : false );
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.Ping#getFormatedString()
	 */
	@Override
	public String getFormatedString() {
		if ( waiting.size() > 0 ) {
			return "Still Pending...";
		} else {
			StringBuilder sb = new StringBuilder();
			
			LinkedList<XServer> servers = new LinkedList<XServer>( responses.keySet() );
			
			Collections.sort( servers, new Comparator<XServer>() {
				@Override
				public int compare( XServer o1, XServer o2 ) {
					return o1.getName().compareTo( o2.getName() );
				}
			} );
			
			for ( XServer s : servers ) {
				long value = responses.get( s );
				sb.append( "\n" ).append( ChatColor.GOLD ).append( s.getName() ).append( ChatColor.GRAY )
						.append( " - " );
				if ( value < 0 ) {
					sb.append( ChatColor.RED ).append( "Not connected!" );
				} else if ( value == Long.MAX_VALUE ) {
					if ( s.isConnected() ) {
						sb.append( ChatColor.RED ).append( "Timeout!" );
					} else {
						sb.append( ChatColor.RED ).append( "Timeout! Connection lost!" );
					}
				} else {
					long diff = value - started;
					if ( diff < 10 ) {
						sb.append( ChatColor.GREEN );
					} else if ( diff < 30 ) {
						sb.append( ChatColor.YELLOW );
					} else if ( diff < 100 ) {
						sb.append( ChatColor.GOLD );
					} else {
						sb.append( ChatColor.RED );
					}
					sb.append( diff ).append( "ms" );
				}
			}
			return sb.toString();
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.Ping#getSender()
	 */
	@Override
	public ComSender getSender() {
		return sender;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.net.Ping#getKey()
	 */
	@Override
	public String getKey() {
		return key;
	}
	
	@Override
	public long getStarted() {
		return started;
	}
	
}