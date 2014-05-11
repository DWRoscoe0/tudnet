package allClasses;

// import java.util.Map;

import javax.swing.tree.TreePath;

public class PathAttributeMetaTool

  extends AttributeMetaTool 
  
  /* This is a Attribute MetaTool for dealing with 
    a MetaNode's path attributes. 
    At first it is only for the SelectionPath attribute.
    
    Attribute values have the following meanings:
    IS: this node is now part of the path.
    WAS: this node was the most recent part of the path
      of all its siblings.
    OLD: this node was part of the path but 
      one of its siblings was a more recent part.

    This is based on the my InfogoraPathHistoryAttribute notes.
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
          with a key of keyString and a value of "IS".
          At least the root must have an "IS" attribute value,
          otherwise an invalid TreePath consisting of only the
          ParentOfRootDataNode will be returned.
          It does not consider UnknownDataNode-s to be part of the path
          even if they have the desired attribute
          because it is an unusable value.
          */
        {
          TreePath scanTreePath=  // Point scanTreePath accumulator...
            DataRoot.getParentOfRootTreePath( );  // ...to parent of root.
          MetaNode scanMetaNode=  // Get root MetaNode.
            MetaRoot.getParentOfRootMetaNode( );
          scanner: while (true) { // Scan all nodes with "IS".
            MetaNode childMetaNode= // Test for a child with "IS" value.
              scanMetaNode.getChildWithAttributeMetaNode( KeyString, "IS" );
            if  // scanMetaNode has no child with "IS" attribute value.
              ( childMetaNode == null)
              break scanner;  // Exit Processor.
            DataNode theDataNode= // Get associated DataNode.
              childMetaNode.getDataNode();
            if // DataNode is an UnknownDataNode.
              ( UnknownDataNode.isOneB( theDataNode ) )
              break scanner;  // Exit Processor.
            scanTreePath=  // Add DataNode to TreePath.
              scanTreePath.pathByAddingChild( theDataNode );
            scanMetaNode= childMetaNode;  // Point to next MetaNode.
            } // Scan all nodes with "IS".
          return scanTreePath;  // Return accumulated TreePath.
          }

    // Instance setter methods.

      public void setPath( )
        /* This method puts path information into the MetaNode DAG.
          It sets the path attributes for the MetaPath 
          attached to this PathAttributeMetaTool instance,
          from the end node all the way to the root
          by setting path MetaNodes' attribute value to "IS".  
          It might set the same path attributes of other MetaNodes 
          to either "WAS", "OLD", or remove them, depending on context.

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

      private void setPathHereAndTowardRoot( MetaPath scanMetaPath )
        /* This is a recursive helper method for the setPath( ) method.
          It does the same thing as that method except 
          it does it for the scanMetaPath argument instead of 
          the MataPath stored in this PathAttributeMetaTool instance.
          */
        {
          MetaNode scanMetaNode= scanMetaPath.getLastMetaNode( );
          String valueString= (String) scanMetaNode.get( keyString );
          if // Our location is on the old path.
            ( ( // We are on an old path MetaNode indicated by value "IS"...
                ( valueString != null ) && valueString.equals( "IS" )
                )
              ||  // ...or...
              ( // ...we have passed the root.
                scanMetaPath == MetaRoot.getParentOfRootMetaPath( ) 
                )
              )
            { // Reset descendants of our location in old path.
              replaceIsWithWasInDescendents( scanMetaNode );
              } // Reset descendants of out location in old path.
            else  // We haven't reached the old path yet.
            { // Recursively process ancestors and set the path attribute.
              setPathHereAndTowardRoot(  // Recurse into ancestors...
                scanMetaPath.getParentMetaPath() // ...starting with partent.
                );
              scanMetaNode.put(  // Set attribute...
                keyString, // ...for this path name...
                "IS"  // ...to be "IS". 
                );
              replaceWasWithOldInSiblings(   // Adjust siblings...
                scanMetaPath  // ...of this path.
                );
              } // Recursively process ancestors and set the path attribute.
          }

      private void replaceWasWithOldInSiblings( MetaPath inMetaPath )
        /* Searches the attributes of the MetaNode specified by inMetaPath
          and all of its sibling MetaNode-s for the attribute value "WAS".
          If it finds this value then it replaces it with "OLD".  
          It doesn't look for any more because there can be a maximum of
          one sibling node with "WAS".
          */
        {
          MetaNode parentMetaNode=  // Get parent MetaNode.
            inMetaPath.getParentMetaPath().getLastMetaNode( );
          Processor: { // Process its children one with "WAS" attribute.
            KeyAndValueMetaPiteratorOfMetaNode // Use iterator to search...
              childKeyAndValueMetaPiteratorOfMetaNode=
                new KeyAndValueMetaPiteratorOfMetaNode( 
                  parentMetaNode.getMetaChildren().getPiteratorOfMetaNode(
                    parentMetaNode
                    ),
                  keyString, 
                  "WAS"  // ...for child/sibling with "WAS" attribute value.
                  );
            if  // There is no child with "WAS".
              ( childKeyAndValueMetaPiteratorOfMetaNode.getE() == null )
              break Processor;  // Exit Processor.
            MetaNode childMetaNode=  // Get a reference to...
              (MetaNode)  // ...the child MetaNode which is...
              childKeyAndValueMetaPiteratorOfMetaNode.
                getE(); // ...that next Entry's value.
            childMetaNode.remove( keyString );  // Remove "WAS" now for purge.
            if ( childMetaNode.purgeTryB() )  // If node is purge-able...
              childKeyAndValueMetaPiteratorOfMetaNode.
                removeV();  // ...remove its map entry...
              else  // ...otherwise we must keep it so...
              childMetaNode.put( keyString, "OLD" ); // ...set "OLD" attribute.
            } // Process its children one with "WAS" attribute.
          }

      private void replaceIsWithWasInDescendents( MetaNode inMetaNode )
        /* This method replaces any "IS" attribute values with
          "WAS" attribute values, for any descendants of inMetaNode,
          thereby removing those nodes from the selection path,
          but remembering them for possible future auto-selections.
          */
        {
          Processor: { // Process this MetaNode.
            MetaNode childMetaNode= // Test for a child with "IS" value.
              inMetaNode.getChildWithAttributeMetaNode( keyString, "IS" );
            if  // inMetaNode has no child with "IS" attribute value.
              ( childMetaNode == null)
              break Processor;  // Exit Processor.
            replaceIsWithWasInDescendents(   // Recurse in descendants.
              childMetaNode
              );
            childMetaNode.put( keyString, "WAS" );  // Replace IS with WAS.
            // Done in above order for faster dirty-flag up-propagation.
            } // Process this MetaNode.
          }

    } // class PathAttributeMetaTool 
