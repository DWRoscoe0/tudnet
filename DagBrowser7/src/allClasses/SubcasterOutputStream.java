package allClasses;

import java.util.Timer;

public class SubcasterOutputStream 

  extends EpiOutputStream<
	  String,
	  SubcasterPacket,
		SubcasterQueue,
		SubcasterPacketManager
    > 

  // This is the EpiOutputStream used by Subcasters 

  {

		SubcasterOutputStream(  // Constructor.
				SubcasterQueue outputSubcasterQueue,
				SubcasterPacketManager theSubcasterPacketManager,
				NamedLong packetCounterNamedLong,
	  		Timer theTimer
				)
			{
			  super(
			  		outputSubcasterQueue,
			  		theSubcasterPacketManager,
			  		packetCounterNamedLong,
			  		theTimer
			  		);
	      }

		}
