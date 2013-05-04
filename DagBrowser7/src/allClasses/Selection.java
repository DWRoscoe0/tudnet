package allClasses;

import javax.swing.tree.TreePath;

public class Selection 

  /* This static class manages DataNode selections.  
    
    Selections are specified by TreePath-s.
    
    Selection history information is stored in the MetaNode DAG.
    This is useful when revisiting DataNode-s and their chldren.
    When a command is given at a node to GoToChild,
    instead of always visiting the first child,
    selection history is used to visit the child last visited.
    
    Originally history information was stored as MRU/LRU lists
    of children in MetaNodes.
    This is being changed to store the information in MetaNode attributes.
	For a while it might be stored in both, 
	until I am certain I can eliminate one.

    */

  { // class Selection

    // Static setter methods.  These write to the MetaNode DAG.
          
      static public void put( TreePath TreePathIn )
        /* This does the same as putAndReturnDataNode(.) except 
          it doesn't return the MetaNode associated with 
          the end of the path.
          It exists mainly to help other code be self-documenting.
          */
        { // put(.)
      	  putAndReturnMetaNode( TreePathIn ); // Update with TreePath.
          } // put(.)

      static public DataNode putAndReturnDataNode( TreePath TreePathIn )
        /* Updates the PathAttributeMetaTool with TreePathIn and returns 
          the user object of the most recently visited child of 
          the tree node at the end of that path,
          or null if there is no such child. 
          */
        { // putAndReturnDataNode(.)
          MetaNode EndOfPathMetaNode=  // Get last MetaNode in path by...
            putAndReturnMetaNode(  // ...updating tree with...
              TreePathIn  // ...the provided TreePath.
              );
          DataNode ChildDataNode=  // Get the last MetaNode's...
            EndOfPathMetaNode.  // ...most recent user object.
              GetLastReferencedChildDataNode( );
          return ChildDataNode;  // return the resulting child user object.
          } // putAndReturnDataNode(.)

      static public MetaNode putAndReturnMetaNode( TreePath InTreePath )
        /* Updates the PathAttributeMetaTool structure
          starting with the root and ending at the node specified by InTreePath.
          * It adds to the structure any part of the path InTreePath 
            that is not in the structure.
          * It reorders the children so the more recently referenced ones
            can be referenced quickly later.
          It also returns the MetaNode at the end of the specified TreePath.
          */
        { // putAndReturnMetaNode()

          PathAttributeMetaTool WorkerPathAttributeMetaTool= 
            new PathAttributeMetaTool( // Create new PathAttributeMetaTool...
              InTreePath,  // ...to work on InTreePath.
              "SelectionPath"
              );
          WorkerPathAttributeMetaTool.setPath( );  // Set path attributes.
          return WorkerPathAttributeMetaTool.getMetaNode();

          } // putAndReturnMetaNode()
          
      static public MetaNode getLastSelectedChildOfMetaNode
        ( MetaNode InMetaNode )
        /* This method returns the most recently selected child MetaNode 
          of InMetaNode.
          
          Prsently it is a pass-through to MetaNode.GetLastChildMetaNode(). 
          */
        { return InMetaNode.GetLastChildMetaNode(); }

      /*
      static private DataNode getLastSelectedChildOfDataNode
        ( MetaNode InMetaNode )
        /* This method returns the user object DataNode from
          the child MetaNode of InMetaNode which was referenced last, 
          or null if there are no child MetaNode-s.  */
      /*
      { // DataNode getLastSelectedChildOfDataNode( MetaNode InMetaNode )
          return InMetaNode.GetLastReferencedChildDataNode( );
          } // DataNode getLastSelectedChildOfDataNode( MetaNode InMetaNode )
      */

    } // class Selection
