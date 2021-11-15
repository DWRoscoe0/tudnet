package allClasses;

import java.net.DatagramPacket;

public class KeyedPacket<K>

	extends EpiPacket

  /* This packet class contains a key K 
    which identifies handlers for the packet.
	  The key might be either: 
	  * the IPAndPort address which selects the Unicaster associated with 
	    a peer at that address, or 
	  * a String identifying a sub-protocol within an already selected Unicaster.

	  When packets are transmitted, the key is stored in the packet
	  and is typically used to direct the packet to its correct destination.

	  When packets are received, the key is typically not stored in the packet,
	  but its the associated un-stored value is used to 
	  lookup the correct handler in a Map.

	  */

  {
  	protected final K keyK; // Identifies the address/protocol with which
  	  // this packet is associated.  It might be either:
  	  // * netcasterIPAndPort, the remote-address, or 
  	  // * subcasterString.

    public KeyedPacket(DatagramPacket theDatagramPacket, K keyK) // Constructor
      {
    	  super( theDatagramPacket );
    		this.keyK= keyK;
        }

    public K getKeyK()
    	{ return keyK; }
    
    public String toString()
      { 
        return 
          "(KeyedPacket:"
          + Nulls.toString(keyK)
          + ","
          + super.toString()
          + ")"; 
        }

  	}
