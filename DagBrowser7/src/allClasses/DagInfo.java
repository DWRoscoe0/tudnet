package allClasses;


import javax.swing.tree.TreePath;
 
class DagInfo

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
          UpdatePath( TreePathIn );  // do this to create initial tree.
          }

    // Setter methods.  These write information to tree.
          
      static public void UpdatePath( TreePath TreePathIn )
        /* This does the same as UpdatePathITreeNode(.) except 
          it doesn't return the ITreeNode associated with the end of the path.
          It exists mainly to help other code be self-documenting.
          */
        { // UpdatePath(.)
          UpdatePathITreeNode( TreePathIn ); // Update with TreePath.
          } // UpdatePath(.)

    // Getter-Setter methods.  These write to and read from info tree.

      static public DagNode UpdatePathDagNode
        ( TreePath TreePathIn )
        /* Updates the DagInfo with TreePathIn and returns the user object of
          the most recently visited child of the tree node at the end of that path,
          or null if there is no such child. 
          */
        { // UpdateToAndGetRecentChildUserObjectAt(.)
          ITreeNode ITreeNodeAtEndOfPath=  // Get last ITreeNode in path by...
            UpdatePathITreeNode(  // ...updating tree with...
              TreePathIn  // ...the provided TreePath.
              );
          DagNode ChildDagNode=  // Get the last ITreeNode's...
            ITreeNodeAtEndOfPath.GetLastReferencedChildDagNode(  // ...most recent user object.
              );
          return ChildDagNode;  // return the resulting child user object.
          } // UpdateToAndGetRecentChildUserObjectAt(.)

      static public ITreeNode UpdatePathITreeNode
        ( TreePath TreePathIn )
        /* Updates the DagInfo structure anchored by RootITreeNode
          starting with the root and ending at the node specified by InTreePath.
          * It adds to the structure any part of the path TreePathIn 
            that is not in the structure.
          * It reorders the children so the more recently referenced ones
            can be referenced later.
          It also returns the ITreeNode at the end of the specified TreePath.
          */
        { // UpdatePathITreeNode()
          Object[] ObjectsInPath=  // calculate array of TreePath elements...
            TreePathIn.getPath();  // ...from input TreePath.
          ValidateRootNode( ObjectsInPath[0] );  // Check the tree root.
          ITreeNode ITreeNodeFinal=  // Final ITreeNode becomes ITreeNode...
            UpdatePathFromArrayITreeNode(  // ...from subtree update...
              //RootITreeNode,  // ...of tree root subtrees...
              ObjectsInPath,  // ...with the path elements...
              1 // ...starting at the 2nd element.
              );
          return ITreeNodeFinal;  // Return final ITreeNode in path.
          } // UpdatePathITreeNode()

    // private methods.

      static private ITreeNode UpdatePathFromArrayITreeNode
        ( Object[] ObjectsOfPath, int IPathIndex )
        /* This method updates the DagInfo structure,
          assumed to be validated and rooted at RootITreeNode,
          along the path selected by the elements of the array ObjectsOfPath,
          starting at the array element whose index is IPathIndex.
          It returns a reference to the last selected ITreeNode.
          */
        { // UpdatePathFromArrayITreeNode()
          ITreeNode ScanITreeNode= RootITreeNode;  // Point to first scan node.
          while   // Update all path elements into tree.
            (IPathIndex < ObjectsOfPath.length)  // Path elements remain.
            { // Update one path element into tree.
              Object DesiredObject=  // Get the user Object from present path element.
                ObjectsOfPath[ IPathIndex ];
              ITreeNode ChildITreeNode=   // Put it in ITreeNode in DagInfo structure.
                ScanITreeNode.PutChildUserObjectITreeNode( DesiredObject );
              ScanITreeNode= ChildITreeNode; // Make child be new scan node.
              IPathIndex++;  // Increment the path element index.
              } // Update one path element into tree.
          return ScanITreeNode;  // return final scan node.
          } // UpdatePathFromArrayITreeNode()
        
      static private void ValidateRootNode( Object FirstTreePathUserObject )
        /* This grouping method makes certain that the info tree root
          has been created and its user object is FirstTreePathUserObject.
          */
        { // ValidateRootNode( Object FirstTreePathUserObject )
          boolean RootNodeCreationNeeded;
          { // calculate whether RootNodeCreationNeeded.
          
            // This null check might be handled using a NullPointerException.
            if (RootITreeNode == null)  // there is no root yet.
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

    } // class DagInfo.
