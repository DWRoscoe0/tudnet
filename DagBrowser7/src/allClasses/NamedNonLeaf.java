package allClasses;

//% import static allClasses.Globals.appLogger;


public abstract class NamedNonLeaf 

  extends AbDataNode

  /* This class is the base class for all named MetaNodes.
    It has a name.  The name can be changed, but this should happen only
    very shortly after construction, and only to replace
    a temporary value set to enable lazy loading of
    the remainder of the node.
    */
  {
		public static final String temporaryNameString= 
				"NamedNonLeaf.temporaryNameString";
	
    private String nameString;  // The name associated with this node.

    /*  //%
    NamedNonLeaf ( String nameString )  // Constructor.
      { 
        //% super( ); 
        this.nameString = nameString;  // Store this node's name.
        }
    */  //%

    NamedNonLeaf()  // Constructor.  ////// This can go?
	    { 
	    	}
    
    public void initializeV()
      /*  //// Change all initializeV(..) methods to return (this)
        so it can be used as a method parameter. 
        */
	    { 
	    	setNameStringV( // Make default name be class name.
	    			//% getClass().getName() 
	    			//% getClass().getCanonicalName()
	    			getClass().getSimpleName()
	    			);
	    	}
    
    public void initializeV( String nameString )
	    { 
	    	setNameStringV( nameString );
	    	}

    public void setNameStringV( String nameString )
      /* Replaces the String representing name of this Object.
		    This should happen once only very shortly after construction, 
		    and only to replace a temporary value set to enable 
		    lazy loading of the remainder of the node.
		    This is used by the class Outline only.
		    */
      {
    	  /*  //%
	  	  if ( // Error checking
		  	    ( this.nameString != temporaryNameString) // the old name.
		  	    || ( nameString == temporaryNameString) // and new name.
		  	    )
	        appLogger.error(
	          "NamedNonLeaf.setNameStringV("
	          +nameString
	          +") with old value=("
	          +this.nameString
	          +")."
	          );
    	  */  //%

        this.nameString= nameString;
        }

    // Getters.

    public String getNameString( )
      /* Returns String representing name of this Object.  */
      {
        return nameString;  // Simply return the name.
        }


    }
