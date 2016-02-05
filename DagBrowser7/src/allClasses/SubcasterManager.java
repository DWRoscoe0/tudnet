package allClasses;

//import static allClasses.Globals.appLogger;

public class SubcasterManager

	extends StreamcasterManager< 
		String, // Key for map.
		SubcasterValue, // Value for map. 
		Subcaster // DataNode in Value.
		>

	/* This class manages a Unicaster's Subcasters.
	  In addition to storing information about them, 
	  it provides methods for:
	  
	  * Testing for their existence, 
	  * Creating them and starting their threads.
	  * Displaying information about them.
	  * Stopping their threads.
	  
	  It is also used to pass packets between 
	  the NetInputStreams and NetOutputStreams of
	  the Subcasters and their associated Unicaster
	  
	  Most of its methods are synchronized.
	  */

  {

		private final UnicasterFactory theUnicasterFactory;
	
		public SubcasterManager(  // Constructor. 
	      DataTreeModel theDataTreeModel,
	      UnicasterFactory theUnicasterFactory
	      )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableListWithMap superclass.
		        theDataTreeModel,
		        "Subcasters",
	          new DataNode[]{} // Initially empty of children.
	      		);

	      // This class's injections.
	      this.theUnicasterFactory= theUnicasterFactory;
	      }

    public synchronized Subcaster buildAddAndStartSubcaster(
    		String keyString
	  		)
	    { 
    	  //appLogger.info( "Creating new Subcaster." );
	      final SubcasterValue resultSubcasterValue=  // Making the Subcaster. 
	      	theUnicasterFactory.makeSubcasterValue( 
	      			keyString 
	      			);
	      addingV( // Adding new Subcaster to data structures.
	          keyString, resultSubcasterValue
	          );
	      resultSubcasterValue.getEpiThread().startV(); // Start its thread.
	      return resultSubcasterValue.getDataNodeD();
	      }

    // Stopping threads is handled by superclass.

    public synchronized void queueToSubcasterInputStreamV(
    		String keyString,
    		NetcasterPacket theNetcasterPacket
	  		)
	    {
    	  ///
	      }

    public synchronized Subcaster tryGettingReadySubcaster(
    		String keyString
	  		)
      /* Tries to return a Subcaster with SockPackets from its NetcasterOutputStream.
        If no Subcaster's have any packets ready then it returns null.
        Doing it this way instead of somehow 
        associating the keyString to the NetcasterPacket
        moves complexity from the NetcasterOutputStream to this class.
        */ 
	    {
    	  return null; ///
	      }

    }
