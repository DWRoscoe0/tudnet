package allClasses;

import java.util.Iterator;
import java.util.List;

import allClasses.AppLog.LogLevel;
import allClasses.epinode.MapEpiNode;
import allClasses.multilink.ListMultiLink;
import allClasses.multilink.MultiLink;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class NamedList 

	extends NamedBranch

  { // Beginning of class NamedList.
  
    /* This class adds to its superclass the ability to store child DataNodes.
      It includes methods for adding children to its stored list of children.
      These methods are used during construction and initialization, 
      but they may be used later also.

      If deletion of children is needed then it must be done in a subclass.

      This class does not do lazy evaluation of its children.
      Further, if lazy evaluation is being done by a subclass,
      then this object knows nothing about it.

      This class uses a List to store its children.  It is 
      one of 3 non-leaf classes that define storage for child references.
      The other 2 classes are 
      * MutableListWithMap, which contains a HashMap for fast child lookup. 
      * StateList, which contains a list of children which
        duplicates some of the regular child list, 
        but only the children that are states.
  
      ///opt Reduce storage used by eliminating redundant child collections,
        not necessarily in this class, but in its subclasses.
  
      ///enh? Should NamedList be converted to a generic class
        whose elements can be something other than DataNodes?
        getChild(int indexI) returns a DataNode, so maybe there's no point.
        Or maybe getChildE(int indexI) could be added 
        to have the best of both worlds? 
      */


		// Instance variables.

    protected MultiLink<DataNode> childMultiLinkOfDataNodes /* The children. */
      = new ListMultiLink<DataNode>(); // Set the List of children to empty.


    /* Constructors and initializers.
     * 
     * An instance of this class can be created by either
     * 
     * * using constructor NamedList(String nameString,DataNode... childDataNodes).
     *   This is the preferred way.
     *   It is simpler for this class and its subclasses.
     *   
     * * using constructor NamedList() followed by calling
     *   initializeV(String nameString,DataNode... childDataNodes).
     */

    public NamedList( String nameString, DataNode... childDataNodes )
      {
        this();
        initializeV(nameString, childDataNodes); 
        }

    public NamedList()
      /* If this constructor is used then care must be taken to call 
        initializeV(String nameString,DataNode... childDataNodes)
        to complete the initialization.
       */
      { 
        }
    
    public void initializeV(String nameString,DataNode... childDataNodes)
    /* This method initializes this NamedList instance.
     * 
     * nameString is the name for this DataNode.
     * 
     * childDataNodes is an array of DataNodes to become this nodes children.
      
      ///fix? This needs to initialize parent references?
      */
    {
  		super.setNameV( nameString );

  		for ( DataNode childDataNode : childDataNodes) // For each new child
  			addAtEndV( childDataNode ); // append child to list.
      }


    // Finalization.

    @Override
    protected int finalizeThisSubtreeI()
      /* This method finalizes this node and its descendants.
       * This includes this node and its children recursively.
       * It returns the number of DataNodes finalized.
       */
      {
        int nodeTotalI= 0;
        for // For each child
          ( DataNode childDataNode : 
            childMultiLinkOfDataNodes.getLinksIterable()
            )
          nodeTotalI+= // recursively finalize it, adding number to total.
            childDataNode.finalizeThisSubtreeI();
        nodeTotalI+=  // Finish by calling finalizer of the base class.
            super.finalizeThisSubtreeI();
        return nodeTotalI;
        }

    
    /* Subtree propagation methods.
     * These methods are used when data needs to be duplicated
     * throughout an entire subtree.
     * 
     * Propagation into subtrees happens recursively.
     * Recursion stops when either of the following occurs:
     * * A leaf DataNode is reach, because a leaf has no children.
     * * A DataNode is reach which already has the value being propagated.
     * 
     * These methods may be called at any time,
     * but are usually called when subtrees are being constructed or inserted.
     */
    
    protected void propagateTreeModelIntoSubtreeV( 
        DataTreeModel theDataTreeModel )
      /* This method propagates theDataTreeModel into 
        the subtree rooted at this DataNode.
        */
      {
        if // Propagate only if 
          ( ( theDataTreeModel != null ) // input TreeModel is not null and 
            && ( this.theDataTreeModel == null )  // our TreeModel is null.
            )
          { // Propagate.
            for // For each child
              (DataNode childDataNode : getChildLazyListOfDataNodes())
              childDataNode.propagateTreeModelIntoSubtreeV(theDataTreeModel);

            super.propagateTreeModelIntoSubtreeV( // Propagate List into super-class. 
                theDataTreeModel // This stores theDataTreeModel.
                ); // It's done last because it is the controlling flag.
            }
        }

    protected void propagateLogLevelIntoSubtreeV( LogLevel theMaxLogLevel )  
      /* This method propagates theMaxLogLevel into 
        the subtree rooted at this DataNode.
        */
      {
        boolean changeNeededB= ( this.theMaxLogLevel != theMaxLogLevel ); 
        if // Propagate further only if new level is different from old one.
          ( changeNeededB )
          { // Propagate further.
            for  // For each child
              (DataNode childDataNode : getChildLazyListOfDataNodes())
              childDataNode.propagateLogLevelIntoSubtreeV(theMaxLogLevel);
            super.propagateLogLevelIntoSubtreeV( // Propagate into super-class. 
                theMaxLogLevel); // the new level.  This eliminates
                // the difference in LogLevel which enabled propagation.
            }
        }


    /* Child addition methods.
     * These methods add child DataNodes to this DataNode.
     * If these methods are called only during construction and initialization
     * and this node is changed in no other way,
     * they it will be an immutable node.
     */

    public void addAtEndV(final DataNode childDataNode)
      /* This method adds childDataNode 
       * at end of this DataNode's list of child DataNodes.
       * If the child is already in the list, this method changes nothing.
       */
      {
        addAtEndB(childDataNode);
        }

    public boolean addAtEndB(final DataNode childDataNode)
      /* This method tries to add childDataNode 
       * at end of this DataNode's list of child DataNodes.
       * If returns true if successful.
       * If the child is already in the list then
         this method changes nothing and returns false.
       */
      {
        boolean resultB= addB( // Try adding.
            -1, // -1 means at end of list. 
            childDataNode
            );
        if (! resultB)
          theAppLog.error( // Logging error if already in list.
              "NamedList.addingB_TEMPORARY(..): Already present."
              );
        return resultB;
        }

	  public synchronized boolean addB(  // Add at index. 
	  		final int requestedIndexI, final DataNode childDataNode 
	  		)
	    /* This method tries to add childDataNode to 
	     * this DataNode's list of child DataNodes.
	     * If childDataNode is already in the list
	     * then this method changes nothing and returns false,
	     * otherwise it adds childDataNode to the list and returns true.
	     * The childDataNode is added at index position requestedIndexI,
	     * or the end of the list if requestedIndexI < 0.
	     * 
	     * If childDataNode is actually added then:
	     * 
	     * * This method notifies all interested listeners about the addition.
	     * 
	     * * This method makes the following adjustments to childDataNode:
	     *   * It stores this DataNode as childDataNode's parent node.
       *   * It propagates theDataTreeModel into the childDataNode,
       *     and its descendants if necessary.
	     *  
	     * These operations are done in a thread-safe manner
	     * by being synchronized and using synchronized methods.
	     * The EDT thread might not be involved at all.
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
            childDataNode.propagateTreeModelIntoSubtreeV( theDataTreeModel );
            childDataNode.setTreeParentToV( this ); // Set child's parent link.
	    	  	int actualIndexI= // Converting 
	    	  			(requestedIndexI < 0) // requested index < 0 
	    	  			? childMultiLinkOfDataNodes.getCountI() // to mean end of list,
            	  : requestedIndexI; // otherwise use requested index.
            childMultiLinkOfDataNodes.addV( // Insert child at correct spot.
                actualIndexI, childDataNode );
            DataNode.signalInsertionV( // Notify interested listeners about add.
                parentDataNode, actualIndexI, childDataNode );
            successB= true; // Returning add success.
    	  	  }
	  		return successB; // Returning whether add succeeded.
	      }

    
    // Scanning/visiting method.
    
    @Override
    protected boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode)
      /* This method is not used for anything now.  It has not been tested.
       * 
       * It was meant to try to process a theMapEpiNode in some way by
       * trying to have each of its children process it.
       * It was to return true if the processing was successful by a child,
       * false otherwise.
       */
      { 
        boolean processedB= false; // Assume all children will fail to process.
        Iterator<DataNode> theIterator= // Prepare a child iterator.
            getChildLazyListOfDataNodes().iterator();
        while (true) { // Process children until something causes exit.
          if (! theIterator.hasNext()) break; // Exit if no more children.
          DataNode childDataNode= theIterator.next(); // Get next child.
          processedB= childDataNode.tryProcessingMapEpiNodeB(theMapEpiNode);
          if (processedB) // Exit if the child successfully processed the data.
            break;
          }
        return processedB; 
        }


    // Child getting methods.'

	  // Child DataNode getter methods.
 
    public DataNode getChild( int indexI ) 
      /* This returns the child with index indexI or 
       * null if no child exists with that index.
       * 
       * If lazy evaluation is being used by a subclass,
       * this object knows nothing about it.
       */
      { 
        return childMultiLinkOfDataNodes.getE(indexI); 
        }
    
    public List<DataNode> getChildLazyListOfDataNodes()
      /* This method returns a List of the child DataNodes.
       * 
       * This method is named what it is to emphasize that 
       * no evaluation of nodes is done.
       * If lazy evaluation is being used by a subclass,
       * this object knows nothing about it.
       */
      { 
        return childMultiLinkOfDataNodes.getListOfEs(); 
        }


	  ///dbg Debug methods.

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
	  		  
	  		  if ( getTreeParentNamedList() == null ) { 
			  		theAppLog.debug(
			  				"checkForNullDataTreeModelV() parentNamedList == null in:" + NL
			  				+ this);
	  		  	break toReturn;
		  		  }

		  		theAppLog.debug(
		  				"checkForNullDataTreeModelV() "
		  				+ "theDataTreeModel == null and  parentNamedList != null in:" + NL
		  				+ this);
	  		  getTreeParentNamedList().logNullDataTreeModelsV(); // Recurs for ancestors.
  				} // toReturn:
  	  	}

    } // class NamedList
