package allClasses;

// import java.util.Map;

import javax.swing.tree.TreePath;

public class PathAttributeMetaTool

  extends AttributeMetaTool 
  
  /* This is a Attribute MetaTool for dealing with 
    a MetaNode's path attributes. 
    At first it is mainly for the SelectionPath attribute.

    It is based on the my InfogoraPathHistoryAttribute notes.
    */
  
  { // class PathAttributeMetaTool 

    // Constructors.

      public PathAttributeMetaTool
        ( TreePath InTreePath, String InKeyString )
        {
          super( InTreePath, InKeyString );  // Superclass does it all.
          }

    // static getter methods.

      public static TreePath buildAttributeTreePath( String KeyString )
        /* This method returns path information from the MetaNode DAG.
          It returns a TreePath comprised of all the DataNodes
          from the MetaNode's which contain attributes 
          with a key of KeyString and a value of "IS".
          At least the root must have an "IS" attribute value,
          otherwise an invalid TreePath consisting of only the
          ParentOfRootDataNode will be returned.
          It does not consider ErrorDataNode-s to be part of the path
          even though they have the desired attribute
          because that is an unusable value.
          */
        {
          TreePath ScanTreePath=  // Point ScanTreePath accumulator...
            DataRoot.getParentOfRootTreePath( );  // ...to parent of root.
          MetaNode ScanMetaNode=  // Get root MetaNode.
            MetaRoot.getParentOfRootMetaNode( );
          Scanner: while (true) { // Scan all nodes with "IS".
            MetaNode ChildMetaNode= // Test for a child with "IS" value.
              ScanMetaNode.getChildWithAttributeMetaNode( KeyString, "IS" );
            if  // ScanMetaNode has no child with "IS" attribute value.
              ( ChildMetaNode == null)
              break Scanner;  // Exit Processor.
            DataNode TheDataNode= // Get associated DataNode.
              ChildMetaNode.getDataNode();
            if // DataNode is an ErrorDataNode.
              ( TheDataNode.equals(
                  ErrorDataNode.getSingletonErrorDataNode()
                  )
                )
              break Scanner;  // Exit Processor.
            ScanTreePath= ScanTreePath.pathByAddingChild( TheDataNode );
            ScanMetaNode= ChildMetaNode;  // Point to next MetaNode.
            } // Scan all nodes with "IS".
          return ScanTreePath;  // Return accumulated TreePath.
          }

    // Instance setter methods.

      public void setPath( )
        /* This method puts path information into the MetaNode DAG.
          It sets the path attributes for the MetaPath 
          attached to this PathAttributeMetaTool instance,
          from the end node all the way to the root
          by setting path MetaNodes' attribute value to "IS".  
          It sets  path attributes of MetaNodes which are not ancestors
          in common with the new path, if any, to the value "WAS".

          This method uses two recursive helper methods 
          to performs the following sequence of operations:
          * Scan toward the root looking for the first "IS" attribute value.
            This is the closest MetaNode in both old and new paths.
          * Scan from that common MetaNode away from the root 
            following to the end of the trail of "IS" attribute values.
          * Reverse scan back toward the common MetaNode,
            replacing "IS" attribute values with "WAS" attribute values.
          * Reverse scan away from the common root back to the starting node 
            while setting "IS" attribute values, 
            and in siblings replacing "WAS" values with "OLD" values.

          */
        {
          setPathHereAndTowardRoot(  // Use helper method starting from...
            getMetaPath()  // ...MetaPath associated with this tool.
            );
          }

      private void setPathHereAndTowardRoot( MetaPath ScanMetaPath )
        /* This is a recursive helper method for the setPath( ) method.
          It does what that method does except that it does it for
          the ScanMetaPath argument instead of 
          the MataPath associated with this PathAttributeMetaTool instance.
          */
        {
          Processor: { // Process based on path attribute value on this node.
            MetaNode ScanMetaNode= ScanMetaPath.getLastMetaNode( );
            String ValueString= (String) ScanMetaNode.get( KeyString );
            if // Our location is on the old path.
              ( ( // We are on an old path MetaNode indicated by value "IS"...
                  ( ValueString != null ) && ValueString.equals( "IS" )
                  )
                ||  // ...or...
                ( // ...we have passed the root.
                  ScanMetaPath == MetaRoot.getParentOfRootMetaPath( ) 
                  )
                )
              { // Reset descendents of our location in old path.
                replaceIsWithWasInDescendents( ScanMetaNode );
                break Processor;  // Exit.
                } // Reset descendents of out location in old path.
            if (true)  // We haven't reached the old path yet.
              { // Recursively process ancestors and set the path attribute.
                setPathHereAndTowardRoot(  // Recurse into ancestors.
                  ScanMetaPath.getParentMetaPath() );
                ScanMetaNode.put( KeyString,    // Set new path attribute...
                  "IS" );  // ...to be "IS".
                replaceWasWithOldInSiblings(   // Adjust siblings...
                  ScanMetaPath  // ...of this path.
                  );
                break Processor;  // Exit.
                } // Recursively process ancestors and set the path attribute.
            } // Process based on path attribute on this node.
          }

      private void replaceWasWithOldInSiblings( MetaPath InMetaPath )
        /* Searches the attributes of the MetaNode specified by InMetaPath
          and its sibling MetaNode-ss for the value "WAS".
          If it finds this value then it replaces it with "OLD".  */
        {
          MetaNode ParentMetaNode=  // Get parent MetaNode.
            InMetaPath.getParentMetaPath().getLastMetaNode( );
          Processor: { // Process its children one with "WAS" attribute.
            KeyAndValueMetaPiteratorOfMetaNode 
              ChildKeyAndValueMetaPiteratorOfMetaNode=
                new KeyAndValueMetaPiteratorOfMetaNode( 
                  ParentMetaNode.getMetaChildren().getPiteratorOfMetaNode(
                    ParentMetaNode
                    ),
                  KeyString, 
                  "WAS"
                  );
            if  // No chld with "WAS".,
              ( ChildKeyAndValueMetaPiteratorOfMetaNode.getE() == null )
              break Processor;  // Exit Processor.
            MetaNode ChildMetaNode=  // Get a reference to...
              (MetaNode)  // ...the child MetaNode which is...
              ChildKeyAndValueMetaPiteratorOfMetaNode.getE(); // .  // ...that next Entry's value.
            ChildMetaNode.remove( KeyString );  // Remove "WAS" for purgeB().
            if ( ChildMetaNode.purgeTryB() )  // If node is purgable...
              ChildKeyAndValueMetaPiteratorOfMetaNode.removeV();  // ...remove its map entry...
              else  // ...otherwise we must keep it so...
              ChildMetaNode.put( KeyString, "OLD" ); // ...set "OLD" attribute.
            } // Process its children one with "WAS" attribute.
          }

      private void replaceIsWithWasInDescendents( MetaNode InMetaNode )
        /* This method replaces any "IS" attribute values with
          "WAS" attribute values, for any descendents of InMetaNode,
          thereby removing those nodes from the selection path,
          but remembering them for possible future auto-selections.
          */
        {
          Processor: { // Process this MetaNode.
            MetaNode ChildMetaNode= // Test for a child with "IS" value.
              InMetaNode.getChildWithAttributeMetaNode( KeyString, "IS" );
            if  // InMetaNode has no child with "IS" attribute value.
              ( ChildMetaNode == null)
              break Processor;  // Exit Processor.
            replaceIsWithWasInDescendents(   // Recurse in descendants.
              ChildMetaNode
              );
            ChildMetaNode.put( KeyString, "WAS" );  // Replace IS with WAS.
            // Done in above order for faster dirty-flag up-propigation.
            } // Process this MetaNode.
          }

    } // class PathAttributeMetaTool 
