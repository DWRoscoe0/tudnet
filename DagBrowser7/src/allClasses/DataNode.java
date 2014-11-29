package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public interface DataNode {

  /* This interface forms the basis of the classes 
    which represent the DAG (Directed Acyclic Graph). 
    Many of its methods are similar to methods in the TreeModel interface.
    
    ??? Normalize field names.
    */

  // Methods with equivalents in TreeModel.

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

  // Additional methods not directly related to TreeModel.

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

    public String GetInfoString( );  /* Returns human-readable information
      about this DataNode as a String.  */

    public String GetNameString( );  /* Returns the name of the DataNode
      as a String.  When possible this should be something which 
      can be used as part of a path-name and which
      identifies this DataNode distinctly from its siblings.
      */

    public JComponent GetDataJComponent( 
      TreePath InTreePath, TreeModel InTreeModel );
      /* Returns a JComponent capable of displaying this DataNode
        and using the TreeModel InTreeModel to provide context.  
        The TreeModel might be ignored.
        */

    /* ??? Add JComponent getSummaryJComponent() which returns a component,
      such as a Label, which summarizes this DataNode,
      at least with the name, but possibly also with a summary value,
      can be used as a part of its parent DataJComponent ???
      */

    /* ??? Add String getSummaryString() which returns a String,
      which summarizes this DataNode, similar to what
      getSummaryJComponent() does.
      
      ??? Maybe use AbDataNode.getHeadString() for this?
      It's otherwise unused.
      */

  } // interface DataNode.
