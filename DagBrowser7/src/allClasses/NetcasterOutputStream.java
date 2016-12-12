package allClasses;

import java.util.Timer;

public class NetcasterOutputStream 

  extends EpiOutputStream<
    IPAndPort,
    NetcasterPacket,
  	NetcasterQueue,
  	NetcasterPacketManager
		> 

  /* This is the EpiOutputStream used by Netcasters 
    (Unicasters and Multicasters).  
    */

  {

		NetcasterOutputStream(  // Constructor.
				NetcasterQueue outputNetcasterQueue,
				NetcasterPacketManager theNetcasterPacketManager,
				NamedLong packetCounterNamedLong,
	  		Timer theTimer
)
			{
			  super(
			  		outputNetcasterQueue,
			  		theNetcasterPacketManager,
			  		packetCounterNamedLong,
			  		theTimer
			  		);
	      }

  	}
