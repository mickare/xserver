package de.mickare.xserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.mickare.xserver.events.XServerEvent;

public abstract class EventHandlerObj<T> implements EventHandler<T> {

  private final HashMap<XServerListener, XServerListenerPlugin<T>> listeners = new HashMap<XServerListener, XServerListenerPlugin<T>>();

  private final XServerPlugin plugin;
  private final EventBus<T> bus;

  protected EventHandlerObj(XServerPlugin plugin) {
    this.plugin = plugin;
    bus = new EventBus<T>(this, plugin.getLogger());
  }

  protected XServerListenerPlugin<T> getListPlugin(T original) {
    for (XServerListenerPlugin<T> lp : listeners.values()) {
      if (lp.getPlugin() == original) {
        return lp;
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.EventHandler#getListeners()
   */
  @Override
  public Map<XServerListener, XServerListenerPlugin<T>> getListeners() {
    return new HashMap<XServerListener, XServerListenerPlugin<T>>(listeners);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.EventHandler#registerListener(T, de.mickare.xserver.XServerListener)
   */
  @Override
  public abstract void registerListener(T plugin, XServerListener lis);

  @Override
  public void registerListenerUnsafe(Object o, XServerListener lis) throws IllegalArgumentException {
    registerListener(checkPluginType(o), lis);

  }

  public abstract T checkPluginType(Object plugin) throws IllegalArgumentException;

  protected synchronized void registerListener(XServerListenerPlugin<T> plugin, XServerListener lis) {
    listeners.put(lis, plugin);
    bus.register(lis, plugin);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.EventHandler#unregisterListener(de.mickare.xserver.XServerListener)
   */
  @Override
  public synchronized void unregisterListener(XServerListener lis) {
    bus.unregister(lis);
    listeners.remove(lis);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.EventHandler#unregisterAll(T)
   */
  @Override
  public synchronized void unregisterAll(T plugin) {
    XServerListenerPlugin<T> lp = getListPlugin(plugin);
    if (lp != null) {
      unregisterAll(lp);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.EventHandler#unregisterAll(de.mickare.xserver.XServerListenerPlugin)
   */
  @Override
  public synchronized void unregisterAll(XServerListenerPlugin<T> plugin) {
    for (Entry<XServerListener, XServerListenerPlugin<T>> e : new HashSet<Entry<XServerListener, XServerListenerPlugin<T>>>(
        listeners.entrySet())) {
      if (e.getValue() == plugin) {
        bus.unregister(e.getKey());
        listeners.remove(e.getKey());
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.EventHandler#unregisterAll()
   */
  @Override
  public synchronized void unregisterAll() {
    for (XServerListener lis : listeners.keySet()) {
      bus.unregister(lis);
    }
    listeners.clear();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.EventHandler#callEvent(de.mickare.xserver.events.XServerEvent)
   */
  @Override
  public synchronized XServerEvent callEvent(final XServerEvent event) {

    if (event == null) {
      throw new IllegalArgumentException("event can't be null");
    }

    long start = System.nanoTime();
    bus.post(event);
    event.postCall();

    long elapsed = System.nanoTime() - start;
    if (elapsed > 500000 && this.plugin.isDebugging()) {
      this.plugin.getLogger().log(Level.WARNING, "Event {0} took {1}ns to process!", new Object[] {event, elapsed});
    }
    return event;
  }


  public abstract void runTask(Boolean sync, XServerListenerPlugin<T> plugin, Runnable run);

}
