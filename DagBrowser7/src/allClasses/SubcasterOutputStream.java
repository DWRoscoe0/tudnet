package allClasses;

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
				NamedInteger packetCounterNamedInteger
				)
			{
			  super(
			  		outputSubcasterQueue,
			  		theSubcasterPacketManager,
			  		packetCounterNamedInteger
			  		);
	      }

		}
