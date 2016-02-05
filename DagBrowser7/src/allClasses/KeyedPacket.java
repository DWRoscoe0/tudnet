package allClasses;

import java.net.DatagramPacket;

public class KeyedPacket<K>

	extends EpiPacket

  /* This packet class contains a key K 
    which identifies handlers for the packet.
	  The key might be the IPAndPort of a peer, or a String identifying
	  a sub-protocol for a particular peer.
	  */
  {
  	protected final K keyK; // Identifies the address/protocol with which
  	  // this packet is associated.  It might be either:
  	  // * netcasterIPAndPort, the remote-address, replaces (InetAddress,portI). 
  	  // * subcasterString.

    public KeyedPacket(  // Constructor.
        DatagramPacket theDatagramPacket,
    		K keyK
        )
      {
    	  super( theDatagramPacket );
    		this.keyK= keyK;
        }

    public K getKeyK()
    	{ return keyK; }

  	}
