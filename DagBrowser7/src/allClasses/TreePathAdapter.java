package allClasses;

public abstract class TreePathAdapter 
  implements TreePathListener
  {
    /* This class provides default methods for TreePathListener.
      */

    public boolean testPartTreePathB(TreePathEvent theTreePathEvent)
      /* This method tests whether theTreePathEvent is legal in
        the current display context.  It is implemented only by
        coordinators, such as DagBrowserPanel.
        */
      { 
        return true;  // Default behavior which won't interfere.
        }

    public void setPartTreePathV(TreePathEvent theTreePathEvent)
      /* This method informs the listener that the source
        is changing the TreePath of the Part that it is displaying,
        and the listener should adjust its state appropriately.
        
        ?? Maybe this method should be called called 
        setTreePathB..) or settingTreePathB(,,).
        */
      { 
        }

    }
