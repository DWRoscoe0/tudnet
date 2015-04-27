package allClasses;

import static allClasses.Globals.appLogger;


public class MutableList 

	/* This class is a NamedList which can be changed.

	  This class includes methods for changing the List
	  and a DataTreeModel.Talker class instance
	  to inform TreeModelListeners of those changes.

	  Except for the constructor, the methods in this class
	  should be called only from the Event Dispatch Thread (EDT).
	  */

  extends NamedList

  {
	  // Variables for injected values.
	
			private DataTreeModel theDataTreeModel; // For reporting List changes.

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

		  public void add(  // Add child at end of List.
		  		final DataNode childDataNode 
		  		)
		    /* This method uses add( indexI , childDataNode ) to add
		      childDataNode at the end of the List.
		      */
		    {
		  	  add( -1, childDataNode ); // Adding with index == -1 to add to end.
		      }
	
		  public void add(  // Add at index. 
		  		final int indexI, final DataNode childDataNode 
		  		)
		    /* This adds childDataNode at index position indexI,
		      or the end of the list if indexI<0. 
		      It also informs theDataTreeModel about the insertion
		      so that TreeModelListeners can be fired.
	  	    */
		    {
        	appLogger.debug("MutableList.add(..) "+childDataNode+" at "+indexI);
		  		final DataNode parentDataNode= this;
  	  		runOrInvokeAndWaitV( // Queuing add to AWT queue. 
        		new Runnable() {

        			@Override  
              public void run() {
              	int insertAtI= (indexI < 0) ? // Converting index < 0 
              		theListOfDataNodes.size() : // to mean end of list,
              	  indexI; // otherwise use raw index.
                theListOfDataNodes.add( insertAtI, childDataNode );
                theDataTreeModel.reportingInsertV( 
    		      		parentDataNode, insertAtI, childDataNode 
    		      		);
                }

              } 
            );
		      }
	
		  public void remove( final DataNode childDataNode )
			  /* This is a convenience method which removes childDataNode 
			    from the list.  It searches the List first.
			    If it is not found then it does nothing.

			    It also informs theDataTreeModel of the removal
			    so TreeModelListeners can be informed.
			    */
		    {
		  	  final DataNode parentDataNode= this;
		  	  runOrInvokeAndWaitV( // Queuing removal of Peer from List.
        		new Runnable() {
              @Override  
              public void run() { 
      		  	  int indexI=  // Searching for child by getting its position index. 
      			    	  getIndexOfChild( childDataNode );
      		    	  if ( indexI != -1) { // Removing and reporting change if child found.
      				  		theListOfDataNodes.remove( indexI );
      				      theDataTreeModel.reportingRemoveV( 
      				      	parentDataNode, indexI, childDataNode 
      				      	);
      		    	  	}
                }  
              } 
            );
		      }

    } // MutableList 
