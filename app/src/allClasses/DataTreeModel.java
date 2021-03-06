package allClasses;

import java.util.HashMap;

import javax.swing.event.TreeModelEvent;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import javax.swing.tree.TreeModel;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class DataTreeModel

  extends TreeModelSupport // Include code that all TreeModels need.

  implements TreeModel

  { // class DataTreeModel.

    /* This class implements the TreeModel interface for
      the TUDNet hierarchy tree consisting of DataNodes.
      
      It's purpose is to provide access to the TUDNet hierarchy for
      the Java Swing GUI components, mainly and directly for JTree,
      but indirectly for other components such as JList.

///////////////////////////////////
 *
      In addition to providing the TreeModel methods needed by the JTree class,
      this class also provides:
      
      All of the methods in this class are meant to be run on 
      the Event Dispatch Thread (EDT) unless stated otherwise.

      * ////rem The method getDataJComponent( TreePath inTreePath )
        which returns a JComponent capable of displaying the node.
  
      * This class maintains a 2-way association between 
        a DataNode and the TreePath that can be followed to get to it.
        This relationship is cached in nodeToPathHashMap.

      ///enh A limitation of the interface TreeModel is its inability 
      to deal with the Directed Acyclic Graph (DAG), in which
      a node can have multiple parents.  /////////////////////
      This is  desirable feature of the TUDNet Hierarchy
      so a solution needs to be found.
      * It might be done by changing the present parent DataNode link to
        a list of parent nodes, or maybe create 
        a dual role (node and node-list) object.
         
      * Threading Considerations:
        * Some methods in this class are not thread-safe
          and must execute in the EDT (Event Dispatch Thread).
          These methods manage data displayed by JTree and 
          other non-tree Components, as part of the graphical user interface.
          An example of this is the code which fires TreeModelListeners
          to update the display when data changes.
          Switching to the EDT is done by calling the invokeAndWait(..) method
          and the EDT methods tend to be private ones to limit their access.
        * Some methods in this class are synchronized, 
          which makes them thread-safe.
          These methods tend to be public, allowing any thread to access them.
          They are called often and execute quickly,
          mostly to aggregate change notifications into DataNode.theTreeChange,
  
      ///pos Use ObjectInterning to use less memory and to run faster.
  
      ///pos Repeat what was done with 
        TitledListViewer and TreeListModel to report ConnectionManager changes
        with DirectoryTableViewer and DirectoryTableModel to report
        file-system changes using Java Watchable, WatchService, WatchEvent, 
        and WatchKey.  Unfortunately to watch sub-directories, each
        individual directory must be registered separately.
        
      */


    // Constants.

    private static final String spacyPathSeperator=  
          " "+Config.pathSeperatorC+" ";


    // Injected dependency variables.

    private DataRoot theDataRoot; // Root of data tree.
    
    private MetaRoot theMetaRoot; // Root of meta-data tree.
    
    private MetaFileManager.Finisher theMetaFileManagerFinisher; 
    
    private Shutdowner theShutdowner;


    // Other variables.

    private HashMap<DataNode,TreePath> nodeToPathHashMap= // Node path cache. 
        new HashMap<DataNode,TreePath>(); /* This is needed because 
          TreeModels need node TreePaths.  
          For this to work depends on DataNode's having only one parent node.
          If they ever have more than one parent then a node's
          path would need to be stored elsewhere.
          The logical places are the JComponents displaying the node.
          */


    // constructor and initialization methods.

    public DataTreeModel( // Constructor. 
        DataRoot theDataRoot, 
        MetaRoot theMetaRoot, 
        MetaFileManager.Finisher theMetaFileManagerFinisher,
        Shutdowner theShutdowner
        )
      {
        this.theDataRoot= theDataRoot;
        this.theMetaRoot= theMetaRoot;
        this.theMetaFileManagerFinisher= theMetaFileManagerFinisher;
        this.theShutdowner= theShutdowner;
        }

    public synchronized void initializeV( 
        DataNode theInitialDataNode 
        )
      /* This is code that couldn't [easily] be done at injection time.

        First it initializes injected variables:
        * It initializes the DataRoot.
        * It initializes the MetaRoot by loading it from external file(s),
          or at least, trying to.
        * theMetaFileManagerFinisher is set to be a shutdown listener in 
          theShutdowner for later writing MetaRoot back to disk if changed.

        Then finally nodeToPathHashMap is seeded with 
        an entry for the sentinel parent of the root tree node
        which is used as a base for tree exploration and growth. 
        */
      {
        theDataRoot.initializeDataNodesV( theInitialDataNode, this ); 
        theMetaRoot.initializeV( );
        theShutdowner.addShutdownerListener( theMetaFileManagerFinisher );

        nodeToPathHashMap.put( // Making and adding root path to our cache. 
            theDataRoot.getParentOfRootDataNode( ), 
            theDataRoot.getParentOfRootTreePath( ) 
            ); // From this root path, descendant node paths will be made.
        }

    public synchronized void finalizeV()
      {
        theAppLog.debug("DataTreeModel.finalizeDataNodesV() called.");
        theDataRoot.finalizeDataNodesV(); 
        }


    /* TreeModel interface methods.
     * Many of these methods simply delegate to a DataNode object
     * which performs the operation context-free.
     * This works because this TreeModel presently does no filtering.
     * If and when filtering is done then
     * other processing will be needed here.
     */

    public synchronized Object getRoot() 
      /* Returns tree root.  
        This is not the TUDNet root DataNode.
        It is the parent of the root.
        See DataRoot for more information.
        */
      {
        return theDataRoot.getParentOfRootDataNode();
        }

    public synchronized Object getChild( 
        Object parentObject, int childIndexI ) 
      /* Returns the Object which is the child of parentObject
        whose child index is childIndexI, 
        or null if the desired child does not exist.
        This operation is delegated to parentObject which
        is assumed to satisfy the DataNode interface.

        This method also calculates and stores the full path to the child 
        in the path cache. 
        */
      {
        DataNode childDataNode= // Getting the child from parent.
            ((DataNode)parentObject).getChild( childIndexI ); 

        if // Add child path to path map if desired child exists.
          ( childDataNode != null )
          { // Add to map the TreePath of this child.  
            DataNode parentDataNode= (DataNode)parentObject; // Get parent.
            TreePath parentTreePath= // Try retrieving parent path from map. 
                translatingToTreePath( parentDataNode );
            if ( parentTreePath != null ) // Add child path if path found.
              nodeToPathHashMap.put( // Making and adding child path to map. 
                  childDataNode, 
                  parentTreePath.pathByAddingChild(childDataNode) 
                  );
            }

        return childDataNode; // Returning the child.
        }

    public synchronized boolean isLeaf( Object NodeObject ) 
      /* Returns an indication whether NodeObject is a leaf.
        This operation is delegated to NodeObject which
        is assumed to satisfy the DataNode interface.
        */
      {
        return ((DataNode)NodeObject).isLeaf();
        }

    public synchronized int getChildCount( Object parentObject ) 
      /* Returns the number of children of the parentObject.
        This operation is delegated to parentObject which
        is assumed to satisfy the DataNode interface.
        */
      {
        return ((DataNode)parentObject).getChildCount();
        }

    public synchronized int getIndexOfChild( 
        Object parentObject, Object childObject 
        ) 
      /* Returns the index of childObject in parentObject.
        This operation is delegated to parentObject which
        is assumed to satisfy the DataNode interface.
        */
      {
        return ((DataNode)parentObject).
          getIndexOfChild( childObject ); 
        }

    public synchronized void valueForPathChanged(
        TreePath theTreePath, Object newValueObject 
        )
      /* If this method is called, it simply logs an error and returns,
       * because TUDNet doesn't do any JTree cell editing.
       * If that changes, see DataNode.TreeChange.SUBTREE_CHANGED.
        */
      { 
        theAppLog.error( "DataTreeModel.valueForPathChanged(..) called" );
        } 


    /* Getter methods that are not part of AbstractTreeModel.
     * 
     * ///opt Some of these might be duplicates of ones in DataNode.
     */

    public synchronized MetaRoot getMetaRoot()
      {
        return theMetaRoot;
        }

    @SuppressWarnings("unused") ////
    private synchronized JComponent getDataJComponent( // Unused.
        TreePath inTreePath )
      /* Returns a JComponent which is appropriate for 
        viewing and possibly manipulating
        the current tree node specified by inTreePath.  
        */
      {
        DataNode inDataNode= // extract...
          (DataNode)  // ...user Object...
          inTreePath.getLastPathComponent();  // ...from the TreePath.
        String errorString= null;
        JComponent resultJComponent;
        try { 
          resultJComponent= 
            inDataNode.getDataJComponent( 
              inTreePath, this 
              );
          }
        catch ( IllegalArgumentException e) {
          errorString= "DataTreeModel.getDataJComponent(TreePath): "+e;
          resultJComponent=  // calculate a blank JLabel with error message.
            new TitledTextViewer( inTreePath, this, errorString );
          }
        if (errorString!=null) // Log error if one produced. 
          theAppLog.error(errorString);
        return resultJComponent;
        }

    public synchronized String getNameString( Object theObject )
      /* Returns a String representing the name of theObject,
        or null if theObject is null.
        This operation is delegated to theObject which
        is assumed to satisfy the DataNode interface.
        */
      {
        String resultString;
        if ( theObject != null )
          resultString= ((DataNode)theObject).getNameString();
          else
          resultString= "NULL-DataTreeModel-Object";
        return resultString;
        }

    public synchronized String getLastComponentNameString(TreePath inTreePath)
      /* Returns String representation of the name of 
        the last element of inTreePath.
        */
      {
        DataNode lastDataNode= 
          (DataNode)(inTreePath.getLastPathComponent());
        String TheNameString= 
          lastDataNode.getNameString();
        return TheNameString;
        }

    public synchronized String getAbsolutePathString(TreePath inTreePath)
      /* Returns String representation of TreePath inTreePath.  
        ?? Maybe rewrite to be recursive so that 
        java-optimized += String operation can be used,
        maybe with StringBuilder with an initial capacity based on
        previous value of path String being calculated.
        */
      { // GetAbsolutePathString(.)
        String resultString= "";  // Initializing resultString to empty.
        while (true) {  // While more TreePath to process...
          if // Handling detection of illegal null TreePath terminator.
            ( inTreePath == null )
            { resultString+= "NULL-PATH-TERMINATOR";
              break;
              }
          DataNode lastDataNode=  // Getting last path element.
            (DataNode)(inTreePath.getLastPathComponent());
          if // Handling detection of normal TreePath root sentinel.
            ( theDataRoot.getParentOfRootTreePath( ).equals( inTreePath ) )
            { if // Handling illegal sentinel-only TreePath.
                ( resultString == "" ) 
                resultString= lastDataNode.getNameString();
              break;
              }
          String headString=  // Making head string be path element name.
            lastDataNode.getNameString();
          if  // Appending separator to headString if needed,...
            ( resultString.length() != 0)  // ...if result String is not empty...
            headString+=  // ...by appending to the head String...
              spacyPathSeperator; // ...the path separator string.
          resultString=  // Prepending...
            headString +  // ...the head String...
            resultString;  // ...to the resultString.
          inTreePath=  // Replacing the TreePath...
            inTreePath.getParentPath();  // ...by its parent TreePath.                
          }
        resultString=  // Prepend...
          "  " +  // ...some space for looks..
          resultString;  // ...to the resultString.
        return resultString;  // Return completed resultString.
        } // GetAbsolutePathString(.)

    public synchronized String getAttributesString(TreePath inTreePath)
      /* Returns a String representing attributes about the last element of 
        TreePath inTreePath. 
        */
      {
        DataNode lastDataNode= (DataNode)(inTreePath.getLastPathComponent());

        return lastDataNode.getMetaDataString();
        }


    // Path and path cache manipulation methods.

      public synchronized boolean cachePathInMapB( TreePath theTreePath )
        /* This method caches an entire path theTreePath.
          It was created to prevent a cache miss and a long node search
          if the selection at startup is a very long MetaPath path.
          Using this method to seed the cache with 
          the initial selection path prevents this.
          Returns true if theTreePath was cached or was already in the cache.
          Returns false if theTreePath was not already in the cache
          and could not be added because the first DataNode was not the root.
            
          ///opt This might not be needed now that 
            searches from root are no longer done.
          */
        {
          boolean inCacheB;
          process: {
            if ( theTreePath == null ) // Invalid TreePath.
              { inCacheB= false; break process; } // Fail.
            DataNode theDataNode= // Getting last element node of path.
                (DataNode)theTreePath.getLastPathComponent();
            TreePath targetTreePath= // Testing whether that node is in cache.
                nodeToPathHashMap.get( theDataNode );
            if ( targetTreePath != null ) // Already cached.
              { inCacheB= true; break process; } // Exit with success.  
            TreePath theParentTreePath= theTreePath.getParentPath();
            inCacheB= cachePathInMapB(  // Try caching parent.
                theParentTreePath ); // This is a recursive call.
            if ( ! inCacheB ) // Parent path is not cached.
              break process; // So exit with fail, inCachedB==false for child.  
            nodeToPathHashMap.put(  // Caching child path.
                theDataNode, theTreePath );
            // Exit with success, inCachedB==true.
            } // process:
          return inCacheB;
          }

      @SuppressWarnings("unused") // It does entire subtree!  Useless.
      private synchronized boolean cacheDescendantsInMapB( //// unused, useless.
          TreePath theTreePath )
        /* This method caches the TreePaths of
          all descendants of the node named by theTreePath.
          theTreePath should already be cached.
          Returns true if any descendants were cached.
          Returns false if no descendants were cached.
          */
        {
          DataNode theDataNode= // Getting last element node of path.
              (DataNode)theTreePath.getLastPathComponent();
          boolean somethingWasCacheB= false; // Assume nothing will be cached.
          int childIndexI= 0;  // Initialize child scan index.
          while ( true ) // Process all children.
            { // Process one child.
              DataNode childDataNode=  // Get the child.
                 (DataNode)getChild( theDataNode, childIndexI );
              if ( childDataNode == null )  // Null means no more children.
                  break;  // so exit while loop.
              TreePath childTreePath= // Testing whether child is in cache.
                  nodeToPathHashMap.get( childDataNode );
              if ( childTreePath == null ) // Cache if not cached.
                { nodeToPathHashMap.put(  // Caching child and its path.
                      childDataNode, childTreePath );
                  cacheDescendantsInMapB( // Caching its descendants. 
                      childTreePath );  // This is recursive call.
                  somethingWasCacheB= true;
                  }
              childIndexI++;  // Increment index for processing next child.
              }
        return somethingWasCacheB;
        }

      private synchronized TreePath translatingToTreePath(
          DataNode targetDataNode )
        /* This method returns the TreePath of targetDataNode.
          It returns the resultant TreePath or null if 
          it was unable to find it in the nodeToPathHashMap or build it.
          Null means the node or one of its ancestors
          was not a descendant of the root, 
          which was added to the map during initialization. 
          
          This method tries to return a value from nodeToPathHashMap first.
          If that fails then tries to build the TreePath recursively
          from the targetDataNode and the translated TreePath of its parent.
          This is possible only because nodes presently have only one parent.
          Another method would be needed if the node tree is replaced by a DAG.
          
          Any TreePaths that are built are saved in the nodeToPathHashMap.
          */
        { 
          TreePath targetTreePath= // Looking in HashMap first.
              nodeToPathHashMap.get( targetDataNode );
          if ( null == targetTreePath ) // If path not in map
            { // build the path from its parent's
              DataNode parentDataNode= targetDataNode.getTreeParentNamedList();
              TreePath parentTreePath= translatingToTreePath(parentDataNode);
              if ( null != parentTreePath ) // If got parent path then
                { // build target path by adding to parent's and add to map.
                  targetTreePath=
                      parentTreePath.pathByAddingChild( targetDataNode );
                  nodeToPathHashMap.put( targetDataNode, targetTreePath );
                  }
              theAppLog.warning( 
                  "DataTreeModel.translatingToTreePath( "
                  + targetDataNode 
                  + " ), cache miss, resolves to" + NL + "  "
                  + targetTreePath
                  ); // Indicates an incorrect child node addition.
              }
          return targetTreePath;   
          }


    /* Hierarchical data aggregation and GUI display.
      
      The following 2 sections comprise the display aggregation system.
      The purpose is to reduce the number of TreeModel events fired
      and the number of times the Event Dispatch Thread (EDT) is activated.
      
      Alternatively, this could be viewed as a hybrid data cache system,
      with the change of a node invalidating both
      the rendering of that node and all its ancestors.
      
      The way this works is by storing in the DataNodes information about
      whether any changes have occurred since the previous display,
      and propagating this information toward the root of the tree.
      When it is time to display, the changed nodes are traversed
      in a depth-first order, and every node that was changed
      is communicated to the appropriate TreeModel listeners.
      As the display is updated, the change information is cleared.

      Displaying is done by calling a single method when display is desired.
      Normally this would be called periodically and immediately after 
      any important changes which should be displayed immediately.

      Thread safety is attained by 2 redundant systems:
      * Public methods which signal changes and 
        the method which displays changes are synchronized.
      . The order in which changes are recorded and later those records cleared
        for nodes and their children is controlled so that 
        no change goes undisplayed, though sometimes a node might be display
        that doesn't need displaying.
        * theTreeChange of a node is set after those of its children.
        * theTreeChange of a node is cleared before those of its children.

      ///opt: Limit listener notification and change record clearing to
      only nodes that are displayed in any part of the displayed GUI.
      This means the node is displayed in either the JTree or JList panes.
      Unfortunately this is difficult because it involves 
      doing ScrollPane Rectangle intersections.
      */
      

      /* EDT reporting methods and their support methods for 
        reporting changes of the TreeModel data.
        One of these methods is called each time the DataNode tree changes.
  
        Reporting changes is tricky for 2 reasons:
        * The TreeModel is used to report changes, 
          but the TreeModel itself doesn't make changes.
        * It must be done on the Event Dispatch Thread (EDT),
          and must be done in real time.  
  
        All of these methods should be called only in the Event Dispatch Thread 
        (EDT), and so should the changes that those calls report,
        or this method should SWITCH to the EDT before firing
        their TreeModelEvent.
        */

        @SuppressWarnings("unused") ///opt Might need later.
        private synchronized void reportInsertV(
            DataNode parentDataNode, 
            int indexI, 
            DataNode childDataNode 
            )
          /* This method creates and fires a single-child TreeModelEvent
            for the insertion of a single child DataNode, childDataNode,
            into theSubjectDataNode at position indexI.
            It also adds the path of the new child to the map for use later.
            */
          {
            theAppLog.error( "THIS IS SUPPOSED TO BE UNUSED CODE!" ); 
            TreePath parentTreePath= // Calculating path of parent. 
              translatingToTreePath( parentDataNode ); // Should be in map.
            if ( parentTreePath != null ) // Do these things only if path found.
              {
                TreeModelEvent theTreeModelEvent= // Construct TreeModelEvent.
                  new TreeModelEvent(
                    this, 
                    parentTreePath, 
                    new int[] {indexI}, 
                    new Object[] {childDataNode}
                    );
                fireTreeNodesInserted( theTreeModelEvent ); // Fire insertion.
                TreePath childTreePath= 
                    parentTreePath.pathByAddingChild(childDataNode);
                nodeToPathHashMap.put( // Making and adding child to cache. 
                    childDataNode, childTreePath );
                //// cacheDescendantsInMapB( childTreePath );
                }
            }

        @SuppressWarnings("unused") ///opt Might need later.
        private synchronized void reportRemoveV( 
            DataNode parentDataNode, 
            int indexI, 
            DataNode childDataNode 
            )
          /* This method creates and fires a single-child TreeModelEvent
            for the removal of a single child DataNode, childDataNode,
            whose previous position was indexI, into theSubjectDataNode.
            */
          {
            theAppLog.error( "THIS IS SUPPOSED TO BE UNUSED CODE!" ); 
            TreePath parentTreePath= // Calculating path of parent. 
              translatingToTreePath( parentDataNode );
            if ( parentTreePath != null ) // Do this only if path found.
              {
                TreeModelEvent theTreeModelEvent= // Constructing TreeModelEvent.
                  new TreeModelEvent(
                    this, 
                    parentTreePath, 
                    new int[] {indexI}, 
                    new Object[] {childDataNode}
                    );
                fireTreeNodesRemoved( theTreeModelEvent ); // Firing removal.

                theAppLog.debug( 
                    "DataTreeModel.reportingRemoveV(..) uncaching "
                    + childDataNode 
                    );
                nodeToPathHashMap.remove( // Removing entry for removed child from map. 
                    childDataNode // This won't remove descendants of child??
                      // Hopefully descendants were already removed.
                      ///fix Recursively remove active children to prevent leak.
                    );
                }
            }

        public synchronized boolean reportChangeB(
            TreePath parentTreePath, DataNode theDataNode 
            )
          /* This method creates and fires a single-child TreeModelEvent
            for the change of a single child DataNode, theDataNode.
            parentTreePath is assumed to be the TreePath
            of the parent of theDataNode.
            This method must be running on the EDT.
            It returns false if all this succeeds.
            It returns true if there was a failure, such as caused by
              the current thread not being the EDT or parentTreePath == null.
             */
          {
            boolean resultB= true; // Assume result for failure.
            toReturn: {
              if ( EDTUtilities.testAndLogIfNotRunningEDTB() ) break toReturn;
              if ( Nulls.testAndLogIfNullB(parentTreePath) ) break toReturn;
              DataNode parentDataNode= // Calculating parent DataNode.
                  (DataNode)parentTreePath.getLastPathComponent();
              int indexI= getIndexOfChild( // Calculating index of the DataNode.
                  parentDataNode, 
                  theDataNode 
                  ); 
              TreeModelEvent theTreeModelEvent= // Constructing TreeModelEvent.
                new TreeModelEvent(
                  this, 
                  parentTreePath, 
                  new int[] {indexI}, 
                  new Object[] {theDataNode}
                  );
              try {
                  fireTreeNodesChanged( // Firing as change event.
                      theTreeModelEvent);
                } catch ( Exception theException  ) {
                  theAppLog.debug( "DataTreeModel.reportingChangeB((..) to "
                    + NL + "  theDataNode=" + theDataNode + " with indexI=" + indexI 
                    + NL + "  in theSubjectDataNode=" + parentDataNode 
                    + ", ignoring Exception " + theException );
                  break toReturn;
                }
              resultB= false; // Indicate success if we got this far.
              } // toReturn:
            return resultB;
            }

        public synchronized boolean reportStructuralChangeB( 
            TreePath parentTreePath )
          /* This method creates and fires a TreeModelEvent
            for the structural change of a subtree identified by parentTreePath.
            This method must be running on the EDT.
            It returns false if all this succeeds.
            It returns true if there was a failure, such as caused by
              the current thread not being the EDT or parentTreePath == null.
             */
          {
            boolean resultB= true; // Assume result for failure.
            toReturn: {
              if ( EDTUtilities.testAndLogIfNotRunningEDTB() ) break toReturn;
              if ( Nulls.testAndLogIfNullB(parentTreePath) ) break toReturn;
              TreeModelEvent theTreeModelEvent= // Constructing TreeModelEvent.
                new TreeModelEvent(
                  this, 
                  parentTreePath, 
                  new int[] {}, // Child indexes are ignored.
                  new Object[] {} // Child references are ignored.
                  );
              fireTreeStructureChanged( theTreeModelEvent );
              resultB= false; // Override result for achieved success.
              } // goReturn:
            return resultB;
            }





      /*  ///opt  Maybe save this somewhere in case I need a DepthFirstSearch.
      private TreePath searchingForTreePath( DataNode targetDataNode )
        /* This method is not needed anymore, because:
          * It might never be called anymore because
            its only caller, translatingToTreePath(..),
            now checks nodeToPathHashMap first.  So this method is a backup.
            The one exception is startup when initial selection path
            is longer than maximum depth.  It will cause error.
            Seeding cache with selection path might fix this.
          * When DataNodes store references to their parent nodes,
            searching will no longer be necessary.

          This method returns a TreePath of targetDataNode,
          or null if node can not be found in the DataNode tree.
          It does this with a breadth-first search of 
          the DataNode tree from the root,
          building candidate TreePaths as it goes,
          and returning the first TreePath that ends in targetDataNode.

          The search algorithm used is slightly different from
          the breadth-first-search commonly shown in the literature.
          This search queues only nodes that have already been checked.
          The only thing that happens to nodes after they are removed
          is that they are expanded.

          ?? If this was actually being called, 
          it could be speeded with some combination of the following:

          * By caching the search result TreePath and checking the cache first.
            This can be enhanced later to handle duplicate references.
            This is actually done in its only caller.

          * By using a starting TreePath different from the root and
            known to be closer to the node that changed. 
            Unfortunately a general way of picking this path is difficult.

          * By using a closest-first search instead of breadth-first search.
            This means searching toward the tree root in addition to
            breadth-first searching into the descendants.
            It implies a starting path which is not the root,
            which could come either from a cache or the last path returned.
            To prevent checking the same nodes, a check for the child node 
            should be skipped when expanding its parent.  

          This method is a bit of a kludge.
          It is used because DataNodes don't know their TreePaths,
          and TreePaths are required by JTree.
          */
      /*
        {
          appLogger.warning( "DataTreeModel.searchingForTreePath(..) called.");
          TreePath resultTreePath= null;  // Defaulting result to null.
          Queue<TreePath> queueOfTreePath = new LinkedList<TreePath>();
          queueOfTreePath.add(theDataRoot.getParentOfRootTreePath( ));
            // Placing root into the initially empty queue.
          int currentDepthI = 0;
          int elementsToDepthIncreaseI = 1; // Init. queued nodes at this depth.
          int nextElementsToDepthIncreaseI = 0;  // Same for next depth.
          queueScanner: while (true) // Searching queue of parent TreePaths.
            { // Searching one parent TreePath.
              TreePath parentTreePath = queueOfTreePath.poll(); 
              if ( parentTreePath == null) break queueScanner;
                // Exiting if queue empty.
              DataNode theSubjectDataNode= 
                (DataNode)parentTreePath.getLastPathComponent(); 
              for // Checking children.
                ( int childIndexI=0; ; childIndexI++ ) 
                { 
                  DataNode childDataNode= 
                    theSubjectDataNode.getChild(childIndexI);
                  if ( childDataNode == null) break;
                    // Exiting checking-children loop if no more children.
                  TreePath childTreePath=
                    parentTreePath.pathByAddingChild( childDataNode );
                  if // Returning result TreePath if target node found.
                    ( childDataNode == targetDataNode )
                    { resultTreePath= childTreePath; break queueScanner; }
                   queueOfTreePath.add( childTreePath );
                   nextElementsToDepthIncreaseI++;
                   }
              if (--elementsToDepthIncreaseI == 0) // 
                { // Handle tree depth increase.
                  if (++currentDepthI > 5) 
                    { appLogger.error( "DataTreeModel.translatingToTreePath():"+
                        NL + "  depth "+currentDepthI+" exceeds maximum, node="+
                        targetDataNode.getLineSummaryString( )
                        );
                      break queueScanner;
                      }
                  elementsToDepthIncreaseI = nextElementsToDepthIncreaseI;
                  nextElementsToDepthIncreaseI = 0;
                  }
              }
          return resultTreePath;
          }
      */

    } // class DataTreeModel.
