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

    // Non-static setter methods.
  
      public void PropigateAttributeUp( )
        /* This method propigates the pressence of the attribute
          up the MetaNode DAG toward the root.  
          */
        { // PropigateAttribute( String KeyString )
          MetaPath ScanMetaPath= getMetaPath();  // Initialize MetaPath scanner.          
          while (true)  // Add or verify attribute in all MetaNode-s up to root.
            { // Add or verify attribute in one MetaNode.
              ScanMetaPath=  // Move path to next closer node toward root.
                ScanMetaPath.getParentMetaPath();
              if // Exit loop if at pseudo-parent of root.
                ( ScanMetaPath == MetaRoot.getParentOfRootMetaPath( ) )
                break;
              MetaNode ScanMetaNode= ScanMetaPath.getLastMetaNode();
              if // Exit loop if attribute there.
                ( ScanMetaNode.containsKey( KeyString ) )
                break;
              // Attribute is not there.
              ScanMetaNode.put( KeyString, "PRESENT" );  // Put attribute there.
              } // Add or verify attribute in one MetaNode.
          } // PropigateAttribute( String KeyString )

    } // class AttributeMetaTool 
