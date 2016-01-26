package allClasses;

public class DataNodeWithKey< K > 

  extends MutableList 
  
  {
	  private K theKeyK;
	  
	  public DataNodeWithKey(  // Constructor.
	      DataTreeModel theDataTreeModel,
	      String typeString,
	  		K theKeyK
	      )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableList superclass.
		        theDataTreeModel,
		        typeString, // Type name but not entire name.
	          new DataNode[]{} // Initially empty array of children.
	      		);
	
	      // This class's injections.
	      this.theKeyK= theKeyK;
        }

    public String getNameString( )
      /* Returns full String representing name of this Object.  */
      {
        return // Returning a combination of
        		super.getNameString( ) + // the type name 
        		"-" + // and
        		theKeyK; // the String conversion of the associated key.
        }

		public K getKeyK()
			{ return theKeyK; }

  	}
