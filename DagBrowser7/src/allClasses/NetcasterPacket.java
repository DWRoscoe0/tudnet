package allClasses;

import java.net.DatagramPacket;

public class NetcasterPacket

  extends KeyedPacket< IPAndPort >

  // These are packet associated with particular addresses.

	{

    public NetcasterPacket(  // Constructor.
        DatagramPacket inDatagramPacket
        )
      /* Constructs a NetcasterPacket associated with 
        DatagramSocket inDatagramSocket and DatagramPacket inDatagramPacket.

        This constructor would not be possible if
        this class had extended DatagramPacket.
        */
      {
	    	super( inDatagramPacket, (IPAndPort)null );
        }

		}
