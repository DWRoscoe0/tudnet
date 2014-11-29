package allClasses;

public class UnknownDataNode extends NamedLeaf

  /* This class is a DataNode that serves as a place-holder
    to indicate an error but also to avoid null pointer references.  
    It has at least 2 pourposes:

    1. It is used when MetaNodes are reconstructed from 
    MetaFile disk storage and a node name is encountered which
    has no associated DataNode at that point in the tree.
    For example, this can happen when 
    an external disk is disconnected, or a file is deleted,
    after the last time the app was run,
    so it is no longer readable.
    
    2. It is used by TreeHelper to extend a Whole TreePath
    to a Part TreePath.
    
    This was a Singleton, but isn't since adding a Name String.

    ??? Change name to NullDataNode or UnknownDataNode
    to better describe its use.
    */

  { // class UnknownDataNode
  
    private UnknownDataNode( )  
      /* Constructor with no arguments to create a DataNode for use as
        a Part path place holder.
        */
      { 
        super("!UnknownDataNode"); // Temporary String provided.
        }  // To make debugging easier.

    public UnknownDataNode( String inString )
      /* Constructor with inString argument to be used as 
        a named place-holder in MetaNode tree when part od
        DataNode tree is missing.
        */
      { 
        super( inString ); // Store node name.
        }  // To make debugging easier.

    public static UnknownDataNode newUnknownDataNode( )
      { 
        return new UnknownDataNode();
        }

    public String GetNameString( )
      /* Returns String representing name of this Object.  */
      {
        return super.GetNameString( );  // Return the attached string.
        }

    public static boolean isOneB( Object inObject )
        /* This method returns true if inDataNode is an UnknownDataNode.
          */
        { 
          return ( 
            (DataNode)inObject instanceof UnknownDataNode 
            );
          }

    } // class UnknownDataNode
