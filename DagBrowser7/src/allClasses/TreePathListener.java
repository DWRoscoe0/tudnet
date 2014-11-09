package allClasses;

//import javax.swing.event.TreeSelectionEvent;

public interface TreePathListener 

  /* Classes that need to know when a TreeAware's Part TreePath has changed
    should implement this Listener.

    ??? Presently it passes TreePath information in a TreeSelectionEvent,
    but this should probably be changed to use its own event,
    a TreePathEvent, eventually.
    The Event doesn't actually need to have a TreePath,
    because that information could be gotten by interogating
    the TreeHelper that is the source of the Event.
    */

  {

    boolean testPartTreePathB(TreePathEvent theTreePathEvent);
      /* This method tests whether theTreePathEvent is legal in
        the current display context.  It is implemented only by
        coordinators, such as DagBrowserPanel.
        */

    void setPartTreePathV( TreePathEvent theTreePathEvent );
      /* This TreePathListener method get the TreePath in
        theTreePathEvent which came from a TreeHelper 
        and does something with it, such as making a selection
        in a JComponent associated with that TreePath.
        It ignores any paths with which it cannot deal.
        */

    }
