package allClasses;

//// import java.io.IOException;
//// import java.util.LinkedHashMap;

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

    /*  ////
    public void updateFieldV( 
        String fieldKeyString, boolean fieldValueB )
      /* If fieldValueB is different from the value presently associated with 
        fieldKeyString, then it replaces the stored value and
        the field "lastModified" is set to the present time.
        */
    /*  ////
      { 
        MapEpiNode theMapEpiNode= childMapEpiNode;

        updateFieldV( theMapEpiNode, fieldKeyString, fieldValueB );
        }
    */  ////

    public static void updateFieldV( 
        MapEpiNode theMapEpiNode, String fieldKeyString, boolean fieldValueB )
      /* If fieldValueB is different from the value presently associated with 
        fieldKeyString, then it replaces the stored value and
        the field "lastModified" is set to the present time.
        */
      { 
        AppMapEpiNode.updateFieldV( theMapEpiNode, fieldKeyString, ""+fieldValueB );
        }
  
    static void updateFieldV( 
        MapEpiNode theMapEpiNode, String fieldKeyString, String fieldValueString )
      /* If fieldValueString is different from the value presently associated with 
        fieldKeyString, then it replaces the stored value and
        the field "lastModified" is set to the present time.
        */
      { 
        boolean changeNeededB= // Calculate whether field needs to be changed. 
            ! fieldValueString.equals(theMapEpiNode.getString(fieldKeyString));
        if (changeNeededB)
          AppMapEpiNode.putFieldWithLastModifiedV( theMapEpiNode, fieldKeyString, fieldValueString );
        }
  
    static void putFieldWithLastModifiedV( 
        MapEpiNode theMapEpiNode, String fieldKeyString, String fieldValueString )
      /* This method stores fieldValueString into the field whose name is fieldKeyString
        but also updates the field "lastModified" with the present time.
        */
      { 
        AppMapEpiNode.putFieldWithTimeModifiedV(
            theMapEpiNode, fieldKeyString, fieldValueString, "lastModified" );
        }
    
    static void putFieldWithTimeModifiedV( MapEpiNode theMapEpiNode, 
        String fieldKeyString, String fieldValueString, String timeModifiedKeyString )
      /* This method stores fieldValueString into the field whose name is fieldKeyString
        but also updates the field "lastModified" with the present time.
        */
      { 
        theMapEpiNode.putV( fieldKeyString, fieldValueString );
        theMapEpiNode.putV( timeModifiedKeyString, ""+System.currentTimeMillis());
        }
    
  
    /*  //// The following is code that was abandoned when
      it was discovered that much more code would be needed to load the new class
      if nested maps of the same type were to be loaded also.
      It is being kept, for now, in case a an easy way to adapt it can be found.
  
    public static AppMapEpiNode tryBlockAppMapEpiNode( 
        RandomAccessInputStream theRandomAccessInputStream, int minIndentI ) 
        throws IOException
      /* This method tries to parse an AppMapEpiNode (YAML map) 
        from theRandomAccessInputStream.
        It is functionally equivalent to the MapEpiNode version
        except that it returns the subclass.
        */
    /*  ////
      {
          AppMapEpiNode resultAppMapEpiNode= null; // Set default failure result.
          MapEpiNode resultMapEpiNode= // Try parsing the superclass.
            tryBlockMapEpiNode(theRandomAccessInputStream, minIndentI);
          if (null != resultMapEpiNode) // If we parsed the superclass
            resultAppMapEpiNode= // convert it to our class.
              new AppMapEpiNode(resultMapEpiNode.theLinkedHashMap);
          return resultAppMapEpiNode; // Return result.
        }

    public AppMapEpiNode(LinkedHashMap<EpiNode,EpiNode> theLinkedHashMap) // constructor.
      {
        super(theLinkedHashMap);
        }
    
    public AppMapEpiNode() // constructor.
      {
        super();
        }

    */  ////

    }
