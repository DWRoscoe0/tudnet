package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public interface DataNode {

  /* This interface forms the basis of the classes 
    which represent the DAG (Directed Acyclic Graph). 
    Many of its methods are similar to methods in 
    the DataTreeModel interface.

    ??? Possile new subclasses:

      ??? Maybe create a subclass CachedDataNode.
		    * It maintains an array of all names of all its children,
		      for fast counts and name searches.
		    * It caches the actual child nodes for fast child getChild(..).

    ??? Possible methods to add:

		  ??? Add JComponent getSummaryJComponent() which returns 
		    a component, such as a Label, which summarizes this DataNode,
		    at least with the name, but possibly also with a summary value,
		    can be used as a part of its parent DataJComponent ???
		  
		  ??? Add String getSummaryString() which returns a String,
		    which summarizes this DataNode, similar to what
		    getSummaryJComponent() does.
		    
		    ??? Maybe use AbDataNode.getHeadString() for this?
		    It's otherwise unused.
	    
    */

  // Methods with equivalents in DataTreeModel, all getters or testers.

    public boolean isLeaf( ); /* Returns true if this is a leaf node,
      which means it can never have children.
      Returns false otherwise.
      */

    public int getChildCount( );  /* Returns the number of children 
      in this node.  */

    public DataNode getChild( int indexI );  /* Returns the child DataNode 
      whose index is indexI if in the range 0 through getChildCount() - 1.
      If IndexI is out of this range then it returns null.
      */

    public int getIndexOfChild( Object childObject ); /* Returns the index of 
      child childObject.  If childObject is not one of the children 
      of this DataNode then it returns -1.  */

  // Methods which return Strings about the node.

    public String getNameString( );  /* Returns the name of the DataNode
      as a String.  This should be an identifying String.
      It will be used as part of a path-name and which
      identifies this DataNode distinctly from its siblings.
      */

    public String getInfoString( );  /* Returns human-readable information
      about this DataNode as a String.  */

  // Other methods.

    public JComponent getDataJComponent( 
      TreePath InTreePath, DataTreeModel InDataTreeModel 
      ); 
      /* Returns a JComponent capable of displaying this DataNode
        and using the DataTreeModel InDataTreeModel to provide context.  
        The DataTreeModel might be ignored.
        */
 
    public int getIndexOfNamedChild( String inString );
      /* Returns the index of the child whose name is inString,
        or -1 if this DataNode has no such child. 
        It assumes and uses a fully functional getChild(.) method.  
        */

    public DataNode getNamedChildDataNode( String inString );
      /* Returns the child DataNode whose name is inString.
          If no such child exists then it returns null.
          This method is used for reading from the MetaFile
          and translating a name into a DataNode subclass.
          */
  } // interface DataNode.
