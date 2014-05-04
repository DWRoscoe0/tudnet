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
          * are not the most recently referenced or 
            are not the top/default node.
    */
      
  { // class MetaTool.
  
    // Instance variables.
    
      private TreePath theTreePath;  /* The path of DataNodes from 
        the DataNode DAG root parent to 
        the DataNode at the location of interest.  */
      private MetaPath theMetaPath;  /* The path of MetaNodes from 
        the MetaNode DAG root parent to 
        the MetaNode that holds the meta-data for
        the DataNode at the location of interest.  */
      
      // This class non-static code block initializes some instance variables.
      {
        theTreePath= DataRoot.getParentOfRootTreePath();
        theMetaPath= MetaRoot.getParentOfRootMetaPath( );
        }
      
    // Constructors.
    
      public MetaTool( TreePath inTreePath )
        /* This constructor builds a MetaTool which is set-up to access 
          the MetaNode for the DAG location associated with inTreePath.
          */
        {
        
          /* The instance variables theTreePath and theMetaPath 
            have already been set by the class non-static code block
            to reference the parents of the roots of 
            their respective DAGs.
            */
          
          Sync( inTreePath );  // Adjust the instance variables so that...
            // ...the locations they represent match...
            // ...the MetaNode associated with inTreePath.

          }
  
    // Instance methods.
    
      public void Sync( TreePath inTreePath )
        /* This recursive method adjusts the instance variables 
          so that the locations they represent match
          the MetaNode associated with inTreePath.
          It does this by comparing inTreePath with instance variable
          theTreePath and if necessary adjusting theTreePath and theMetaPath 
          to match inTreePath.
          It tries to do this incrementally and recursively, 
          so if inTreePath and theTreePath are very similar,
          then syncing will be very fast.
          */
        { // Sync( TreePath inTreePath )
          if ( inTreePath == theTreePath ) // Paths are same reference.
            ; // Do nothing because they are already in sync.
          else if ( inTreePath.equals( theTreePath ) ) // Paths are equal.
            theTreePath= inTreePath;  // Copy reference.  Now they're in sync.
          else if  // Paths are different but have the same length.
            ( inTreePath.getPathCount() == theTreePath.getPathCount() )
            SyncSameLengthsButDifferent( inTreePath );
          else if // New path is longer than old path. 
            ( inTreePath.getPathCount() > theTreePath.getPathCount() )
            SyncLonger( inTreePath );
          else // New path is shorter than old path. 
            SyncShorter( inTreePath );
          } // Sync( TreePath inTreePath )

      private void SyncLonger( TreePath inTreePath )
        /* This method handles the Sync case when
          the new path is longer than the old path.
          */
        {
          Sync( inTreePath.getParentPath() );  // Sync with shorter new path.
          SyncTheMetaPathFromTreePath( inTreePath );
          theTreePath= inTreePath;  // Add last TreePath element by assigning.
          }

      private void SyncShorter( TreePath inTreePath )
        /* This method handles the Sync case when
          the new path is shorter than the old path.
          */
        {
          theTreePath= theTreePath.getParentPath();  // Shorten old path.
          theMetaPath=  // Shorten MetaPath by removing last MetaNode.
            theMetaPath.getParentMetaPath( );
          Sync( inTreePath );  // Sync the shorter paths with new path.
          }

      private void SyncSameLengthsButDifferent( TreePath inTreePath )
        /* This method handles the Sync case when
          the new path is the same length as the old path,
          but they are known to be unequal.
          */
        {
          theTreePath= theTreePath.getParentPath();  // Shorten old path.
          theMetaPath=  // Shorten MetaPath by removing last MetaNode.
            theMetaPath.getParentMetaPath( );
          Sync( inTreePath.getParentPath() );  // Sync shortened paths.
          SyncTheMetaPathFromTreePath( inTreePath );
          theTreePath= inTreePath;  // Add last element by assigning.
          }

      private void SyncTheMetaPathFromTreePath( TreePath inTreePath )
        /* This is a helper method for some of the above Sync... methods.  
          It extends the MetaNode DAG if needed, 
          and adds an element to theMetaPath,
          to match the DataNode DAG path inTreePath.
          */
        {
          Object DataObject=  // Get user Object from new TreePath element.
            inTreePath.getLastPathComponent( );
          MetaNode ChildMetaNode=   // Put it in MetaNode as child MetaNode.
            theMetaPath.
            getLastMetaNode().
            PutChildUserObjectMetaNode( DataObject );
          theMetaPath=  // Add resulting child MetaNode as path element by...
            new MetaPath(  // ...constructing new MataPath from...
              theMetaPath,  // ...old MetaPath...
              ChildMetaNode  // ...and ChildMetaNode as new path element.
              );
          }
  
    // Instance getter methods.

      public MetaPath getMetaPath()
        /* Returns the MetaPath associated with this tool.  */
        { return theMetaPath; }

      public MetaNode getMetaNode()
        /* Returns the MetaNode associated with this tool.  */
        { return theMetaPath.getLastMetaNode(); }

    } // class MetaTool.
