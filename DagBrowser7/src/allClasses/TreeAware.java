package allClasses;

//import java.beans.PropertyChangeListener;

public interface TreeAware
  extends TreePathListener

  /* This interface describes what a JComponent must implement
    to integrate it into the Infogora's DataNode DAG browser.
    These are the things a Viewer needs to know to be used
    for viewing it's type of DataNode in the Infogora DAG.
    Such a Viewer needs to know how to do many things.

    * Most of them can be done by a base or extended TreeHelper object.
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

    }
  