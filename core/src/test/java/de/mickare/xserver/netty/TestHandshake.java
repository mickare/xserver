package de.mickare.xserver.netty;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;

import de.mickare.xserver3.NetworkManager;
import de.mickare.xserver3.netty.Client;
import de.mickare.xserver3.netty.Server;
import de.mickare.xserver3.security.Security;

public class TestHandshake {

  private final static Logger loggerA = Logger.getLogger("A");
  private final static Logger loggerB = Logger.getLogger("B");

  private NetworkManager managerA, managerB;

  @Before
  public void setUp() throws Exception {
    managerA = new NetworkManager(loggerA, true);
    managerB = new NetworkManager(loggerB, false);

    Security sec = new SimpleSecurity();

    Server s = new Server(managerA, sec, new Server.Config(999));
    Client c = new Client(managerB, sec);
    
    s.startAsync();
    c.connect(server, action)
    
  }

  @After
  public void tearDown() throws Exception {

  }

}
