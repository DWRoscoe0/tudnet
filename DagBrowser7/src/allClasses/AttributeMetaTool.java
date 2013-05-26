package allClasses;

import javax.swing.tree.TreePath;

public class AttributeMetaTool 

  extends MetaTool

  /* This is a MetaTool for dealing with a MetaNode's attributes. */

  { // class AttributeMetaTool

    // Instance variables.

      String KeyString;  // Key String associated with all attribute.

    // Constructors.

      public AttributeMetaTool( TreePath InTreePath, String InKeyString )
        {
          super( InTreePath );  // Construct superclass.
          KeyString= InKeyString;  // Initialize KeyString.
          }

    // Instance getter methods.

      public Object get( )
        { return getMetaNode().get( KeyString ); }

    // Instance setter methods.

      public Object put( Object ValueObject )
        { 
          Object ResultObject=  // Put new attribute value and save old one.
            getMetaNode().put( KeyString, ValueObject ); 
          PropigateAttributeUp( );  // Propigate the attribute toward root.
          return ResultObject;  // Return old value.
          }

      public Object remove( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        { // remove(..)
          Object ResultObject=  // Remove attribute key and value.
            getMetaNode().remove( KeyString ); 
          return ResultObject;  // Return old value.
          } // remove(..)
    // Instance setter methods.
  
      public void PropigateAttributeUp( )
        /* This method propigates the pressence of the attribute
          up the MetaNode DAG toward the root.  
          Maybe add CLEAN attribute???
          */
        { // PropigateAttribute( String KeyString )
          MetaPath ScanMetaPath=   // Initialize MetaPath scanner.
            getMetaPath();
          while (true)  // Add or verify attribute in MetaNode-s here to root.
            { // Add or verify attribute in one MetaNode.
              ScanMetaPath=  // Move path to node next closer toward root.
                ScanMetaPath.getParentMetaPath();
              if // Exit loop if at pseudo-parent of root.
                ( ScanMetaPath == MetaRoot.getParentOfRootMetaPath( ) )
                break;
              MetaNode ScanMetaNode= ScanMetaPath.getLastMetaNode();
              if // Exit loop if any attribute value there.
                ( ScanMetaNode.containsKey( KeyString ) )
                break;
              // Attribute is not there.
              ScanMetaNode.put(  // Put attribute in this MetaNode...
                KeyString,  // ...for the preseet key...
                "PRESENT"  // ...with place-holder value "PRESENT".
                );
              } // Add or verify attribute in one MetaNode.
          } // PropigateAttribute( String KeyString )

    } // class AttributeMetaTool 
