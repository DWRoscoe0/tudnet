package allClasses;

import javax.swing.tree.TreePath;

public class AttributeMetaTool

  extends MetaTool

  /* This is a MetaTool for dealing with a MetaNode's attributes. */

  { // class AttributeMetaTool

    // Constants.

      private final String PlaceHolderValueString= "PRESENT";
        /* This is used to indicate the MetaNode has a null value for
          this attribute but some of its descendants do not.  */

    // Instance variables.

      String KeyString;  // Key String associated with all attribute.

    // Constructors.

      public AttributeMetaTool( TreePath InTreePath, String InKeyString )
        {
          super( InTreePath );  // Construct superclass.
          KeyString= InKeyString;  // Initialize KeyString.
          }

    // Instance getter methods.

    // Instance setter methods.

      public Object Xput( Object InValueObject )
        /* This is like a normal put(..), with additional functionality.
          It adds place-holder attributes with the same KeyString
          between the current MetaNode and the root.
          This is done so that all instances of attributes with
          the same KeyString can be found quickly.
          */
        { 
          Object ResultObject=  // Get present and result attribute value.
            getMetaNode().put( KeyString, InValueObject ); 
          PropagateAddition( );  // Add place-holder if needed.
          return ResultObject;  // Return old value.
          }

      public Object put( Object InValueObject )
        /* This is like a normal put(..), to the KeyString entry
          of the Map, but with some additional functionality:
          * If an attribute entry is added then
            t adds place-holder attributes with the same KeyString
            between the current MetaNode and the root if needed.
            This is done so that all instances of attributes with
            the same KeyString can be found quickly from the root.
          * It removes an attribute entry if 
            the new value InValueObject is null.
            It also removes place-holder attributes with the same KeyString
            between the current MetaNode and the root.if possible.
          The place-holder attributes are maintained 
          to faciliate deletion of nodes that hold no useful data.

          // ??? attribute searchers should be refactored for keys and values.
          // ??? Might mean creating special Piterators.
          */
        { 
          MetaNode TheMetaNode= getMetaNode();  // Cache the MetaNode.
          Object OldObject=  // Get present attribute value.
             TheMetaNode.get( KeyString );
          { // Take appropriate action.
            if ( OldObject == InValueObject )  // Attribute is not changing.
              ; // So do nothing.
            else if ( OldObject == null )  // Adding missing attribute.
              { // Add the attribute.
                TheMetaNode.put( KeyString, InValueObject ); // Add new value.
                PropagateAddition( );  // Add place-holders if needed.
                } // Add the attribute.
            else if ( InValueObject == null )  // Removing present attribute.
              { // Remove the attribute.
                WideRemoval( );  // Remove it and its place-holders if needed.
                } // Remove the attribute.
            else // Replace present non-null attribute value with new value.
              TheMetaNode.put( KeyString, InValueObject );  // Replace value.
            } // Take appropriate action.
          return OldObject;  // Return old attribute value as result.
          }

      /*
      private Object Xget( )
        /* This is a pass-through to MetaNode's equivalent method.  */
      /*
            { return getMetaNode().get( KeyString ); }

      /*
      private Object remove( String KeyString ) 
        /* This is a pass-through to MetaNode's equivalent method.  */
      /*
        {
          System.out.print( "!" );  // Debug.
          return getMetaNode().remove( KeyString ); 
          }
      */
    
      public void WideRemoval( )
        /* This method propagates the deletion of 
          the attribute at this location up the MetaNode DAG toward the root.
          It either replaces the attribute with a place-holder attribute,
          if any children have the same attribute key,
          or it removes the attribute completely and removes 
          the place-holder attributes of any ancestor nodes 
          that have no children with the same attribute key.
              
          ??? Following is PropagateAddition(..) code being changed.
          */
        {
          Process: {  // Process.
            MetaPath ScanMetaPath=  // Initialize MetaPath of present position.
              getMetaPath();
            MetaNode ScanMetaNode=  // Get MetaNode at this position.
              ScanMetaPath.getLastMetaNode();

            if  // There is already a null-representing place-holder here. 
              ( ScanMetaNode.get( KeyString ) == PlaceHolderValueString )
              break Process; // Do nothing and exit.

            // There is something else here.
            if  // At least one of the children has an attribute with same key.
              ( ScanMetaNode.getChildWithKeyMetaNode( KeyString ) != null )
              { // Replace with place-holder and exit.
                ScanMetaNode.put(  // Replace the attribute in this MetaNode...
                  KeyString,  // ...for the same KeyString...
                  PlaceHolderValueString  // ...with place-holder ValueString.
                  );
                break Process;  // Exit.
                } // Replace with place-holder and exit.
            
            while (true)  // Remove regular and place-holder attributes.
              { // Remove one attribute and test for removal of the one in parent.
                ScanMetaNode.remove( KeyString );  // Remove attribute here.
                ScanMetaPath=  // Move MetaPath one MetaNode closer to root.
                  ScanMetaPath.getParentMetaPath();
                ScanMetaNode=  // Get MetaNode at this new location.
                  ScanMetaPath.getLastMetaNode();
                if  // There is no null-representing place-holder here. 
                  ( ScanMetaNode.get( KeyString ) != PlaceHolderValueString )
                  break Process; // Exit.
                if  // At least one of the children has a same-key attribute.
                  ( ScanMetaNode.getChildWithKeyMetaNode( KeyString ) 
                    != 
                    null 
                    )
                  break;  // Exit loop.
                } // Remove one attribute and test for removal of the one in parent.
            } // Process.
          /* remove and loop to remove ancestor place-holders.
            while (true)  // Remove or verify attribute in MetaNode-s here to root.
              { // Remove or verify attribute in one MetaNode.
                ScanMetaPath=  // Move MetaPath one node closer to root.
                  ScanMetaPath.getParentMetaPath();
                if // Exit loop if at pseudo-parent of root.
                  ( ScanMetaPath == MetaRoot.getParentOfRootMetaPath( ) )
                  break;
                ScanMetaNode=  // Get MetaNode at this position.
                  ScanMetaPath.getLastMetaNode();

                if // Exit loop if the node has an attribute with same KeyString.
                  ( ScanMetaNode.containsKey( KeyString ) )
                  break;
                // Attribute is not there.
                ScanMetaNode.put(  // Put an attribute in this MetaNode...
                  KeyString,  // ...for the same KeyString...
                  PlaceHolderValueString  // ...with place-holder ValueString.
                  );
                } // Remove or verify attribute in one MetaNode.
          */
          }

      public void PropagateAddition( )
        /* This method propagates the addition of the attribute
          up the MetaNode DAG toward the root.
          It does it by adding place-holder attributes where needed.
          This is done so that all instances of attributes with
          the same KeyString can be found quickly.
          */
        {
          MetaPath ScanMetaPath=   // Initialize MetaPath for scanning.
            getMetaPath();
          while (true)  // Add or verify attribute in MetaNode-s here to root.
            { // Add or verify attribute in one MetaNode.
              ScanMetaPath=  // Move MetaPath one node closer to root.
                ScanMetaPath.getParentMetaPath();
              if // Exit loop if at pseudo-parent of root.
                ( ScanMetaPath == MetaRoot.getParentOfRootMetaPath( ) )
                break;
              MetaNode ScanMetaNode=  // Get MetaNode at this position.
                ScanMetaPath.getLastMetaNode();
              if // Exit loop if the node has an attribute with same KeyString.
                ( ScanMetaNode.containsKey( KeyString ) )
                break;
              // Attribute is not there.
              ScanMetaNode.put(  // Put an attribute in this MetaNode...
                KeyString,  // ...for the same KeyString...
                PlaceHolderValueString  // ...with place-holder ValueString.
                );
              } // Add or verify attribute in one MetaNode.
          }

    } // class AttributeMetaTool 
