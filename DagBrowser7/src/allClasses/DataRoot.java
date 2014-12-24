package allClasses;

import javax.swing.tree.TreePath;

public class DataRoot {

  /* This class manages the root of the Data DAG.  
    In addition to providing access to the root of the DAG,
    it also provides a pseudo-root, which is the parent of the root,
    which can be used as a sentinel to make code which 
    manipulates TreePath-s simpler, for example
    less checking for null references.
    
    ??? Statics are in the process of being eliminated from this class.
    */

  // Variables.

    /* Initializing these variables is tricky and a little confusing because:
      * DataNode-s are built right to left, 
        with parents referencing their children.
      * TreePath-s:
        * Are built left to right, from a new child and
          a smaller TreePath referencing all the child's ancestors.
        * Some of the TreePath constructors provided by Java which
          could make the following code more self-documenting
          are not public and can not be referenced here.

      Some of the variables that were in this section were removed when
      it was realized that the JTree class can make use of only
      one TreePath sentinel level, which is done with 
      JTree.setRootVisible( false ).  There is no easy way to
      hide the first two levels instead of only the first.

      */

    private DataNode rootDataNode; // Root node of data DAG.

    private DataNode parentOfRootDataNode; // Parent of root node.

    private TreePath parentOfRootTreePath; // Path to parent node.

    private TreePath rootTreePath; // Path to root node.

  private static DataRoot theDataRoot; /// Temporary for static access.

  DataRoot( DataNode rootDataNode ) {  // Constructor.
    theDataRoot= this;  /// Temporary for static access.

    this.rootDataNode= rootDataNode;

    parentOfRootDataNode= // Setting parent of root node...
      new SingleChildDataNode(   // to be single child parent of...
        rootDataNode // ..root node.
        );

    parentOfRootTreePath= // Setting path to parent...
      new TreePath(   // ...to be TreePath consisting of only...
        parentOfRootDataNode  // ...the parent node.
        );

    rootTreePath= // Setting path to root...
      parentOfRootTreePath.  // ...to be the TreePath to parent...
        pathByAddingChild(  // ...and adding...
          rootDataNode  // ...the root node.
          );
    }

  public static DataRoot getIt() { /// Temporary for static access.
    return theDataRoot;
    }
  
  // Methods.

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
          && (! aTreePath.equals( theDataRoot.getParentOfRootTreePath( ) ) )
          ; 
        }

  }
