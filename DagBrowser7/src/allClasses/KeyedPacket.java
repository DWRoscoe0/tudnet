package allClasses;

import java.net.DatagramPacket;

public class KeyedPacket<K>

	extends EpiPacket

  /* This class a key K to a packet which identifies the handler for it.
	  The key might be the IPAndPort of a peer, or a String identifying
	  a subprotocol for that peer.
	  */
  {
  	protected final K keyK; // Identifies the address/protocol with which
  	  // this packet is associated.  It might be either:
  	  // * IPAndPort, the remote-address, replacing (InetAddress,portI). 
  	  // * subcasterString.

    public KeyedPacket(  // Constructor.
        DatagramPacket theDatagramPacket,
    		K keyK
        )
      {
    	  super( theDatagramPacket );
    		this.keyK= keyK;
        }

  	}
