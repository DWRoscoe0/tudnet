package allClasses;

import javax.swing.tree.TreePath;


public class MetaRoot {

  /* This class holds the MetaNode-s tree root, and some Path selection code:
   * 
   * * Code which is here and should be here is code for
   *   managing the MetaNode tree, including holding its root,
   *   and doing loading and saving of that tree in file storage.
   *   
   * * Code which is here but should NOT be here is code for
   * 

   *   
   */

  // Dependency Injection variables.
    DataRoot theDataRoot; // Data with which the  meta-data is associated.
    MetaFileManager theMetaFileManager; // For loading and saving in files.
 
  // Other instance variables. 
    private MetaNode rootMetaNode;  // Root of MetaNode tree.
    private MetaNode parentOfRootMetaNode;  /* Pseudo-parent of root.
      This is the same tree as rootMetaNode, but can be used as
      a sentinel record to eliminate checking for null during
      MetaPath traversals toward the root.  */
    private MetaPath parentOfRootMetaPath;  // Path of pseudo-parent of root.

  MetaRoot(  // Constructor.
      DataRoot theDataRoot, 
      MetaFileManager theMetaFileManager 
      )
    /* The need for theDataRoot should be obvious.
      theMetaFileManager is a dependency also because
      it is used in the construction of MetaNodes
      so that their children can be lazy-loaded.
      */
    {
      //appLogger.info( "MetaRoot constructor starting.");

      this.theDataRoot= theDataRoot;
      this.theMetaFileManager= theMetaFileManager;
      }

  public void initializeV( )
    /* This method does initialization beyond 
      the and dependency injection assignments done by the constructor.
      It loads the MetaRoot data from the external MetaFile(s),
      replacing whatever MetaRoot data is active now.
      It will load at least the root node.
      Other nodes might be lazy-loaded later if needed.
      */
    { 
      MetaNode loadedMetaNode=  // Trying to load new MetaNode DAG state...
        theMetaFileManager.start();  // ...from from MetaFile.
      if // Calculating rootMetaNode based on load result.
        ( ( loadedMetaNode != null) && // Meta-data was loaded, and
          !UnknownDataNode.isOneB( // load error. 
          		loadedMetaNode.getDataNode( ) 
          		)
          )
        {
          rootMetaNode= // Storing as root MetaNode
            loadedMetaNode;  // the loaded MetaNode.
          }
        else
        rootMetaNode= // Storing as root MetaNode
          theMetaFileManager.makeMetaNode( // a single MetaNode referencing
            theDataRoot.getRootDataNode( ) // the root DataNode.
            );
      initializeInnerDependenciesV( );
      }

  private void initializeInnerDependenciesV( )
    /* This method calculates the state that depends on
      the root MetaNode.
      */
    { 
      parentOfRootMetaNode= // Making parent of root MetaNode be...
        theMetaFileManager.makeSingleChildMetaNode( // ...a MetaNode whose...
          rootMetaNode, // ...one-child is root MetaNode and whose object is...
          theDataRoot.getParentOfRootDataNode( ) // ...parent of root DataNode.
          );
          
      parentOfRootMetaPath=  // Making parentOfRootMetaPath be...
        new MetaPath( // ...MetaPath to parent node.
          null, parentOfRootMetaNode 
          );

      rootMetaNode.put(  // Forcing Selection attribute on Root.
        selectionAttributeString, "IS"
        ); // This guarantees success of buildAttributeTreePath( ).
          // This is compatible with both loaded and non-loaded meta data,
          // because the root node should always be part of the selection path.
      }

  // Methods.

    public DataRoot getTheDataRoot( )  // ?? Use separate DataRoot injection?
      { return theDataRoot; }

    public MetaNode getRootMetaNode( )
      { return rootMetaNode; }

    public MetaNode getParentOfRootMetaNode( )
      { return parentOfRootMetaNode; }

    public MetaPath getParentOfRootMetaPath( )
      { return parentOfRootMetaPath; }

    /* ?? Maybe add these methods, though maybe with different names:

      public boolean isRootB( MetaNode )

      public boolean isParentOfRootB( MetaNode )

      public boolean isParentOfRootB( MetaPath ) or isNullB(..)

      */

