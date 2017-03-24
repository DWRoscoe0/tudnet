package allClasses;

public class NamedLeaf
  
  /* This is a utility class that appears to be 
    simply a leaf with a name.  
   */

	extends NamedNonLeaf
  
  { // class NamedLeaf

		public static NamedLeaf makeNamedLeaf( String nameString )
			{
				NamedLeaf theNamedLeaf= new NamedLeaf();
				theNamedLeaf.initializeV( nameString );

	  		return theNamedLeaf;
	  		}
	
    public boolean isLeaf( ) 
      {
        return true;  // Overriding superclass non-leaf result.
        }
      
    } // class NamedLeaf
