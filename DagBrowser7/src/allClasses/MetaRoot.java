package allClasses;

import javax.swing.tree.TreePath;

public class MetaRoot 
  {
    // variables.  all static, so this is a static class.
    
      public static MetaNode RootMetaNode;  /* Root of tree which 
        holds Dag information.  */

    // Constructor.

      public MetaRoot( TreePath TreePathIn )  
        /* This constructor doesn't construct any instance,
          but it does initialize the static variables,
          from TreePathIn.
          */
        { 
          MetaPath.UpdatePath( TreePathIn );  // do this to create initial tree.
          }
        
      static public MetaNode SetRootMetaNode( Object FirstTreePathUserObject )
        /* This method sets or checks the Root MetaNode to be FirstTreePathUserObject.  */
        { // SetRootMetaNode( Object FirstTreePathUserObject )
          // This null check might be handled using a NullPointerException.
          if (RootMetaNode == null)  // there is no root yet.
            { // Create tree root.
              // System.out.println( "MetaPath root assignment");
              RootMetaNode=  // Replace present root with...
                new MetaNode(  // ...a new MetaNode containing...
                   FirstTreePathUserObject   // ...the correct user object.
                );
              } // Create tree root.

          if  // root has incorrect user object.
            (!RootMetaNode.getUserObject().equals( FirstTreePathUserObject ))
            { // Handle root changing error.
              System.out.println( "MetaPath root changed!");
              System.exit( 0 );  // Abort program.
              } // Handle root changing error.
          return RootMetaNode;  // Return root as result.
          } // SetRootMetaNode( Object FirstTreePathUserObject )
        
    }
