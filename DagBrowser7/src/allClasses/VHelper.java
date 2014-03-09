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
