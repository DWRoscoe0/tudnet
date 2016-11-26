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
				NamedInteger packetCounterNamedInteger,
	  		Timer theTimer
				)
			{
			  super(
			  		outputSubcasterQueue,
			  		theSubcasterPacketManager,
			  		packetCounterNamedInteger,
			  		theTimer
			  		);
	      }

		}
