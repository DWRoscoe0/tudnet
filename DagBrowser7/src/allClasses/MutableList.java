package allClasses;

//// import javax.swing.SwingUtilities;

//import static allClasses.Globals.appLogger;

public class MutableList extends NamedList

	/* This class is a DataNode which contains a mutable List of child DataNodes
	  Its superclass, NamedList, contains an immutable List of child DataNodes
	 * whose contents can be changed.

	  This class includes methods for adding elements to, 
	  and removing elements from, its List.

	  For thread safety, and because DataNodes are accessed by  
	  the Event Dispatch Thread (EDT), some methods are synchronized and  
	  switch to the Event Dispatch Thread (EDT) to do the work of
	  * changing the list and 
	  * calling TreeModel methods to inform TreeModelListeners of changes.
	  
	  ///? A reference to a DataTreeModel is provided by constructor injection
	    and stored in this object.  This is simple, 
	    but also inefficient in storage.  Is there a better way>
	  */

  {
	  // Variables for injected values, none.
	
    // Constructors, none.

    // Initialization methods, none.

	  // DataNode methods which change the node's state.

		  protected void addRawV(  // Add child at end of List.
		  		final DataNode childDataNode 
		  		)
		    /* This method adds childDataNode at the end of the List
		      It does not try to do it on the EDT,
		      so it doesn't need theDataTreeModel.
		      It is used only in special circumstances.
		      
		      ?? This is used by the Outline class for lazy construction.
		      Using addB(..) in this case causes:
		        Uncaught Exception, AWT-EventQueue-1, java.lang.StackOverflowError
		      I don't understand why.  Maybe it's TreePath translation.
		      */
		    {
		  		theListOfDataNodes.add( theListOfDataNodes.size(), childDataNode );
		      }

		  public boolean addB(  // Add child at end of List.
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
		  		final int indexI, final DataNode childDataNode 
		  		)
		    /* This method adds childDataNode to the list.

		      If childDataNode is already in the List 
		      then this method does nothing and returns false,
		      otherwise it adds childDataNode to the List and returns true. 
		      It adds childDataNode at index position indexI,
		      or the end of the list if indexI<0. 

		      If childDataNode is actually added then
		      it also informs theDataTreeModel about it
		      so that TreeModelListeners can be notified.
		      These notifications are done in a thread-safe manner 
		      on the EDT using SwingUtilities.invokeAndWait(Runnable).
		      
		      ///org Replace addSuccessB array with a MutableBoolean.
	  	    */
		    {
        	//appLogger.debug("MutableList.add(..) "+childDataNode+" at "+indexI);
		  		final boolean addSuccessB[]= new boolean[1]; // Array for result.
		  		final DataNode parentDataNode= this; // Because vars must be final.
		  		EDTUtilities.runOrInvokeAndWaitV( // Queuing add to AWT queue. 
        		new Runnable() {
        			@Override  
              public void run() {
      		  	  int searchIndexI=  // Searching for child by getting its index. 
      			    	  getIndexOfChild( childDataNode );
    		    	  if  // Removing and reporting change if child found. 
	  		    	    ( searchIndexI != -1)
    		    	  	addSuccessB[0]= false; // Returning failure, already there.
    		    	  	else
	    		    	  { // Adding to List because it's not there yet.
      		    	  	int insertAtI= (indexI < 0) ? // Converting index < 0 
			              		theListOfDataNodes.size() : // to mean end of list,
			              	  indexI; // otherwise use raw index.
		                addPhysicallyV( insertAtI, childDataNode );
		                notifyGUITreeModelAboutAdditionV(
		                		parentDataNode, insertAtI, childDataNode );
    		    	  		addSuccessB[0]= true; // Returning add success.
    		    	  	  }
                }
              } 
            );
		  		return addSuccessB[0]; // Returning whether add succeeded.
		      }

      private void notifyGUITreeModelAboutAdditionV(
      		DataNode parentDataNode, int insertAtI, DataNode childDataNode )
		    {
		      if // Notify TreeModel only if there is one referenced. 
		        ( theDataTreeModel != null ) {
		        theDataTreeModel.reportingInsertV( 
		      		parentDataNode, insertAtI, childDataNode 
		      		); // Done for showing the addition.
		        theDataTreeModel.reportingChangeV( 
		      		parentDataNode 
		      		); // Done in case # of children changes parent's appearance.
		      	}
		      }

		  private void addPhysicallyV(
		  		final int indexI, final DataNode childDataNode 
		  		)
		    /* This method adds childDataNode to this nodes list of children,
		      but also propagates information into that child:
		      * theDataTreeModel, into the childDataNode,
		        and its descendants if necessary.
		      * this NamedList, as the child's parent node.
		      */
		    {
		  		childDataNode.propagateIntoSubtreeV( theDataTreeModel );
		  		
		  		childDataNode.setParentToV( this ); // Link child to this node.
          theListOfDataNodes.add( // Link this node to child. 
          		indexI, childDataNode );
		      }
	
		  public synchronized boolean removeB( final DataNode childDataNode )
		    /* If childDataNode is not in the List 
		      then it does nothing and returns false,
		      otherwise it removes childDataNode from the List and returns true. 

			    It also informs theDataTreeModel of the removal, if any,
			    so TreeModelListeners can be informed.

		      It does all this in a thread-safe manner using 
		      safelyReportingChangeV(..).
		      
		      ///org Replace single element boolean array with a MutableBoolean.
			    */
		    {
		  	  final DataNode parentDataNode= this;
		  		final boolean removedB[]= new boolean[1]; // Array element for result.
		  		EDTUtilities.runOrInvokeAndWaitV( // Queuing removal of Unicaster.
        		new Runnable() {
              @Override  
              public void run() { 
		          	//appLogger.debug("MutableList.remove(..): run() starting.");
      		  	  int indexI=  // Searching for child by getting its index. 
      			    	  getIndexOfChild( childDataNode );
    		    	  if  // Removing and reporting change if child found. 
    		    	    ( indexI == -1)
	    		    	  {
	  		          	//appLogger.debug("MutableList.remove(..): not present.");
	    		    	  	removedB[0]= false; // Returning indication of not in List.
	    		    	  	}
    		    	  	else
	    		    	  {
	  		          	//appLogger.debug("MutableList.remove(..): removing.");
	    				  		theListOfDataNodes.remove( indexI );
	    				      theDataTreeModel.reportingRemoveV( 
	    				      	parentDataNode, indexI, childDataNode 
	    				      	); // To actually remove child from display.
		                theDataTreeModel.reportingChangeV( 
		    		      		parentDataNode 
		    		      		); // In case # of children changes parent's appearance.
	    		    	  	removedB[0]= true; // Returning indication of removal.
	    		    	  	}
                }  
              } 
            );
		  		return removedB[0]; // return indication of success of remove().
		      }

    } // MutableList 
