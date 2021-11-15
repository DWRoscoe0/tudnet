package allClasses;

import java.net.DatagramPacket;

public class SubcasterPacketManager

  extends PacketManager<String,SubcasterPacket>

  {
		// Injected variables.
	    // None.

		public SubcasterPacketManager( String theString ) // Constructor.
			{
			  super( theString );
				}

    // Superclass abstract methods.

		SubcasterPacket produceKeyedPacketE( DatagramPacket theDatagramPacket )
			{ 
			  return new SubcasterPacket( theDatagramPacket, theKeyK );
			  }

  	}

