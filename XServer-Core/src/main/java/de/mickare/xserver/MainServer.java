package de.mickare.xserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import de.mickare.xserver.net.ConnectionObj;
import de.mickare.xserver.util.MyStringUtils;

public class MainServer implements Runnable {
	
	private final ServerSocket server;
	private final AbstractXServerManagerObj manager;
	private final AtomicReference<Future<?>> task = new AtomicReference<Future<?>>( null );
	
	protected MainServer( ServerSocket server, AbstractXServerManagerObj manager ) {
		// super( "XServer Main Server Thread" );
		this.server = server;
		this.manager = manager;
	}
	
	public void close() throws IOException {
		synchronized ( task ) {
			this.server.close();
			if ( this.task.get() != null ) {
				this.task.get().cancel( true );
				this.task.set( null );
			}
		}
	}
	
	public boolean isClosed() {
		synchronized ( task ) {
			return this.server.isClosed();
		}
	}
	
	@Override
	public void run() {
		while ( !isClosed() ) {
			try {
				try {
					new ConnectionObj( server.accept(), manager );
				} catch ( SocketTimeoutException ste ) {
					
				}
			} catch ( IOException e ) {
				manager.getLogger().warning( "Exception while connecting: " + e.getMessage() + "\n"
						+ MyStringUtils.stackTraceToString( e ) );
			}
		}
	}
	
	public void start( ServerThreadPoolExecutor stpool ) {
		synchronized ( task ) {
			if ( task.get() == null ) {
				task.set( stpool.runServerTask( this ) );
			}
		}
	}
	
}
