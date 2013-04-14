package allClasses;

import javax.swing.tree.TreePath;

public class DataRoot

  /* This class manages the root of the Data DAG.  */

  { // DataRoot
  
    // Variables.
    
      private static DataNode RootDataNode;  // Root of DataNode DAG.
      private static DataNode ParentOfRootDataNode;  /* Pseudo-parent of DAG root.
        This is in the same tree as RootDataNode, but can be used as
        a sentinel record to eliminate checking for null during
        TreePath traversals toward the root.  */
      private static TreePath ParentOfRootTreePath;  /* This is the TreePath
        associated with ParentOfRootDataNode.  */
        
      static
      /* This class static code block initializes the static variables.  */
      {
        // RootDataNode= InRootDataNode;  // Save the root.  ???
        RootDataNode= new InfogoraRoot( );  // Define root node.
        ParentOfRootDataNode= // Initialize a usable Parent for root.
          new SingleChildDataNode( RootDataNode );
        ParentOfRootTreePath=  // Initialize associated TreePath.
          new TreePath( ParentOfRootDataNode );

        // MetaRoot.Initialize( ); // initialize MetaNode DAG from DataNode DAG.
        }

    // Methods.

      // public static void Initialize( )  // ( DataNode InRootDataNode )
        /* This initializes the static variables.
          InRootDataNode is used as the root of the Data DAG.
          */
      //  {}

      public static DataNode getRootDataNode( )
        { return RootDataNode; }

      public static DataNode getParentOfRootDataNode( )
        { return ParentOfRootDataNode; }

      public static TreePath getParentOfRootTreePath( )
        { return ParentOfRootTreePath; }

    } // DataRoot
