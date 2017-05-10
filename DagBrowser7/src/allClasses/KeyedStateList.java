package allClasses;

public class KeyedStateList< K > 

	extends StateList  // was AndState and MutableList before that. 
  
  /* This class adds a Key value to an AndState StateList.
    The Key is typically used for two things:
    * As an extension of the base name when 
      forming the full name String of this node.
    * As the key for a hash table lookup.

    Because sub-classes of this class typically do more than simply hold data,
    this class extends AndState, so it can itself be 
    a state-machine and have sub-states.

    */
  
  {
	  private K theKeyK;
	  
	  public KeyedStateList(  // Constructor.
	      String baseNameString,
	  		K theKeyK
	      )
	    {
	  		initializeV(
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
