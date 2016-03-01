package allClasses;

//import static allClasses.Globals.appLogger;

public class SubcasterManager

	extends StreamcasterManager< 
		String, // Key for map.
		Subcaster, // DataNode in Value.
		SubcasterValue // Value for map. 
		>

	/* This class is a Streamcaster specialized to manage 
    a Unicaster's Subcasters.
    */
	  
  {

		private final UnicasterFactory theUnicasterFactory;
	
		public SubcasterManager(  // Constructor. 
	      DataTreeModel theDataTreeModel,
	      AppGUIFactory theAppGUIFactory,
	      UnicasterFactory theUnicasterFactory
	      )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableListWithMap superclass.
		        theDataTreeModel,
		        "Subcasters",
			      theAppGUIFactory,
	          new DataNode[]{} // Initially empty of children.
	      		);

	      // This class's injections.
	      this.theUnicasterFactory= theUnicasterFactory;
	      }

    public synchronized Subcaster getOrBuildAddAndStartSubcaster(
    		String keyString
	  		)
      /* This method returns the Subcaster associated with keyString.
        If necessary it builds and starts the Subcaster.
        */
	    { 
	      SubcasterValue resultSubcasterValue=  // Making the Subcaster. 
	      			          childHashMap.get(keyString);
	      if // Making Subcaster if it doesn't exist.
		      ( resultSubcasterValue == null )
		      { // Making and starting the Subcaster.
	    	  	//appLogger.info( "Creating new Subcaster." );
			      resultSubcasterValue=  // Making the Subcaster. 
			      	theUnicasterFactory.makeSubcasterValue( 
			      			keyString 
			      			);
			      addingV( // Adding new Subcaster to data structures.
			          keyString, resultSubcasterValue
			          );
			      resultSubcasterValue.getEpiThread().startV(); // Start its thread.
		    		}
    		Subcaster theSubcaster= // Get its Subcaster data node from value. 
    				resultSubcasterValue.getDataNodeD(); 
	      return theSubcaster;
	      }

    // Stopping threads is handled by superclass.

    }
