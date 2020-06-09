package allClasses;

import java.util.Arrays;
import java.util.Iterator;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import allClasses.AppLog.LogLevel;
import allClasses.multilink.ListMultiLink;
import allClasses.multilink.MultiLink;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class NamedList 

	extends NamedBranch  // Will override all remaining leaf behavior.

	implements 
    //// MultiLink<DataNode>, already done in superclass.
    Iterable<DataNode>
  
  /* This is a utility class that is simply a List with a name.
    It is is immutable after construction and initialization, 
    but subclasses could add mutation abilities.
    Also, there is nothing preventing changes in
    its children or other descendants.
    
    ///opt Reduce storage used by eliminating redundant child collections. 
    This one of 3 non-leaf classes that define storage for child references.
    This class uses a List to store its children.
    The other 2 are 
    * MutableListWithMap, which contains a HashMap for fast child lookup. 
    * StateList, which contains a list of children which
      duplicates some of the regular child list, but only the ones that are states.
    This might be possible by defining some new classes, such as
    * EmptyList, whose subclasses can use the type of child lists
      that best fits its purpose.  
     
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
		        );

	    protected MultiLink<DataNode> childMultiLinkOfDataNodes= // Set to an empty
          new ListMultiLink<DataNode>(); // ListMultiList of DataNodes.

	    
	    /* Constructors: An instance of this class can be created by either
	       * using NamedList(String nameString,DataNode... inDataNodes).
	         This is the preferred way.  It is simpler for this class and its subclasses. 
	       * using NamedList() followed by 
	         initializeV(String nameString,DataNode... inDataNodes).
	         

	     */
      
      public NamedList(
          String nameString, 
          DataNode... inDataNodes 
          )
        { 
          initializeV(nameString, inDataNodes); 
          }
      
      public NamedList()
        /* If this constructor is used then 
          initializeV(String nameString,DataNode... inDataNodes)
          should be called afterward to do remaining initialization.
         */
        { 
          }
	    
	    public void initializeV(String nameString,DataNode... inDataNodes)
      /* This initializes the NamedList with a name and
        0 or more DataNodes from the array inDataNodes.
        Theoretically it could be used for 
        many different types of DataNodes.
        
        ///fix This needs to initialize parent references.
        */
      {
    		super.initializeV( nameString );

	  		for ( DataNode theDataNode : inDataNodes) // For each new child
	  			addAtEndB( theDataNode ); // append child to list.
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
	      by being synchronized and using synchronized methods.
	      The EDT thread might not be used at all.
  	    */
	    {
	  		final boolean successB; 
	  		final DataNode parentDataNode= this; // Because vars must be final.
	  	  int searchIndexI=  // Searching for child by getting its index. 
		    	  getIndexOfChild( childDataNode );
    	  if ( searchIndexI != -1) // Child already in list.
    	  	successB= false; // Returning add failure.
    	  	else // Child was not found in list.
	    	  { // Adding to List because it's not there yet.
	    	  	int actualIndexI= // Converting 
	    	  			(requestedIndexI < 0) // requested index < 0 
            		//// ? theListOfDataNodes.size() // to mean end of list,
	    	  			? childMultiLinkOfDataNodes.getLinkCountI() // to mean end of list,
            	  : requestedIndexI; // otherwise use requested index.
            addPhysicallyV( actualIndexI, childDataNode );
            childMultiLinkOfDataNodes.addV(actualIndexI, childDataNode );
            notifyTreeModelAboutAdditionV(
            		parentDataNode, actualIndexI, childDataNode );
            successB= true; // Returning add success.
    	  	  }
	  		return successB; // Returning whether add succeeded.
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
	      
	      This is not synchronized because it is called only by addB(..).
	      */
	    {
	  		childDataNode.propagateIntoSubtreeV( theDataTreeModel );
	  		
	  		childDataNode.setParentToV( this ); // Link child to this node.

	  		theListOfDataNodes.add( // Link this node to child. 
        		indexI, childDataNode );
	      }

    protected int finalizeDataNodesI()
      /* This override method finalizes all the children and then the base class. 
        It increases and returns nodeCountI by the number of nodes finalized.*/
      {
        int nodeTotalI= 0;
        //// for ( DataNode theDataNode : theListOfDataNodes) // For each child
        for ( DataNode theDataNode : childMultiLinkOfDataNodes) // For each child
          nodeTotalI+=   // recursively finalize it, adding the number finalized to total.
            theDataNode.finalizeDataNodesI();
        nodeTotalI+= super.finalizeDataNodesI(); // Finalize base class
        return nodeTotalI;
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
			  		//// for ( DataNode theDataNode : theListOfDataNodes) // For each child
		  	    for ( DataNode theDataNode : childMultiLinkOfDataNodes) // For each child
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

	  protected boolean propagateIntoSubtreeB( LogLevel theMaxLogLevel )  ///enh Make boolean.
	    /* This method propagates theMaxLogLevel into 
		    this node and any of its descendants which need it.
		    It acts only if the present level is different from theMaxLogLevel.
		    */
		  {
	  		boolean changeNeededB= ( this.theMaxLogLevel != theMaxLogLevel ); 
		  	if // Propagate into children only if new level limit is different.
	  	    ( changeNeededB )
			  	{
		  			propagateIntoDescendantsV(theMaxLogLevel);
				  	super.propagateIntoSubtreeB( // Propagate into super-class. 
				  			theMaxLogLevel); // the new level.  This eliminates
				  			// the difference in LogLevel which enabled propagation.
			  		}
		  	return changeNeededB;
		  	}

	  protected void propagateIntoDescendantsV( LogLevel theMaxLogLevel )
	    /* This method propagates theMaxLogLevel into the descendants,
	      but not into this node.  It remains unchanged.
		    */
		  {
	  		//// for ( DataNode theDataNode : theListOfDataNodes)  // For each child
	      for ( DataNode theDataNode : childMultiLinkOfDataNodes)  // For each child
	  			theDataNode.propagateIntoSubtreeB( // recursively propagate  
	  					theMaxLogLevel); // the new level into child subtree.
		  	}
	  
    protected boolean reportChangeInChildB( final DataNode childDataNode )
      /* This method reports a change to the DataTreeModel of 
        a change in childDataNode, which must be one of this node's children.
        This will cause an update of the user display 
        to show that change if needed.
        This method returns true if successful, false if there was an error,
          such as a null parameter or the the DataNode tree root
          not being reachable from the child. 
        */
    	{
    		theDataTreeModel.signalChangeV( childDataNode );
    	  return true;
				}

    protected boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode)
      /* This method tries processing a MapEpiNode.
        It does this by trying to have each child process the theMapEpiNode 
        by its method of the same name, until one of them returns true,
        or the end of the child list is reached.
        It returns true if the processing was successful by a child, false otherwise.
        This is a general purpose child scanner which can be used by subclasses
        or overridden for customized behavior.
        */
      { 
        boolean processedB= false; // Assume all children will fail to process.
        Iterator<DataNode> theIterator= // Prepare a child iterator.
            //// theListOfDataNodes.listIterator();
            childMultiLinkOfDataNodes.iterator();
        while (true) { // Process children until something causes exit.
          if (! theIterator.hasNext()) break; // Exit if no more children.
          DataNode theDataNode= theIterator.next(); // Get next child.
          processedB= theDataNode.tryProcessingMapEpiNodeB(theMapEpiNode);
          if (processedB) break; // Exit if the child successfully processed the data.
          }
        return processedB; 
        }

	  
	  // interface DataNode methods.
 
    public DataNode getChild( int indexI ) 
      /* This returns the child with index indexI or null
        if no such child exists.
        */
      { return childMultiLinkOfDataNodes.getLinkL(indexI); }
    /*  ////
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
    */  ////
    
	  public Iterator<DataNode> iterator() 
		  {
		    //// return theListOfDataNodes.iterator();
	      return childMultiLinkOfDataNodes.iterator();
		  	}

	  
	  /* Related originally auto-generated method stubs for interface MultiLink.
	    They simply pass control to the same-named methods in the childMultiLink.
	    */
    
    @Override
    public boolean hasNoLinks() {
      return childMultiLinkOfDataNodes.hasNoLinks();
      }

    @Override
    public int getLinkCountI() {
      return childMultiLinkOfDataNodes.getLinkCountI();
      }

    @Override
    public DataNode getLinkL(int indexI) {
      return childMultiLinkOfDataNodes.getLinkL(indexI); 
      }

    @Override
    public int getIndexOfLinkI(DataNode theL) {
      return childMultiLinkOfDataNodes.getIndexOfLinkI(theL);
      }

	  
	  ///dbg methods.
	  
  	protected void logNullDataTreeModelsV()  ///dbg
  	  /* Recursively checks for null TreeModel in node and ancestors.
  	   */
  	  {
  		  toReturn: {
	  		  if (theDataTreeModel != null) {
			  		theAppLog.debug(
			  				"checkForNullDataTreeModelV() theDataTreeModel != null in:" + NL
			  				+ this);
	  		  	break toReturn;
	  		  	}
	  		  
	  		  if ( parentNamedList == null ) { 
			  		theAppLog.debug(
			  				"checkForNullDataTreeModelV() parentNamedList == null in:" + NL
			  				+ this);
	  		  	break toReturn;
		  		  }

		  		theAppLog.debug(
		  				"checkForNullDataTreeModelV() "
		  				+ "theDataTreeModel == null and  parentNamedList != null in:" + NL
		  				+ this);
	  		  parentNamedList.logNullDataTreeModelsV(); // Recurs for ancestors.
  				} // toReturn:
  	  	}

    } // class NamedList
