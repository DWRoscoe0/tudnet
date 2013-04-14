package allClasses;

public class SingleChildDataNode

  extends AbDataNode
  
  /* This class is a DataNode that has only a single child DataNode.
    It serves as a parent of the root of the DataNode DAG.
    This makes the MetaNode:ioDataNode(...) code simpler because 
    it doesn't need to handle the root DataNode as 
    a special case.  */

  { // class SingleChildDataNode

    // Variables.
      private DataNode ChildDataNode;  // It's only child DataNode.
      
    // Constructor.

      public SingleChildDataNode( DataNode InDataNode )
        { // SingleChildDataNode( DataNode InDataNode )
          ChildDataNode= InDataNode;  // Save DataNode as child.
          } // SingleChildDataNode( DataNode InDataNode )
    
    // A subset of delegated AbstractTreeModel methods.

      public DataNode getChild( int IndexI ) 
        /* This returns the child with index IndexI.  */
        {
          switch ( IndexI ) {
            case 0: return ChildDataNode;
            }
          return null;  // anything else returns null.
          }

      public String GetNameString( )
        /* Returns String representing name of this Object.  */
        {
          return // Return name which is composite of...
            "PARENT-OF-" +  // ...this and...
            ChildDataNode.GetNameString( );  // ...this.
          }

    } // class SingleChildDataNode
