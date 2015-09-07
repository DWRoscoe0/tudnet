package allClasses;

import static allClasses.Globals.appLogger;


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

    NamedNonLeaf ( String nameString )  // Constructor.
      { 
        super( ); 
        this.nameString = nameString;  // Store this node's name.
        }

    public String getNameString( )
      /* Returns String representing name of this Object.  */
      {
        return nameString;  // Simply return the name.
        }

    public void setNameStringV( String nameString )
      /* Replaces the String representing name of this Object.
		    This should happen once only very shortly after construction, 
		    and only to replace a temporary value set to enable 
		    lazy loading of the remainder of the node.
		    */
      {
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

        this.nameString= nameString;
        }


    }
