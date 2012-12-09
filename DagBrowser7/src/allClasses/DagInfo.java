package allClasses;

import java.util.Iterator;
import java.util.Map;

import javax.swing.tree.TreePath;
 
class DagInfo

  /* This class is used to store information associated with
    nodes of the Infogora DAG (Directed Acyclic Graph).
    This includes information about how recently 
    each child of a node was referenced or visited.
    Generally a location with the DAG is specified with
    a TreePath when information is to be read or stored.
    
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
                PutLastUserChildITreeNode( ScanITreeNode, DesiredObject );
              ScanITreeNode= ChildITreeNode; // Make child be new scan node.
              IPathIndex++;  // Increment the path element index.
              } // Update one path element into tree.
          return ScanITreeNode;  // return final scan node.
          } // ITreeNodeFromUpdatedSubtreesWith()

          
      // The following are low-level LRU child routines.
      
      static private DagNode GetRecentChildDagNode( ITreeNode InITreeNode )
        /* This grouping method returns the user object DagNode of 
          the most recently visited child of 
          the ITreeNode specified by InITreeNode,
          or it returns null if there is no such object.
          */
        { // GetRecentChildDagNode( ITreeNode InITreeNode )
          DagNode RecentChildDagNode= null;// assume default value of null.
          switch (0) { // override with child if there is one.
            default:  // always start here.  switch allows break-outs.
            if (InITreeNode == null)  // no ITreeNode was provided.
              break ;  // so keep the null result.
            /*
            if // ITreeNode was provided but there are no children attached.
              (InITreeNode.getChildCount() == 0)  
              break ;  // so keep the default null result.
            ITreeNode  // there are children so try to get last one.
              LastChildITreeNode= ((ITreeNode)(InITreeNode.getLastChild()));
            */
            ITreeNode LastChildITreeNode= GetLastChildITreeNode( InITreeNode );
            if (LastChildITreeNode == null)  // there is no last child.
              break ;  // so keep the default null result.
            RecentChildDagNode=  // Result recent child DagNode is...
              (DagNode)  // ...a DagNode caste of...
              LastChildITreeNode.   // ...the last child's...
              getUserObject();  // user object.
            } // override with child if there is one.
          return RecentChildDagNode; // return resulting DagNode, or null if none.
          } // GetRecentChildDagNode( ITreeNode InITreeNode )

      static public ITreeNode GetLastChildITreeNode(  ITreeNode InITreeNode )
        /* This grouping method gets the last child referenced in an ITreeNode InITreeNode,
          or null if there is none.  InITreeNode must not be null.
          */
        { // GetLastChildITreeNode( ITreeNode InITreeNode )
          ITreeNode LastChildITreeNode= null;  // Assume there is no last child.
          
          //if (InITreeNode.getChildCount() != 0)  // There are children.
          //  LastChildITreeNode= // Get the last one as an ITreeNode.
          //    ((ITreeNode)(InITreeNode.getLastChild()));
              
          // Get last child referenced from LinkedHashMap.
          Iterator < Map.Entry < Object, ITreeNode > > MapIterator=  // Get an iterator...
            InITreeNode.  // ...for this ITreeNode's...
            ChildrenLinkedHashMap.entrySet().iterator();  // ...HashMap entries.
          while // Use Iterator to get the HashMap's last Entry's Value.
            ( MapIterator.hasNext() ) // If there is a next Entry...
            LastChildITreeNode= // ...get a reference to...
              (ITreeNode)  // ...the ITreeNode which is...
              MapIterator.next().  // ...that next Entry's...
              getValue();  // ...Value.

          return LastChildITreeNode; // return last child ITreeNode result, if any.
          } // GetLastChildITreeNode( ITreeNode InITreeNode )

      static private ITreeNode PutLastUserChildITreeNode
        ( ITreeNode ParentITreeNode, Object DesiredObject )
        /* This method puts the object DesiriedObject in a child ITreeNode
          at the LRU position within the parent ITreeNode ParentITreeNode.
          It creates and uses a new ITreeNode if one with the DesiredObject
          does not already exist.
          In either case, it returns the/a ITreeNode with the DesiredObject.
          */
        { // ITreeNode PutLastUserChildITreeNode( ParentITreeNode, DesiredObject )
          ITreeNode NewChildITreeNode= // Create new ITreeNode with desired Object.
            new ITreeNode( DesiredObject );  // It might or mignt not be used ahead.

          ITreeNode MapChildITreeNode=  // Try to get the ITreeNode and move-to-front...
            ParentITreeNode.ChildrenLinkedHashMap.get(  // ...in the child LinkedHashMap...
              DesiredObject );  // ... from the entry containing the DesiredObject.
          if ( MapChildITreeNode == null ) // Create new HashMap entry if not there.
            { // Create new HashMap entry.
              MapChildITreeNode= // Create new ITreeNode with desired Object.
                //new ITreeNode( DesiredObject );
                NewChildITreeNode;  
              ParentITreeNode.ChildrenLinkedHashMap.put(   // Add new entry which maps...
                DesiredObject,  // ...DesiredObject to...
                MapChildITreeNode  // ... the ITreeNode containing it created earlier.
                );
              } // Create new HashMap entry.

          /*
          // ???? following block of old DefaultMutableTreeNode code to eventually be removed.
          ITreeNode ChildITreeNode=  // search for node with user object.
            UserObjectSearchITreeNode( ParentITreeNode, DesiredObject );
          { // remove found child node from visits tree or create new one.
            if (ChildITreeNode == null) // child node wasn't found.
              ChildITreeNode= // create new node with desired Object.
                // new ITreeNode(DesiredObject);
                NewChildITreeNode;  
            else // child node was found.
              ChildITreeNode.removeFromParent();  // unlink child from its parent.
            } // remove found child node from visits tree or create new one.
          ParentITreeNode.insert( // insert...
            ChildITreeNode, // ...the old unlinked or new created child...
            ParentITreeNode.getChildCount() // ...as last child of parent.
            );
          */

          // return ChildITreeNode;  // Return new/old child as result.
          return MapChildITreeNode;  // Return new/old child from map as result.
          } // ITreeNode PutLastUserChildITreeNode( ParentITreeNode, DesiredObject )

      /* ???
      static private ITreeNode UserObjectSearchITreeNode
        (ITreeNode ThisITreeNode,Object DesiredObject)
        /* This grouping method searches ThisITreeNode's children
          for one containing the user object DesiredObject.
          */
      /* ???
        { // UserObjectSearchITreeNode(.)
          ITreeNode ChildITreeNode= null; // set null default result.
          int SearchIndexI=  // initialize search index to be...
            ThisITreeNode.getChildCount();  // ... child count.
          while  // search for child starting with last one.
            (SearchIndexI > 0 && ChildITreeNode == null)  // children remain.
            { // test one child.
              SearchIndexI--;  // decrement child index to select next one.
              ITreeNode SearchChildITreeNode= // get child.
                (ITreeNode)
                ThisITreeNode.getChildAt(SearchIndexI);
              Object ChildElementObject=  // get child's user object.
                SearchChildITreeNode.getUserObject();
              if  // record this child node if it has desired user Object.
                ( ChildElementObject.equals(DesiredObject) )  // correct one?
                ChildITreeNode=  // yes, record found child node as result.
                  SearchChildITreeNode; 
              } // test one child.
          return  ChildITreeNode;  // return result child.
          } // UserObjectSearchITreeNode(.)
        */

    // auto-expand-collapse methods, under development, moved to class TreeExpansion.

    } // class DagInfo.
