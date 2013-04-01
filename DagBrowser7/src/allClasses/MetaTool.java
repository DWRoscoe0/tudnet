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
    
    Possible optimizations (not needed yet):
        
      Space:
        Eliminate nodes which:
          * have no children,
          * have no attributes, and
          * are not the most recently referenced or are not the top/default node.

    */
      
  { // class MetaTool.
  
    // Instance variables.
    
      private TreePath DataTreePath;  // Identifies data DAG position.
      private MetaPath TheMetaPath;  // Identifies associated meta DAG position.
      
      // This class non-static code block initializes some instance variables.
      {
        DataTreePath= DataRoot.getParentOfRootTreePath();
        TheMetaPath= MetaRoot.getParentOfRootMetaPath( );
        }

      //Stack<Object> PathStack;  // ?? may not be needed soon.
      //private MetaNode TerminalMetaNode;  // ?? may not be needed soon.
      
    // Constructors.  None at this time.
    
      public MetaTool( TreePath InTreePath )
        /* This constructor takes InTreePath and builds a Stack
          containing its path elements in order ready to scan
          or build MetaNode-s associated with those elements.
          */
        { // MetaTool( TreePath InTreePath )
          Sync( InTreePath );  // Sync with InTreePath.

          // DataTreePath= InTreePath;  // Save TreePath.

          /*
          PathStack= new Stack<Object>();
          do  // Build Stack of path elements by...
            { // ...pushing TreePath element onto Stack...
              PathStack.push(  // Push onto the Stack...
                InTreePath.getLastPathComponent( )  // ...last path element.
                );
              InTreePath= InTreePath.getParentPath( );  // Remove last element.
              } // ...pushing TreePath element onto Stack.
            while ( InTreePath != null );  // Until none of TreePath remains.
          PathStack.pop();  // Pop off the pseudo-parent of root element.
          */
          } // MetaTool( TreePath InTreePath )
  
    // Instance methods.
    
      private void Sync( TreePath InTreePath )
        /* This recursive method syncs the old DataTreePath 
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

      //private MetaNode UpdatePathFromStackMetaNode
      /*
      private void UpdatePathFromStackMetaNode
        ( )
        /* This private grouping method is similar to 
          UpdatePathMetaNode( TreePath TreePathIn ) except that 
          the path is specified by stack elements instead of a TreePath.
          */
      /*
        { // UpdatePathFromStackMetaNode()
          MetaNode ScanMetaNode= // Initialize ScanMetaNode to be...
            MetaRoot.getParentOfRootMetaNode( );  // ...parent of MetaRoot.
          while   // Update all remaining path elements into Meta DAG.
            ( ! PathStack.empty( ) )  // Path elements remain in stack.
            { // Update one path element into structure.
              Object DesiredObject=  // Get user Object from path element...
                PathStack.pop( );  // ...and simultaneously remove from Stack.
              MetaNode ChildMetaNode=   // Put it in MetaNode in MetaTool structure.
                ScanMetaNode.PutChildUserObjectMetaNode( DesiredObject );
              ScanMetaNode= ChildMetaNode; // Make child be new scan node.
              } // Update one path element into structure.
          TerminalMetaNode= ScanMetaNode;  // Save reference to final MetaNode.
          // return ScanMetaNode;  // return final scan node.
          } // UpdatePathFromStackMetaNode()
      */
  
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
          // WorkerMetaTool.Sync( InTreePath );  // Run for test.

          return WorkerMetaTool.TheMetaPath.getLastMetaNode();

          /*
          WorkerMetaTool.
              UpdatePathFromStackMetaNode( ); // ...from the update of the structure...
                //PathStack  // ...using the Path stack.
                //);w
          return WorkerMetaTool.TerminalMetaNode;  // Return final MetaNode.
          */
          } // UpdatePathMetaNode()

    public MetaPath getMetaPath( )
      { return TheMetaPath; }

    } // class MetaTool.
