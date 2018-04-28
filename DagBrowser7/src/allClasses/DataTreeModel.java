package allClasses;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
////import java.util.LinkedList;
////import java.util.Queue;

import javax.swing.event.TreeModelEvent;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import allClasses.DataNode.UpdateLevel;

import javax.swing.tree.TreeModel;

import static allClasses.AppLog.LogLevel.*;
import static allClasses.Globals.*;  // appLogger;

public class DataTreeModel

  extends AbstractTreeModel

  implements TreeModel, Serializable

  /* This class implements an extended TreeModel 
    used for browsing the Infogora hierarchy tree (eventually DAG).
    It's purpose is to provide link Infogora hierarchy to
    Java GUI components, especially the Java JTree.
    Because of this, most of the code, including extensions,
    is meant to be run on the Event Dispatch Thread (EDT).
    This should be true of all methods unless stated otherwise.
     
    This class implements, or will implement, the following extensions 
    beyond the basic TreeModel capabilities needed by the JTree class.
	
	  * The method getDataJComponent( TreePath inTreePath )
	    which returns a JComponent capable of displaying the node.

	  * This class maintains a 2-way association between 
	    a DataNode and the TreePath (or TreePaths in the case of DAGs)
	    that can be followed to get to them.
	    This relationship is cached in nodeToPathHashMap.

    ///enh One understandable drawback of the superclass TreeModel is
    its inability to deal with the general Directed Graph,
    or even the Directed Acyclic Graph (DAG), in which
    a node can have multiple parents.
    This is  desirable feature of the Infogora Hierarchy
    so a solution needs to be found.
    * It will probably be done with a HashMap.
    * It might be done by changing the present node parent link to
      a list of parent nodes, or maybe create 
      a dual role (node and node-list) object.
			 
    * Threading Restrictions:
      Because this class will be used with JTree as part of a user interface,
    	there are some threading restrictions:
	    * Changes to the data managed by this model, as well as
	    	notifications about changes to that data, 
	    	the ones which fire TreeModelListeners,
	      must happen in the EDT (Event Dispatch Thread).
	      This is what is done now, using the invokeAndWaitV(..) method.
	    * ///enh Unfortunately using the EDT each time 
	      makes data changes inefficient.
	      Using synchronization combined with HierarchicalUpPropagation
	      might be sufficient to make change notification
	      both efficient and thread-safe.
	      This change is underway.

    ///pos Use ObjectInterning to use less memory and to run faster.

    ///pos Repeat what was done with 
    	TitledListViewer and TreeListModel to report ConnectionManager changes
    	with DirectoryTableViewer and DirectoryTableModel to report
	    file-system changes using Java Watchable, WatchService, WatchEvent, 
	    and WatchKey.  Unfortunately to watch sub-directories, each
	    individual directory must be registered separately.
	    
    */

  { // class DataTreeModel.

    // Constants.

        private static final String spacyFileSeperator= 
          " "+File.separator+" ";

    // Injected dependency variables.

        private DataRoot theDataRoot; // Root of data tree.
        
        private MetaRoot theMetaRoot; // Root of meta-data tree.
        
        private MetaFileManager.Finisher theMetaFileManagerFinisher; 
        
        private Shutdowner theShutdowner;

    // Other variables.

      private HashMap<DataNode,TreePath> nodeToPathHashMap= // For cache because 
      		new HashMap<DataNode,TreePath>(); // TreeModels need node TreePaths. 

    // constructor and initialization methods.

        public DataTreeModel( 
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

        public void initializeV( 
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
            theDataRoot.initializeV( theInitialDataNode, this ); 
            theMetaRoot.initializeV( );
            theShutdowner.addShutdownerListener( theMetaFileManagerFinisher );

            nodeToPathHashMap.put( // Making and adding root path to our cache. 
        	  		theDataRoot.getParentOfRootDataNode( ), 
        	  		theDataRoot.getParentOfRootTreePath( ) 
  	      			); // From this root path, descendant node paths will be made.
            }

    // AbstractTreeModel/TreeModel interface methods.

      /* The following getter methods simply delegate to the parentObject, 
        which is assumed to be a DataNode,
        and can perform the operation context-free.
        This is because this TreeModel does no filtering.
        If and when filtering is done then 
        other processing will be needed here.
        */

      public Object getRoot() 
        /* Returns tree root.  
          This is not the Infogora root DataNode.
          It is the parent of the root.
          See DataRoot for more information.
          */
        {
          return theDataRoot.getParentOfRootDataNode();
          }

      public Object getChild( Object parentObject, int childIndexI ) 
        /* Returns the Object which is the child of parentObject
          whose child index is childIndexI.  The child must exist.
          This operation is delegated to parentObject which
          is assumed to satisfy the DataNode interface.
          */
        {
      	  DataNode childDataNode= // Getting the child from parent.
      	  		((DataNode)parentObject).getChild( childIndexI ); 

      	  if ( childDataNode != null )
	      	  { // Calculating and adding to map the TreePath of child for later.  
		      	  DataNode parentDataNode= (DataNode)parentObject;
		          TreePath parentTreePath= // Try retrieving path of parent from map. 
		            	translatingToTreePath( parentDataNode );
		          if ( parentTreePath != null ) // Add child path if path found.
			      	  nodeToPathHashMap.put( // Making and adding child path to map. 
				      			childDataNode, 
				      			parentTreePath.pathByAddingChild(childDataNode) 
				      			);
	      	  	}

      	  return childDataNode; // Returning the child.
          }

      public boolean isLeaf( Object NodeObject ) 
        /* Returns an indication whether NodeObject is a leaf.
          This operation is delegated to NodeObject which
          is assumed to satisfy the DataNode interface.
          */
        {
          return ((DataNode)NodeObject).isLeaf();
          }

      public int getChildCount( Object parentObject ) 
        /* Returns the number of children of the parentObject.
          This operation is delegated to parentObject which
          is assumed to satisfy the DataNode interface.
          */
        {
          return ((DataNode)parentObject).getChildCount();
          }

      public int getIndexOfChild( 
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

      public void valueForPathChanged( 
          TreePath theTreePath, Object newValueObject 
          )
        /* Unimplemented because Infogora doesn't edit the DAG/tree, yet
          Simply logs an error and returns.
          //// and throws an Error.  
          */
        { 
      	  String messageString= "DataTreeModel.valueForPathChanged(..) called";
	      	appLogger.error( messageString );
      	  ///elim   throw new Error(messageString); 
      	  } 
      
    // Getter methods which are not part of AbstractTreeModel.

      public MetaRoot getMetaRoot()
        {
          return theMetaRoot;
          }

      public JComponent getDataJComponent( TreePath inTreePath )
        /* Returns a JComponent which is appropriate for 
          viewing and possibly manipulating
          the current tree node specified by inTreePath.  
          */
        {
          DataNode InDataNode= // extract...
            (DataNode)  // ...user Object...
            inTreePath.getLastPathComponent();  // ...from the TreePath.
          JComponent ResultJComponent; 
          try { 
            ResultJComponent= 
              InDataNode.getDataJComponent( 
                inTreePath, this 
                );
            }
          catch ( IllegalArgumentException e) {
            ResultJComponent= null;  
            ResultJComponent=  // calculate a blank JLabel with message.
              new TitledTextViewer( inTreePath, this, "getDataJComponent : "+e );
            }
          return ResultJComponent;
          }

      public String getNameString( Object theObject )
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

      public String getLastComponentNameString(TreePath inTreePath)
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

      public String getAbsolutePathString(TreePath inTreePath)
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
                spacyFileSeperator; // ...the File separator string.
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

      public String getInfoString(TreePath inTreePath)
        /* Returns a String representing information about 
          TreePath inTreePath. 
          */
        {
          DataNode lastDataNode= 
            (DataNode)(inTreePath.getLastPathComponent());
          return lastDataNode.getInfoString();
          }


    /* Reporting methods and their support methods for 
      reporting changes of the TreeModel data.
        
      Reporting changes is tricky for 2 reasons:
      * The TreeModel is used to report changes, 
        but the TreeModel itself doesn't make changes.
      * It must be done on the Event Dispatch Thread (EDT),
        and must be done in real time.  
        This is done using invokeAndWaitV(..).

      All of these methods should be called only in the Event Dispatch Thread 
      (EDT), and so should the changes that those calls report.
      */

      private void reportingInsertV(
          DataNode parentDataNode, 
          int indexI, 
          DataNode childDataNode 
          )
        /* This method creates and fires a single-child TreeModelEvent
          for the insertion of a single child DataNode, childDataNode,
          into parentDataNode at position indexI.
          It also adds the path of the new child to the map for user later.
          */
        {
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
		        	cacheDescendantsInMapB( childTreePath );
	          	}
          }

      public void reportingRemoveV( 
          DataNode parentDataNode, 
          int indexI, 
          DataNode childDataNode 
          )
      	/* This method creates and fires a single-child TreeModelEvent
          for the removal of a single child DataNode, childDataNode,
          whose previous position was indexI, into parentDataNode.
          */
        {
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

		          appLogger.debug( 
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

      private void safelyReportingChangeV( final DataNode theDataNode ) ///elim
        /* This is a thread-safe version of reportingChangeV( theDataNode ).
          It uses EDTUtilities.runOrInvokeAndWaitV(..) to switch to
          the EDT (Event Dispatch Thread) if that thread is not already active,
          then it calls reportingChangeV( theDataNode ).
          ///dbg ///enh Add AtomicBoolean and pass back error results.
          */
	      {
      		EDTUtilities.runOrInvokeAndWaitV( // Do following on EDT thread. 
		    		new Runnable() {
		    			@Override  
		          public void run() {
		    				reportingChangeV( theDataNode );
		            }
		          } 
		        );
		      }

      public void reportingChangeV( DataNode theDataNode )
      	/* This method creates and fires a single-child TreeModelEvent
          for the change of a single child DataNode, theDataNode.
		      This method must be running on the EDT.
          */
        {
	      	TreePath theTreePath= // Calculating path of the DataNode. 
	          	translatingToTreePath( theDataNode );
	        if ( theTreePath == null ) {
	        	appLogger.error( 
	        	  "DataTreeModel.reportingChangeV(..): failure in\n"+
	        	    "  translatingToTreePath(..) "+theDataNode 
	        	  );
	        	return;
	        	}
	        TreePath parentTreePath= // Calculating path of the parent DataNode. 
	            theTreePath.getParentPath();
	        reportingChangeB( parentTreePath, theDataNode );
        	}

      public boolean reportingChangeB( 
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
	      	if ( EDTUtilities.testAndLogIfNotRunningEDTB() ) return true; ///tmp
	      	if ( Nulls.testAndLogIfNullB(parentTreePath) ) return true; ///tmp
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

          fireTreeNodesChanged( theTreeModelEvent );  // Firing as change event.
          return false;
          }

      public boolean reportingStructuralChangeB( TreePath parentTreePath )
	    	/* This method creates and fires a TreeModelEvent
		      for the structural change of a subtree identified by parentTreePath.
		      This method must be running on the EDT.
		      It returns false if all this succeeds.
		      It returns true if there was a failure, such as caused by
	      	  the current thread not being the EDT or parentTreePath == null.
   	      */
        {
	      	if ( EDTUtilities.testAndLogIfNotRunningEDTB() ) return true; ///tmp
	      	if ( Nulls.testAndLogIfNullB(parentTreePath) ) return true; ///tmp
          
          TreeModelEvent theTreeModelEvent= // Constructing TreeModelEvent.
            new TreeModelEvent(
            	this, 
            	parentTreePath, 
            	new int[] {}, // Child indexes are ignored.
            	new Object[] {} // Child references are ignored.
            	);

  				appLogger.debug( "DataTreeModel.reportingStructuralChangeB() "
  						+ ((DataNode)parentTreePath.getLastPathComponent()).
  							getNodePathString() );
          fireTreeStructureChanged( theTreeModelEvent );
          return false;
          }

    // Path and path cache manipulation methods.

      public boolean cachePathInMapB( TreePath theTreePath )
        /* This method caches an entire path theTreePath.
          It was created to prevent a cache miss and a long node search
          if the selection at startup is a very long MetaPath path.
          Using this method to seed the cache with 
          the initial selection path prevents this.
          Returns true if theTreePath was cached or was already in the cache.
          Returns false if theTreePath was not already in the cache
            and could not be added because the first DataNode was not the root.
            
          ///elim This might not be needed now that 
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

      public boolean cacheDescendantsInMapB( TreePath theTreePath ) ///tmp
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

      public TreePath translatingToTreePath( DataNode targetDataNode )
	      /* This method returns the TreePath of targetDataNode,
  	      or null if node can not be found in the DataNode tree.
  	      It tries to return a value from nodeToPathHashMap first.
  	      If that fails then tries to build the TreePath recursively
  	      from the targetDataNode and the translated TreePath of its parent.
  	      TreePaths that are built are cached in the nodeToPathHashMap.
  	      It returns the resultant TreePath or null if 
  	      it was unable to find or build the TreePath.
  	      */
      	{ 
      	  TreePath targetTreePath= // Looking in cache first.
      	  		nodeToPathHashMap.get( targetDataNode );
	        if ( targetTreePath == null ) // Building the path if not in cache.
		        {
      	  		DataNode parentDataNode= targetDataNode.parentNamedList;
          	  TreePath parentTreePath= translatingToTreePath(parentDataNode);
			        if ( parentTreePath != null ) // Caching build result, if any.
				        {
			      	  	targetTreePath= 
			      	  			parentTreePath.pathByAddingChild( targetDataNode );
				        	nodeToPathHashMap.put( targetDataNode, targetTreePath );
				        	}
	        		appLogger.warning( 
	        				"DataTreeModel.translatingToTreePath( "
	        				+ targetDataNode 
	        				+ " ), cache miss, resolves to\n  "
	        				+ targetTreePath
	        				); // Indicates an incorrect child node addition.
			        }
      	  return targetTreePath;   
      	  }

      public TreePath OLDtranslatingToTreePath( DataNode targetDataNode )
	      /* This method returns a TreePath of targetDataNode,
  	      or null if node can not be found in the DataNode tree.
  	      It tries to return a value from nodeToPathHashMap first.
  	      If that fails then tries to build the TreePath 
  	        and then caches the result.
  	      It returns the resultant TreePath or null if 
  	      it was unable to find or build the TreePath.
  	      */
      	{ 
      	  TreePath targetTreePath= // Looking in cache first.
      	  		nodeToPathHashMap.get( targetDataNode );
	        if ( targetTreePath == null ) // Building the path if not in cache.
		        {
			        targetTreePath= // Build path manually by tracing ancestors. 
			        		buildTreePath( targetDataNode );
			        if ( targetTreePath != null ) // Caching build result, if any.
			        	nodeToPathHashMap.put( targetDataNode, targetTreePath );
	        		appLogger.warning( 
	        				"DataTreeModel.translatingToTreePath( "
	        				+ targetDataNode 
	        				+ " ), cache miss, resolves to\n  "
	        				+ targetTreePath
	        				); // Indicates an incorrect child node addition.
			        }
      	  return targetTreePath;   
      	  }

      private TreePath buildTreePath( DataNode theDataNode )
        /* This method returns the TreePath of theDataNode, if it exists.
          To do this it follows the trail of DataNode.parentNamedList links 
          back to the root, then builds a TreePath in reverse from that.
          It is unable to reach the root of the tree then it returns null.

          ///enh? This could be made more efficient by recursing using
          translatingToTreePath(..) on the parent node, a simple expression.
          ///elim This separate method might not be needed at all.
            See its only caller, translatingToTreePath(..)
          */
        {
      		appLogger.logB(TRACE, "DataTreeModel.buildTreePath(..) called.");
      	  Deque<DataNode> stackDeque= new ArrayDeque<DataNode>(10); 
      	  while (true) { // Stack all nodes in path to root.
      	  	if ( theDataNode == null ) break;
      	  	stackDeque.addFirst( theDataNode );
      	  	theDataNode= theDataNode.parentNamedList;
      	  	}
      		TreePath theTreePath;
      	  boolean unrootedBranchB; 
    	  	DataNode firstDataNode;
      	  { // Build TreePath from stacked nodes.
      	  	firstDataNode= stackDeque.removeFirst();
	      	  unrootedBranchB= // Rooted branch error check. 
	      	  		(firstDataNode != theDataRoot.getParentOfRootDataNode( ) );
    	  	  theTreePath= new TreePath(firstDataNode);
	      	  while (true) {
	      	  	if ( stackDeque.peekFirst() == null ) break;
	      	  	theTreePath= 
	      	  			theTreePath.pathByAddingChild( stackDeque.removeFirst());
	      	  	}
      	  	}
  	  	  if ( unrootedBranchB ) // Handle possible branch rooting error.
	  	  	  { appLogger.warning( 
	        				"DataTreeModel.buildTreePath(..) unrooted branch\n"
	        				+ theTreePath
	        				);
	      	  	theTreePath= null; // Set result to null to signal error.
	  	  	  	}
          return theTreePath;
          }


    /* The following code is used to aggregate changes to the
      Infogora hierarchy for later and more efficient display.
      The displaying is done with the firing of notification events 
      to GUI components such as JTrees. 
      These methods do not need to be called on the Event Dispatch Thread (EDT).
      
      This is not a general-purpose up-propagation system.
      It is highly customized for Java, its JTree class, and its GUI.
      */

      public void signalChangeV( DataNode theDataNode )
	      /* This method signals the change of a single DataNode, 
		      theDataNode.
		      */
        {
	    	  signalSubtreeChangeV( // Convert to a subtree change. 
	    	  		theDataNode );
        	}

      public void signalInsertionV( 
          DataNode parentDataNode, 
          int indexI, 
          DataNode childDataNode 
          )
        /* This method signals the insertion of a single child DataNode, 
          childDataNode, into parentDataNode at child position indexI.
		      ///opt Implement without StructuralChange.
          */
        {
      	  signalStructureChangeInV( parentDataNode, indexI, childDataNode );
          }

      public void signalRemovalV( 
          DataNode parentDataNode, 
          int indexI, 
          DataNode childDataNode 
          )
	      /* This method signals the removal of a single child DataNode, 
		      childDataNode, from parentDataNode at child position indexI.
		      ///opt Implement without StructuralChange.
		      */
		    {
      	  signalStructureChangeInV( parentDataNode, indexI, childDataNode );
		      }

      private void signalStructureChangeInV(
          DataNode parentDataNode, 
          int indexI, 
          DataNode childDataNode 
		      )
        /* This method is used to signal a parentDataNode about 
          the insertion or deletion of an unnamed child node.
          */
        {
						appLogger.debug( "DataTreeModel.signalStructureChangeInV() "
							+ childDataNode.getNodePathString() + " at position " + indexI );
						
						switch ( parentDataNode.theUpdateLevel ) {
			  		case NONE:
			  		case SUBTREE_CHANGED:
		      	  parentDataNode.theUpdateLevel= // Mark structure changed. 
		      	  	DataNode.UpdateLevel.STRUCTURE_CHANGED;
		      	  signalSubtreeChangeV( // Propagate as ordinary change to parent. 
		      	  		parentDataNode.parentNamedList  );
			  			break;
			  			
			  		case STRUCTURE_CHANGED:
			  	  	; // Desired status is already set, so do nothing.
			  	  	break;
			  		}
          }

      private void signalSubtreeChangeV( 
		      DataNode theDataNode
		      )
        /* This method signals that a change happened
          in this node or one of its subtrees.
          If it needs to change the update status of theDataNode then
          it will also propagate the change to the node's parent if needed.
          */
        {
      	  if ( theDataNode == null ) // No node to update. 
      	  	; // Do nothing.
      	  else // Update this node and its not updated ancestors.
		  	  	switch ( theDataNode.theUpdateLevel ) {
				  		case NONE:
			      	  theDataNode.theUpdateLevel= // Mark node changed. 
			      	  	DataNode.UpdateLevel.SUBTREE_CHANGED;
			      	  signalSubtreeChangeV( // Propagate to parent, if any. 
			      	  		theDataNode.parentNamedList );
				  			break;
				  			
		  	  		case STRUCTURE_CHANGED:
				  		case SUBTREE_CHANGED:
				  	  	; // Anything else means no new changes are needed.
				  	  	break;
				  		}
          }

      public void displayTreeModelChangesV()
        /* This method switches to the EDT 
          and calls displayUpdatedNodesFromRootV(). 
          */
        {
      		appLogger.trace( "DataTreeModel.displayTreeModelChangesV()" );
	    		EDTUtilities.runOrInvokeAndWaitV( // Do following on EDT thread. 
		    		new Runnable() {
		    			@Override  
		          public void run() {
		    				displayUpdatedNodesFromRootV();
		            }
		          } 
		        );
        	}

      private void displayUpdatedNodesFromRootV()
        /* This method displays any nodes 
          that need displaying starting with 
          the root of the Infogora hierarchy.
          */
        {
      		appLogger.trace( "DataTreeModel.displayUpdatedNodesFromRootV()" );
	      	displayUpdatedNodesFromV( // Display from...
	      			theDataRoot.getParentOfRootTreePath( ), 
	      			theDataRoot.getRootDataNode( ) 
	        		);
        	}

      private void displayUpdatedNodesFromV(
      		TreePath parentTreePath, DataNode theDataNode 
      		)
        /* This method displays any nodes 
          that need displaying starting with theDataNode.  
          The TreePath of its parent is parentTreePath.
          */
        {
	  	    if ( theDataNode == null ) // Nothing to display. 
	    	  	; // Do nothing.
	    	  else { // Check this subtree.
		    		appLogger.trace( "DataTreeModel.displayUpdatedNodesFromV() "
		            + theDataNode.getNodePathString() );
	    	  	// Display this node any updated descendants.
		  	  	switch ( theDataNode.theUpdateLevel ) {
				  		case NONE:
				  	  	; // Nothing in this subtree needs displaying.
				  	  	break;
			  	  	case STRUCTURE_CHANGED: 
				  			displayStructuralChangeV( parentTreePath, theDataNode );
			  	  		break;
				  		case SUBTREE_CHANGED:
				  			displayChangedSubtreeV( parentTreePath, theDataNode );
				  	  	break;
				  		}
	    	  	}
        }

  		private void displayStructuralChangeV( 
  				TreePath parentTreePath, DataNode theDataNode 
      		)
  		  /* This method displays a subtree rooted at theDataNode,
          a subtree which is known to have changed.
          The TreePath of its parent is parentTreePath.
          The descendants are displayed recursively first.
          */
	      {
  				appLogger.debug( "DataTreeModel.displayStructuralChangeV() "
  						+theDataNode.getNodePathString() );
  			  TreePath theTreePath= parentTreePath.pathByAddingChild(theDataNode);
    			reportingStructuralChangeB( theTreePath ); // Display by reporting
    			  // to the listeners.

    			resetUpdatesInSubtreeV( theDataNode );
	      	}

  		private void displayChangedSubtreeV(
      		TreePath parentTreePath, DataNode theDataNode 
      		)
        /* This method displays a subtree rooted at theDataNode,
          a subtree which is known to have changed,
          The TreePath of its parent is parentTreePath.
          The descendants are displayed recursively first.
          The update status of all the nodes of any subtree display is reset. 
          */
	      {
  				appLogger.trace( "DataTreeModel.displayChangedSubtreeV() "
  						+ theDataNode.getNodePathString() );
      		TreePath theTreePath= parentTreePath.pathByAddingChild(theDataNode); 
			    int childIndexI= 0;  // Initialize child scan index.
			    while ( true ) // Recursively display any updated descendants.
			      { // Process one descendant subtree.
			        DataNode childDataNode=  // Get the child, the descendant root.
			           (DataNode)getChild( theDataNode, childIndexI );
			        if ( childDataNode == null )  // Null means no more children.
			            break;  // so exit while loop.
			        displayUpdatedNodesFromV( // Recursively display descendant. 
			        		theTreePath, childDataNode ); 
			        childIndexI++;  // Increment index for processing next child.
			      	}
					reportingChangeB(  // Display possible appearance change of root node.
							parentTreePath, theDataNode );
			    theDataNode.theUpdateLevel= // Reset root update status. 
			    		UpdateLevel.NONE;
	      	}

  		private void resetUpdatesInSubtreeV( DataNode theDataNode )
        /* This method resets the update status of 
          all the nodes in a subtree rooted at theDataNode.
          The node's descendants that need resetting 
          are reset recursively first.
          All nodes that need resetting 
          are assumed to be connected to theDataNode.
          */
	      {
	  	  	switch ( theDataNode.theUpdateLevel ) {
			  		case NONE:
			  	  	; // Do nothing.
			  	  	break;
			  	  default: 
			  	  	int childIndexI= 0;  // Initialize child scan index.
					    while ( true ) // Recursively reset appropriate descendants.
					      { // Process one child subtree.
					        DataNode childDataNode=  // Get the child.
					           (DataNode)getChild( theDataNode, childIndexI );
					        if ( childDataNode == null )  // Null means no more children.
					            break;  // so exit while loop.
					        resetUpdatesInSubtreeV( // Recursively reset child subtree. 
					        		childDataNode ); 
					        childIndexI++;  // Increment index for processing next child.
					      	}
					    theDataNode.theUpdateLevel= // Reset root node update status. 
					    		UpdateLevel.NONE;
			  	  	break;
			  		}
	      	}

    /*  ///elim  Maybe save this somewhere in case I need a DepthFirstSearch.
      @SuppressWarnings("unused") ///elim No longer used.
			private TreePath searchingForTreePath( DataNode targetDataNode )
        /*  ///elim This method is not needed anymore, because:
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
      /*  ///elim
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
            	DataNode parentDataNode= 
            		(DataNode)parentTreePath.getLastPathComponent(); 
              for // Checking children.
                ( int childIndexI=0; ; childIndexI++ ) 
                { 
                	DataNode childDataNode= 
                	  parentDataNode.getChild(childIndexI);
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
	                			"\n  depth "+currentDepthI+" exceeds maximum, node="+
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
      */   ///elim

    } // class DataTreeModel.