  // Factory methods, so users don't need to use new operator.

    public BooleanAttributeMetaTool makeBooleanAttributeMetaTool( 
        TreePath InTreePath, String InKeyString 
        )
      { 
        return new BooleanAttributeMetaTool(
          this, InTreePath, InKeyString
          ); 
        }

    public PathAttributeMetaTool makePathAttributeMetaTool( 
        TreePath InTreePath, String InKeyString 
        )
      {
        return new PathAttributeMetaTool( 
          this, InTreePath, InKeyString 
          );
        }



    /* The following code came from the old static Selection class.
      ///org  It should probably not be here and should instead
      be in a new class called SelectionAttributeMetaTool.

      This code is used to manage the user's Swing GUI selection path,
      mainly for the JTree component, using attribute key "SelectionPath".
      See class AttributeMetaTool for information about attribute encoding.

      Selections are identified with TreePath-s of DataNodes.
      Past selections are stored as DataNode meta-data in the MetaNode DAG,
      which is a structure which parallels a subset of the DataNode DAG.
      This meta-data is useful for reselecting 
      previously selected DataNode-s and their children.
      When a GoToChild command is given at a DataNode,
      instead of selecting the first child DataNode,
      selection meta-data is used to reselect 
      the most recently selected child, if there is one.

      Note, because in the TUDNet app 
      the selection in the right pane is normally 
      a child of the selection in the left pane,
      and because sometimes actual DataNodes are deleted or moved,
      a selection might point to a non-existent node.
      In these cases a TreePath might be created which
      ends in a special node called an UnknownDataNode.

      Originally selection history information was stored as 
      one MRU/LRU lists of children in each MetaNode.  
      Now it's stored in MetaNode attributes with key "SelectionPath".
      */

    static final String selectionAttributeString= // Key String to use.
        "SelectionPath"; 

    // Getter methods.  These read from the MetaNode DAG.

      public TreePath getSelectionPathTreePath( )
        /* This method returns a TreePath of DataNodes which 
          identify the user's current DataNode selection.
          This is the TreePath associated with "SelectionPath".
          This method always returns a non-null TreePath.
          It also never returns Dataroot.getParentOfRootTreePath(),
          which is the sentinel representing the empty TreePath.
          The TreePath returned always contains at least the root node.
          */
        { 
          TreePath resultTreePath= // Calculating tentative result
            getAttributeTreePath( // path built from
              MetaRoot.selectionAttributeString // selection attribute nodes.
              );
          if  // Replacing with root path if result path was empty.
            ( resultTreePath == theDataRoot.getParentOfRootTreePath() )
            resultTreePath= theDataRoot.getRootTreePath();
          return resultTreePath;
          }

      public TreePath getAttributeTreePath( String keyString )
        /* This method returns path information from the MetaNode DAG.
          It returns a TreePath comprised of all the DataNodes
          from the MetaNode's which contain attributes 
          with a key of keyString and a value of "IS".
          It does not consider UnknownDataNode-s to be part of the path
          even if they have the desired attribute
          because it is an unusable value.
          At least the root must have an "IS" attribute value,
          otherwise it will return Dataroot.getParentOfRootTreePath(),
          which is a sentinel value which can not used for
          anything but a termination marker.
          
          ?? This probably doesn't belong with the Selection methods
          because it takes a keyString for any type of path.
          */
        {
          TreePath scanTreePath=  // Point scanTreePath accumulator...
          		theDataRoot.getParentOfRootTreePath( );  // ...to parent of root.
          MetaNode scanMetaNode=  // Get root MetaNode.
            getParentOfRootMetaNode( );
          scanner: while (true) { // Scan all nodes with "IS".
            MetaNode childMetaNode= // Test for a child with "IS" value.
              scanMetaNode.getChildWithAttributeMetaNode( keyString, "IS" );
            if  // scanMetaNode has no child with "IS" attribute value.
              ( childMetaNode == null)
              break scanner;  // Exit Processor.
            DataNode theDataNode= // Get associated DataNode.
              childMetaNode.getDataNode();
            if // DataNode is an UnknownDataNode.
            	( ! DataNode.isAUsableDataNodeB( theDataNode ) )
              break scanner;  // Exit Processor.
            scanTreePath=  // Add DataNode to TreePath.
              scanTreePath.pathByAddingChild( theDataNode );
            scanMetaNode= childMetaNode;  // Point to next MetaNode.
            } // Scan all nodes with "IS".
          return scanTreePath;  // Return accumulated TreePath.
          }

