package allClasses;

import javax.swing.tree.TreePath;

public class TreeExpansion 

  /* This class contains several static methods to help manage 
    auto-expand and auto-collapse in JTrees of the DataNode DAG/tree.
    It does this with DataNode meta-data stored in the MetaNode DAG.
    It doesn't actually do any expansions or collapses of nodes.
    It only stores and retrieves information about them.
    */

  {

    static private BooleanAttributeMetaTool newAutoExpandedAttributeMetaTool
      ( TreePath InTreePath )
      /* This method returns a BooleanAttributeMetaTool 
        that's ready to use for accessing the "AutoExpanded" attribute 
        in the MetaNode associated with InTreePath.  
        */
      { return new BooleanAttributeMetaTool( InTreePath, "AutoExpanded" ); }

    static public void SetAutoExpanded
      ( TreePath InTreePath, boolean InAutoExpandedB )
      /* This method stores InAutoExpandedB
        as the boolean value of the AutoExpanded attribute
        of the MetaNode associated with InTreePath.
        */
      { 
        BooleanAttributeMetaTool WorkerBooleanAttributeMetaTool=
          newAutoExpandedAttributeMetaTool( InTreePath );
        WorkerBooleanAttributeMetaTool.putAttributeB( InAutoExpandedB );
        }

    static public boolean GetAutoExpandedB( TreePath InTreePath )
      /* This method returns the boolean value of 
        the AutoExpanded attribute
        of the MetaNode associated with InTreePath.
        */
      { 
        BooleanAttributeMetaTool WorkerBooleanAttributeMetaTool=
          newAutoExpandedAttributeMetaTool( InTreePath );
        return WorkerBooleanAttributeMetaTool.getAttributeB( );
        }

    static public TreePath FollowAutoExpandToTreePath
      ( TreePath StartTreePath )
      /* This method tries to follow a chain of 
        the most recently selected and AutoExpanded MetaNodes
        starting with the MetaNode associated with StartTreePath
        and moving away from the root.
        It returns:
          The TreePath associated with the first MetaNode 
          without the AutoExpanded attribute set, or 
          
          Null if there were no AutoExpanded MetaNodes at all.
        */
      {
        TreePath ScanTreePath=   // Initialize TreePath scanner to be...
          StartTreePath;  // ...the start TreePath.
        BooleanAttributeMetaTool ScanBooleanAttributeMetaTool= // Make tool.
          newAutoExpandedAttributeMetaTool( StartTreePath );
        while (true) // Follow chain of nodes with AutoExpanded attribute set.
          { // Try to process one node.
            if  // Exit loop if AutoExpanded attribute of MetaNode not set.
              ( ! ScanBooleanAttributeMetaTool.getAttributeB( ) )
              break;  // Exit loop.  We're past the last AutoExpanded node.
            MetaNode ChildOfMetaNode=  // Get recently selected child MetaNode.
              Selection.getLastSelectedChildOfMetaNode(
                ScanBooleanAttributeMetaTool.getMetaNode()
                );
            if ( ChildOfMetaNode == null ) // Exit loop if no such child.
              break;  // Exit loop.  Meta data is corrupted.
            Object ChildOfDataNode=  // Get associated child DataNode.
              ChildOfMetaNode.getDataNode();
            // Setup next possible iteration,
            ScanTreePath=  // create ScanTreePath of next node...
              ScanTreePath.pathByAddingChild( // ...by adding to old path...
                ChildOfDataNode);  // ...the child DataNode.
            ScanBooleanAttributeMetaTool.Sync( // Sync the tool with...
              ScanTreePath );  // ...the new ScanTreePath.
            } // Try to process one node.
        if  // Adjust result for special case of not moving at all.
          ( ScanTreePath == StartTreePath ) // If we haven't moved...
          ScanTreePath=  null;  // ...replace ScanTreePath with null.
        return ScanTreePath;  // Return final ScanTreePath as result.
        }

    }
