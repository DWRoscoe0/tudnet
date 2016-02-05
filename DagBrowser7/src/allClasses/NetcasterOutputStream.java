package allClasses;

public class NetcasterOutputStream 

  extends NetOutputStream<
    IPAndPort,
    NetcasterPacket,
  	NetcasterQueue,
  	NetcasterPacketManager
		> 

  /* This is the NetOutputStream used by Netcasters 
    (Unicasters and Multicasters.  
    */

  {

		NetcasterOutputStream(  // Constructor.
				NetcasterQueue outputNetcasterQueue,
				NetcasterPacketManager theNetcasterPacketManager,
				NamedInteger packetCounterNamedInteger
				)
			{
			  super(
			  		outputNetcasterQueue,
			  		theNetcasterPacketManager,
			  		packetCounterNamedInteger
			  		);
	      }

  	}
