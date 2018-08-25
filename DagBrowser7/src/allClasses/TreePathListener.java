package allClasses;

//import javax.swing.event.TreeSelectionEvent;

public interface TreePathListener 

  /* Classes that need to know when a TreeAware's Part TreePath has changed
    should implement this Listener.

    This Listener processes changes to the selection 
    within a tree that is being displayed.
    Compare this with TreeModelListener, which process
    changes to the content or structure of a tree being displayed.

    ///rev The TreePathEvent received by this listener
    doesn't actually need to have a TreePath in it
    because that information could be gotten by interrogating
    the TreeHelper that is the source of the Event.
    */

  {

    boolean testPartTreePathB(TreePathEvent theTreePathEvent);
      /* This method tests whether theTreePathEvent is legal in
        the current display context.  It is implemented only by
        coordinators, such as DagBrowserPanel.
        */

    void setPartTreePathV( TreePathEvent theTreePathEvent );
      /* This TreePathListener method gets the TreePath in
        theTreePathEvent which came from a TreeHelper 
        and does something with it, such as making a selection
        in a JComponent associated with that TreePath.
        It ignores any paths with which it cannot deal.
        */

    }
