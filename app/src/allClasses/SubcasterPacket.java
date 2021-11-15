package allClasses;

import java.net.DatagramPacket;

public class SubcasterPacket 

	extends KeyedPacket< String >

  // These are packets associated with particular protocol Subcaster.

	{

    public SubcasterPacket(  // Constructor.
        DatagramPacket theDatagramPacket,
        String theString
        )
      {
    		super( theDatagramPacket, theString );
        }

		}
