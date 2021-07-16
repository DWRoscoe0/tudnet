package allClasses;

import static allClasses.AppLog.theAppLog;

import javax.swing.tree.TreePath;

public class DataRoot {

  /* This class manages the root of the Data DAG.  
    In addition to providing access to the root of the DAG,
    it also provides a pseudo-root, which is the parent of the root,
    which can be used as a sentinel to make code which 
    manipulates TreePath-s simpler, for example
    less checking for null references.

    Some of the variables that were in this section were removed when
    it was realized that the JTree class can hide only
    one TreePath sentinel level, which is done by calling
    JTree.setRootVisible( false ).  There is no easy way to
    hide the first two levels instead of only the first level.
    */

  // Variables.

    private DataNode rootDataNode; // Root node of data DAG.

    private DataNode parentOfRootDataNode; // Parent of root node.

    private TreePath parentOfRootTreePath; // Path to parent node.

    private TreePath rootTreePath; // Path to root node.

  DataRoot( )  // Non-injecting constructor. 
	  {
	    }

  // Injector methods.

  public void initializeDataNodesV( 
      DataNode rootDataNode, DataTreeModel theDataTreeModel 
      )
    /* This method sets the root data node to rootDataNode and
      adjusts all dependent variables.
      Doing this correctly is tricky and a little confusing because:
      * DataNode-s are built right to left, 
        with parents referencing their children.
      * TreePath-s:
        * Are built left to right, from a new child and
          a smaller TreePath referencing all the child's ancestors.
        * Some of the TreePath constructors provided by Java which
          could make the following code more self-documenting
          are not public and can not be used here.
      This method also propagates theDataTreeModel into the structure
      starting with the parent of the root, so that all present nodes
      and all nodes added to the tree later, will have it.
      */
    { 
      this.rootDataNode= rootDataNode;  // Setting root DataNode.

      parentOfRootDataNode= // Calculating parent of root node...
        new SingleChildDataNode(   // to be single child parent of...
          rootDataNode // ..root node.
          );

      // Use propagateDownV(..) to store theDataTreeModel in structure.
      parentOfRootDataNode.propagateIntoSubtreeV(  
          theDataTreeModel // This will be copied to all non-leaf nodes.
          ); 

      parentOfRootTreePath= // Calculating path to parent...
        new TreePath(   // ...to be TreePath consisting of only...
          parentOfRootDataNode  // ...the parent node.
          );

      rootTreePath= // Calculating path to root...
        parentOfRootTreePath.  // ...to be the TreePath to parent...
          pathByAddingChild(  // ...and adding...
            rootDataNode  // ...the root node.
            );
      }

  public void finalizeDataNodesV()
    /* This method finalizes the DataNode tree by finalizing the root DataNode.  */
    { 
      theAppLog.info("DataRoot.finalizeDataNodesV() begins.");
      int nodeTotalI= rootDataNode.finalizeDataNodesI();
      theAppLog.info(
          "DataRoot.finalizeDataNodesV() ends, total nodes finalized=" + nodeTotalI);
      }

  // Other methods.

    public DataNode getRootDataNode( )
      { return rootDataNode; }

    public DataNode getParentOfRootDataNode( )
      { return parentOfRootDataNode; }

    public TreePath getParentOfRootTreePath( )
      { return parentOfRootTreePath; }

    public TreePath getRootTreePath( )
      { return rootTreePath; }

    public boolean isLegalB( TreePath aTreePath)
      /* This method is used to test whether a TreePath is legal
        to be accessed for viewing.  For that it must be non-null
        and not the TreePath of the sentinel parent of the root.
        equals(..) is used instead of == because aTreePath
        might not have been constructed by this class,
        for example it might have come from JTree.
        */
      { 
        return 
          (aTreePath != null) 
          && (! aTreePath.equals( getParentOfRootTreePath( ) ) )
          ; 
        }

  }
