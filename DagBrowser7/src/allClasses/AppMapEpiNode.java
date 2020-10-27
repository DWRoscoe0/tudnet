package allClasses;

import java.util.Objects;


public class AppMapEpiNode 

  // extends MapEpiNode 

  /* This class was originally meant to subclass MapEpiNode,
    until it was realized that to be useful a new loading method
    would be needed to load nested maps.
    
    So now this class serves only as a repository for static methods
    that do useful things with MapEpiNodes.
    */
  
  {

    // Static methods for updating fields.

    public static void updateFieldV( 
        MapEpiNode theMapEpiNode, String fieldKeyString, boolean fieldValueB )
      /* If fieldValueB is different from the value presently associated with 
        fieldKeyString, then it replaces the stored value and
        the field "lastModified" is set to the present time.
        */
      { 
        AppMapEpiNode.updateFieldV( 
            theMapEpiNode, fieldKeyString, ""+fieldValueB );
        }
  
    static void updateFieldV( 
        MapEpiNode theMapEpiNode, 
        String fieldKeyString, 
        String fieldValueString 
        )
      /* If fieldValueString is different from the value 
       * presently associated with fieldKeyString, 
       * then it replaces the stored value and
       * the field "lastModified" is set to the present time.
       */
      { 
        boolean changeNeededB= // Calculate whether field needs to be changed. 
            ! Objects.equals(
                fieldValueString,
                theMapEpiNode.getString(fieldKeyString)
                );
        if (changeNeededB) { // Change storage is needed.
          if (null != fieldValueString) // Act based on whether new value is null.
            theMapEpiNode.putV( fieldKeyString, fieldValueString ); // Store in entry.
            else
            theMapEpiNode.removeV(fieldKeyString); // Remove entry.
          updateLastModifiedTimeV(theMapEpiNode); // Update time-stamp.
          }
        }

    static void updateLastModifiedTimeV(MapEpiNode theMapEpiNode) 
      /* This method updates the field "lastModified" with the present time.
        */
      { 
        theMapEpiNode.putV( "lastModified", ""+System.currentTimeMillis());
        }

    }
