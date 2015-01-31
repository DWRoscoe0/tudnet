package allClasses;

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
			
	    MutableList (   // Constructor.
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

		  public void add( DataNode childDataNode ) // Add child at end of List.
		    /* This is a convenience method which adds childDataNode 
		      at end of the List.

			    It also informs theDataTreeModel so that TreeModelListeners
			    can be informed.

		      It might be better to maintain and add to a sorted List???
		      There is none, so a sorted Set might be better.
		      */
		    {
		      add( theListOfDataNodes.size(), childDataNode );
		      }
	
		  public void add( int indexI, DataNode childDataNode ) // Add at index.
		    /* This adds childDataNode at the specified indexI position

		      It also informs theDataTreeModel so that TreeModelListeners
		      can be informed.
	  	    */
		    {
		      theListOfDataNodes.add( indexI, childDataNode );
	
		      theDataTreeModel.reportingInsertV( this, indexI, childDataNode );
		      }
	
		  public void remove( DataNode childDataNode )
			  /* This is a convenience method which removes childDataNode 
			    from the list.  It searches the List first.
			    If it is not found then it does nothing.

			    It also informs theDataTreeModel of the removal
			    so TreeModelListeners can be informed.
			    */
		    {
		  	  int indexI=  // Searching for child by getting its position index. 
		    	  getIndexOfChild( childDataNode );
	    	  if ( indexI != -1) { // Removing and reporting change if child found.
			  		theListOfDataNodes.remove( indexI );
			      theDataTreeModel.reportingRemoveV( 
			      	this, indexI, childDataNode 
			      	);
	    	  	}
		      }
    } // MutableList 
