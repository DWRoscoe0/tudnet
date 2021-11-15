package allClasses;

import javax.swing.tree.TreePath;

public class TreePathEvent extends TestEvent {

  /* This class is for passing TreePath-s,
    usually for the purpose of making a selection
    in a tree or tree node viewer.

    ?? It was made an extension of a TestEvent so
    it could be used to test whether a TreePath
    is legal in a particular context.
    However testability is now being added by having
    a separate listener method, so extending TestEvent
    for this will no longer be necessary.
    It might eventually be removed.
    */
    
  private TreePath theTreePath;

	public TreePathEvent(
    Object source, TreePath inTreePath, boolean inDoB)
	{
	  super(source,inDoB);
    theTreePath= inTreePath;
	  }

	public TreePathEvent(
    Object source, TreePath inTreePath)
	{
	  super(source,true);
    theTreePath= inTreePath;
	  }

  public void setTreePathV(TreePath inTreePath)
    {
      theTreePath= inTreePath;
      }

  public TreePath getTreePath()
    {
      return theTreePath;
      }
}
