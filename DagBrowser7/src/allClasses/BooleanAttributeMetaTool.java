package allClasses;

import javax.swing.tree.TreePath;

class BooleanAttributeMetaTool 

  extends AttributeMetaTool

  /* This is am Attribute MetaTool for dealing with 
    a MetaNode's boolean attributes. 
    */

  {

    // Constructors.

      public BooleanAttributeMetaTool( 
          MetaRoot theMetaRoot, TreePath InTreePath, String InKeyString 
          )
        {
          super( theMetaRoot, InTreePath, InKeyString ); // Superclass does all.
          }

    // Static methods.

      private boolean getNodeAttributeB(  // Could be static.
          MetaNode TheMetaNode, String InKeyString 
          )
        /* This method returns the boolean value of the attribute 
          in TheMetaNode with attribute name InKeyString.
          It does this by getting the associated Value String 
          and translating it to a boolean value.
          */
        { 
          boolean ResultB= false;  // Setting default value of false.

          do {  // Changing default value if needed.
            Object ValueObject= TheMetaNode.get( InKeyString );
            if   // Returning with false if Value not there.
              ( ValueObject == null )
              break;  // Exiting with default false result.
            if   // Returning with true if attribute value is "T".
              ( ValueObject.equals( "T" ) )
              ResultB= true;
            } while (false);

          return ResultB; // Return final result value.
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
            keyString   // ...with attribute Key in this instance's keyString.
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

  }
