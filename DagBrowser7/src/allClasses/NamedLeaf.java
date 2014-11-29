package allClasses;

public class NamedLeaf  extends AbDataNode
  
  /* This is a utility class that appears to be simply a leaf with a name.  
   */
  
  { // class NamedLeaf

    private String theString;  // The name associated with this node.

    NamedLeaf ( String inString )  // Constructor.
      { 
        super( ); 
        theString = inString;  // Store this node's name.
        }
    
      public boolean isLeaf( ) 
        {
          return true;  // It is a leaf.
          }

      public String toString( )
        /* Returns String representing name of this Object.  */
        {
          return theString;  // Simply return the name.
          }
      
    } // class NamedLeaf
