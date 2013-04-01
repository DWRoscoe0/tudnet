package allClasses;

//import javax.swing.tree.TreePath;

public class MetaRoot 
  /* This class manages the root of the MetaNode-s structure.  */
  {
    // variables.  all static, so this is a static class.
    
      private static MetaNode RootMetaNode;  /* Root of tree which 
        holds Dag information.  */
      private static MetaNode ParentOfRootMetaNode;  /* Pseudo-parent of root.
        This is the same tree as RootMetaNode, but can be used as
        a sentinel record to eliminate checking for null during
        MetaPath traversals toward the root.  */
      private static MetaPath ParentOfRootMetaPath;  /* MetaPath associated with
        ParentOfRootMetaNode.  */

      static
      /* This class static code block initializes the static variables.  */
      { // Initialize MetaRoot static fields.
        DataNode InRootDataNode= DataRoot.getRootDataNode( );

        RootMetaNode=  // Try to load Root MetaNode DAG state...
          MetaFile.start(  // ...from from MetaFile for...
            InRootDataNode );  // ...DataNode-s rooted at InRootDataNode.

        //MetaFile.finish();  // Debug ????

        if // Make root MetaNode DAG a single MetaNode if...
          ( ( RootMetaNode == null) || // ...the above loaded nothing, or...
            ( RootMetaNode.getDataNode( ) ==  // ...the loaded MetaNode contained...
              ErrorDataNode.getSingletonErrorDataNode( )  // ...an ErrorDataNode.
              )
            )
          { // Make root MetaNode DAG a single MetaNode.
            RootMetaNode=  // Initialize present MetaNode tree root with...
              new MetaNode(  // ...a new MetaNode containing...
                InRootDataNode   // ...the INFOGORA-ROOT DataNode.
                );
            } // Make root MetaNode DAG a single MetaNode.

        ParentOfRootMetaNode= // Make parent of root MetaNode be...
          new SingleChildMetaNode( // ...a MetaNode whose one-child is...
            RootMetaNode, // ...the root MetaNode and whose object is...
            DataRoot.getParentOfRootDataNode( ) // ...parent of root DataNode.
            );
            
        ParentOfRootMetaPath=  // Make ParentOfRootMetaPath be...
          new MetaPath( null, ParentOfRootMetaNode );  // MetaPath to parent node.

        // ???? I need to add root to it's parent.
        /*
        MetaTool.UpdatePath(  // Initialize MetaTool...
          new TreePath(   // ...with a single element TreePath to...
            InRootDataNode   // ...the root DataNode.
            ) 
          );
        */
          
        } // Initialize MetaRoot static fields.

    // Methods.

      //public static void Initialize( )
      /* This initializes the static variables from InRootDataNode.
          DataRoot must already be initialized.
          */
      //  { }  // All code was moved to static code block.
    
      public static MetaNode getRootMetaNode( )
        { return RootMetaNode; }

      public static MetaNode getParentOfRootMetaNode( )
        { return ParentOfRootMetaNode; }

      public static MetaPath getParentOfRootMetaPath( )
        { return ParentOfRootMetaPath; }

    }
