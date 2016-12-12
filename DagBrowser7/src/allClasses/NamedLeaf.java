package allClasses;

public class NamedLeaf
  
  /* This is a utility class that appears to be 
    simply a leaf with a name.  
   */

	extends NamedNonLeaf
  
  { // class NamedLeaf

    NamedLeaf ( String nameString )  // Constructor.
      { 
        super( nameString );
        }
    
      public boolean isLeaf( ) 
        {
          return true;  // Overriding superclass non-leaf result.
          }
      
    } // class NamedLeaf
