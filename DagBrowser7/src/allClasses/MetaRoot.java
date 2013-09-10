package allClasses;

//import javax.swing.tree.TreePath;

public class MetaRoot
  /* This class manages the root of the MetaNode-s structure.  */
  { // class MetaRoot.
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
        { // Make root MetaNode DAG a single MetaNode.
          RootMetaNode=  // Initialize present MetaNode tree root with...
            new MetaNode(  // ...a new MetaNode containing...
              DataRoot.getRootDataNode( )  // ...the INFOGORA-ROOT DataNode.
              );
          RootMetaNode.put(  // Add attribute for initial Selection.
            Selection.SelectionAttributeString, "IS");  // ??? Kludgy.
          ParentOfRootMetaNode= // Make parent of root MetaNode be...
            new SingleChildMetaNode( // ...a MetaNode whose one-child is...
              RootMetaNode, // ...the root MetaNode and whose object is...
              DataRoot.getParentOfRootDataNode( ) // ...parent of root DataNode.
              );
          } // Make root MetaNode DAG a single MetaNode.

        MetaNode loadedMetaNode=  // Try to load Root MetaNode DAG state...
          MetaFile.start();  // ...from from MetaFile.

        if // Make root MetaNode DAG a single MetaNode if...
          ( ( loadedMetaNode == null) || // ...the above loaded nothing, or...
            ( loadedMetaNode.getDataNode( ) ==  // ...the loaded MetaNode contained...
              ErrorDataNode.getSingletonErrorDataNode( )  // ...an ErrorDataNode.
              )
            )
          ;  // Do nothing, thereby leaving defaults.
          else
          { 
            RootMetaNode= loadedMetaNode;  // Store as Root MetaNode.
            ParentOfRootMetaNode= // Make parent of root MetaNode be...
              new SingleChildMetaNode( // ...a MetaNode whose one-child is...
                RootMetaNode, // ...the root MetaNode and whose object is...
                DataRoot.getParentOfRootDataNode( ) // ...parent of root DataNode.
                );
            }
            
        ParentOfRootMetaPath=  // Make ParentOfRootMetaPath be...
          new MetaPath( null, ParentOfRootMetaNode );  // MetaPath to parent node.
          
        } // Initialize MetaRoot static fields.

    // Methods.

      public static MetaNode getRootMetaNode( )
        { return RootMetaNode; }

      public static MetaNode getParentOfRootMetaNode( )
        { return ParentOfRootMetaNode; }

      public static MetaPath getParentOfRootMetaPath( )
        { return ParentOfRootMetaPath; }

    } // class MetaRoot.
