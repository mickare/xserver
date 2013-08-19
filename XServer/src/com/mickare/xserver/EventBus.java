package com.mickare.xserver;

// Class from MD5 - BungeeCord

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.mickare.xserver.annotations.XEventHandler;

public class EventBus
{
	
    private final Map<Class<?>, Map<Object, Method[]>> eventToHandler = new HashMap<>();
    private final Map<Method, Boolean> synced = Collections.synchronizedMap(new HashMap<Method, Boolean>());
    private final Map<Method, JavaPlugin> plugins = Collections.synchronizedMap(new HashMap<Method, JavaPlugin>());
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Logger logger;

    private final EventHandler myhandler;

    public EventBus(EventHandler myhandler, Logger logger)
    {
        this.logger = ( logger == null ) ? Logger.getGlobal() : logger;
        this.myhandler = myhandler;
    }

    public void post(final Object event)
    {
        lock.readLock().lock();
        try
        {
            final Map<Object, Method[]> handlers = eventToHandler.get( event.getClass() );
            if ( handlers != null )
            {
                for ( final Map.Entry<Object, Method[]> handler : handlers.entrySet() )
                {
                    for ( final Method method : handler.getValue() )
                    {
                    	myhandler.runTask(synced.get(method) , plugins.get(method),new Runnable(){
        					@Override
        					public void run() {
		                        try
		                        {
		                            method.invoke( handler.getKey(), event );
		                        } catch ( IllegalAccessException ex )
		                        {
		                            throw new Error( "Method became inaccessible: " + event, ex );
		                        } catch ( IllegalArgumentException ex )
		                        {
		                            throw new Error( "Method rejected target/argument: " + event, ex );
		                        } catch ( InvocationTargetException ex )
		                        {
		                            logger.log( Level.WARNING, MessageFormat.format( "Error dispatching event {0} to listener {1}", event, handler.getKey() ), ex.getCause() );
		                        }
        					}
        				});
                    }
                }
            }
        } finally
        {
            lock.readLock().unlock();
        }
    }

    private Map<Class<?>, Set<Method>> findHandlers(Object listener)
    {
        Map<Class<?>, Set<Method>> handler = new HashMap<>();
        for ( Method m : listener.getClass().getDeclaredMethods() )
        {
        	XEventHandler annotation = m.getAnnotation( XEventHandler.class );
            if ( annotation != null )
            {
                Class<?>[] params = m.getParameterTypes();
                if ( params.length != 1 )
                {
                    logger.log( Level.INFO, "Method {0} in class {1} annotated with {2} does not have single argument", new Object[]
                    {
                        m, listener.getClass(), annotation
                    } );
                    continue;
                }

                Set<Method> existing = handler.get( params[0] );
                if ( existing == null )
                {
                    existing = new HashSet<>();
                    handler.put( params[0], existing );
                }
                synced.put(m, annotation.sync());
                existing.add( m );
            }
        }
        return handler;
    }

    public void register(Object listener, JavaPlugin plugin)
    {
        Map<Class<?>, Set<Method>> handler = findHandlers( listener );
        lock.writeLock().lock();
        try
        {
            for ( Map.Entry<Class<?>, Set<Method>> e : handler.entrySet() )
            {
                Map<Object, Method[]> a = eventToHandler.get( e.getKey() );
                if ( a == null )
                {
                    a = new HashMap<>();
                    eventToHandler.put( e.getKey(), a );
                }
                
                for(Method m : e.getValue()) {
                	plugins.put(m, plugin);
                }
                
                Method[] baked = new Method[ e.getValue().size() ];
                a.put( listener, e.getValue().toArray( baked ) );
            }
        } finally
        {
            lock.writeLock().unlock();
        }
    }

    public void unregister(Object listener)
    {
        Map<Class<?>, Set<Method>> handler = findHandlers( listener );
        lock.writeLock().lock();
        try
        {
            for ( Map.Entry<Class<?>, Set<Method>> e : handler.entrySet() )
            {
                Map<Object, Method[]> a = eventToHandler.get( e.getKey() );
                if ( a != null )
                {
                    a.remove( listener );
                    if ( a.isEmpty() )
                    {
                        eventToHandler.remove( e.getKey() );
                    }
                }
            }
        } finally
        {
            lock.writeLock().unlock();
        }
    }
}
