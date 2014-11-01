package allClasses;

import javax.swing.tree.TreePath;

public class TreePathEvent extends TestEvent {

  /* This class is for passing TreePath-s,
    usually for the purpose of making a selection
    in a tree or tree node viewer.
    It is an extension of a TestEvent so
    it can be used to test whether a TreePath
    is legal in a particular context.
    */
    
  private TreePath theTreePath;

	public TreePathEvent(
    Object source, boolean inDoB, TreePath inTreePath)
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
