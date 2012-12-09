package allClasses;


import javax.swing.tree.TreePath;
 
class DagInfo

  /* This class is used to store information associated with
    nodes of the Infogora DAG (Directed Acyclic Graph).
    This includes information about how recently 
    each child of a node was referenced or visited.
    Generally a location within the DAG is specified with
    a TreePath when information is to be read or stored.
    
    Possible speed optimizations (not needed yet).
      Possible optimization ??
        Speed could be improved by caching the most recently used
        TreePath argument(s) and their end ITreeNode-s
        instead of always searching from the root.
        Checking the node, its parent, and possibly grandparent, 
        would be the most likely candidates to be needed next.
    */
      
  { // class DagInfo.
  
    // variables.  all static, so this is a static class.
    
      private static ITreeNode RootITreeNode;  /* Root of tree which 
        holds Dag information.  */

    // Constructor.

      public DagInfo( TreePath TreePathIn )  
        /* This constructor doesn't construct any instance,
          but it does initialize the static variables,
          from TreePathIn.
          */
        { 
          UpdateTreeWith( TreePathIn );  // do this to create initial tree.
          }

    // Setter methods.  These write information to tree.
          
      static public void UpdateTreeWith( TreePath TreePathIn )
        /* This does the same as ITreeNodeFromUpdatedTreeWith(.) except 
          it doesn't return the ITreeNode associated with the end of the path.
          It exists mainly to help other code be self-documenting.
          */
        { // UpdateTreeWith(.)
          ITreeNodeFromUpdatedTreeWith( TreePathIn ); // Update with TreePath.
          } // UpdateTreeWith(.)

    // Getter-Setter methods.  These write to and read from info tree.
            
      static public DagNode UpdateTreeAndGetRecentChildDagNodeAt
        ( TreePath TreePathIn )
        /* Updates the visits tree with TreePathIn
          and returns the user object of
          the most recently visited child of the tree node
          at the end of that path,
          or null if there is no such child. 
          */
        { // UpdateToAndGetRecentChildUserObjectAt(.)
          ITreeNode ITreeNodeAtEndOfPath=  // Get last ITreeNode in path by...
            ITreeNodeFromUpdatedTreeWith(  // ...updating tree with...
              TreePathIn  // ...the provided TreePath.
              );
          DagNode ChildDagNode=  // Get the last ITreeNode's...
            ITreeNodeAtEndOfPath.GetRecentChildDagNode(  // ...most recent user object.
              );
          return ChildDagNode;  // return the resulting child user object.
          } // UpdateToAndGetRecentChildUserObjectAt(.)

      static public ITreeNode ITreeNodeFromUpdatedTreeWith
        ( TreePath TreePathIn )
        /* Updates the tree anchored by RootITreeNode
          starting with the root and ending at 
          the node specified by InTreePath.
          * It adds to the tree any part of the path TreePathIn that is not in the tree.
          * It reorders the children so the referenced links are last, 
            because they are the most recently referenced ones.
          It also returns the ITreeNode at the end of the specified TreePath.
          */
        { // ITreeNodeFromUpdatedTreeWith()
          Object[] ObjectsInPath=  // calculate array of TreePath elements...
            TreePathIn.getPath();  // ...from input TreePath.
          ValidateRootNode( ObjectsInPath[0] );  // Check the tree root.
          ITreeNode ITreeNodeFinal=  // Final ITreeNode becomes ITreeNode...
            ITreeNodeFromUpdatedSubtreesWith(  // ...from subtree update...
              //RootITreeNode,  // ...of tree root subtrees...
              ObjectsInPath,  // ...with the path elements...
              1 // ...starting at the 2nd element.
              );
          return ITreeNodeFinal;  // Return final ITreeNode in path.
          } // ITreeNodeFromUpdatedTreeWith()

    // private methods.
        
      static private void ValidateRootNode( Object FirstTreePathUserObject )
        /* This grouping method makes certain that the info tree root
          has been created and its user object is FirstTreePathUserObject.
          This might be rewritten using NullPointerException handling.
          */
        { // ValidateRootNode( Object FirstTreePathUserObject )
          boolean RootNodeCreationNeeded;
          { // calculate whether RootNodeCreationNeeded.
            if (RootITreeNode == null)  // there is no root.
              RootNodeCreationNeeded= true;
            else if  // root has incorrect user object.
              (!RootITreeNode.getUserObject().equals( FirstTreePathUserObject ))
              { // root replacement needed!
                System.out.println( "DagInfo root replacement1");
                RootNodeCreationNeeded= true;
                } // root replacement needed!
            else  // root has correct user object.
              RootNodeCreationNeeded= false;
            } // calculate whether RootNodeCreationNeeded.
          if (RootNodeCreationNeeded)  // Create tree root if needed.
            { // Create tree root.
              // System.out.println( "DagInfo root assignment");
              RootITreeNode=  // Replace present root with...
                new ITreeNode(  // ...a new ITreeNode containing...
                   FirstTreePathUserObject   // ...the correct user object.
                );
              } // Create tree root.
          } // ValidateRootNode( Object FirstTreePathUserObject )

      static private ITreeNode ITreeNodeFromUpdatedSubtreesWith
        ( Object[] ObjectsOfPath, int IPathIndex )
        /* This method updates the assumed validated info tree
          along the path selected by the elements of the array ObjectsOfPath,
          starting at the array element whose index is IPathIndex.
          It returns a reference to the last selected tree ITreeNode.
          */
        { // ITreeNodeFromUpdatedSubtreesWith()
          ITreeNode ScanITreeNode= RootITreeNode;  // Point to first scan node.
          while   // Update all path elements into tree.
            (IPathIndex < ObjectsOfPath.length)  // Path elements remain.
            { // Update one path element into tree.
              Object DesiredObject=  // cache user Object from array.
                ObjectsOfPath[ IPathIndex ];
              ITreeNode ChildITreeNode=   // make/place new/old child at LRU position.
                ScanITreeNode.PutLastUserChildITreeNode( DesiredObject );
              ScanITreeNode= ChildITreeNode; // Make child be new scan node.
              IPathIndex++;  // Increment the path element index.
              } // Update one path element into tree.
          return ScanITreeNode;  // return final scan node.
          } // ITreeNodeFromUpdatedSubtreesWith()


    // auto-expand-collapse methods, under development, moved to class TreeExpansion.

    } // class DagInfo.
