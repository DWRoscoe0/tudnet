package allClasses;

public class ErrorDataNode 
  extends AbDataNode

  /* This class is a DataNode that serves as a place-holder
    to indicate an error but also to avoid null pointer references.  
    It is used when MetaNodes are reconstructed from 
    MetaFile disk storage and a node name is encountered which
    has no associated DataNode at that point in the tree.
    For example, this can happen when an external disk is disconnected
    after the last time the app was run and its folders
    are no longer readable.
    
    ??? Change name to NullDataNode or UnknownDataNode
    to better describe its use.
    ??? Change to include the associated node name so that
    it can be rewritten to MetaFile disk storage at exit time.
    */

  { // class ErrorDataNode
  
    private static ErrorDataNode TheErrorDataNode= // The single instance.
      new ErrorDataNode();

    private ErrorDataNode( )
      // Constructor.
      { 
        // Misc.DbgOut( "  ErrorDataNode( ) " );
        }  // To make debugging easier.

    public String GetNameString( )
      /* Returns String representing name of this Object.  */
      {
        return "ErrorDataNode";
        }
        
    public static ErrorDataNode getSingletonErrorDataNode( )
      { 
        // System.out.print( "  getSingletonErrorDataNode( ) " );
        return TheErrorDataNode; 
        }

    } // class ErrorDataNode
