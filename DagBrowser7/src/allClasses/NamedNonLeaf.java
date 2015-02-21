package allClasses;

public abstract class NamedNonLeaf 

  extends AbDataNode

  {

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

    }
