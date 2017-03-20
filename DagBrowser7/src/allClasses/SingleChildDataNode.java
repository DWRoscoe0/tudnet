package allClasses;

public class SingleChildDataNode

  extends //% AbDataNode
  DataNode
  
  /* This class is a DataNode that has only a single child DataNode.
    It serves as a parent of the root of the DataNode DAG.
    This makes the MetaNode:ioDataNode(...) code simpler because 
    it doesn't need to handle the root DataNode as 
    a special case.  

    This is used by only DataRoot, it might be appropriate to:
    * Change the name to RootDataNode.
    * Add another method isAncestorOfRootB() to to AbDataNode
      which only this class returns as true to make
      testing for in the illegal pseudo-parent status easier and faster.
    */

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

      public String getNameString( )
        /* Returns String representing name of this Object.  */
        {
          return // Return name which is composite of...
            "PARENT-OF:" +  // ...this and...
            ChildDataNode.getNameString( );  // ...this.
          }

    } // class SingleChildDataNode
