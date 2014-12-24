package allClasses;

public class SingleChildMetaNode 

  extends MetaNode 

  /* This class is a MetaNode that has only a single child MetaNode.
    It serves as a parent of the root of the MetaNode DAG.
    This makes code such as MetaTool.UpdatePath(..) simpler because
    it doesn't need to handle the root MetaNode as 
    a special case.  */

  { // class SingleChildMetaNode

    // Constructors.

      /* ???
      public SingleChildMetaNode ( ///
          MetaNode InChildMetaNode, 
          DataNode InDataNode 
          )
        /* This constructor constructs a MetaNode 
          whose DataNode is InDataNode and with
          a single child MetaNode InChildMetaNode 
          and no attributes.  */
        /* ???
        {
          super(   // Use superclass constructor...
            InDataNode  // ...to store associated DataNode.
            );

          theMetaChildren.add(  // Store the SingleChildMetaNode's...
            InChildMetaNode // ...one and only child MetaNode.
            );
          }
        */

      public SingleChildMetaNode ( /// ???
          MetaFileManager theMetaFileManager,
          MetaNode InChildMetaNode, 
          DataNode InDataNode 
          )
        /* This constructor constructs a MetaNode 
          whose DataNode is InDataNode and with
          a single child MetaNode InChildMetaNode 
          and no attributes.  */
        {
          super(   // Use superclass constructor...
            theMetaFileManager,
            InDataNode  // ...to store associated DataNode.
            );

          theMetaChildren.add(  // Store the SingleChildMetaNode's...
            InChildMetaNode // ...one and only child MetaNode.
            );
          }

    } // class SingleChildMetaNode
