package allClasses;

import java.io.IOException;
import java.net.InetAddress;

import static allClasses.Globals.*;  // appLogger;

public class NetworkThread extends Thread {

	/** This Thread class does network operations.
	 *  Presently it does nothing. 
	 */

  private static NetworkThread theNetworkThread= // Create Singleton Thread.
    new NetworkThread();
  
  static { theNetworkThread.start(); }  // Start Singleton Thread.
  
  public static void activateV( )
    /* Normally this method is called at app start-up.
      It makes certain that the NetworkThread 
      which does network communication is running.
      It calls nothing but its class loader will
      construct and start the thread.
      */
    { 
      // When here, the thread is running.
      }

  public void run() 
    /* This method does network communication.
      Presently it only looks for other peers using IPMulticast.
      */
    {
      PeerDiscovery mPeerDiscovery;
      try {
        //InetAddress mcInetAddress= InetAddress.getByName("203.0.113.0");
        //InetAddress mcInetAddress= InetAddress.getByName("224.0.0.1");
        InetAddress mcInetAddress= InetAddress.getByName("239.255.0.0");
          /* RFC2365, "Administratively Scoped IP Multicast", allocates
            239.0.0.0 to 239.255.255.255 for use local and organizational
            scopes for multicast addresses.  They are not meant to be used
            outside of those scopes.  There are two expandable scopes:
              Local Scope -- 239.255.0.0/16
              Organization Local Scope -- 239.192.0.0/14
            Routers are supposed to block packets outside of thee ranges.
            */
        
	      mPeerDiscovery = new PeerDiscovery( 
          mcInetAddress,  // multicastGroupIP
          7777,  // port
          1  // TTL for LAN only.
          );

        // constructs broadcast-based peer
        // PeerDiscovery bpd = new PeerDiscovery( groupIP, port);

        // queries the group, and waits for responses
        InetAddress[] peers = mPeerDiscovery.getPeers( 
          1000  // Listen 1 second for responses.
          );
      
        appLogger.info("PeerDiscovery beginning of peers responded.");
        for (InetAddress peer: peers)
          appLogger.info("peer="+peer);
        appLogger.info("PeerDiscovery end of peers responded.");

        // when you're done... stay connected to respond to queries.
        //mPeerDiscovery.disconnect();
      } catch (IOException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
      }

      appLogger.info("PeerDiscovery done");
      }
  }
