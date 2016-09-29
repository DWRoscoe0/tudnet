package allClasses;

public class DataNodeWithKey< K > 

  extends MutableList 
  
  /* This class adds a Key value to a MutableList.
    The Key is typically used for two things:
    * As an extension of the base name when 
      forming the full name String of this node.
    * As the key for a hash table lookup.
   */
  
  {
	  private K theKeyK;
	  
	  public DataNodeWithKey(  // Constructor.
	      DataTreeModel theDataTreeModel,
	      String baseNameString,
	  		K theKeyK
	      )
	    {
	  	  super( // Constructing MutableList superclass with injections.
	  		    theDataTreeModel,
		        baseNameString, // Base name but not entire name.
	          new DataNode[]{} // Initially empty array of children.
	      		);
	
	      // This class's injections.
	      this.theKeyK= theKeyK;
        }

    public String getNameString( )
      /* Returns full String representing name of this Object.  */
      {
        return // Returning a combination of
        		super.getNameString( ) + // the base name 
        		"-" + // and
        		theKeyK; // the String conversion of the associated key.
        }

		public K getKeyK()
		  /* Returns the key, typically used for hash table lookups.  */
			{ return theKeyK; }

  	}
