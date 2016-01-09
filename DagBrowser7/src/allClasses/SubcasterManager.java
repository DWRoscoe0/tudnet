package allClasses;

//import static allClasses.Globals.appLogger;

public class SubcasterManager

	extends MutableListWithMap< 
		String, // Key for map.
		SubcasterValue, // Value for map. 
		Subcaster // DataNode in Value.
		>

	/* This class manages a Unicaster's Subcasters.
	  It provides methods for creating them, storing references to them,
	  displaying them, testing for their existence, and destroying them.
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

  	}
