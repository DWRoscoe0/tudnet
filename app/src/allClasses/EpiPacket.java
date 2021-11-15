package allClasses;

import java.net.DatagramPacket;

public class EpiPacket 

  /* This purpose of this class was to be an extendible DatagramPacket.
    The DatagramPacket class is final so it is not extendible.
   	*/

  {

		private final DatagramPacket theDatagramPacket; // The packet.

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

    public String toString()
      { 
        return 
            "(EpiPacket:" 
            + Nulls.toString(PacketManager.gettingPacketString(theDatagramPacket)) 
            + ")"; 
        }

  	}
