package allClasses;

//import static allClasses.Globals.appLogger;

public class SubcasterManager

	extends StreamcasterManager< 
		String, // Key for map.
		Subcaster, // DataNode in Value.
		SubcasterValue // Value for map. 
		>

	/* This class is a StreamcasterManager specialized to manage 
    a Unicaster's Subcasters.
    */
	  
  {

	  // Injected dependency variables.
	  private final UnicasterFactory theUnicasterFactory;
	  private DefaultBooleanLike leadingDefaultBooleanLike= null;
	  		// Determines whether Subcasters will be leaders vs. followers.

		public SubcasterManager(  // Injecting constructor. 
	      AppGUIFactory theAppGUIFactory,
	      UnicasterFactory theUnicasterFactory,
	  	  DefaultBooleanLike leadingDefaultBooleanLike
	      )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableListWithMap superclass.
		        "Subcasters",
			      theAppGUIFactory,
	          new DataNode[]{} // Initially empty of children.
	      		);

	      // This class's constructor injections.
	      this.theUnicasterFactory= theUnicasterFactory;
    	  this.leadingDefaultBooleanLike= leadingDefaultBooleanLike; 
	      }

		/*  //%
    public synchronized void setLeadingV( // Injecting setter. 
    		DefaultBooleanLike leadingDefaultBooleanLike )
    	{ 
    	  this.leadingDefaultBooleanLike= 
    	  		leadingDefaultBooleanLike; 
    	  }
		*/  //%

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
			      			keyString, leadingDefaultBooleanLike
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
