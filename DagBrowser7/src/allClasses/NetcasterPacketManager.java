package allClasses;

import java.net.DatagramPacket;

public class NetcasterPacketManager

  extends PacketManager<IPAndPort,NetcasterPacket>

	{
		// Injected variables.
	    // None.

		public NetcasterPacketManager( IPAndPort theIPAndPort ) // Constructor.
			{
			  super( theIPAndPort );
				}


		// Definitions of superclass abstract methods.
		
		NetcasterPacket produceKeyedPacketE( 
				DatagramPacket theDatagramPacket, IPAndPort theIPAndPort 
				)
			{ 
			  return new NetcasterPacket( theDatagramPacket, theIPAndPort );
			  }


  	}

