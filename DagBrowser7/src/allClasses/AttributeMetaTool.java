package allClasses;

import javax.swing.tree.TreePath;

abstract class AttributeMetaTool

  extends MetaTool

  /* This is a MetaTool specialized for dealing with 
    a MetaNode's attributes. 

    Attribute are name-value pairs.
    Values for attributes handled by this class are as follows:

    * "" (blank): default value, and all descendants have the same value.
    * "$MIXED$": default value (same meaning as above) but some descendants 
      do not have the default value.
    * (anything else): all other values, with various meanings.

    */

  {

    // Constants.

      private final String PlaceHolderValueString= "$MIXED$";
        /* This attribute Map special value is used to indicate that
          the MetaNode has a null/default value for this attribute 
          but at least one of its descendants MetaNodes do not.
          Any other otherwise-unused value could be used here.  
          */

    // Instance variables.

      String keyString;  // String giving name of Key of attribute of interest.

    // Constructors.

      protected AttributeMetaTool( 
          MetaRoot theMetaRoot, 
          TreePath inTreePath, 
          String inKeyString 
          )
        {
          super( theMetaRoot, inTreePath );  // Construct superclass.

          keyString= inKeyString;  // Initialize keyString.
          }

    // Instance methods.

      protected Object get( )
      /* This is like a normal get(..) from the keyString entry of a Map.  
        It returns the value associated with the Key and TreePath
        associated with this tool.
        This method will not return PlaceHolderValueString
        but will return "" instead.
        If you want the actual value, including PlaceHolderValueString,
        use getRaw() instead.
        */
      { 
        Object valueObject= getRaw(); // Getting raw attribute value.
        if (valueObject == PlaceHolderValueString)
        	valueObject= "";  // Replace with default value.
        return valueObject; // Returning the attribute value.
      	}

      protected Object getRaw( )
      /* This is like a normal get(..), from the keyString entry
        of a Map.  It returns the value associated with the key
        associated with this tool.
        This method will return PlaceHolderValueString
        if that is the value of the attribute.
        */
      { 
        MetaNode theMetaNode= getMetaNode();  // Getting the MetaNode.
        Object valueObject=  // Getting present attribute value.
           theMetaNode.get( keyString );
        return valueObject; // Returning the attribute value.
      	}

      protected Object put( Object inValueObject )
        /* This is like a normal put(..) to the keyString entry
          of a Map, but with some additional functionality:
          * If an attribute entry is added then
            it adds PlaceHolderValueString as the value of the attributes with 
            the same keyString between the current MetaNode and the root 
            if needed.
          * It removes an attribute entry if 
            the new value inValueObject is null.
            It also removes place-holder attributes with 
            the same keyString between the current MetaNode and 
            the root if possible.
          The place-holder attributes are maintained 
          to be able to quickly find all instances 
          of attributes with the same keyString starting from the root,
          and to facilitate deletion of MetaNodes that 
          no longer hold useful data.
          */
        { 
          MetaNode theMetaNode= getMetaNode();  // Cache the MetaNode.
          Object oldObject=  // Get present attribute value.
             theMetaNode.get( keyString );
          { // Updating the attribute in the appropriate way.
            if   // Doing nothing if attribute value is not changing.
              ( oldObject == inValueObject )
              ; // Doing nothing.
            else if   // Adding missing attribute if needed.
              ( oldObject == null )
              { // Adding the attribute.
                theMetaNode.put(  // Adding new value.
                  keyString, inValueObject 
                  );
                propagateAdditionV( );  // Adding place-holders if needed.
                } // Adding the attribute.
            else if // Removal of present attribute needed.
              ( inValueObject == null )
              removeOrReplaceV( );  // Removing or replacing with place-holder.
            else // Replacing present non-null value with the new value.
              theMetaNode.put( keyString, inValueObject );  // Replacing value.
            } // Updating the attribute in the appropriate way.
          return oldObject;  // Return old attribute value as result.
          }
    
      private void removeOrReplaceV( )
        /* This method removes the attribute at the present position 
          by either deleting it or replacing it by a place-holder value.
          If any children have the same attribute key,
          then it replaces the attribute value with a place-holder value. 
          If no children have the same attribute key,
          then it removes the attribute completely.
          Then it scans toward the root and removes any place-holder values
          for those ancestors which have no children with the same key.
          */
        {
          toReturn: {  // toReturn.
            MetaPath scanMetaPath=  // Get MetaPath of position.
              getMetaPath();
            MetaNode scanMetaNode=  // Get MetaNode at this position.
              scanMetaPath.getLastMetaNode();

            if  // Exiting if null-representing place-holder is already here. 
              ( scanMetaNode.get(keyString) == PlaceHolderValueString )
              break toReturn; // Do nothing and exit.

            // There is a non-null something else here.
            if  // At least one of the children has an attribute with same key.
              ( scanMetaNode.getChildWithKeyMetaNode( keyString ) != null )
              { // Replacing attribute with place-holder and exit.
                scanMetaNode.put(  // Replacing the attribute in this MetaNode
                  keyString,  // for the same keyString
                  PlaceHolderValueString  // with place-holder ValueString.
                  );
                break toReturn;  // Exit.
                }
            
            // None of the children had the same attribute key.
            while (true)  // Removing attribute and place-holders toward root.
              { // Removing attribute here and checking toward root.
                scanMetaNode.remove( keyString );  // Removing attribute here.
                scanMetaPath=  // Moving MetaPath one MetaNode closer to root.
                  scanMetaPath.getParentMetaPath();
                scanMetaNode=  // Getting MetaNode at this new location.
                  scanMetaPath.getLastMetaNode();
                if  // Exiting if no null-representing place-holder here. 
                  ( scanMetaNode.get( keyString ) != PlaceHolderValueString )
                  break toReturn; // Exiting because regular attribute is here.
                if  // Exiting if any of its children has the same key.
                  ( scanMetaNode.getChildWithKeyMetaNode(keyString) != null )
                  break;  // Exiting because place-holder here is justified.
                }
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
                PlaceHolderValueString  // ...with place-holder valueString.
                );
              } // Add or verify attribute in one MetaNode.
          }

    }
