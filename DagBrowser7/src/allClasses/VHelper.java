package allClasses;

import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

public interface VHelper 
  { // interface VHelper

    // ViewHelper pass-through methods.
  
      public TreePath GetSelectedChildTreePath();
        /* This method returns the TreePath representing the selected position. 
          It is called by only DagBrowserPanel.focusGained(.).  
          */
  
      // public void NotifyTreeSelectionListenersV( boolean InternalB );
        /* Notifies any TreeSelectionListeners
          of the currently selected TreePath.
          For efficienty it should be called only when
          the selected TreePath has actually changed.
          InternalB has the following meanings:
            true: the selection is within the same DataNode as the previous 
              selection, so it can be handled by this DagNodeViewer.
            false: the selection is to be handled by a new DagNodeViewer.
          */

      public void addTreeSelectionListener( TreeSelectionListener listener );
        
      public void SetSelectedChildTreePath(TreePath InSelectedChildTreePath);
        /* This method stores, but does not select, the TreePath 
          representing the selected position. 
          So no Listener is activated by this routine.
          That must be done by fireValueChanged(.).
          
          It might be a good idea to rewrite existing code so that
          selection is always the last thing done,
          and combine this method with NotifyTreeSelectionListenersV(.).
          */

    } // interface VHelper
