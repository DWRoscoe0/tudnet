package allClasses;

public interface TreeAware

  /* A TreeAware object is a JComponent subclass that implements the TreeAware interface.
    The TreeAware interface contains only one method, the purpose of which 
    is to return the JComponent's TreeHelper object.
    
    A TreeHelper is an object that helps a JComponent deal with
    the Infogora DAG as a tree.  For more information,
    see the TreeHelper class.
    */

	{

    public TreeHelper getTreeHelper(); 
      /* Returns the TreeHelper object associated with this object. */
    
	  }
  