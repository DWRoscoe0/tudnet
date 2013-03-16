package allClasses;

import javax.swing.tree.TreePath;

public class MetaRoot 
  /* This class manages the root of the MetaNode-s structure.  */
  {
    // variables.  all static, so this is a static class.
    
      public static MetaNode ParentOfRootMetaNode;
      private static MetaNode RootMetaNode;  /* Root of tree which 
        holds Dag information.  */

    // Constructor.

      public MetaRoot( DataNode InRootDataNode )
      /* This constructor doesn't construct any instance of this static class,
          but it does initialize the static variables from InRootDataNode.
          DataRoot must already be initialized.
          */
        { 
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
              DataRoot.ParentOfRootDataNode // ...parent of root DataNode.
              );
          // ???? I need to add root to it's parent.
          MetaPath.UpdatePath(  // Initialize MetaPath...
            new TreePath(   // ...with a single element TreePath to...
              InRootDataNode   // ...the root DataNode.
              ) 
            );
            
          }

    // Methods.
    
      public static MetaNode GetRootMetaNode( )
        /* Returns the root MetaNode. */
        {
          return RootMetaNode;  // Return root as result.
          }
        
/*    public static MetaNode xSetRootMetaNode
        ( Object RootTreePathUserObject )
        // ??? This is going to be removed.
        /* This method sets or checks the Root MetaNode is 
          RootTreePathUserObject.  This should probably be renamed because
          it doesn't set the Root now.  It only checks it.  */
/*      { // SetRootMetaNode( Object RootTreePathUserObject )

          /* 
          // This null check might be handled using a NullPointerException.
          if (RootMetaNode == null)  // there is no root yet.
            { // Create tree root.
              // System.out.println( "MetaPath root assignment");
              RootMetaNode=  // Replace present root with...
                new MetaNode(  // ...a new MetaNode containing...
                   RootTreePathUserObject   // ...the correct user object.
                );
              } // Create tree root.
          */

/*        if  // root has incorrect user object.
            ( RootMetaNode.getUserObject() != RootTreePathUserObject )
            { // Handle root changing error.
              System.out.println( "MetaPath root changed!");
              System.exit( 0 );  // Abort program.
              } // Handle root changing error.
          return RootMetaNode;  // Return root as result.
          } // SetRootMetaNode( Object RootTreePathUserObject )
*/      
    }
