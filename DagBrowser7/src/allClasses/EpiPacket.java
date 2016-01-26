package allClasses;

import java.net.DatagramPacket;

public class EpiPacket 

  {

		private final DatagramPacket theDatagramPacket; // The packet.
	  // It might or might not IP and port.

    public EpiPacket(  // Constructor.
        DatagramPacket theDatagramPacket
        )
      {
    		this.theDatagramPacket= theDatagramPacket;
        }

    public DatagramPacket getDatagramPacket()
      {
        return theDatagramPacket;
        }

  	}
