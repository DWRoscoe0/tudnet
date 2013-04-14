package allClasses;

// import java.util.List;
// import java.util.Stack;

import javax.swing.tree.TreePath;
 
class MetaTool

  /* This class is used to store meta-data associated with
    nodes of the Infogora DAG (Directed Acyclic Graph).
    This includes information about which node children
    were referenced or visited most recently.
    A location within the DAG is specified with
    a TreePath when information is to be read or stored.
    
    Possible enhancements:
        
      Subclasses:
        AttributeTool: MetaTool for operating on Attributes.
      Space:
        Eliminate nodes which:
          * have no children,
          * have no attributes, and
          * are not the most recently referenced or are not the top/default node.

    */
      
  { // class MetaTool.
  
    // Instance variables.
    
      private TreePath DataTreePath;  // Identifies data DAG path from root.
      private MetaPath TheMetaPath;  // Identifies associated meta DAG path.
      
      // This class non-static code block initializes some instance variables.
      {
        DataTreePath= DataRoot.getParentOfRootTreePath();
        TheMetaPath= MetaRoot.getParentOfRootMetaPath( );
        }
      
    // Constructors.  None at this time.
    
      public MetaTool( TreePath InTreePath )
        /* This constructor takes InTreePath and builds a MetaTool
          with DataTreePath set to InTreePath and
          TheMetaPath set to an equivalent path using the Sync(..) method.
          */
        { // MetaTool( TreePath InTreePath )
        
          // Instance variables DataTreePath and TheMetaPath are already set.
          
          Sync( InTreePath );  // Sync instance variables with InTreePath.

          } // MetaTool( TreePath InTreePath )
  
    // Instance methods.
    
      public void Sync( TreePath InTreePath )
        /* This recursive method syncs the [old] DataTreePath 
          with the new InTreePath.
          At the same time it causes TheMetaPath to match.
          It tries to do this incrementally and recursively, 
          assuming the paths have a lot in common at the left end.
          */
        { // Sync( TreePath InTreePath )
          if ( InTreePath == DataTreePath ) // Paths are same reference.
            ; // Do nothing because they are already in sync.
          else if ( InTreePath.equals( DataTreePath ) ) // Paths are equal.
            DataTreePath= InTreePath;  // Copy reference.  Now they're in sync.
          else if  // Paths are different but have the same length.
            ( InTreePath.getPathCount() == DataTreePath.getPathCount() )
            SyncSameLengthsButDifferent( InTreePath );
          else if // New path is longer than old path. 
            ( InTreePath.getPathCount() > DataTreePath.getPathCount() )
            SyncLonger( InTreePath );
          else // New path is shorter than old path. 
            SyncShorter( InTreePath );
          } // Sync( TreePath InTreePath )

      private void SyncLonger( TreePath InTreePath )
        { // SyncLonger()
          Sync( InTreePath.getParentPath() );  // Sync with shorter new path.
          UpdateTheMetaPathFromTreePath( InTreePath );
          DataTreePath= InTreePath;  // Add last TreePath element by assigning.
          } // SyncLonger()

      private void SyncShorter( TreePath InTreePath )
        { // SyncShorter()
          DataTreePath= DataTreePath.getParentPath();  // Shorten old path.
          TheMetaPath=  // Shorten MetaPath by removing last MetaNode.
            TheMetaPath.getParentMetaPath( );
          Sync( InTreePath );  // Sync the shorter paths with new path.
          } // SyncShorter()

      private void SyncSameLengthsButDifferent( TreePath InTreePath )
        { // SyncSameLengthsButDifferent()
          DataTreePath= DataTreePath.getParentPath();  // Shorten old path.
          TheMetaPath=  // Shorten MetaPath by removing last MetaNode.
            TheMetaPath.getParentMetaPath( );
          Sync( InTreePath.getParentPath() );  // Sync shortened paths.
          UpdateTheMetaPathFromTreePath( InTreePath );
          DataTreePath= InTreePath;  // Add last element by assigning.
          } // SyncSameLengthsButDifferent()

      private void UpdateTheMetaPathFromTreePath( TreePath InTreePath )
        { // UpdateTheMetaPathFromTreePath()
          Object DataObject=  // Get user Object from new TreePath element.
            InTreePath.getLastPathComponent( );
          MetaNode ChildMetaNode=   // Put it in MetaNode as child MetaNode.
            TheMetaPath.
            getLastMetaNode().
            PutChildUserObjectMetaNode( DataObject );
          TheMetaPath=  // Add resulting child MetaNode as path element by...
            new MetaPath(  // ...constructing new MataPath from...
              TheMetaPath,  // ...old MetaPath...
              ChildMetaNode  // ...and ChildMetaNode as new path element.
              );
          } // UpdateTheMetaPathFromTreePath()
  
    // Non-static getter methods.

      public MetaPath getMetaPath()
        { return TheMetaPath; }

      public MetaNode getMetaNode()
        { return TheMetaPath.getLastMetaNode(); }

    // Static setter methods.  These write information to tree.
          
      static public void UpdatePath( TreePath TreePathIn )
        /* This does the same as UpdatePathITreeNode(.) except 
          it doesn't return the MetaNode associated with 
          the end of the path.
          It exists mainly to help other code be self-documenting.
          */
        { // UpdatePath(.)
          UpdatePathMetaNode( TreePathIn ); // Update with TreePath.
          } // UpdatePath(.)

      static public DataNode UpdatePathDataNode
        ( TreePath TreePathIn )
        /* Updates the MetaTool with TreePathIn and returns 
          the user object of the most recently visited child of 
          the tree node at the end of that path,
          or null if there is no such child. 
          */
        { // UpdateToAndGetRecentChildUserObjectAt(.)
          MetaNode EndOfPathMetaNode=  // Get last MetaNode in path by...
            UpdatePathMetaNode(  // ...updating tree with...
              TreePathIn  // ...the provided TreePath.
              );
          DataNode ChildDataNode=  // Get the last MetaNode's...
            EndOfPathMetaNode.GetLastReferencedChildDataNode(  // ...most recent user object.
              );
          return ChildDataNode;  // return the resulting child user object.
          } // UpdateToAndGetRecentChildUserObjectAt(.)

      static public MetaNode UpdatePathMetaNode( TreePath InTreePath )
        /* Updates the MetaTool structure anchored
          starting with the root and ending at the node specified by InTreePath.
          * It adds to the structure any part of the path InTreePath 
            that is not in the structure.
          * It reorders the children so the more recently referenced ones
            can be referenced quickly later.
          It also returns the MetaNode at the end of the specified TreePath.
          */
        { // UpdatePathMetaNode()

          MetaTool WorkerMetaTool= new MetaTool( // Create new MetaTool...
              InTreePath  // ...to work on InTreePath.
              );
          return WorkerMetaTool.getMetaNode();

          } // UpdatePathMetaNode()

    } // class MetaTool.
