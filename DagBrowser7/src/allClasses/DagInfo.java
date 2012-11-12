package allClasses;

import javax.swing.tree.TreePath;
 
class DagInfo

  /* This class is used to store information associated with
    nodes of the Infogora DAG (Directed Acyclic Graph).
    This includes information about how recently 
    each child of a node was referenced.
    
    Possible speed optimizations (not needed yet).
      ITreeNodeFromUpdatedSubtreesWith( ):
        refactor: avoid mutating unless it's needed.
        do fast check for path already using last child.
        Call MoveToTail method only if needed.
      Possible optimization ??
        Speed could be improved by caching the most recently used
        TreePath argument(s) and their end ITreeNode-s
        instead of always searching from the root.
        Checking the node, its parent, and possibly grandparent, 
        would be the most likely candidates to be needed next.
      ITreeNodeFromUpdatedSubtreesWith():
        Use loop controlled by NullPointerException 
        to trigger ValidateRootNode( ObjectsInPath[0] ) 
        or other initialization.
    */
      
  { // class DagInfo.
  
    // variables.  all static, so this is a static class.
    
      private static ITreeNode RootITreeNode;  /* Root of tree which 
        holds Dag information.  */

    // constructor.

      public DagInfo( TreePath TreePathIn )  
        /* This constructor doesn't construct any instance,
          but it does initialize the static variables,
          from TreePathIn.
          */
        { 
          UpdateTreeWith( TreePathIn );  // do this to create initial tree.
          }

    // setter methods.  these write information to tree.
          
      static public void UpdateTreeWith( TreePath TreePathIn )
        /* This does the same as ITreeNodeFromUpdatedTreeWith(.) except 
          it doesn't return the ITreeNode associated with the end of the path.
          It exists mainly to help other code be self-documenting.
          // It also does nothing if the argument is null.
          */
        { // UpdateTreeWith(.)
          //if ( TreePathIn != null )  // Update with TreePath if not null.
          //  ITreeNodeFromUpdatedTreeWith( TreePathIn ); // Update with TreePath.
          //  else
          //  System.out.println( "DagInfo. UpdateTreeWith( null )"); // ???
          ITreeNodeFromUpdatedTreeWith( TreePathIn ); // Update with TreePath.
          } // UpdateTreeWith(.)

    // getter-setter methods.  these write to and read from info tree.
            
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
          DagNode ChildDagNode=  // Get the user object which is...
            GetRecentChildDagNode(  // ...the most recently referenced child of...
              ITreeNodeAtEndOfPath  // ...the last node.
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

      static private DagNode GetRecentChildDagNode( ITreeNode ITreeNodeIn )
        /* This grouping method returns the user object DagNode of 
          the most recently visited child of 
          the ITreeNode specified by ITreeNodeIn,
          or it returns null if there is no such object.
          */
        { // GetRecentChildUserObject( ITreeNode ITreeNodeIn )
          DagNode RecentChildDagNode= null;// assume default value of null.
          switch (0) { // override with child if there is one.
            default:  // always start here.  switch allows break-outs.
            if (ITreeNodeIn == null)  // no ITreeNode was provided.
              break ;  // so keep the null result.
            if // ITreeNode was provided but there are no children attached.
              (ITreeNodeIn.getChildCount() == 0)  
              break ;  // so keep the default null result.
            ITreeNode  // there are children so try to get last one.
              LastChildITreeNode= ((ITreeNode)(ITreeNodeIn.getLastChild()));
            if (LastChildITreeNode == null)  // there is no last child.
              break ;  // so keep the default null result.
            RecentChildDagNode=  // Result recent child DagNode is...
              (DagNode)  // ...a DagNode caste of...
              LastChildITreeNode.   // ...the last child's...
              getUserObject();  // user object.
            } // override with child if there is one.
          return RecentChildDagNode; // return resulting DagNode, or null if none.
          } // GetRecentChildUserObject( ITreeNode ITreeNodeIn )

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
              ITreeNode ChildITreeNode=  // search for node with user object.
                UserObjectSearchITreeNode( ScanITreeNode, DesiredObject );
              { // remove found child node from visits tree or create new one.
                if (ChildITreeNode == null) // child node wasn't found.
                  ChildITreeNode= // create node with desired Object.
                    new ITreeNode(DesiredObject);
                else // child node was found.
                  ChildITreeNode.removeFromParent();  // unlink child.
                } // remove found child node from visits tree or create new one.
              ScanITreeNode.insert( // insert...
                ChildITreeNode, // ...the unlinked old or newly created child...
                ScanITreeNode.getChildCount() // ...as last child of parent.
                );
              ScanITreeNode= ChildITreeNode; // Make child be new scan node.
              IPathIndex++;  // Increment the path element index.
              } // Update one path element into tree.
          return ScanITreeNode;  // return final scan node.
          } // ITreeNodeFromUpdatedSubtreesWith()

      static private ITreeNode UserObjectSearchITreeNode
        (ITreeNode TheITreeNode,Object DesiredObject)
        /* This grouping method searches TheITreeNode's children
          for one containing the user object DesiredObject.
          */
        { // UserObjectSearchITreeNode(.)
          ITreeNode ChildITreeNode= null; // set null default result.
          int SearchIndexI=  // initialize search index to be...
            TheITreeNode.getChildCount();  // ... child count.
          while  // search for child starting with last one.
            (SearchIndexI > 0 && ChildITreeNode == null)  // children remain.
            { // test one child.
              SearchIndexI--;  // decrement child index to select next one.
              ITreeNode SearchChildITreeNode= // get child.
                (ITreeNode)
                TheITreeNode.getChildAt(SearchIndexI);
              Object ChildElementObject=  // get child's user object.
                SearchChildITreeNode.getUserObject();
              if  // record this child node if it has desired user Object.
                ( ChildElementObject.equals(DesiredObject) )  // correct one?
                ChildITreeNode=  // yes, record found child node as result.
                  SearchChildITreeNode; 
              } // test one child.
          return  ChildITreeNode;  // return result child.
          } // UserObjectSearchITreeNode(.)

    // auto-expand-collapse methods, under development.

      static public void SetAutoExpanded
        ( TreePath TreePathIn, boolean AutoExpandInB )
        /* This method stores the value of the AutoExpandedB flag
          from the value AutoExpandInB
          in the info tree node specified by TreePathIn.
          */
        { 
          ITreeNode TheITreeNode= ITreeNodeFromUpdatedTreeWith( TreePathIn );
          TheITreeNode.AutoExpandedB= AutoExpandInB;
          }
      
      static public boolean GetAutoExpandedB( TreePath TreePathIn )
        /* This method returns the value of the AutoExpandedB flag
          in the info tree node specified by TreePathIn.
          */
        { 
          ITreeNode TheITreeNode= ITreeNodeFromUpdatedTreeWith( TreePathIn );
          return TheITreeNode.AutoExpandedB; 
          }

      static public TreePath FollowAutoExpandToTreePath
        ( TreePath StartTreePath )
        /* This method tries to follow a chain of 
          the most recently visited AutoExpanded nodes
          starting with the node named by StartTreePath.
          It returns the TreePath of the first node not AutoExpanded,
          or null if there was no AutoExpanded node.
          
          ?? base on code from ITreeNodeFromUpdatedSubtreesWith(.).
          */
        { // FollowAutoExpandToTreePath( TreePath StartTreePath )
          TreePath ScanTreePath=   // initialize TreePath scanner to be...
            StartTreePath;  // ...start TreePath.
          ITreeNode ScanITreeNode= // initialize ITreeNode scanner to be...
            ITreeNodeFromUpdatedTreeWith( // ...ITreeNode at end...
              ScanTreePath );  // ...of ScanTreePath.
          while   // follow chain of all nodes with auto-expanded flag set.
            ( ScanITreeNode.AutoExpandedB )  // auto-expanded flag set?
            { // yes, process one auto-expanded node.
              ITreeNode ChildITreeNode= // get most recently referenced child.
                (ITreeNode)ScanITreeNode.getChildAt(
                  ScanITreeNode.getChildCount()-1);
              Object ChildUserObject= ChildITreeNode.getUserObject();
              ScanTreePath=  // create ScanTreePath of next node...
                ScanTreePath.pathByAddingChild( // ...by adding to it...
                  ChildUserObject);  // ...the child user Object.
              ScanITreeNode= ChildITreeNode; // make next scan node be child.
              } // yes, process one auto-expanded node.
          if ( ScanTreePath == StartTreePath ) // if we haven't moved.
            ScanTreePath=  null;  // replace ScanTreePath with null.
          return ScanTreePath;  // return final ScanTreePath as result.
          } // FollowAutoExpandToTreePath( TreePath StartTreePath )

    } // class DagInfo.
