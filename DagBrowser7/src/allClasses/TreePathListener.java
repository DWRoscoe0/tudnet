package allClasses;

import javax.swing.event.TreeSelectionEvent;

public interface TreePathListener 

  /* Classes that need to know when a TreeAware's TreePath has change
    should implement this Listener.
    Presently it passes TreePath information in a TreeSelectionEvent,
    but this should probably be changed to a new TreePathEvent eventually.
    */

  {

    public void partTreeChangedV( TreeSelectionEvent theTreeSelectionEvent );

    }
