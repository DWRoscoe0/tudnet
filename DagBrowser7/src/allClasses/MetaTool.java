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
          SyncTheMetaPathFromTreePath( InTreePath );
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
          SyncTheMetaPathFromTreePath( InTreePath );
          DataTreePath= InTreePath;  // Add last element by assigning.
          } // SyncSameLengthsButDifferent()

      private void SyncTheMetaPathFromTreePath( TreePath InTreePath )
        /* This is a helper method for the above Sync... methods.  
          It extends the MetaNode DAG, and adds an element to TheMetaPath,
          to match the DataNode DAG path InTreePath.
          */
        { // SyncTheMetaPathFromTreePath()
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
          } // SyncTheMetaPathFromTreePath()
  
    // Instance getter methods.

      public MetaPath getMetaPath()
        { return TheMetaPath; }

      public MetaNode getMetaNode()
        { return TheMetaPath.getLastMetaNode(); }

    } // class MetaTool.
