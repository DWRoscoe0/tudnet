package allClasses;

import static allClasses.AppLog.theAppLog;

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
    private int lastSizeI= 0; /* Last known size of list.
      This variable is used to save the size of the List so that
      treeStructureChanged(..) will know how big an interval to use
      when it calls fireContentsChanged(..).
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
        }

    // Initialization/setter methods.

      public void setDataTreeModelV(DataTreeModel newDataTreeModel)
        /* This method is similar to TreeHelper's method of the same name. 
          It sets a new DataTreeModel by storing it in a TreeHelper variable,
          and it adjusts TreeModelListener registrations.
          The purpose is to allow this ListModel to use tree node children
          as the source of its data, and to know when it changes.
    
          First it adjusts TreeModelListener registrations.
          If the present DataTreeModel is not null,
          then this TreeListModel is a listener of that model,
          so it unregisters it.
          Next, if newDataTreeModel is not null,
          then this TreeListModel is registered as 
          a listener of the newDataTreeModel.
    
          After listener registrations are done, the newDataTreeModel 
          is stored in a variable as the present DataTreeModel.
    
          In normal usage this method should be called only twice:
          * once with newDataTreeModel != null, during initialization,
          * and once with newDataTreeModel == null during finalization,
            mainly to prevent TreeModelListener memory leaks.
          However it should be able to be called any number of times
          with any of 4 null/not-null combinations.
          */
        {
          DataTreeModel oldDataTreeModel= this.theDataTreeModel; // Save old model.
          this.theDataTreeModel= newDataTreeModel; // Store new model.

          // Adjust listener registrations.
          if ( oldDataTreeModel != null ) {
            oldDataTreeModel.removeTreeModelListener( this );
            saveSizeV(0); // Reset to zero the last saved size because model removed.
            }
          if ( newDataTreeModel != null ) {
            newDataTreeModel.addTreeModelListener( this );
            saveSizeV(); // Update last size from new model.
            }
          }

    /* Each of these ListModel interface methods translates
      into calls to TreeModel interface methods using the context of
      the present tree node children as list elements.
      */
      
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
              theAppLog.debug("TreeListModel.getSize(): null TreeModel!");
              return 0;
              }
            else
            return theDataTreeModel.getChildCount( theListDataNode ); 
          }

    /* Each of these TreeModelListener interface methods translates 
      a TreeModelListener method call with a TreeModelEvent into
      one or more ListDataListener method calls with ListDataEvents,
      but only when the TreeModelEvent is about 
      the subset of child nodes covered by this TreeListModel.
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

    // Miscellaneous.

      private void saveSizeV()
        /* Saves last known list size.  See lastSizeI for more information.  */
        { saveSizeV( getSize() ); }

      private void saveSizeV(int sizeI)
        /* Saves sizeI as last known list size.  See lastSizeI for more information.  */
        { lastSizeI= sizeI; }

    } // class TreeListModel
