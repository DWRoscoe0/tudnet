package allClasses;

//import javax.swing.JComponent;
//import javax.swing.event.TreeSelectionListener;
//import javax.swing.tree.TreePath;

public interface TreeAware

  /* These are the things a Viewer needs to know to be used
    for viewing it's type of DataNode in the Infogora DAG.
    Such a Viewer needs to know how to do many things.

    * Most of them can be done by a base or extended TreeHelper objected.
      For that there is getTreeHelper().
      
    * It also needs to select based on a TreePath.
      This is done by a TreePathListener.
      
    * ???
    
    */

	{
    // Note,, default method implementations coming in Java 8 interfaces!

    // TreeHelper pass-through methods.  See class TreeHelper for details.
  
      public TreeHelper getTreeHelper();
        // Returns the TreeHelper object which can do TreePath things.

      //public void addTreePathListener( TreePathListener listener );
        // This is so the DagBrowserPanel can listen to the right sub-pael.

      //public TreePath getWholeTreePath();  // This method returns the TreePath...
        // ...of the Whole node being displayed.

      //public TreePath getPartTreePath();  // This method returns the TreePath...
        // ...of the Part of the Whole being displayed.
    }
  