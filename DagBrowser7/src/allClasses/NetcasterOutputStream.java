package allClasses;

public class NetcasterOutputStream 

  extends EpiOutputStream<
    IPAndPort,
    NetcasterPacket,
  	NetcasterQueue,
  	NetcasterPacketManager
		> 

  /* This is the EpiOutputStream used by Netcasters 
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
