package de.mickare.xserver;

// Class from MD5 - BungeeCord

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.mickare.xserver.annotations.XEventHandler;
import de.mickare.xserver.events.XServerEvent;
import de.mickare.xserver.util.concurrent.CloseableLock;
import de.mickare.xserver.util.concurrent.CloseableReadWriteLock;
import de.mickare.xserver.util.concurrent.CloseableReentrantReadWriteLock;

public class EventBus<T> {

  private final Map<Class<?>, Map<Object, Method[]>> eventToHandler = new HashMap<>();
  private final Map<Method, Boolean> synced = Collections.synchronizedMap(new HashMap<Method, Boolean>());
  private final Map<Method, String> channeled = Collections.synchronizedMap(new HashMap<Method, String>());
  private final Map<Method, XServerListenerPlugin<T>> plugins =
      Collections.synchronizedMap(new HashMap<Method, XServerListenerPlugin<T>>());
  private final CloseableReadWriteLock lock = new CloseableReentrantReadWriteLock();
  private final Logger logger;

  private final EventHandler<T> myhandler;

  public EventBus(EventHandler<T> myhandler, Logger logger) {
    this.logger = (logger == null) ? Logger.getGlobal() : logger;
    this.myhandler = myhandler;
  }

  private Map<Object, Method[]> getHandlers(final XServerEvent event) {
    try (CloseableLock c = lock.readLock().open()) {
      return eventToHandler.get(event.getClass());
    }
  }

  public void post(final XServerEvent event) {
    final Map<Object, Method[]> handlers = getHandlers(event);
    if (handlers != null) {
      for (final Map.Entry<Object, Method[]> handler : handlers.entrySet()) {
        for (final Method method : handler.getValue()) {
          final String channel = channeled.get(method);
          if (channel != null && !channel.isEmpty()) {
            if (!channel.equals(event.getChannel())) {
              continue;
            }
          }
          myhandler.runTask(synced.get(method), plugins.get(method), new Runnable() {
            @Override
            public void run() {
              try {
                method.invoke(handler.getKey(), event);
              } catch (IllegalAccessException ex) {
                throw new Error("Method became inaccessible: " + event, ex);
              } catch (IllegalArgumentException ex) {
                throw new Error("Method rejected target/argument: " + event, ex);
              } catch (InvocationTargetException ex) {
                logger.log(Level.WARNING, MessageFormat.format("Error dispatching event {0} to listener {1}", event, handler.getKey()),
                    ex.getCause());
              }
            }
          });
        }
      }
    }
  }

  private Map<Class<?>, Set<Method>> findHandlers(Object listener) {
    Map<Class<?>, Set<Method>> handler = new HashMap<>();
    for (Method m : listener.getClass().getDeclaredMethods()) {
      XEventHandler annotation = m.getAnnotation(XEventHandler.class);
      if (annotation != null) {
        Class<?>[] params = m.getParameterTypes();
        if (params.length != 1) {
          logger.log(Level.INFO, "Method {0} in class {1} annotated with {2} does not have single argument",
              new Object[] {m, listener.getClass(), annotation});
          continue;
        }

        Set<Method> existing = handler.get(params[0]);
        if (existing == null) {
          existing = new HashSet<>();
          handler.put(params[0], existing);
        }
        synced.put(m, annotation.sync());
        channeled.put(m, annotation.channel());
        existing.add(m);
      }
    }
    return handler;
  }

  public void register(Object listener, XServerListenerPlugin<T> plugin) {
    Map<Class<?>, Set<Method>> handler = findHandlers(listener);
    try (CloseableLock c = lock.writeLock().open()) {
      for (Map.Entry<Class<?>, Set<Method>> e : handler.entrySet()) {
        Map<Object, Method[]> a = eventToHandler.get(e.getKey());
        if (a == null) {
          a = new ConcurrentHashMap<>();
          eventToHandler.put(e.getKey(), a);
        }

        for (Method m : e.getValue()) {
          plugins.put(m, plugin);
        }

        Method[] baked = new Method[e.getValue().size()];
        a.put(listener, e.getValue().toArray(baked));
      }
    }
  }

  public void unregister(Object listener) {
    Map<Class<?>, Set<Method>> handler = findHandlers(listener);
    try (CloseableLock c = lock.writeLock().open()) {
      for (Map.Entry<Class<?>, Set<Method>> e : handler.entrySet()) {
        Map<Object, Method[]> a = eventToHandler.get(e.getKey());
        if (a != null) {
          a.remove(listener);
          if (a.isEmpty()) {
            eventToHandler.remove(e.getKey());
          }
        }
      }
    }
  }
}
