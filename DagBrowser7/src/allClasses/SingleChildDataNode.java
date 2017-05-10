package allClasses;

public class SingleChildDataNode

	extends NamedList
  
  /* This class is a DataNode that has only a single child DataNode.
    It serves as a parent of the root of the DataNode DAG.
    This makes the MetaNode:ioDataNode(...) code simpler because 
    it doesn't need to handle the root DataNode as 
    a special case.  

    ///org This is used by only DataRoot, it might be appropriate to:
    * Change the name to RootDataNode.
    * Add another method isAncestorOfRootB() to to DataNode
      which only this class returns as true to make
      testing for in the illegal pseudo-parent status easier and faster.
    */

  { // class SingleChildDataNode

    // Variables.  None.
      
    // Constructor.

      public SingleChildDataNode( DataNode InDataNode )
        {
      		super.initializeV(
            "PARENT-OF:" +  // ...this and...
            	InDataNode.getNameString( ),
            new DataNode[] { InDataNode }
            );
          }

    } // class SingleChildDataNode
