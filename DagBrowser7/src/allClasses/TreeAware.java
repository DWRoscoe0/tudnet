package allClasses;

public interface TreeAware

  /* This interface describes what a JComponent must implement
    to integrate it into the Infogora's DataNode DAG browser.
    These are the things a Viewer needs to know to be used
    for viewing it's type of DataNode in the Infogora DAG.
    Such a Viewer needs to know how to do many things.

    * Most of them can be done by a base or extended TreeHelper object.
      For that there is getTreeHelper() which returns the TreeHelper
      associated with the JComponent.
      
    * It also needs to select a child from the displayed children
      based on a TreePath.  This is done by a TreePathListener.
      
    * ???
    
    */

	{
    // Note, default method implementations coming in Java 8 interfaces!

    // TreeHelper pass-through methods.  See class TreeHelper for details.
  
      public TreeHelper getTreeHelper();
        /* Returns the TreeHelper object associated with the JComponent
          which can do TreePath things.
          */

    }
  