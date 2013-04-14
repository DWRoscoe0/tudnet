package allClasses;

import javax.swing.tree.TreePath;

public class TreeExpansion {

  /* This is an encapsulation of the code to do auto-expand-collapse of
    the tree managed by JTree.
    */

    static private BooleanAttributeMetaTool newBooleanAttributeMetaTool
      ( TreePath InTreePath )
      /* Returns a BooleanAttributeMetaTool that's ready to use for accessing
        the "AutoExpanded" attribute in the MetaNode at InTreePath.  */
      { return new BooleanAttributeMetaTool( InTreePath, "AutoExpanded" ); }

    static public void SetAutoExpanded
      ( TreePath InTreePath, boolean AutoExpandInB )
      /* This method stores the boolean value AutoExpandInB
        as the value of the AutoExpandedB attribute
        of the MetaNode specified by InTreePath.
        */
      { 
        BooleanAttributeMetaTool WorkerBooleanAttributeMetaTool=
          newBooleanAttributeMetaTool( InTreePath );
        WorkerBooleanAttributeMetaTool.putAttributeB( AutoExpandInB );
        }

    static public boolean GetAutoExpandedB( TreePath InTreePath )
      /* This method returns the value of the AutoExpandedB attribute
        in the info tree node specified by InTreePath.
        */
      { 
        BooleanAttributeMetaTool WorkerBooleanAttributeMetaTool=
          newBooleanAttributeMetaTool( InTreePath );
        return WorkerBooleanAttributeMetaTool.getAttributeB( );
        }

    /*
    static private boolean GetAutoExpandedB( MetaNode TheMetaNode )
      /* This method returns the boolean value of 
        the AutoExpandedB attribute in TheMetaNode.
        */
    /*
    { 
        return BooleanAttributeMetaTool.
          getNodeAttributeB( TheMetaNode, "AutoExpanded" );
        }
    */

    static public TreePath FollowAutoExpandToTreePath
      ( TreePath StartTreePath )
      /* This method tries to follow a chain of 
        the most recently visited AutoExpanded nodes
        starting with the node named by StartTreePath
        and moving away from the root.
        It returns the TreePath of the first node not AutoExpanded,
        or null if there was no AutoExpanded nodes at all.
        
        ?? base on code from ITreeNodeFromUpdatedSubtreesWith(.).
        */
      { // FollowAutoExpandToTreePath( TreePath StartTreePath )
        TreePath ScanTreePath=   // initialize TreePath scanner to be...
          StartTreePath;  // ...start TreePath.
        BooleanAttributeMetaTool ScanBooleanAttributeMetaTool=
          newBooleanAttributeMetaTool( StartTreePath );
        while   // follow chain of all nodes with auto-expanded attribute set.
          ( ScanBooleanAttributeMetaTool.getAttributeB( ) )
          { // yes, process one auto-expanded node.
            MetaNode ChildMetaNodeOf= 
              ScanBooleanAttributeMetaTool.getMetaNode().
              GetLastChildMetaNode( );
            Object ChildUserObject= ChildMetaNodeOf.getDataNode();
            // Setup next possible iteration,
            ScanTreePath=  // create ScanTreePath of next node...
              ScanTreePath.pathByAddingChild( // ...by adding to it...
                ChildUserObject);  // ...the child user Object.
            ScanBooleanAttributeMetaTool.Sync( ScanTreePath );
            } // yes, process one auto-expanded node.
        if  // Handle special case of not moving at all.
          ( ScanTreePath == StartTreePath ) // If we haven't moved...
          ScanTreePath=  null;  // ...replace ScanTreePath with null.
        return ScanTreePath;  // Return final ScanTreePath as result.
        } // FollowAutoExpandToTreePath( TreePath StartTreePath )
}
