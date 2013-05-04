package allClasses;

import java.util.Map;

import javax.swing.tree.TreePath;

public class PathAttributeMetaTool

  extends AttributeMetaTool 
  
  /* This is a MetaTool for dealing with a MetaNode's path attributes. 
    It is based on the my InfogoraPathHistoryAttribute notes.
    */
  
  { // class PathAttributeMetaTool 

    // Constructors.

      public PathAttributeMetaTool
        ( TreePath InTreePath, String InKeyString )
        {
          super( InTreePath, InKeyString );  // Superclass does it all.
          }

    // Static methods.


    // Instance methods.

    // Instance setter methods.

      public Object put( Object ValueObject )
        { 
          return super.put( ValueObject );
          }

      public void setPath( )
        /* This method sets the path attributes for the MetaPath 
          attached to this PathAttributeMetaTool instance,
          from the end node all the way to the root
          by setting path MetaNodes' attribute value to "IS".  
          It unsets any old path MetaNodes which are not ancestors
          in common with the new path, if any, 
          by setting their path attribute values to "WAS".

          This method uses two recursive helper methods 
          to performs the following sequence of operations:
          * Scan toward the root looking for the first "IS" attribute value.
            This is the closest MetaNode in both old and new paths.
          * Scan from that common MetaNode away from the root 
            following to the end of the trail of "IS" attribute values.
          * Reverse scan back toward the common MetaNode,
            replacing "IS" attribute values with "WAS" attribute values.
          * Reverse scan away from root back to the starting node while 
            setting "IS" attribute values, 
            and in siblings replacing "WAS" values with "OLD" values.

          */
        { // setPath( )
          setPathHereAndTowardRoot(  // Use helper method starting from...
            getMetaPath()  // ...MetaPath associated with this tool.
            );
          } // setPath( )

      private void setPathHereAndTowardRoot( MetaPath ScanMetaPath )
        /* This is a recursive helper method for the setPath( ) method.
          It does what that method does except that it does it for
          the ScanMetaPath argument instead of 
          the MataPath associated with this PathAttributeMetaTool instance.
          */
        { // setPathHereAndTowardRoot( .. )
          Processor: { // Process based on path attribute value on this node.
            MetaNode ScanMetaNode= ScanMetaPath.getLastMetaNode( );
            String ValueString= (String) ScanMetaNode.get( KeyString );
            if // Our location is on the old path.
              ( ( // We are on an old path MetaNode...
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
                replaceWasByOldInSiblings( ScanMetaPath );  // Adjust siblings.
                break Processor;  // Exit.
                } // Recursively process ancestors and set the path attribute.
            } // Process based on path attribute on this node.
          } // setPathHereAndTowardRoot( .. )

      private void replaceWasByOldInSiblings( MetaPath InMetaPath )
        /* Searches the attributes of the MetaNode specified by InMetaPath
          and its sibling MetaNode-ss for the value "WAS".
          If it finds this value then it replaces it with "OLD".  */
        { // replaceWasByOldInSiblings( .. )
          MetaNode ParentMetaNode=  // Get parent MetaNode.
            InMetaPath.getParentMetaPath().getLastMetaNode( );
          Processor: { // Process this MetaNode for a child with "WAS" attribute.
            Piterator< Map.Entry < Object, MetaNode > > ChildPiterator= 
              ParentMetaNode.getChildWithAttributePiterator( KeyString, "WAS" );
            if ( ChildPiterator.getE() == null )  // No chld with "WAS".,
              break Processor;  // Exit Processor.
            MetaNode ChildMetaNode=  // Get a reference to...
              (MetaNode)  // ...the child MetaNode which is...
              ChildPiterator.getE().  // ...that next Entry's...
              getValue();  // ...Value.
            ChildMetaNode.remove( KeyString );  // Remove "WAS" to enable purgeB().
            if ( ChildMetaNode.purgeB() )  // If node is purgable...
              ChildPiterator.remove();  // ...remove its map entry...
              else  // ...otherwise we must keep it so...
              ChildMetaNode.put( KeyString, "OLD" ); // ...set attribute to "OLD".
            } // Process this MetaNode for a child with "WAS" attribute.
          } // replaceWasByOldInSiblings( .. )

      private void replaceIsWithWasInDescendents( MetaNode InMetaNode )
        /* This method replaces any "IS" attribute values with
          "WAS" attribute values, for any descendents of InMetaNode,
          thereby removing those nodes from the path.
          */
        { // replaceIsWithWasInDescendents( .. )
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
          } // replaceIsWithWasInDescendents( .. )

  } // class PathAttributeMetaTool 
