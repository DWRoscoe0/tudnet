package allClasses;

import javax.swing.tree.TreePath;

public class Selection 

  /* This static class helps  manage DataNode selections.  
    Selections are specified by TreePath-s of DataNodes.

    Past selections are stored as DataNode meta-data in the MetaNode DAG,
    which is a structure which parallels a subset of the DataNode DAG.
    This meta-data is useful for reselecting 
    previously selected DataNode-s and their children.
    When a GoToChild command is given at a DataNode,
    instead of selecting the first child DataNode,
    selection meta-data is used to reselect 
    the most recently selected child, if there is one.

    Note, because in the Infogora app 
    the selection in the right pane is a child of
    the selection in the left pane,
    and because sometimes actual DataNodes are deleted or moved,
    a selection might point to a non-existant node.
    In these cases a TreePath might be created which
    ends in a special node called an ErrorDataNode.

    Originally selection history information was stored as 
    one MRU/LRU lists of children in each MetaNode.  
    Now it's stored in MetaNode attributes with key "SelectionPath".
    */

  { // class Selection

    final static String SelectionAttributeString=      "SelectionPath"; 
    
    // Static getter methods.  These read from the MetaNode DAG.

      public static TreePath buildAttributeTreePath( )
        /* This method returns a TreePath of DataNodes which 
          identifies the current DataNode selection.
          The path is built from the sequence of DataNodes
          associated with the MetaNode's which have attributes 
          with key == "SelectionPath" and value == "IS",
          starting at the MetaNode DAG root.
          */
        { 
          return // Return the...
            PathAttributeMetaTool.buildAttributeTreePath( // ...path built...
              SelectionAttributeString // ...from selection attribute nodes.
              );
          }
          
      public static MetaNode getLastSelectedChildOfMetaNode
        ( MetaNode InMetaNode )
        /* This method returns the most recently selected child MetaNode 
          of InMetaNode.
          
          Prsently it is a pass-through to MetaNode.GetLastChildMetaNode(). 
          */
        { 
          return GetLastSelectedChildMetaNode( InMetaNode ); 
          }

      public static MetaNode GetLastSelectedChildMetaNode
        ( MetaNode InMetaNode )
        /* This method returns the child MetaNode that was selected last
          of the parent node InMetaNode. 
          If no child MetaNode has the attribute then null is returned.
          It does this by searching for the child with an attribute
          with key == "SelectionPath" and value == "WAS".
          */
        {
          MetaNode ChildMetaNode= // Test for a child with "WAS" value.
            InMetaNode.getChildWithAttributeMetaNode( 
              SelectionAttributeString, 
              "WAS" 
              );
          return ChildMetaNode; // Return last child MetaNode result, if any.
          }
    
      public static DataNode getLastSelectedChildDataNode
        ( MetaNode InMetaNode )
        /* This method gets the user object DataNode from
          the child MetaNode in InMetaNode 
          which was selected last, or null if there isn't one.
          It also returns null if the Child DataNode
          appears to be an ErrorDataNode,
          because that is an unusable value.
          */
        {
          DataNode ResultChildDataNode=  // Assume default result of null.
            null;
          Process: { // Override result with child if there is one.
            MetaNode LastChildMetaNode= 
              GetLastSelectedChildMetaNode( InMetaNode );
            if // there is no last selected child.
              (LastChildMetaNode == null)
              break Process;  // So exit and keep the default null result.

            ResultChildDataNode=  // Result recent child DataNode is...
              LastChildMetaNode.   // ...the last child's...
              getDataNode();  // user object.
            if // Result child DataNode is not an ErrorDataNode.
              ( ! ResultChildDataNode.equals(
                  ErrorDataNode.getSingletonErrorDataNode()
                  )
                )
              break Process;  // Exit with that okay result.

            ResultChildDataNode= null; // Replace unusable value with null.
            } // Override result with child if there is one.

          return ResultChildDataNode; // return resulting DataNode, or null if none.
          }

    // Static setter methods.  These write to the MetaNode DAG.
          
      public static void set( TreePath TreePathIn )
        /* This does the same as putAndReturnDataNode(.) except 
          it doesn't return the MetaNode associated with 
          the end of the path.
          It exists mainly to help other code be self-documenting.
          */
        {
      	  setAndReturnMetaNode( TreePathIn ); // Update with TreePath.
          }

      public static DataNode setAndReturnDataNode( TreePath TreePathIn )
        /* Updates the PathAttributeMetaTool with TreePathIn and returns 
          the DataNode of the most recently visited child MetaNode of 
          the MetaNode at the end of that path,
          or it returns null if there is no such child. 
          */
        {
          MetaNode EndOfPathMetaNode=  // Get last MetaNode in path by...
            setAndReturnMetaNode(  // ...updating tree with...
              TreePathIn  // ...the provided TreePath.
              );
          DataNode ChildDataNode=  // Get that last MetaNode's...
            getLastSelectedChildDataNode(  // ...last selected child DataNode.
              EndOfPathMetaNode );
              
          return ChildDataNode;  // Return the resulting child DataNode.
          }

      public static MetaNode setAndReturnMetaNode( TreePath InTreePath )
        /* Updates the "SelectionPath" attributes of the MetaNode DAG
          starting with the root and ending at 
          the MetaNode specified by InTreePath.
          If it needs to then it adds MetaNodes 
          to the DAG in the appropriate places.
          It returns the MetaNode at the end of the specified TreePath.
          */
        {

          PathAttributeMetaTool WorkerPathAttributeMetaTool= 
            new PathAttributeMetaTool( // Create new PathAttributeMetaTool...
              InTreePath,  // ...to work on InTreePath.
              "SelectionPath"
              );
          WorkerPathAttributeMetaTool.setPath( );  // Set path attributes.
          return WorkerPathAttributeMetaTool.getMetaNode();

          }

    } // class Selection
