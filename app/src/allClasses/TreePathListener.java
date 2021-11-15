package allClasses;

//import javax.swing.event.TreeSelectionEvent;

public interface TreePathListener 

  /* Classes that need to know when a TreePath has changed
    should implement this Listener.  
    These classes are typically displaying part or parts of a tree, 
    those parts being identified by a TreePath.
    It must respond when that TreePath changes
    by displaying a different part or parts. 

    Compare this with TreeModelListener, which responds to
    changes in the content or structure of a tree being displayed,
    not the location within the tree.

    This class is similar to Java's TreeSelectionLListener, but
    it PathListener is simpler because it communicates 
    only single node selections, but TreeSelectionListener 
    communicates multiple simultaneous node selections.

    ///rev The TreePathEvent received by this listener
    doesn't actually need to have a TreePath in it
    because that information could be gotten by interrogating 
    the source of the event.
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
