package allClasses;

public class NamedLeaf  

  extends NamedNonLeaf
  
  /* This is a utility class that appears to be 
    simply a leaf with a name.  
   */
  
  { // class NamedLeaf

    NamedLeaf ( String nameString )  // Constructor.
      { 
        super( nameString );
        }
    
      public boolean isLeaf( ) 
        {
          return true;  // Overriing superclass non-leaf result.
          }
      
    } // class NamedLeaf
