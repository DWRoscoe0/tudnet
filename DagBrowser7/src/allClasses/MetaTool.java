package allClasses;

// import java.util.List;
// import java.util.Stack;

import javax.swing.tree.TreePath;
 
abstract class MetaTool

  /* This class is a tool for reading and writing the meta-data 
    in the MetaNodes associated with the DataNodes of 
    the Infogora DAG (Directed Acyclic Graph).
    This includes information about which node children
    were selected, referenced or visited most recently.
    A location within the DAG is specified with
    a TreePath when information is to be read or stored.

    This tool maintains two paths:
    * A TreePath which represents a path to a DataNode 
      in the DataNode DAG.
    * A MetaPath which represents a path in a MetaNode
      to the MetaNode DAG which contains data about 
      the associated DataNode.
    These paths are maintained in sync.
    When one changes, the other changes to an equivalent value.

    Possible enhancements ???
        
      Space:
        Eliminate nodes which:
          * have no children,
          * have no attributes, and
          * are not the most recently referenced or 
            are not the top/default node.
    */
      
  { // class MetaTool.
  
    // Injected dependency variables.

      MetaRoot theMetaRoot;
  
    // Other instance variables.
    
      private TreePath theTreePath;  /* The path of DataNodes from 
        the DataNode DAG root parent to 
        the DataNode at the location of interest.  */
      private MetaPath theMetaPath;  /* The path of MetaNodes from 
        the MetaNode DAG root parent to 
        the MetaNode that holds the meta-data for
        the DataNode at the location of interest.  */

     // Constructors.
      public MetaTool( MetaRoot theMetaRoot, TreePath inTreePath )
        /* This constructor builds a MetaTool which is set-up to access 
          the MetaNode for the DAG location associated with inTreePath.
          It does this by initializing both path instance variables
          to point to the roots of their respective DAGs,
          and then syncing them to inTreePath.
          */
        {
          ///this.theMetaRoot= MetaRoot.get();
          this.theMetaRoot= theMetaRoot;
          
          theTreePath=  // Initializing DataNode TreePath.
            ///DataRoot.getIt().getParentOfRootTreePath();
            theMetaRoot.getTheDataRoot().getParentOfRootTreePath();
          theMetaPath=  // Initializing MetaNode TreePath.
            theMetaRoot.getParentOfRootMetaPath( );
          
          Sync( inTreePath ); // Adjusting the instance variables so...
            // ...the locations they represent match...
            // ...the MetaNode associated with inTreePath.

          }

    // Instance methods.
    
      protected void Sync( TreePath inTreePath )
        /* This recursive method adjusts the instance variables 
          so that the locations they represent match
          the MetaNode associated with inTreePath.
          It does this by comparing inTreePath with instance variable
          theTreePath and if necessary adjusting 
          theTreePath and theMetaPath to match inTreePath.
          It tries to do this incrementally and recursively, 
          so if inTreePath and theTreePath are very similar,
          then syncing will be very fast.
          */
        {
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
          }

      private void SyncLonger( TreePath inTreePath )
        /* This method handles the Sync case when
          the new path is longer than the old path.
          */
        {
          Sync(   // Sync with shorter new path.
            inTreePath.getParentPath() 
            );
          SyncTheMetaPathFromTreePath( inTreePath );
          theTreePath=   // Add last TreePath element by assigning.
            inTreePath;
          }

      private void SyncShorter( TreePath inTreePath )
        /* This method handles the Sync case when
          the new path is shorter than the old path.
          */
        {
          theTreePath=  // Shorten old path by removing last DataNode.
            theTreePath.getParentPath();
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
        /* This is a helper method for 
          some of the above Sync... methods.  
          It extends the MetaNode DAG if needed, 
          and adds an element to theMetaPath,
          to match the DataNode DAG path inTreePath.
          */
        {
          Object DataObject=  // Get Object from new TreePath element.
            inTreePath.getLastPathComponent( );
          MetaNode ChildMetaNode= // Put it in MetaNode as child.
            theMetaPath.
            getLastMetaNode().
            PutChildUserObjectMetaNode( DataObject );
          theMetaPath=  // Add resulting child MetaNode to path by...
            new MetaPath(  // ...constructing new MataPath from...
              theMetaPath,  // ...old MetaPath...
              ChildMetaNode  // ...and ChildMetaNode as new element.
              );
          }
  
    // Instance getter methods.

      protected MetaPath getMetaPath()
        /* Returns the MetaPath associated with this tool.  */
        { return theMetaPath; }

      protected MetaNode getMetaNode()
        /* Returns the MetaNode associated with this tool.  */
        { return theMetaPath.getLastMetaNode(); }

    } // class MetaTool.
