package allClasses;

import javax.swing.tree.TreePath;

import static allClasses.Globals.*;  // For appLogger;

public class MetaRoot {

  // This class manages the root of the MetaNode-s structure.  

  // Dependency Injection variables.
    DataRoot theDataRoot; // Data with which this meta-data is associated.
    MetaFileManager theMetaFileManager; // For loading and saving meta-data.
 
  // Other instance variables. 
    private MetaNode rootMetaNode;  /* Root of tree which 
      holds DAG information.  */
    private MetaNode parentOfRootMetaNode;  /* Pseudo-parent of root.
      This is the same tree as rootMetaNode, but can be used as
      a sentinel record to eliminate checking for null during
      MetaPath traversals toward the root.  */
    private MetaPath parentOfRootMetaPath;  /* MetaPath associated with
      parentOfRootMetaNode.  */

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
      appLogger.info( "MetaRoot constructor starting.");

      this.theDataRoot= theDataRoot;
      this.theMetaFileManager= theMetaFileManager;
      }

  public void initializeV( )
    /* This method does initialization beyond simple assignment
      and dependency injection done by constructors.
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
      calculateInnerDependenciesV( );
      }

  private void calculateInnerDependenciesV( )
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

    /* ?? Maybe add these, though maybe with different names:

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

    private PathAttributeMetaTool makePathAttributeMetaTool( 
        TreePath InTreePath, String InKeyString 
        )
      {
        return new PathAttributeMetaTool( 
          this, InTreePath, InKeyString 
          );
        }

  // Code from Selection class.

    /* This code came from static Selection class.
      It helps to manage DataNode selections.  
      Selections are identified with TreePath-s of DataNodes.

      Past selections are stored as DataNode meta-data in the MetaNode DAG,
      which is a structure which parallels a subset of the DataNode DAG.
      This meta-data is useful for reselecting 
      previously selected DataNode-s and their children.
      When a GoToChild command is given at a DataNode,
      instead of selecting the first child DataNode,
      selection meta-data is used to reselect 
      the most recently selected child, if there is one.

      Note, because in the Infogora app 
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

    final static String selectionAttributeString= // Key String to use.
      "SelectionPath"; 

    // Getter methods.  These read from the MetaNode DAG.

      public TreePath buildAttributeTreePath( String KeyString )
        /* This method returns path information from the MetaNode DAG.
          It returns a TreePath comprised of all the DataNodes
          from the MetaNode's which contain attributes 
          with a key of keyString and a value of "IS".
          It does not consider UnknownDataNode-s to be part of the path
          even if they have the desired attribute
          because it is an unusable value.
          At least the root must have an "IS" attribute value,
          otherwise it will return Dataroot.getParentOfRootTreePath(),
          which is a sentinel value which can not for
          anything but a termination marker.
          */
        {
          TreePath scanTreePath=  // Point scanTreePath accumulator...
          		theDataRoot.getParentOfRootTreePath( );  // ...to parent of root.
          MetaNode scanMetaNode=  // Get root MetaNode.
            getParentOfRootMetaNode( );
          scanner: while (true) { // Scan all nodes with "IS".
            MetaNode childMetaNode= // Test for a child with "IS" value.
              scanMetaNode.getChildWithAttributeMetaNode( KeyString, "IS" );
            if  // scanMetaNode has no child with "IS" attribute value.
              ( childMetaNode == null)
              break scanner;  // Exit Processor.
            DataNode theDataNode= // Get associated DataNode.
              childMetaNode.getDataNode();
            if // DataNode is an UnknownDataNode.
              ( ! AbDataNode.isUsableB( theDataNode ) )
              break scanner;  // Exit Processor.
            scanTreePath=  // Add DataNode to TreePath.
              scanTreePath.pathByAddingChild( theDataNode );
            scanMetaNode= childMetaNode;  // Point to next MetaNode.
            } // Scan all nodes with "IS".
          return scanTreePath;  // Return accumulated TreePath.
          }

      public MetaNode getLastSelectedChildMetaNode
        ( MetaNode inMetaNode )
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

      public DataNode getLastSelectedChildDataNode
        ( MetaNode inMetaNode )
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

    // Static getter methods.  These read from the MetaNode DAG.

      public TreePath buildAttributeTreePath( )
        /* This method returns a TreePath of DataNodes which 
          identifies the current DataNode selection.
          The path is built from the sequence of DataNodes
          associated with the MetaNode's which have attributes 
          with key == "SelectionPath" and value == "IS",
          starting at the MetaNode DAG root.
          This method always returns a non-null TreePath.
          It also never returns Dataroot.getParentOfRootTreePath(),
          which is the sentinel representing the empty TreePath.
          The TreePath returned always contains at least the root node.
          */
        { 
          TreePath resultTreePath= // Calculating tentative result...
            buildAttributeTreePath( // ...path built...
              MetaRoot.selectionAttributeString // ...from selection attribute nodes.
              );
          if  // Replacing with root path if result path was empty.
            ( resultTreePath == theDataRoot.getParentOfRootTreePath() )
            resultTreePath= theDataRoot.getRootTreePath();
          return resultTreePath;
          }
          
      public void set( TreePath inTreePath )
        /* This does the same as putAndReturnDataNode(.) except 
          it doesn't return the anything.
          It exists mainly to help other code be self-documenting.
          */
        {
      	  setAndReturnMetaNode( inTreePath ); // Update with TreePath.
          }

      public DataNode setAndReturnDataNode( TreePath inTreePath )
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
            setAndReturnMetaNode(  // ...updating tree with...
              inTreePath  // ...the provided TreePath.
              );
          DataNode childDataNode=  // Get that last MetaNode's...
            getLastSelectedChildDataNode(  // ...last selected child DataNode.
              endOfPathMetaNode );
              
          return childDataNode;  // Return the resulting child DataNode.
          }

      private MetaNode setAndReturnMetaNode( TreePath inTreePath )
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
