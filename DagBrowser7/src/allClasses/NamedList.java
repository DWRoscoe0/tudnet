package allClasses;

import static allClasses.Globals.appLogger;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

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
	
    public void initializeV(
        String nameString, 
        DataNode... inDataNodes 
        )
      /* This initializes the NamedList with
        0 or more DataNodes from the array inDataNodes.
        Theoretically it could be used for 
        many different types of DataNodes.
        */
      {
    		super.initializeV( nameString );

        theListOfDataNodes= // Creating and storing the DataNode List to be
          new ArrayList<DataNode>(  // a mutable ArrayList from
            Arrays.asList(  // an immutable List made from
              inDataNodes  // the input array.
              )
            );  // the input array.
        }

	  protected void propagateIntoSubtreeV( DataTreeModel theDataTreeModel )
	    /* This method propagates into this node and any of its descendants
	      which need it. 
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

	  protected void setAndPropagateDownLogMaxLevelV( LogLevel theMaxLogLevel )
		  {
		  	if // Propagate into children only new level limit is different.
	  	    ( this.theMaxLogLevel != theMaxLogLevel )
			  	{
			  		for ( DataNode theDataNode : theListOfDataNodes)  // For each child
			  			theDataNode.setAndPropagateDownLogMaxLevelV( // propagate  
			  					theMaxLogLevel); // the new level.
			  	  super.setAndPropagateDownLogMaxLevelV( // Propagate into super-class. 
		  					theMaxLogLevel); // the new level.  This eliminates
  			  	      // the difference which allowed this propagation.
			  		}
		  	}

	  protected boolean logB( LogLevel theLogLevel )
	  	{
	  		return ( theLogLevel.compareTo( theMaxLogLevel ) <= 0 ) ;
	  		}

	  protected void logV( LogLevel theLogLevel, String theLogString )
	  	{
		  	logV( theLogLevel, theLogString, null, false );
	  		}

	  protected void logV( 
	  		LogLevel theLogLevel, 
	  		String theLogString, 
	  		Throwable theThrowable, 
	  		boolean consoleB )
  	{
  		if ( logB(theLogLevel) )
	      appLogger.logLabeledEntryV( 
	      		theLogLevel, theLogString, theThrowable, consoleB );
  	  }
	  
    protected void reportChangeInChildV( final DataNode childDataNode )
      /* This method reports a change of childDataNode,
        which must be one of this node's children.
        */
    	{
    	  final DataNode parentDataNode= this;
    	  EDTUtilities.runOrInvokeAndWaitV( // Do following on EDT thread. 
		    		new Runnable() {
		    			@Override  
		          public void run() {
		  	      	TreePath parentTreePath= // Calculating path of the this parent. 
		  	          	theDataTreeModel.translatingToTreePath( parentDataNode );
		    				theDataTreeModel.reportingChangeV( 
		    						parentTreePath, childDataNode 
		    						);
		            }
		          }
		    		);
		        
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
