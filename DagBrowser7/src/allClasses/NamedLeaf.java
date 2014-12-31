package allClasses;

public class NamedLeaf  

  extends AbDataNode
  
  /* This is a utility class that appears to be 
    simply a leaf with a name.  
   */
  
  { // class NamedLeaf

    private String nameString;  // The name associated with this node.

    NamedLeaf ( String nameString )  // Constructor.
      { 
        super( ); 
        this.nameString = nameString;  // Store this node's name.
        }
    
      public boolean isLeaf( ) 
        {
          return true;  // It is a leaf.
          }

      public String toString( )
        /* Returns String representing name of this Object.  */
        {
          return nameString;  // Simply return the name.
          }
      
    } // class NamedLeaf
