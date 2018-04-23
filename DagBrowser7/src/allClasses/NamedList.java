package allClasses;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.tree.TreePath;

import allClasses.AppLog.LogLevel;

public class NamedList 

  extends NamedNonLeaf  // Will override all remaining leaf behavior.
  
  /* This is a utility class that is simply a List with a name.
    It is meant to be immutable after construction and initialization, 
    but misbehaving subclasses could change it.
    Also, there is nothing preventing changes in
    its children or other descendants.
    */
  
  { // class NamedList
	
		// Instance variables.
	
			protected DataTreeModel theDataTreeModel; // For reporting DAG changes.
			  // This also doubles as a flag for the propagateDownV(..) method.
			  // theDataTreeModel != null means down-propagation into this node
			  // and all its descendants has already occurred and 
			  // need not be done again, except for new added children. 
	
	    protected List<DataNode> theListOfDataNodes= // Set to empty,
	    		new ArrayList<DataNode>(  // a mutable ArrayList from
		        Arrays.asList(  // an immutable List made from
		        		emptyListOfDataNodes() // an empty DataNode list.
		            )
		        ); // This might be replaced with a SelfReturningListOrNode.
	        ///enh Distinguish between active children and lazy children.
	        // See MetaFileManager for ideas.

    public void initializeV(
        String nameString, 
        DataNode... inDataNodes 
        )
      /* This initializes the NamedList with
        0 or more DataNodes from the array inDataNodes.
        Theoretically it could be used for 
        many different types of DataNodes.
        
        ///fix This needs to initialize parent references.
        */
      {
    		super.initializeV( nameString );

	  	  /*  ////
        theListOfDataNodes= // Creating and storing the DataNode List to be
          new ArrayList<DataNode>(  // a mutable ArrayList from
            Arrays.asList(  // an immutable List made from
              inDataNodes  // the input array.
              )
            );  // the input array.
	  	  */  ////

	  		for ( DataNode theDataNode : inDataNodes) // For each new child
	  			addAtEndB( theDataNode );
        }

	  public boolean addAtEndB(  // Add child at end of List.
	  		final DataNode childDataNode 
	  		)
	    /* This method uses addB( indexI , childDataNode ) to add
	      childDataNode at the end of the List.  Otherwise it's identical.
	      */
	    {
	  	  return addB(  // Adding with index == -1 which means end of list.
	  	  		-1, childDataNode 
	  	  		);
	      }

	  public synchronized boolean addB(  // Add at index. 
	  		final int requestedIndexI, final DataNode childDataNode 
	  		)
	    /* This method adds childDataNode to the list.

	      If childDataNode is already in the List 
	      then this method does nothing and returns false,
	      otherwise it adds childDataNode to the List and returns true. 
	      It adds childDataNode at index position requestedIndexI,
	      or the end of the list if requestedIndexI < 0. 

	      If childDataNode is actually added then
	      it also informs theDataTreeModel about it
	      so that TreeModelListeners can be notified.

	      These operations are done in a thread-safe manner 
	      on the EDT using SwingUtilities.invokeAndWait(Runnable).
	      
	      ///org Replace addSuccessB array with a MutableBoolean.
  	    */
	    {
      	//appLogger.debug("MutableList.add(..) "+childDataNode+" at "+indexI);
	  		//// final boolean addSuccessB[]= new boolean[1]; // Array for result.
	  		final AtomicBoolean successAtomicBoolean= new AtomicBoolean(false); 
	  		final DataNode parentDataNode= this; // Because vars must be final.
	  		EDTUtilities.runOrInvokeAndWaitV( // Queuing operations to AWT queue. 
      		new Runnable() {
      			@Override  
            public void run() {
    		  	  int searchIndexI=  // Searching for child by getting its index. 
    			    	  getIndexOfChild( childDataNode );
  		    	  if ( searchIndexI != -1) // Child already in list.
  		    	  	successAtomicBoolean.set(false); // Returning add failure.
  		    	  	else // Child was not found in list.
    		    	  { // Adding to List because it's not there yet.
    		    	  	int actualIndexI= // Converting 
    		    	  			(requestedIndexI < 0) // requested index < 0 
		              		? theListOfDataNodes.size() // to mean end of list,
		              	  : requestedIndexI; // otherwise use requested index.
	                addPhysicallyV( actualIndexI, childDataNode );
	                notifyTreeModelAboutAdditionV(
	                		parentDataNode, actualIndexI, childDataNode );
	                successAtomicBoolean.set(true); // Returning add success.
  		    	  	  }
              }
            } 
          );
	  		return successAtomicBoolean.get(); // Returning whether add succeeded.
	      }

    private void notifyTreeModelAboutAdditionV(
    		DataNode parentDataNode, int insertAtI, DataNode childDataNode )
	    {
	      if // Notify TreeModel only if there is one referenced. 
	        ( theDataTreeModel != null ) 
		      {
		      	theDataTreeModel.signalInsertionV( 
		      			parentDataNode, insertAtI, childDataNode 
			      		);
		      	/*  ////
		        theDataTreeModel.reportingInsertV( 
		      		parentDataNode, insertAtI, childDataNode 
		      		); // For showing the addition.
		        theDataTreeModel.reportingChangeV( 
		      		parentDataNode 
		      		); // In case # of children changes parent's appearance.
		      	*/  ////
		      	}
	      }

	  private void addPhysicallyV(
	  		final int indexI, final DataNode childDataNode 
	  		)
	    /* This method adds childDataNode to this node's list of children,
	      but into that child it propagates the following information:
	      * theDataTreeModel, into the childDataNode,
	        and its descendants if necessary.
	      * this NamedList, as the child's parent node.
	      However, it does not do TreeModel notifications.
	      */
	    {
	  		childDataNode.propagateIntoSubtreeV( theDataTreeModel );
	  		
	  		childDataNode.setParentToV( this ); // Link child to this node.

	  		theListOfDataNodes.add( // Link this node to child. 
        		indexI, childDataNode );
	      }

	  protected void propagateIntoSubtreeV( DataTreeModel theDataTreeModel )
	    /* This method propagates theDataTreeModel into 
	      this node and any of its descendants which need it. 
	      */
		  {
		  	if // Propagate into children DataTreeModel only if 
		  	  ( ( theDataTreeModel != null ) // input TreeModel is not null but 
		  	  	&& ( this.theDataTreeModel == null )  // our TreeModel is null.
		  	  	)
			  	{
			  		for ( DataNode theDataNode : theListOfDataNodes) // For each child
			  			theDataNode.propagateIntoSubtreeV(  // recursively propagate
			  					theDataTreeModel // the TreeModel 
			  					);
			  	  super.propagateIntoSubtreeV( // Propagate List into super-class. 
			  	  		theDataTreeModel 
			  	  		);
		  		  this.theDataTreeModel=  // Finally set TreeModel last because
		  		  		theDataTreeModel; // it is the controlling flag.
			  		}
		  	}

	  protected void propagateIntoSubtreeV( LogLevel theMaxLogLevel )
	    /* This method propagates theMaxLogLevel into 
		    this node and any of its descendants which need it. 
		    */
		  {
		  	if // Propagate into children only if new level limit is different.
	  	    ( this.theMaxLogLevel != theMaxLogLevel )
			  	{
			  		for ( DataNode theDataNode : theListOfDataNodes)  // For each child
			  			theDataNode.propagateIntoSubtreeV( // recursively propagate  
			  					theMaxLogLevel); // the new level.
			  	  super.propagateIntoSubtreeV( // Propagate into super-class. 
		  					theMaxLogLevel); // the new level.  This eliminates
  			  	      // the difference LogLevel which allowed propagation.
			  		}
		  	}
	  
    protected boolean reportChangeInChildB( final DataNode childDataNode )
      /* This method reports a change of childDataNode,
        which must be one of this node's children, to theDataTreeModel,
        which will update the user display to show the change if needed.
        IT returns true if success, false if there was an error,
          such as a null parameter or the the DataNode tree root
          not being reachable from the child. 
        */
    	{
    		theDataTreeModel.signalChangeV( childDataNode );
    	  return true;
    		/*  ////
    	  final DataNode parentDataNode= this;
    		final AtomicBoolean successAtomicBoolean= new AtomicBoolean(true); 
    	  EDTUtilities.runOrInvokeAndWaitV( // Do following on EDT thread. 
		    		new Runnable() {
		    			@Override  
		          public void run() {
		  	      	TreePath parentTreePath= // Calculating path of the this parent. 
		  	          	theDataTreeModel.translatingToTreePath( parentDataNode );
		    				if ( theDataTreeModel.reportingChangeB( 
		    						parentTreePath, childDataNode 
		    						) )
		              successAtomicBoolean.set(false); // Returning reporting error.
		            }
		          }
		    		);
    	  if ( ! successAtomicBoolean.get() )
    	  	Misc.noOp(); ///dbg  For inter-thread error breakpoints
				return successAtomicBoolean.get(); // Returning whether add succeeded.
				*/  ////
				}

	  
	  // interface DataNode methods.
 
    public DataNode getChild( int indexI ) 
      /* This returns the child with index indexI or null
        if no such child exists.
        */
      {
        DataNode resultDataNode;  // Allocating result space.

        if  // Handling index which is out of range.
          ( (indexI < 0) || (indexI >= theListOfDataNodes.size()) )
          resultDataNode= null;  // Setting result to null.
        else  // Handling index which is in range.
          resultDataNode=   // Setting result to be child from...
            theListOfDataNodes.get(   // ...DataNode List...
              indexI  // ...at the desired position.
              );

        return resultDataNode;
        }

    } // class NamedList
