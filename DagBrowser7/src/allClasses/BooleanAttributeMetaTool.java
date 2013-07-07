package allClasses;

import javax.swing.tree.TreePath;

public class BooleanAttributeMetaTool 

  extends AttributeMetaTool

  /* This is a MetaTool for dealing with a MetaNode's boolean attributes. */

  { // class BooleanAttributeMetaTool 

    // Constructors.

      public BooleanAttributeMetaTool
        ( TreePath InTreePath, String InKeyString )
        {
          super( InTreePath, InKeyString ); // Superclass does all initialization.
          }

    // Static methods.

      static public boolean getNodeAttributeB
        ( MetaNode TheMetaNode, String InKeyString )
        /* This method returns the boolean value of attribute in TheMetaNode
          with attribute name InKeyString.
          It does this by getting the Value String and translating it.
          */
        { 
          boolean ResultB= false;  // Set default value of false.
          do {  // Change default value if needed.
            Object ValueObject= TheMetaNode.get( InKeyString );
            if ( ValueObject == null )  // Value not there means false.
              break;  // Exit and thereby use default false result.
            if ( ValueObject.equals( "T" ) )  // Only a value of T means true.
              ResultB= true;
            } while (false); // Change default value if needed.
          return ResultB;
          }

    // Instance methods.

      public boolean getAttributeB( )
        /* This method returns the boolean value of the attribute
          stored and selected by this BooleanAttributeMetaTool.
          */
        { 
          return getNodeAttributeB(  // Get the attribute in...
            getMetaPath( ).  // ...this instance's MethPath's...
              getLastMetaNode(),  // ...last MetaNode...
            KeyString   // ...with attribute Key in this instance's KeyString.
            );
          }

      public void putAttributeB( boolean ValueB )
        /* This method sets the attribute value to ValueB,
          translating to the appropriate ValueString equivalent.  
          */
        { 
          String ValueString=  // Translate boolean Value into string.
            //( ValueB ? "T" : "F" );
            ( ValueB ? "T" : null );
          put( ValueString );  // Store value String.
          }

  } // class BooleanAttributeMetaTool 
