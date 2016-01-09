package allClasses;

//import static allClasses.Globals.appLogger;

public class MutableList 

	/* This class is a NamedList whose contents can be changed.

	  This class includes methods for changing the List.
	  It uses a DataTreeModel to inform TreeModelListeners of those changes.
	  For thread safety some methods are synchronized and 
	  switch to the Event Dispatch Thread (EDT) to call the TreeModel methods.
	  */

  extends NamedList

  {
	  // Variables for injected values.
	
			protected DataTreeModel theDataTreeModel; // For reporting List changes.

    // Constructors.
			
	    public MutableList (   // Constructor.
	        DataTreeModel theDataTreeModel,
	        String nameString, 
	        DataNode... inDataNodes 
	        )
	      {
	        super( nameString, inDataNodes ); // Constructing base class. 
	
	        // Storing injected values stored in this class.
	        this.theDataTreeModel= theDataTreeModel;
	        }

	  // DataNode methods which change the node's state.

		  protected void addRawV(  // Add child at end of List.
		  		final DataNode childDataNode 
		  		)
		    /* This method adds childDataNode at the end of the List
		      It does not try to do it on the EDT.
		      
		      ?? This is used and apparently needed by Outline.
		      Using addB(..) causes a stack overflow.
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
		    /* If childDataNode is already in the List 
		      then it does nothing and returns false,
		      otherwise it adds childDataNode to the List and returns true. 
		      It adds childDataNode at index position indexI,
		      or the end of the list if indexI<0. 
		      
		      It also informs theDataTreeModel about any insertion
		      so that TreeModelListeners can be fired.
		      
		      It does all this in a thread-safe manner using 
		      safelyReportingChangeV(..).
	  	    */
		    {
        	//appLogger.debug("MutableList.add(..) "+childDataNode+" at "+indexI);
		  		final boolean addSuccessB[]= new boolean[1]; // Array for result.
		  		final DataNode parentDataNode= this; // Because vars must be final.
		  		theDataTreeModel.runOrInvokeAndWaitV( // Queuing add to AWT queue. 
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
		                theListOfDataNodes.add( insertAtI, childDataNode );
		                theDataTreeModel.reportingInsertV( 
		    		      		parentDataNode, insertAtI, childDataNode 
		    		      		);
		                theDataTreeModel.reportingChangeV( 
		    		      		parentDataNode 
		    		      		); // In case # of children changes parent's appearance.
    		    	  		addSuccessB[0]= true; // Returning add success.
    		    	  	  }
                }
              } 
            );
		  		return addSuccessB[0]; // Returning whether add succeeded.
		      }
	
		  public synchronized boolean removeB( final DataNode childDataNode )
		    /* If childDataNode not in the List 
		      then it does nothing and returns false,
		      otherwise it removes childDataNode from the List and returns true. 

			    It also informs theDataTreeModel of the removal, if any,
			    so TreeModelListeners can be informed.

		      It does all this in a thread-safe manner using 
		      safelyReportingChangeV(..).
			    */
		    {
		  	  final DataNode parentDataNode= this;
		  		final boolean removedB[]= new boolean[1]; // Array element for result.
		  	  theDataTreeModel.runOrInvokeAndWaitV( // Queuing removal of Unicaster from List.
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
	    				      	);
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
