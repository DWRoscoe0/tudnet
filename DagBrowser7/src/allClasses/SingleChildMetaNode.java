package allClasses;

public class SingleChildMetaNode 

  extends MetaNode 

  /* This class is a MetaNode that has only a single child MetaNode.
    It serves as a parent of the root of the MetaNode DAG.
    This makes code such as MetaTool.UpdatePath(..) simpler because
    it doesn't need to handle the root MetaNode as 
    a special case.  */

  { // class SingleChildMetaNode

    // Constructor.

      public SingleChildMetaNode
        ( MetaNode InChildMetaNode, DataNode InDataNode )
        /* This constructor constructs a MetaNode 
          whose DataNode is InDataNode and with
          a single child MetaNode InChildMetaNode 
          and no attributes.  */
        {

          super(   // Use superclass constructor...
            InDataNode  // ...to store associated DataNode.
            );

          theMetaChildren.put(  // Store...
            InChildMetaNode // ...the specified single child MetaNode.
            );

          }

    } // class SingleChildMetaNode
