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
        { // SingleChildMetaNode( DataNode InDataNode )

          super(   // Use superclass constructor...
            InDataNode  // ...to store associated DataNode.
            );

          ChildrenLinkedHashMap.put( // Add a child map entry which maps...
            InChildMetaNode.  // ...the child MetaNode's...
            getDataNode(),  // ...associated DataNode to...
            InChildMetaNode  // ...the child MetaNode itself.
            );

          } // SingleChildMetaNode( DataNode InDataNode )

    } // class SingleChildMetaNode
