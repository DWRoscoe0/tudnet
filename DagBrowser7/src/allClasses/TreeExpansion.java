package allClasses;

import javax.swing.tree.TreePath;

public class TreeExpansion {

  /* This is an encapsulation of the code to do auto-expand-collapse of
    the tree managed by JTree.
    */

    static public void SetAutoExpanded
      ( TreePath TreePathIn, boolean AutoExpandInB )
      /* This method stores the value of the AutoExpandedB flag
        from the value AutoExpandInB
        in the info tree node specified by TreePathIn.
        */
      { 
        ITreeNode TheITreeNode= DagInfo.ITreeNodeFromUpdatedTreeWith( TreePathIn );
        
        // TheITreeNode.AutoExpandedB= AutoExpandInB;
        if ( AutoExpandInB )  // Add or remove attribute depending on AutoExpandInB.
          TheITreeNode.put( "AutoExpanded", "T" );  // Add node's AutoExpanded attribute.
          else
          TheITreeNode.remove( "AutoExpanded" );  // Remove node's AutoExpanded attribute.
        }

    static public boolean GetAutoExpandedB( TreePath TreePathIn )
      /* This method returns the value of the AutoExpandedB flag
        in the info tree node specified by TreePathIn.
        */
      { 
        ITreeNode TheITreeNode= DagInfo.ITreeNodeFromUpdatedTreeWith( TreePathIn );
        // return TheITreeNode.AutoExpandedB; 
        return GetAutoExpandedB( TheITreeNode );
        }

    static public boolean GetAutoExpandedB( ITreeNode TheITreeNode )
      /* This method returns the value of the AutoExpandedB flag
        in the info tree node specified by TheITreeNode.
        */
      { 
        // return TheITreeNode.AutoExpandedB; 
        return   // Return whether the property is present.
          TheITreeNode.containsKey( "AutoExpanded" );
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
        		DagInfo.ITreeNodeFromUpdatedTreeWith( // ...ITreeNode at end...
            ScanTreePath );  // ...of ScanTreePath.
        while   // follow chain of all nodes with auto-expanded flag set.
          ( GetAutoExpandedB( ScanITreeNode ) )  // auto-expanded flag set?
          { // yes, process one auto-expanded node.
            ITreeNode ChildITreeNode= // get most recently referenced child.
              // (ITreeNode)ScanITreeNode.getChildAt(
              //   ScanITreeNode.getChildCount()-1);
              DagInfo.GetLastChildITreeNode(  ScanITreeNode );
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
}
