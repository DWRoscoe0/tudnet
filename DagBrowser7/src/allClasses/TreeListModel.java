package allClasses;

//import static allClasses.Globals.appLogger;

import javax.swing.AbstractListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
//import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TreeListModel

  //% extends AbstractListModel<Object> 
	extends AbstractListModel<DataNode>

  implements TreeModelListener
  
  /* This class implements a ListModel which gets its data from
    a DataNode using context from a DataTreeModel.
    
    ////// Should this class be generic on <Object> like its base class?
      Class AbstractListModel<E> is parameterized.
    */

  { // class TreeListModel

	  // Injected dependency variables.
    //% private final Object theListDataNode;  //Node in tree with source data.
		private final DataNode theListDataNode;  //Node in tree with list data. //////
	  private final TreePath theTreePath;  // Path to that node.

	  // Other instance variables.
	  private DataTreeModel theDataTreeModel;  // Model of tree containing node.

    TreeListModel(  // Constructor.
    		//% Object theListDataNode, TreePath theTreePath
    		DataNode theListDataNode, TreePath theTreePath  //////
    		)
      {
	      this.theListDataNode= theListDataNode;
	      this.theTreePath= theTreePath;
        }

    // Initialization/setter methods.

      public DataTreeModel setDataTreeModel(DataTreeModel newDataTreeModel)
	      /* Sets new DataTreeModel.  
	        This was added to help plug Listener leaks.
          If this ListModel is a TreeModelListener of the old DataTreeModel
          then it removes itself from its listener list.
          It adds itself to the listener list of the new DataTreeModel.
	      	It returns the old DataTreeModel.
	      	In normal use it will be called only twice:
	      	* once with newDataTreeModel != null,
	      	* and once with newDataTreeModel == null,
	      	but it should be able to work with any sequence.
          */
	      {
      	  DataTreeModel oldDataTreeModel= this.theDataTreeModel;

      	  if ( oldDataTreeModel != null )
	          oldDataTreeModel.removeTreeModelListener( this ); 

	    	  if ( newDataTreeModel != null )
	    	  	newDataTreeModel.addTreeModelListener( this );

	    	  this.theDataTreeModel= newDataTreeModel;

	        return oldDataTreeModel;
	    	  }

    // ListModel interface methods.
	    
	    @Override
	    //% public Object getElementAt(int indexI) 
	    public DataNode getElementAt(int indexI)
	      {
	        return (DataNode) theDataTreeModel.getChild( theListDataNode, indexI ); ////////
	        }
	
	    @Override
	    public int getSize()
	      /* ?? This is being called from the EDT when theDataTreeModel is null,
	       * apparently after it is finalized.
	       * For now catch it and return 0.
	       * Eventually figure out why it's happening.
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
      a ListDataListener method call with a ListDataEvent,
      but only when the TreeModelEvent is about the subset of nodes
      covered by this TreeListModel.
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
	    	  	{ // Sending an equivalent ListModelEvent for each child.
	    	  		for (int childI: theTreeModelEvent.getChildIndices()) {
	    	  			fireIntervalAdded( 
		    	  				ListDataEvent.INTERVAL_ADDED,
		    	  		  	childI,
		    	  		  	childI
		    	  		  	);
	    	  		  }
	    	  		}
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
	    	  	{ // Sending an equivalent ListModelEvent.
	    	  		for (int childI: theTreeModelEvent.getChildIndices()) {
	    	  			fireIntervalRemoved( 
		    	  				ListDataEvent.INTERVAL_REMOVED,
		    	  		  	childI,
		    	  		  	childI
		    	  		  	);
	    	  		  }
	    	  		}
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
	    	  	{ // Firing an equivalent ListModelEvent.
	    	  		for (int childI: theTreeModelEvent.getChildIndices()) {
	    	  			fireContentsChanged(
		    	  				ListDataEvent.CONTENTS_CHANGED,
		    	  		  	childI,
		    	  		  	childI
		    	  		  	);
	    	  		  }
	    	  		}
	    	  }

	    public void treeStructureChanged(TreeModelEvent theTreeModelEvent) 
	      /* This method is ignored because there is 
	        no ListDataListener equivalent.
	        */
	       {}

    } // class TreeListModel
