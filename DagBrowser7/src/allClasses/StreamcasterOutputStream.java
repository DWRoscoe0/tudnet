package allClasses;


public class StreamcasterOutputStream

  extends NetOutputStream<
	  String,
	  SubcasterPacket,
		SubcasterQueue,
		SubcasterPacketManager
    > 

  // This is the NetOutputStream used by Subcasters 

  {
	
		StreamcasterOutputStream(  // Constructor.
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
