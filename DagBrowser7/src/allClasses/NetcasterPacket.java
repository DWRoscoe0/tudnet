package allClasses;

import java.net.DatagramPacket;

public class NetcasterPacket

  extends KeyedPacket< IPAndPort >

  // These are packets associated with particular peer Unicaster.

	{

    public NetcasterPacket(  // Constructor.
        DatagramPacket theDatagramPacket,
        IPAndPort theIPAndPort
        )
      {
	    	super( theDatagramPacket, theIPAndPort );
        }

		}
