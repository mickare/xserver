package de.mickare.xserver.net;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import de.mickare.xserver.AbstractXServerManagerObj;
import de.mickare.xserver.Message;
import de.mickare.xserver.XType;
import de.mickare.xserver.events.XServerDisconnectEvent;
import de.mickare.xserver.events.XServerLoggedInEvent;
import de.mickare.xserver.events.XServerMessageIncomingEvent;
import de.mickare.xserver.events.XServerMessageOutgoingEvent;
import de.mickare.xserver.exceptions.NotInitializedException;
import de.mickare.xserver.util.Encryption;

public class XServerObjHome extends XServerObj {

  private String password;

  public XServerObjHome(String name, String host, int port, String password, AbstractXServerManagerObj manager) {
    super(name, host, port, password, manager);
    this.password = Encryption.MD5(password);
  }

  public XServerObjHome(String name, String host, int port, String password, XType type, AbstractXServerManagerObj manager) {
    super(name, host, port, password, type, manager);
    this.password = Encryption.MD5(password);
  }

  public XServerObjHome(XServerObj home) {
    this(home.getName(), home.getHost(), home.getPort(), home.getPassword(), home.getType(), home.getManager());
    this.password = home.getPassword();
  }

  private volatile boolean connected = false;

  private final AtomicLong recordSecondPackageCount = new AtomicLong(0);
  private final AtomicLong lastSecondPackageCount = new AtomicLong(0);
  private long lastSecond = 0;
  private long packageCount = 0;


  @Override
  public synchronized void connect() throws IOException, InterruptedException, NotInitializedException {
    if (connected) {
      getManager().getEventHandler().callEvent(new XServerDisconnectEvent(this));
    }
    connected = true;
    getManager().getEventHandler().callEvent(new XServerLoggedInEvent(this));
  }

  @Override
  public synchronized void connectSoft() throws NotInitializedException, IOException, InterruptedException {
    if (!isConnected()) {
      this.connect();
    }
  }


  @Override
  protected void unsetConnection(ConnectionObj con) {}

  @Override
  protected void setConnection(Connection con) {}

  @Override
  protected void setLoginConnection(Connection con) {}

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.net.XServer#isConnected()
   */
  @Override
  public boolean isConnected() {
    return connected;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.net.XServer#disconnect()
   */
  @Override
  public synchronized void disconnect() {
    if (this.connected) {
      this.connected = false;
      getManager().getEventHandler().callEvent(new XServerDisconnectEvent(this));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.net.XServer#sendMessage(de.mickare.xserver.Message)
   */
  @Override
  public boolean sendMessage(Message message) throws IOException {
    if (!this.getManager().isRunning() || !valid()) {
      return false;
    }
    this.getManager().getEventHandler().callEvent(new XServerMessageOutgoingEvent(this, message));
    this.tickPacket();
    this.getManager().getEventHandler().callEvent(new XServerMessageIncomingEvent(this, message));
    return true;

  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.net.XServer#ping(de.mickare.xserver.net.Ping)
   */
  @Override
  public void ping(Ping ping) throws InterruptedException, IOException {
    if (!valid() || !this.getManager().isRunning()) {
      return;
    }

    this.tickPacket();
    PingObj.receive(ping.getKey(), this);
  }

  @Override
  public void flushCache() {}

  @Override
  public String getPassword() {
    return password;
  }

  private void tickPacket() {
    if (System.currentTimeMillis() - lastSecond > 1000) {
      lastSecondPackageCount.set(packageCount);
      if (packageCount > recordSecondPackageCount.get()) {
        recordSecondPackageCount.set(packageCount);
      }
      packageCount = 0;
      lastSecond = System.currentTimeMillis();
    }
    packageCount++;
  }

  @Override
  public XType getType() {
    return getManager().getPlugin().getHomeType();
  }

  protected void setType(XType type) {}

  @Override
  public long getSendingRecordSecondPackageCount() {
    return this.recordSecondPackageCount.get();
  }

  @Override
  public long getSendinglastSecondPackageCount() {
    return this.lastSecondPackageCount.get();
  }

  @Override
  public long getReceivingRecordSecondPackageCount() {
    return this.recordSecondPackageCount.get();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mickare.xserver.net.XServer#getReceivinglastSecondPackageCount()
   */
  @Override
  public long getReceivinglastSecondPackageCount() {
    return this.lastSecondPackageCount.get();
  }

}
