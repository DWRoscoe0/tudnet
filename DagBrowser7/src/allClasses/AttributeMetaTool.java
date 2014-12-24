package allClasses;

import javax.swing.tree.TreePath;

abstract class AttributeMetaTool

  extends MetaTool

  /* This is a MetaTool specialized for dealing with 
    a MetaNode's attributes. 
    */

  {

    // Constants.

      private final String PlaceHolderValueString= "PRESENT";
        /* This Map value is used to indicate that
          the MetaNode has a null value for
          this attribute but some of its descendants do not.  
          */

    // Instance variables.

      String keyString;  // Key String naming attribute of interest.

    // Constructors.

      protected AttributeMetaTool( 
          MetaRoot theMetaRoot, 
          TreePath inTreePath, 
          String inKeyString 
          )
        {
          ///super( MetaRoot.get(), inTreePath );  // Construct superclass.
          super( theMetaRoot, inTreePath );  // Construct superclass.

          keyString= inKeyString;  // Initialize keyString.
          }

    // Instance methods.

      protected Object put( Object inValueObject )
        /* This is like a normal put(..), to the keyString entry
          of a Map, but with some additional functionality:
          * If an attribute entry is added then
            it adds place-holder attributes with the same keyString
            between the current MetaNode and the root if needed.
          * It removes an attribute entry if 
            the new value inValueObject is null.
            It also removes place-holder attributes with 
            the same keyString between the current MetaNode and 
            the root if possible.
          The place-holder attributes are maintained 
          to facilitate deletion of nodes that hold no useful data
          an to be able to quickly fin all instances 
          of attributes with the same keyString.

          // ??? attribute searchers should be re-factored for 
            keys and values.
          // ??? Might mean creating special Piterators.
          */
        { 
          MetaNode theMetaNode= getMetaNode();  // Cache the MetaNode.
          Object oldObject=  // Get present attribute value.
             theMetaNode.get( keyString );
          { // Taking appropriate action.
            if   // Doing nothing if attribute value is not changing.
              ( oldObject == inValueObject )
              ; // Doing nothing.
            else if   // Adding missing attribute if needed.
              ( oldObject == null )
              { // Adding the attribute.
                theMetaNode.put(  // Adding new value.
                  keyString, inValueObject 
                  );
                propagateAdditionV( );  // Adding place-holders.
                } // Adding the attribute.
            else if   // Removing present attribute needed.
              ( inValueObject == null )
              removeOrReplaceV( );  // Remove or store place-holder.
            else // Replace present non-null attribute value with new value.
              theMetaNode.put( keyString, inValueObject );  // Replace value.
            } // Taking appropriate action.
          return oldObject;  // Return old attribute value as result.
          }
    
      private void removeOrReplaceV( )
        /* This method removes the attribute at the present position 
          by either deleting it or replacing it by a place-holder value.
          If it is removed then it propagates the removal 
          up the MetaNode DAG toward the root.
          It either replaces the attribute with a place-holder 
          attribute, if any children have the same attribute key,
          or it removes the attribute completely and removes 
          the place-holder attributes of any ancestor nodes 
          that have no children with the same attribute key.
          */
        {
          toReturn: {  // toReturn.
            MetaPath scanMetaPath=  // Get MetaPath of position.
              getMetaPath();
            MetaNode scanMetaNode=  // Get MetaNode at this position.
              scanMetaPath.getLastMetaNode();

            if  // Exitting if null-representing place-holder here. 
              ( scanMetaNode.get(keyString) == PlaceHolderValueString )
              break toReturn; // Do nothing and exit.

            // There is a non-null something else here.
            if  // At least one of the children has an attribute with same key.
              ( scanMetaNode.getChildWithKeyMetaNode( keyString ) != null )
              { // Replace attribute with place-holder and exit.
                scanMetaNode.put(  // Replace the attribute in this MetaNode...
                  keyString,  // ...for the same keyString...
                  PlaceHolderValueString  // ...with place-holder ValueString.
                  );
                break toReturn;  // Exit.
                } // Replace attribute with place-holder and exit.
            
            while (true)  // Remove attribute and place-holders toward root.
              { // Remove one attribute and test for removal in parent.
                scanMetaNode.remove( keyString );  // Remove attribute here.
                scanMetaPath=  // Move MetaPath one MetaNode closer to root.
                  scanMetaPath.getParentMetaPath();
                scanMetaNode=  // Get MetaNode at this new location.
                  scanMetaPath.getLastMetaNode();
                if  // There is no null-representing place-holder here. 
                  ( scanMetaNode.get( keyString ) != PlaceHolderValueString )
                  break toReturn; // So a regular attribute is here.  Exit.
                if  // At least one of the children has a same-key attribute.
                  ( scanMetaNode.getChildWithKeyMetaNode(keyString) != null )
                  break;  // Place-holder justified.  Exit loop.
                } // Remove one attribute and test for removal in parent.
            } // toReturn.
          }

      private void propagateAdditionV( )
        /* This method propagates the addition of the attribute
          up the MetaNode DAG toward the root.
          It does it by adding place-holder attributes where needed.
          This is done so that all instances of attributes with
          the same keyString can be found quickly from the root.
          */
        {
          MetaPath scanMetaPath=   // Initialize MetaPath for scanning.
            getMetaPath();
          while (true)  // Add or verify attribute in MetaNode-s here to root.
            { // Add or verify attribute in one MetaNode.
              scanMetaPath=  // Move MetaPath one node closer to root.
                scanMetaPath.getParentMetaPath();
              if // Exit loop if at pseudo-parent of root.
                ( scanMetaPath == theMetaRoot.getParentOfRootMetaPath( ) )
                break;
              MetaNode scanMetaNode=  // Get MetaNode at this position.
                scanMetaPath.getLastMetaNode();
              if // Exit loop if the node has an attribute with same keyString.
                ( scanMetaNode.containsKey( keyString ) )
                break;
              // Attribute is not there.  We must add place-holder.
              scanMetaNode.put(  // Put an attribute in this MetaNode...
                keyString,  // ...for the same keyString...
                PlaceHolderValueString  // ...with place-holder ValueString.
                );
              } // Add or verify attribute in one MetaNode.
          }

    }
