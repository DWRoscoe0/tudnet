package allClasses;

import javax.swing.tree.TreePath;

public class TreeExpansion {

  /* This is an encapsulation of the code to do auto-expand-collapse of
    the tree managed by JTree.
    */

    static public void SetAutoExpanded
      ( TreePath InTreePath, boolean AutoExpandInB )
      /* This method stores the value of the AutoExpandedB flag
        from the value AutoExpandInB
        in the info tree node specified by InTreePath.
        */
      { 
        // MetaNode TheMetaNode= MetaTool.UpdatePathMetaNode( InTreePath );
        MetaTool WorkerMetaTool=  // Create new MetaTool...
          new MetaTool( InTreePath ); // ...to work on InTreePath.
        MetaNode TheMetaNode=  // Get MetaNode from end of MetaPath.
          WorkerMetaTool.getMetaPath( ).getLastMetaNode();
        
        String ValueString=  // Translate boolean Value into string.
          ( AutoExpandInB ? "T" : "F" );
        TheMetaNode.put( "AutoExpanded", ValueString );  // Store value String.
        }

    static public boolean GetAutoExpandedB( TreePath InTreePath )
      /* This method returns the value of the AutoExpandedB flag
        in the info tree node specified by InTreePath.
        */
      { 
        MetaTool WorkerMetaTool=  // Create new MetaTool...
          new MetaTool( InTreePath ); // ...to work on InTreePath.
        MetaNode TheMetaNode=  // Get MetaNode from end of MetaPath.
          WorkerMetaTool.getMetaPath( ).getLastMetaNode();
        return GetAutoExpandedB( TheMetaNode );
        }

    static public boolean GetAutoExpandedB( MetaNode TheMetaNode )
      /* This method returns the boolean value of the AutoExpandedB flag
        in TheMetaNode.
        */
      { 
        boolean ResultB= false;
        switch (0) {
          default:
          Object ValueObject= TheMetaNode.get( "AutoExpanded" );
          if ( ValueObject == null )  // Value not there means false.
            break;
          if ( ValueObject.equals( "T" ) )  // Only a value of T means true.
            ResultB= true;
          }
        return ResultB;
        
        //return   // Return whether the property is present.
        //  TheMetaNode.containsKey( "AutoExpanded" );
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
        MetaNode ScanMetaNode= // initialize MetaNode scanner to be...
        		MetaTool.UpdatePathMetaNode( // ...ITreeNode at end...
            ScanTreePath );  // ...of ScanTreePath.
        while   // follow chain of all nodes with auto-expanded flag set.
          ( GetAutoExpandedB( ScanMetaNode ) )  // auto-expanded flag set?
          { // yes, process one auto-expanded node.
            MetaNode ChildMetaNode= // get most recently referenced child.
              // (MetaNode)ScanITreeNode.getChildAt(
              //   ScanITreeNode.getChildCount()-1);
                ScanMetaNode.GetLastChildMetaNode( );
            Object ChildUserObject= ChildMetaNode.getDataNode();
            ScanTreePath=  // create ScanTreePath of next node...
              ScanTreePath.pathByAddingChild( // ...by adding to it...
                ChildUserObject);  // ...the child user Object.
            ScanMetaNode= ChildMetaNode; // make next scan node be child.
            } // yes, process one auto-expanded node.
        if ( ScanTreePath == StartTreePath ) // if we haven't moved.
          ScanTreePath=  null;  // replace ScanTreePath with null.
        return ScanTreePath;  // return final ScanTreePath as result.
        } // FollowAutoExpandToTreePath( TreePath StartTreePath )
}
