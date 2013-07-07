package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public interface DataNode 

  /* This interface forms the basis of the classes which represent
    the DAG (Directec Acyclic Graph). 
    Many of its methods are similar to methods in the TreeModel interface.
    */

  { // interface DataNode.

    public boolean isLeaf( ); /* Returns indication of whether 
      this is a leaf node,
      which means it can never have children.
      */

    public int getChildCount( );  /* Returns the number of children 
      this node has.  */

    public DataNode getChild( int IndexI );  /* Returns the child node 
      whose index is IndexI.
      Valid index values are 0 through the child count -1.
      If IndexI is out of this range then null is returned instead.
      */

    public int getIndexOfChild( Object ChildObject );  /* Returns the index of 
      child ChildObject.  If ChildObject is not one of the children
      then -1 is returned.  */

    
  // Additional, non-tree methods.

    public int getIndexOfNamedChild( String InString );
      /* Returns the index of the child whose name is InString,
        or -1 if this node's has no such child. 
        It assumes a functional getChild(.) method.  */

    public DataNode getNamedChildDataNode( String InString );
        /* Returns the child DataNode whose name is InNameString.
          If no such child exists, then it can:
            return null, or
            return a dummy DataNode to represent a past child,
              for example an IFile representing a deleted file.
          This method is used for reading from the MetaFile.  */

    public String GetInfoString( );  /* Returns a String containing 
      human-readable information about this DataNode.  */

    public String GetNameString( );  /* Returns a String which is the name 
      of the node.  When possible this should be something which 
      can be used as part of a path-name and which
      identifies this node distinctly from its siblings.
      */

    public JComponent GetDataJComponent( 
      TreePath InTreePath, TreeModel InTreeModel );
      /* Returns a JComponent capable of displaying this DataNode.  
        using the TreeModel InTreeModel to provide context.  
        This ignores the TreeModel for DataNode subclasses
        which do not yet support it.
        */

  } // interface DataNode.
