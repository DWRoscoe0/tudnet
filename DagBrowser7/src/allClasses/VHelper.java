package allClasses;

//import javax.swing.JComponent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

public interface VHelper 
  //extends JComponent

	{
    // Note,, default method implementations coming in Java 8 interfaces!

    // ViewHelper pass-through methods.
  
      public void addTreeSelectionListener( TreeSelectionListener listener );

      public TreePath getSubjectTreePath();
        // This method returns the TreePath representing the subject being displayed.

      public TreePath getSelectionTreePath();
        // This method returns the TreePath representing the selection in the subject.
    }
  