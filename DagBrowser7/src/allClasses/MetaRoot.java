package allClasses;

//import javax.swing.tree.TreePath;

import static allClasses.Globals.*;  // For appLogger;

public class MetaRoot {

  /* This class manages the root of the MetaNode-s structure.  

    ??? Eliminate all statics and static references to this class.

    */

  // variables.
  
    private static MetaNode rootMetaNode;  /* Root of tree which 
      holds Dag information.  */
    private static MetaNode parentOfRootMetaNode;  /* Pseudo-parent of root.
      This is the same tree as rootMetaNode, but can be used as
      a sentinel record to eliminate checking for null during
      MetaPath traversals toward the root.  */
    private static MetaPath parentOfRootMetaPath;  /* MetaPath associated with
      parentOfRootMetaNode.  */

    static { // Initializing MetaRoot static fields.  ??? divide/shorten.

      /* This class static code block initializes the static variables.  */

      } // Initializing MetaRoot static fields.

  MetaFileManager theMetaFileManager;

  MetaRoot( MetaFileManager theMetaFileManager )  // Constructor.
    {
      appLogger.info( "MetaRoot constructor starting.");

      this.theMetaFileManager= theMetaFileManager;

      { // Setting root MetaNode DAG to default of a single MetaNode.
        rootMetaNode=  // Initialize present MetaNode tree root with...
          new MetaNode(  // ...a new MetaNode containing...
            DataRoot.getRootDataNode( )  // ...the Infogora-Root DataNode.
            );
        parentOfRootMetaNode= // Making parent of root MetaNode be...
          new SingleChildMetaNode( // ...a MetaNode whose one-child is...
            rootMetaNode, // ...the root MetaNode and whose object is...
            DataRoot.getParentOfRootDataNode( ) // ...parent of root DataNode.
            );
        } // Setting root MetaNode DAG to default of a single MetaNode.

      MetaNode loadedMetaNode=  // Trying to load new MetaNode DAG state...
        ///MetaFileManager.start();  // ...from from MetaFile.  ???
        theMetaFileManager.start();  // ...from from MetaFile.  ???

      if // Handling load failure, either...
        ( ( loadedMetaNode == null) || // ...nothing was loaded, or...
          ( loadedMetaNode.getDataNode( ) == // ...the loaded data had...
            UnknownDataNode.newUnknownDataNode( )  // ...an error.
            )
          )
        ;  // Doing nothing, thereby using default of single MetaNode.
        else // Handling case ofThe load succeeding.
        { // Replacing default data with the loaded data.
          rootMetaNode= loadedMetaNode;  // Store loaded MetaNode as Root.
          parentOfRootMetaNode= // Recalculating parent of root MetaNode...
            new SingleChildMetaNode( // ...to be MetaNode whose one-child is...
              rootMetaNode, // ...the root MetaNode and whose object is...
              DataRoot.getParentOfRootDataNode( ) // ...parent of root node.
              );
          }
          
      parentOfRootMetaPath=  // Making parentOfRootMetaPath be...
        new MetaPath( // ...MetaPath to parent node.
          null, parentOfRootMetaNode 
          );
      rootMetaNode.put(  // Forcing Selection attribute on Root.
        Selection.selectionAttributeString, "IS");  // ???.
        // This guarantees success Selection.buildAttributeTreePath( ).
        // This is compatible with both loaded and non-loaded MetaNodes,
        // because the root node should always be in the selection path.
        
      }

  // Methods.

    public static MetaNode getRootMetaNode( )
      { return rootMetaNode; }

    public static MetaNode getParentOfRootMetaNode( )
      { return parentOfRootMetaNode; }

    public static MetaPath getParentOfRootMetaPath( )
      { return parentOfRootMetaPath; }

    /* ??? Maybe add these, though maybe with different names:

      public static boolean isRootB( MetaNode )

      public static boolean isParentOfRootB( MetaNode )

      public static boolean isParentOfRootB( MetaPath ) or isNullB(..)

      */

  }