      public MetaNode getLastSelectedChildMetaNode( MetaNode inMetaNode )
        /* This method returns the child MetaNode that was selected last
          of the parent node inMetaNode. 
          If no child MetaNode has the attribute then null is returned.
          It does this by searching for the child with an attribute
          with key == "SelectionPath" and value == "WAS".
          */
        {
          MetaNode childMetaNode= // Test for a child with "WAS" value.
            inMetaNode.getChildWithAttributeMetaNode( 
              MetaRoot.selectionAttributeString, 
              "WAS" 
              );
          return childMetaNode; // Return last child MetaNode result, if any.
          }

      public DataNode getLastSelectedChildDataNode( MetaNode inMetaNode )
        /* This method gets the user object DataNode from
          the child MetaNode in inMetaNode 
          which was selected last, or null if there isn't one.
          It also returns null if the Child DataNode
          appears to be an UnknownDataNode,
          because that is an unusable value.
          */
        {
          DataNode resultChildDataNode=  // Assume default result of null.
            null;
          process: { // Override result with child if there is one.
            MetaNode lastChildMetaNode= 
              getLastSelectedChildMetaNode( inMetaNode );
            if // there is no last selected child.
              (lastChildMetaNode == null)
              break process;  // So exit and keep the default null result.

            resultChildDataNode=  // Result recent child DataNode is...
              lastChildMetaNode.   // ...the last child's...
              getDataNode();  // user object.
            if // Result child DataNode is not an UnknownDataNode.
              ( ! UnknownDataNode.isOneB( resultChildDataNode ) )
              break process;  // Exit with that okay result.

            resultChildDataNode= null; // Replace unusable value with null.
            } // Override result with child if there is one.

          return resultChildDataNode; // return resulting DataNode, or null if none.
          }

    // Setter methods.  These write to the MetaNode DAG.

      public void set( TreePath inTreePath )
        /* This does the same as putAndReturnDataNode(.) except 
          it doesn't return anything.
          It exists mainly to help method-calling code be self-documenting.
          */
        {
      	  setSelectionPathAndReturnMetaNode( // Update with TreePath. 
      	      inTreePath );
          }

      public DataNode setSelectionPathAndReturnDataNode( TreePath inTreePath )
        /* Updates the "SelectionPath" attributes of the MetaNode DAG
          starting with the root and ending at 
          the MetaNode specified by inTreePath.
          Then it returns the DataNode of the most recently 
          selected/visited child MetaNode of 
          the MetaNode at the end of that path,
          or it returns null if there is no such child. 
          */
        {
          MetaNode endOfPathMetaNode=  // Get last MetaNode in path by...
            setSelectionPathAndReturnMetaNode(  // ...updating tree with...
              inTreePath  // ...the provided TreePath.
              );
          DataNode childDataNode=  // Get that last MetaNode's...
            getLastSelectedChildDataNode(  // ...last selected child DataNode.
              endOfPathMetaNode );
              
          return childDataNode;  // Return the resulting child DataNode.
          }

      private MetaNode setSelectionPathAndReturnMetaNode( TreePath inTreePath )
        /* Updates the "SelectionPath" attributes of the MetaNode DAG
          starting with the root and ending at 
          the MetaNode specified by inTreePath.
          If it needs to then it adds MetaNodes 
          to the DAG in the appropriate places.
          It returns the MetaNode at the end of the specified TreePath.
          */
        {
          PathAttributeMetaTool workerPathAttributeMetaTool= 
            makePathAttributeMetaTool( // Create PathAttributeMetaTool...
              inTreePath,  // ...to work on inTreePath's...
              MetaRoot.selectionAttributeString  // ...selection path attribute.
              );
          workerPathAttributeMetaTool.setPath( );  // Set path attributes.
          return workerPathAttributeMetaTool.getMetaNode();
          }
  
  }
