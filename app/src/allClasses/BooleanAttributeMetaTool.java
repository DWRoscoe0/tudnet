package allClasses;

import javax.swing.tree.TreePath;

class BooleanAttributeMetaTool 

  extends AttributeMetaTool

  /* This is am Attribute MetaTool for dealing with 
    a MetaNode's boolean attributes. 

    Attribute are name-value pairs.
    Values for attributes handled by this class are as follows:

    * "" (blank) : default value, which is FALSE.  
      All descendants have the same value.
    * "$MIXED$" : default value, FALSE, 
      and at least some descendants do not have the same value.
    * "T": TRUE.
    * (anything else) : illegal value.

    */

  {

    // Constructors.

      public BooleanAttributeMetaTool( 
          MetaRoot theMetaRoot, TreePath inTreePath, String inKeyString 
          )
        {
          super( // Superclass does all. 
              theMetaRoot, inTreePath, inKeyString 
              );
          }

    // Static methods.

      public static boolean getNodeAttributeB(
          MetaNode theMetaNode, String inKeyString 
          )
        /* This method returns the boolean value of the attribute 
          in theMetaNode with attribute name InKeyString.
          It does this by getting the associated Value String 
          and translating it to a boolean value.
          */
        { 
          boolean resultB= false;  // Setting default value of false.

          do {  // Changing default value if needed.
            Object valueObject= theMetaNode.get( inKeyString );
            if   // Returning with false if Value not there.
              ( valueObject == null )
              break;  // Exiting with default false result.
            if   // Returning with true if attribute value is "T".
              ( valueObject.equals( "T" ) )
              resultB= true;
            } while (false);

          return resultB; // Return final result value.
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
          translating to the appropriate valueString equivalent.  
          */
        { 
          String valueString=  // Translate boolean Value into string.
            ( ValueB ? "T" : null );
          put( valueString );  // Store value String.
          }

  }
