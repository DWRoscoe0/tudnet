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
				NamedLong packetCounterNamedLong,
	  		char delimiterChar
				)
			{
			  super(
			  		outputSubcasterQueue,
			  		theSubcasterPacketManager,
			  		packetCounterNamedLong,
			  		delimiterChar
			  		);
	      }

		}
