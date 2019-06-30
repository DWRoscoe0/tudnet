package allClasses;


public abstract class NamedNonLeaf 

  extends DataNode

  /* This class is the base class for all named MetaNodes.
    It adds a name and methods to manipulate that name
    to the base DataNode.  

    The name can be changed, but this should happen only
    very shortly after construction, and only to replace
    a temporary value set to enable lazy loading of
    the remainder of the node.
    */
  {
		public static final String temporaryNameString= 
				"NamedNonLeaf.temporaryNameString";
	
    private String nameString;  // The name associated with this node.
    
    public void initializeV()
      /* ///? Change all initializeV(..) methods to return (this)
        so it can be used later by caller as a method parameter. 
        */
	    { 
	    	setNameStringV( // Make default name be class name.
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
    		Nulls.fastFailNullCheckT(nameString);
        this.nameString= nameString;
        }

    // Getters.

    public String getNameString()
      /* Returns String representing name of this Object.  */
      {
    		Nulls.fastFailNullCheckT(nameString);
        return nameString;  // Simply return the name.
        }


    }
