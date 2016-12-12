package allClasses;

public class SubcasterInputStream 

	extends EpiInputStream<
	  String,
	  SubcasterPacket,
		SubcasterQueue,
		SubcasterPacketManager
		>
	
	{
	
		public SubcasterInputStream( // Constructor. 
			SubcasterQueue receiverToSubcasterSubcasterQueue, 
			NamedLong packetCounterNamedLong
			)
		{
	  	super( 
	    		receiverToSubcasterSubcasterQueue, 
	    		packetCounterNamedLong
	    		);
			}

}
