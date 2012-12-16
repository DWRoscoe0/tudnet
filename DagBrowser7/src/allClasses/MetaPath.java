package allClasses;

import javax.swing.tree.TreePath;
 
class MetaPath

  /* This class is used to store meta-data associated with
    nodes of the Infogora DAG (Directed Acyclic Graph).
    This includes information about which node children
    were referenced or visited most recently.
    A location within the DAG is specified with
    a TreePath when information is to be read or stored.
    
    Possible optimizations (not needed yet):
    
      Speed:
        To reduce TreePath tracing, 
        cach the most recently used TreePath argument(s).
        Check for the path, its parent, and possibly grandparent.
        
      Space:
        Eliminate nodes which:
          * have no children,
          * have no attributes, and
          * are not the most recently referenced or are not the top/default node.

    */
      
  { // class MetaPath.
  
    // Setter methods.  These write information to tree.
          
      static public void UpdatePath( TreePath TreePathIn )
        /* This does the same as UpdatePathITreeNode(.) except 
          it doesn't return the MetaNode associated with the end of the path.
          It exists mainly to help other code be self-documenting.
          */
        { // UpdatePath(.)
          UpdatePathMetaNode( TreePathIn ); // Update with TreePath.
          } // UpdatePath(.)

      static public DataNode UpdatePathDataNode
        ( TreePath TreePathIn )
        /* Updates the MetaPath with TreePathIn and returns the user object of
          the most recently visited child of the tree node at the end of that path,
          or null if there is no such child. 
          */
        { // UpdateToAndGetRecentChildUserObjectAt(.)
          MetaNode EndOfPathMetaNode=  // Get last MetaNode in path by...
            UpdatePathMetaNode(  // ...updating tree with...
              TreePathIn  // ...the provided TreePath.
              );
          DataNode ChildDataNode=  // Get the last MetaNode's...
            EndOfPathMetaNode.GetLastReferencedChildDataNode(  // ...most recent user object.
              );
          return ChildDataNode;  // return the resulting child user object.
          } // UpdateToAndGetRecentChildUserObjectAt(.)

      static public MetaNode UpdatePathMetaNode( TreePath TreePathIn )
        /* Updates the MetaPath structure anchored
          starting with the root and ending at the node specified by InTreePath.
          * It adds to the structure any part of the path TreePathIn 
            that is not in the structure.
          * It reorders the children so the more recently referenced ones
            can be referenced later.
          It also returns the MetaNode at the end of the specified TreePath.
          */
        { // UpdatePathITreeNode()
          Object[] ObjectsInPath=  // The ObjectsInPath array becomes...
            TreePathIn.getPath();  // ...TreePathIn translated to an array.
          MetaNode FinalMetaNode=  // Final MetaNode becomes MetaNode...
            UpdatePathFromArrayMetaNode(  // ...from the update of the structure...
              ObjectsInPath // ...using the ObjectsInPath array.
              );
          return FinalMetaNode;  // Return final MetaNode in path.
          } // UpdatePathITreeNode()

    // private methods.

      static private MetaNode UpdatePathFromArrayMetaNode( Object[] ObjectsInPath)
        /* This grouping method is similar to UpdatePathMetaNode( TreePath TreePathIn )
          except that the path is specified by the array of path elements ObjectsInPath
          instead of a TreePath.
          */
        { // UpdatePathFromArrayITreeNode()
          MetaNode ScanMetaNode= // Initialize ScanMetaNode to be...
            MetaRoot.SetRootMetaNode( ObjectsInPath[0] );  // ...checked 1st path element.
          int IPathIndex= 1;  // Initialize path element array index to 2nd element.
          while   // Update all remaining path elements into structure.
            (IPathIndex < ObjectsInPath.length)  // Path elements remain.
            { // Update one path element into structure.
              Object DesiredObject=  // Get the user Object from present path element.
                ObjectsInPath[ IPathIndex ];
              MetaNode ChildMetaNode=   // Put it in MetaNode in MetaPath structure.
                ScanMetaNode.PutChildUserObjectMetaNode( DesiredObject );
              ScanMetaNode= ChildMetaNode; // Make child be new scan node.
              IPathIndex++;  // Increment the path element index.
              } // Update one path element into structure.
          return ScanMetaNode;  // return final scan node.
          } // UpdatePathFromArrayITreeNode()

    } // class MetaPath.
