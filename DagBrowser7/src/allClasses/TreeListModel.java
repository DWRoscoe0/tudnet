package allClasses;

//import static allClasses.Globals.appLogger;

import javax.swing.AbstractListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
//import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TreeListModel

	extends AbstractListModel<DataNode>

  implements TreeModelListener
  
  /* This class implements a ListModel which gets its data from
    a DataNode using context from a DataTreeModel.
    It also listens for TreeModelEvents and translates them to
    an equivalent sequence of ListModelEvents.
    
    Because this is a Java GUI class,
    its methods should only be called on EDT thread. 

    ///? Should this class be generic on <Object> like its base class?
      Class AbstractListModel<E> is parameterized.
    */

  { // class TreeListModel

	  // Injected dependency variables.
		private final DataNode theListDataNode;  //Node in tree with list data.
	  private final TreePath theTreePath;  // TreePath to that node.

	  // Other instance variables.
	  private DataTreeModel theDataTreeModel;  // Model of tree containing node.
	  private int lastSizeI; /* Last known size of list.
      This variable is used to save the size of the List so that
      treeStructureChanged(..) will know how big an interval to use
      when it calls fireContentsChanges(..).
      Unfortunately it needs to be saved from every place where
      the number of elements in the list might have changed.
      The reason getSize() can not be used instead is because that returns
      the size after the change had already happened.
      
      ///enh A more robust way of dealing with this problem might be to
      create an array of nodes mirroring the child nodes from the TreeModel,
      adjusting this array in response to change events from the TreeModel,
      and use this array as the basis for this ListModel. 
      */

    TreeListModel(  // Constructor.
    		DataNode theListDataNode, TreePath theTreePath
    		)
      {
	      this.theListDataNode= theListDataNode;
	      this.theTreePath= theTreePath;
	  		saveSizeV();
        }

    // Initialization/setter methods.

      public DataTreeModel setDataTreeModel(DataTreeModel newDataTreeModel)
	      /* This method sets a new DataTreeModel to be newDataTreeModel.
	        This was added to help plug Listener leaks.
          If this ListModel is a TreeModelListener of the old DataTreeModel
          then it removes itself from its listener list.
          It adds itself to the listener list of the new DataTreeModel.
          
	      	This method returns the old DataTreeModel.
	      	In normal use it will be called only twice:
	      	* once during initialization with newDataTreeModel != null,
	      	* and once during finalization with newDataTreeModel == null,
	      	but it should be able to work with any sequence.
          */
	      {
      	  DataTreeModel oldDataTreeModel= this.theDataTreeModel;

      	  if ( oldDataTreeModel != null )
	          oldDataTreeModel.removeTreeModelListener( this ); 

	    	  if ( newDataTreeModel != null )
	    	  	newDataTreeModel.addTreeModelListener( this );

	    	  this.theDataTreeModel= newDataTreeModel;

		  		saveSizeV();
	        return oldDataTreeModel;
	    	  }

    // ListModel interface methods.
	    
	    @Override
	    public DataNode getElementAt(int indexI)
	      {
	        return (DataNode) theDataTreeModel.getChild( theListDataNode, indexI );
	        }
	
	    @Override
	    public int getSize()
	      /* This is being called from the EDT when theDataTreeModel is null,
	        apparently after it is finalized.
	        For now catch it and return 0.
	        ///fix Eventually figure out why it's happening.
	       */
	      {
	    	  if ( theDataTreeModel == null )
		    	  {
		    		  //appLogger.debug("TreeListModel.getSize(): null pointer!");
		    		  return 0;
		    	    }
		    	  else
		        return theDataTreeModel.getChildCount( theListDataNode ); 
	        }

    /* Each of these TreeModelListener interface methods translates 
      a TreeModelListener method call with a TreeModelEvent into
      one or more ListDataListener method calls with ListDataEvents,
      but only when the TreeModelEvent is about the subset of nodes
      covered by this TreeListModel.
      The translation is one-to-many because
      a TreeModelEvent contains an array of child indexes of interest,
      but a ListDataEvent contains an interval of child indexes. 
      */

	    public void treeNodesInserted(TreeModelEvent theTreeModelEvent)
	      /* Translates theTreeModelEvent reporting an insertion into 
	        an equivalent ListDataEvent and notifies the ListDataListeners.
	        */
	      {
	    		//appLogger.debug("TreeListModel.treeNodesInserted(..)");
	    	  if ( // Ignoring event if it doesn't have our TreePath. 
	    	      	!theTreePath.equals(theTreeModelEvent.getTreePath())
	    	      	)
	    	  	; // Doing nothing.
	    	  	else
	    	  	{ // Sending an equivalent ListModelEvent for each child in array.
	    	  		for (int childI: theTreeModelEvent.getChildIndices()) {
	    	  			fireIntervalAdded( 
		    	  				ListDataEvent.INTERVAL_ADDED,
		    	  		  	childI,
		    	  		  	childI
		    	  		  	);
	    	  		  }
	    	  		}
		  		saveSizeV();
	    	  }

	    public void treeNodesRemoved(TreeModelEvent theTreeModelEvent)
	      /* Translates theTreeModelEvent reporting a removal into 
		      an equivalent ListDataEvent and notifies the ListDataListeners.
	        */
	      {
	    		//appLogger.debug("TreeListModel.treeNodesRemoved(..)");
	    	  if ( // Ignoring event if it doesn't have our TreePath. 
	    	      	!theTreePath.equals(theTreeModelEvent.getTreePath())
	    	      	)
	    	  	; // Doing nothing.
	    	  	else
            { // Sending an equivalent ListModelEvent for each child in array.
	    	  		for (int childI: theTreeModelEvent.getChildIndices()) {
	    	  			fireIntervalRemoved( 
		    	  				ListDataEvent.INTERVAL_REMOVED,
		    	  		  	childI,
		    	  		  	childI
		    	  		  	);
	    	  		  }
	    	  		}
		  		saveSizeV();
	    	  }

	    public void treeNodesChanged(TreeModelEvent theTreeModelEvent) 
	      /* Translates theTreeModelEvent reporting a DataNode change into 
		      an equivalent ListDataEvent and notifies the ListDataListeners.
		      */
	      {
	    		//appLogger.debug("TreeListModel.treeNodesChanged(..)");
	    	  if ( // Ignoring event if parent doesn't have our TreePath. 
	    	      	!theTreePath.equals(theTreeModelEvent.getTreePath())
	    	      	)
	    	  	; // Doing nothing.
	    	  	else
            { // Sending an equivalent ListModelEvent for each child in array.
	    	  		for (int childI: theTreeModelEvent.getChildIndices()) {
	    	  			fireContentsChanged(
		    	  				ListDataEvent.CONTENTS_CHANGED,
		    	  		  	childI,
		    	  		  	childI
		    	  		  	);
	    	  		  }
	    	  		}
		  		saveSizeV();
	    	  }

	    public void treeStructureChanged(TreeModelEvent theTreeModelEvent) 
	      /* This method is implemented by calling fireContentsChanged(..)
	        for an interval which is the entire last known size of the List. 
	        
	        Implementing this by signaling removal of all elements,
	        then inserting all new ones would eliminate the need for lastSizeI
	        because one would still need to know it to know how many to remove.
	        */
	      {
		  		fireContentsChanged(
	  				ListDataEvent.CONTENTS_CHANGED,
	  		  	0,
	  		  	lastSizeI - 1
	  		  	);
		  		saveSizeV();
	     		}

	    private void saveSizeV()
		    {
		    	lastSizeI= getSize(); 
		    	}

    } // class TreeListModel
