package allClasses;

// import java.util.List;
// import java.util.Stack;

import javax.swing.tree.TreePath;
 
class MetaTool

  /* This class is used to store meta-data associated with
    DataNodes of the Infogora DAG (Directed Acyclic Graph).
    This includes information about which node children
    were selected, referenced or visited most recently.
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
    
      private TreePath TheTreePath;  /* The path of DataNodes from 
        the DataNode DAG root parent to 
        the DataNode at the location of interest.  */
      private MetaPath TheMetaPath;  /* The path of MetaNodes from 
        the MetaNode DAG root parent to 
        the MetaNode that holds the meta-data for
        the DataNode at the location of interest.  */
      
      // This class non-static code block initializes some instance variables.
      {
        TheTreePath= DataRoot.getParentOfRootTreePath();
        TheMetaPath= MetaRoot.getParentOfRootMetaPath( );
        }
      
    // Constructors.  None at this time.
    
      public MetaTool( TreePath InTreePath )
        /* This constructor builds a MetaTool setup to access 
          the MetaNode for the DAG location associated with InTreePath.
          */
        {
        
          /* The instance variables TheTreePath and TheMetaPath 
            have already been set by the class non-static code block
            to reference the parents of the roots of 
            their respective DAGs.
            */
          
          Sync( InTreePath );  // Adjust the instance variables so that...
            // ...the locations they represent match...
            // ...the MetaNode associated with InTreePath.

          }
  
    // Instance methods.
    
      public void Sync( TreePath InTreePath )
        /* This recursive method adjusts the instance variables 
          so that the locations they represent match
          the MetaNode associated with InTreePath.
          It does this by comparing InTreePath with instance variable
          DataTree and if necessary adjusting TheTreePath and TheMetaPath 
          to match InTreePath.
          It tries to do this incrementally and recursively, 
          so if InTreePath and TheTreePath are very similar,
          then syncing will be very fast.
          */
        { // Sync( TreePath InTreePath )
          if ( InTreePath == TheTreePath ) // Paths are same reference.
            ; // Do nothing because they are already in sync.
          else if ( InTreePath.equals( TheTreePath ) ) // Paths are equal.
            TheTreePath= InTreePath;  // Copy reference.  Now they're in sync.
          else if  // Paths are different but have the same length.
            ( InTreePath.getPathCount() == TheTreePath.getPathCount() )
            SyncSameLengthsButDifferent( InTreePath );
          else if // New path is longer than old path. 
            ( InTreePath.getPathCount() > TheTreePath.getPathCount() )
            SyncLonger( InTreePath );
          else // New path is shorter than old path. 
            SyncShorter( InTreePath );
          } // Sync( TreePath InTreePath )

      private void SyncLonger( TreePath InTreePath )
        /* This method handles the Sync case when
          the new path is longer than the old path.
          */
        {
          Sync( InTreePath.getParentPath() );  // Sync with shorter new path.
          SyncTheMetaPathFromTreePath( InTreePath );
          TheTreePath= InTreePath;  // Add last TreePath element by assigning.
          }

      private void SyncShorter( TreePath InTreePath )
        /* This method handles the Sync case when
          the new path is shorter than the old path.
          */
        {
          TheTreePath= TheTreePath.getParentPath();  // Shorten old path.
          TheMetaPath=  // Shorten MetaPath by removing last MetaNode.
            TheMetaPath.getParentMetaPath( );
          Sync( InTreePath );  // Sync the shorter paths with new path.
          }

      private void SyncSameLengthsButDifferent( TreePath InTreePath )
        /* This method handles the Sync case when
          the new path is the same length as the old path,
          but they are known to be unequal.
          */
        {
          TheTreePath= TheTreePath.getParentPath();  // Shorten old path.
          TheMetaPath=  // Shorten MetaPath by removing last MetaNode.
            TheMetaPath.getParentMetaPath( );
          Sync( InTreePath.getParentPath() );  // Sync shortened paths.
          SyncTheMetaPathFromTreePath( InTreePath );
          TheTreePath= InTreePath;  // Add last element by assigning.
          }

      private void SyncTheMetaPathFromTreePath( TreePath InTreePath )
        /* This is a helper method for some of the above Sync... methods.  
          It extends the MetaNode DAG if needed, 
          and adds an element to TheMetaPath,
          to match the DataNode DAG path InTreePath.
          */
        {
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
          }
  
    // Instance getter methods.

      public MetaPath getMetaPath()
        /* Returns the MetaPath associated with this tool.  */
        { return TheMetaPath; }

      public MetaNode getMetaNode()
        /* Returns the MetaNode associated with this tool.  */
        { return TheMetaPath.getLastMetaNode(); }

    } // class MetaTool.
