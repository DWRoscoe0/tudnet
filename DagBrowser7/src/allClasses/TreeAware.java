package allClasses;

//import javax.swing.JComponent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

public interface TreeAware

	{
    // Note,, default method implementations coming in Java 8 interfaces!

    // TreeHelper pass-through methods.  See class TreeHelper for details.
  
      public void addTreeSelectionListener( TreeSelectionListener listener );
        // This is so the DagBrowserPanel can listen to the right sub-pael.

      public TreePath getWholeTreePath();  // This method returns the TreePath...
        // ...of the Whole node being displayed.

      public TreePath getPartTreePath();  // This method returns the TreePath...
        // ...of the Part of the Whole being displayed.
    }
  