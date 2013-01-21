package allClasses;

import javax.swing.tree.TreePath;

public class MetaRoot 
  /* This class manages the root of the MetaNode-s structure.  */
  {
    // variables.  all static, so this is a static class.
    
      private static MetaNode RootMetaNode;  /* Root of tree which 
        holds Dag information.  */

    // Constructor.

      public MetaRoot( DataTreeModel InDataTreeModel )  
        /* This constructor doesn't construct any instance,
          but it does initialize the static variables,
          from TreeModel.
          */
        { 
          Object RootTreePathUserObject=   // Initialize the root user object...
            InDataTreeModel.getRoot();  // ...from TreeModel root.

          RootMetaNode=  // Initialize present MetaNode tree root with...
            new MetaNode(  // ...a new MetaNode containing...
               RootTreePathUserObject   // ...the root user object.
            );

          MetaPath.UpdatePath(  // Initialize MetaPath...
            new TreePath(   // ...with a single element TreePath to...
              RootTreePathUserObject   // ...the root user object.
              ) 
            );
          }

    // Methods.
    
      public static MetaNode GetRootMetaNode( )
        /* Returns the root MetaNode. */
        {
          return RootMetaNode;  // Return root as result.
          }
        
      public static MetaNode SetRootMetaNode( Object RootTreePathUserObject )
        /* This method sets or checks the Root MetaNode is 
          RootTreePathUserObject.  This should probably be renamed because
          it doesn't set the Root now.  It only checks it.  */
        { // SetRootMetaNode( Object RootTreePathUserObject )

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

          if  // root has incorrect user object.
            (!RootMetaNode.getUserObject().equals( RootTreePathUserObject ))
            { // Handle root changing error.
              System.out.println( "MetaPath root changed!");
              System.exit( 0 );  // Abort program.
              } // Handle root changing error.
          return RootMetaNode;  // Return root as result.
          } // SetRootMetaNode( Object RootTreePathUserObject )
        
    }
