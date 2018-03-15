package allClasses;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.event.TreeModelEvent;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;

import static allClasses.Globals.*;  // appLogger;

public class DataTreeModel

  extends AbstractTreeModel

  implements TreeModel, Serializable
  
  /* This class implements an extended TreeModel 
    used for browsing the Infogora hierarchy DAG.
    It implements, or will implement, the following extensions beyond
    the basic TreeModel capabilities needed by the JTree class.
	
	  * The method getDataJComponent( TreePath inTreePath )
	    which returns a JComponent capable of displaying the node.

	  * This class will eventually maintain a 2-way association between 
	    a DataNode and the TreePath (or TreePaths in the case of DAGs)
	    that can be followed to get to them.

	    ?? One understandable drawback of the superclass TreeModel is
	    its inability to deal with the general Directed Graph,
	    or even the Directed Acyclic Graph, in which
	    a node can have multiple parents.
	    This is  desirable feature of the Infogora Hierarchy
	    so a solution needs to be found.
	    It will probably be done with a HashMap.

			?? Maybe add a parent list to nodes.
			Maybe create a dual role (node and node-list) object.
			 
    Because this class will be used with JTree as part of a user interface,
    there are some threading restrictions:
    * Changes to the data managed by this model, as well as
    	notifications about changes to that data, 
    	the ones which fire TreeModelListeners,
      must happen in the EDT (Event Dispatch Thread).
      This is what is done now, using the invokeAndWaitV(..) method.
    * ///enh Unfortunately using the EDT each time makes data changes inefficient.
      Using synchronization combined with HierarchicalUpPropagation
      might be sufficient to make change notification
      both efficient and thread-safe.

    ?? Use ObjectInterning to use less memory and to run faster.

    ?? Repeat what was done with 
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

      HashMap<DataNode,TreePath> theHashMap= new HashMap<DataNode,TreePath>();
    
    // constructor methods.

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

        ///org public void initializeV(..) should be moved to here. 

    // AbstractTreeModel/TreeModel interface methods.

      /* The following getter methods simply delegate to 
        the parentObject, which is assumed to be a DataNode,
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

      public Object getChild( Object parentObject, int IndexI ) 
        /* Returns the Object which is the child of parentObject
          whose index is IndexI.  The child must exist.
          This operation is delegated to parentObject which
          is assumed to satisfy the DataNode interface.
          */
        {
      	  DataNode childDataNode= // Getting the child from parent.
      	  		((DataNode)parentObject).getChild( IndexI ); 

      	  { // Calculating and adding to map the TreePath of child for later.  
	      	  DataNode parentDataNode= (DataNode)parentObject;
	          TreePath parentTreePath= // Retrieving path of parent from map. 
	            	translatingToTreePath( parentDataNode );
	          if ( parentTreePath != null ) // Add child path if path found.
		      	  theHashMap.put( // Making and adding child path to map. 
			      			childDataNode, parentTreePath.pathByAddingChild(childDataNode) 
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
        /* Unimplemented.  Throws an error.  
          ///? This could be converted to a call to another method.
          */
        { 
      	  throw new Error("DataTreeModel.valueForPathChanged(..) called"); 
      	  } 

    // Setter methods which are not part of AbstractTreeModel.

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

          Then finally theHashMap is seeded with 
          an entry for the sentinel parent of the root tree node
          which is used as a base for tree exploration and growth. 
          */
        {
          theDataRoot.initializeV( theInitialDataNode, this ); 
          theMetaRoot.initializeV( );
          theShutdowner.addShutdownerListener( theMetaFileManagerFinisher );

          theHashMap.put( // Making and adding root path to our map. 
      	  		theDataRoot.getParentOfRootDataNode( ), 
      	  		theDataRoot.getParentOfRootTreePath( ) 
	      			);
          }

      public void cachePathInMapV( TreePath theTreePath )
        /* This method caches an entire path theTreePath.
          It was created to prevent a cache miss and a long search
          if the selection at startup is a very long path.
          Seeding the cache with the initial selection path prevents this.
          */
      	{
      	  DataNode theDataNode= // Getting last element of path.
      	  		(DataNode)theTreePath.getLastPathComponent();
	    	  TreePath targetTreePath= // Testing whether present in cache.
	    	  		theHashMap.get( theDataNode );
	        if ( targetTreePath == null ) // Caching if not already present.
		        {
	        		cachePathInMapV( theTreePath.getParentPath() ); // Caching parent.
	        		  // This is a recursive call.
			        theHashMap.put( theDataNode, theTreePath ); // Caching child.
			        }
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

      public void reportingInsertV( 
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
		
		        	theHashMap.put( // Making and adding child to map for later. 
		        			childDataNode, parentTreePath.pathByAddingChild(childDataNode) 
		        			);
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
		
		        	theHashMap.remove( // Removing entry for removed child from map. 
		        			childDataNode // This won't remove descendants of child?? 
		        			);
		          }
          }

      public void safelyReportingChangeV( final DataNode theDataNode )
        /* This is a thread-safe version of reportingChangeV( theDataNode ).
          It's mainly to save code needed to report appearance changes
          by eliminating need to make lengthy calls to runOrInvokeAndWaitV(..).
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
	        reportingChangeV( parentTreePath, theDataNode );
        	}
	       
      public void reportingChangeV( 
      		TreePath parentTreePath, DataNode theDataNode 
      		)
	    	/* This method creates and fires a single-child TreeModelEvent
		      for the change of a single child DataNode, theDataNode,
		      and whose parent has tree path parentTreePath.
		      This method must be running on the EDT.
   	      */
        {
	      	if ( EDTUtilities.checkNotRunningEDTB() ) return; ///tmp
	      	if ( parentTreePath == null ) return; ///tmp
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
          }

      public TreePath translatingToTreePath( DataNode targetDataNode )
	      /* This method returns a TreePath of targetDataNode,
  	      or null if node can not be found in the DataNode tree.
  	      It tries to return a value from theHashMap first.
  	      If that fails then tries returning TreePath from a search
  	        and caches the result.
  	      If that fails then it returns null.
  	      
  	      ?? Consider storing parentsObject in AbDataNode.
  	      This would contain TreePath or parent DataNode information
  	      caching of explosively computationally intensive searches 
  	      can be eliminated.
  	        * TreePaths would still be cached, but they would be TreePaths
  	          that are constructed by following parent links,
  	          not by searching huge sections of the DataNode tree.
  	        * TreePath building could use cached TreePaths of parent DataNodes,
  	          and TreePath interning, to increase speed and reduce memory use.
  	        ? At first there could be a single parent link stored.
  	        ? Later a list of multiple parent links might be needed.
  	      */
      	{ 
      	  TreePath targetTreePath= // Looking in cache first.
      	  		theHashMap.get( targetDataNode );
	        if ( targetTreePath == null ) // Doing a search if not in cache.
		        {
	        		////appLogger.warning( 
			        ////		"DataTreeModel.translatingToTreePath( "
			        ////		+ targetDataNode 
			        ////	+ " ), cache miss." 
			        ////		);
			        targetTreePath= // Generating path using a search.   ///doc 
			        		///elim searchingForTreePath( targetDataNode );
			        		buildTreePath( targetDataNode );
			        if ( targetTreePath != null ) // Caching search result, if any.
			        	theHashMap.put( targetDataNode, targetTreePath );
	        		appLogger.warning( 
	        				"DataTreeModel.translatingToTreePath( "
	        				+ targetDataNode 
	        				+ " ), cache miss, resolves to\n  "
	        				+ targetTreePath
	        				);
			        }
      	  return targetTreePath;   
      	  }

      private TreePath buildTreePath( DataNode theDataNode )
        /*  ///doc This method replaces searchingForTreePath(..).
          This method returns a TreePath of targetDataNode.
          To do this it follows the trail of parentNamedList links 
          back to the root and builds a TreePath from that.
          ///ehn? This could be made more efficient by recursing using
          translatingToTreePath(..) on the parent node, a simple expression.
          ///elim This separate method might not be needed at all.
            See its only caller, translatingToTreePath(..)
          */
        {
      		////appLogger.warning( "DataTreeModel.buildTreePath(..) called.");
      	  Deque<DataNode> stackDeque= new ArrayDeque<DataNode>(10); 
      	  while (true) { // Stack all nodes in path to root.
      	  	if ( theDataNode == null ) break;
      	  	stackDeque.addFirst( theDataNode );
      	  	theDataNode= theDataNode.parentNamedList;
      	  	}
      		TreePath theTreePath= null;  // Defaulting result to null.
      	  { // Build TreePath from stacked nodes.
	      	  theTreePath= new TreePath(stackDeque.removeFirst());
	      	  while (true) {
	      	  	if ( stackDeque.peekFirst() == null ) break;
	      	  	theTreePath= 
	      	  			theTreePath.pathByAddingChild( stackDeque.removeFirst());
	      	  	}
      	  	}
          return theTreePath;
          }

      @SuppressWarnings("unused") ///elim
			private TreePath searchingForTreePath( DataNode targetDataNode )
        /*  ///elim This method is not needed anymore, because:
          * It might never be called anymore because
	          its only caller, translatingToTreePath(..),
	          now checks theHashMap first.  So this method is a backup.
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

    } // class DataTreeModel.
