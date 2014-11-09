package allClasses;

import javax.swing.tree.TreePath;

public class Selection 

  /* This static class helps  manage DataNode selections.  
    Selections are identified with TreePath-s of DataNodes.

    Past selections are stored as DataNode meta-data in the MetaNode DAG,
    which is a structure which parallels a subset of the DataNode DAG.
    This meta-data is useful for reselecting 
    previously selected DataNode-s and their children.
    When a GoToChild command is given at a DataNode,
    instead of selecting the first child DataNode,
    selection meta-data is used to reselect 
    the most recently selected child, if there is one.

    Note, because in the Infogora app 
    the selection in the right pane is normally 
    a child of the selection in the left pane,
    and because sometimes actual DataNodes are deleted or moved,
    a selection might point to a non-existent node.
    In these cases a TreePath might be created which
    ends in a special node called an UnknownDataNode.

    Originally selection history information was stored as 
    one MRU/LRU lists of children in each MetaNode.  
    Now it's stored in MetaNode attributes with key "SelectionPath".
    */

  { // class Selection

    final static String selectionAttributeString= // Key String to use.
      "SelectionPath"; 
    
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
              selectionAttributeString // ...from selection attribute nodes.
              );
          }
          
      public static MetaNode getLastSelectedChildOfMetaNode
        ( MetaNode inMetaNode )
        /* This method returns the most recently selected child MetaNode 
          of inMetaNode.
          
          Prsently it is a pass-through to MetaNode.GetLastChildMetaNode(). 
          */
        { 
          return getLastSelectedChildMetaNode( inMetaNode ); 
          }

      private static MetaNode getLastSelectedChildMetaNode
        ( MetaNode inMetaNode )
        /* This method returns the child MetaNode that was selected last
          of the parent node inMetaNode. 
          If no child MetaNode has the attribute then null is returned.
          It does this by searching for the child with an attribute
          with key == "SelectionPath" and value == "WAS".
          */
        {
          MetaNode childMetaNode= // Test for a child with "WAS" value.
            inMetaNode.getChildWithAttributeMetaNode( 
              selectionAttributeString, 
              "WAS" 
              );
          return childMetaNode; // Return last child MetaNode result, if any.
          }
    
      public static DataNode getLastSelectedChildDataNode
        ( MetaNode inMetaNode )
        /* This method gets the user object DataNode from
          the child MetaNode in inMetaNode 
          which was selected last, or null if there isn't one.
          It also returns null if the Child DataNode
          appears to be an UnknownDataNode,
          because that is an unusable value.
          */
        {
          DataNode resultChildDataNode=  // Assume default result of null.
            null;
          process: { // Override result with child if there is one.
            MetaNode lastChildMetaNode= 
              getLastSelectedChildMetaNode( inMetaNode );
            if // there is no last selected child.
              (lastChildMetaNode == null)
              break process;  // So exit and keep the default null result.

            resultChildDataNode=  // Result recent child DataNode is...
              lastChildMetaNode.   // ...the last child's...
              getDataNode();  // user object.
            if // Result child DataNode is not an UnknownDataNode.
              ( ! UnknownDataNode.isOneB( resultChildDataNode ) )
              break process;  // Exit with that okay result.

            resultChildDataNode= null; // Replace unusable value with null.
            } // Override result with child if there is one.

          return resultChildDataNode; // return resulting DataNode, or null if none.
          }

    // Static setter methods.  These write to the MetaNode DAG.
          
      public static void set( TreePath inTreePath )
        /* This does the same as putAndReturnDataNode(.) except 
          it doesn't return the anything.
          It exists mainly to help other code be self-documenting.
          */
        {
      	  setAndReturnMetaNode( inTreePath ); // Update with TreePath.
          }

      public static DataNode setAndReturnDataNode( TreePath inTreePath )
        /* Updates the "SelectionPath" attributes of the MetaNode DAG
          starting with the root and ending at 
          the MetaNode specified by inTreePath.
          Then it returns the DataNode of the most recently 
          selected/visited child MetaNode of 
          the MetaNode at the end of that path,
          or it returns null if there is no such child. 
          */
        {
          MetaNode endOfPathMetaNode=  // Get last MetaNode in path by...
            setAndReturnMetaNode(  // ...updating tree with...
              inTreePath  // ...the provided TreePath.
              );
          DataNode childDataNode=  // Get that last MetaNode's...
            getLastSelectedChildDataNode(  // ...last selected child DataNode.
              endOfPathMetaNode );
              
          return childDataNode;  // Return the resulting child DataNode.
          }

      private static MetaNode setAndReturnMetaNode( TreePath inTreePath )
        /* Updates the "SelectionPath" attributes of the MetaNode DAG
          starting with the root and ending at 
          the MetaNode specified by inTreePath.
          If it needs to then it adds MetaNodes 
          to the DAG in the appropriate places.
          It returns the MetaNode at the end of the specified TreePath.
          */
        {

          PathAttributeMetaTool workerPathAttributeMetaTool= 
            new PathAttributeMetaTool( // Create new PathAttributeMetaTool...
              inTreePath,  // ...to work on inTreePath's...
              selectionAttributeString  // ...selection path attribute.
              );
          workerPathAttributeMetaTool.setPath( );  // Set path attributes.
          return workerPathAttributeMetaTool.getMetaNode();

          }

    } // class Selection
