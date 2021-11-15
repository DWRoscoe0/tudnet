package allClasses;

class MetaPath  // Name reassigned.  MetaTool replaced it.

  /* This class represents a path in a tree or DAG of MetaNode-s
    similar to how the Java TreePath class represents a path
    in a tree of nodes that contain Object-s.
    */
      
  { // class MetaPath.

    // Instance variables.
    
      private final MetaNode LastMetaNode;  // Last MetaNode in MetaPath.
      private final MetaPath ParentMetaPath;  // Parent MetaPath.
      private final int PathCountI;  // Number of MetaNodes in path.
      
    // Constructors.
    
      public MetaPath( MetaPath InParentMetaPath, MetaNode InLastMetaNode )
        /* This constructor constructs a new PataPath from
          the parent metaPath and the MetaNode it will reference.
          */
        { // MetaPath( TreePath InTreePath )
          LastMetaNode= InLastMetaNode;
          ParentMetaPath= InParentMetaPath;
          if ( ParentMetaPath == null )  // Calculate length of new path.
            PathCountI= 1;
            else
            PathCountI= ParentMetaPath.getPathCountI( ) + 1;
          } // MetaPath( TreePath InTreePath )
          
    // Instance methods.

      public int getPathCountI( )
        { return PathCountI; }

      public MetaPath getParentMetaPath( )
        { return ParentMetaPath; }

      public MetaNode getLastMetaNode( )
        { return LastMetaNode; }

    } // class MetaPath.
