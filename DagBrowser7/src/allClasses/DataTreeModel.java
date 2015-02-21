package allClasses;

import java.io.File;
import java.io.Serializable;
//import java.util.LinkedList;
//import java.util.Queue;

import java.util.LinkedList;
import java.util.Queue;

import javax.swing.event.TreeModelEvent;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;

//import static allClasses.Globals.*;  // appLogger;

public class DataTreeModel 

  extends AbstractTreeModel

  implements TreeModel, Serializable
  
  /* This class implements an extended TreeModel 
    used for browsing the Infogora.

    One of the more important extensions is
    a method getDataJComponent( TreePath inTreePath )
    which returns a JComponent capable of displaying the node.

    ??? Repeat what was done with 
    	TitledListViewer and TreeListModel to report ConnectionManager changes
    with 
	    DirectoryTableViewer and DirectoryTableModel to report
	    file-system changes using Java Watchable, WatchService, WatchEvent, 
	    and WatchKey.

    ?? One understandable drawback of the TreeModel is
    its inability to deal with the general Directed Graph,
    or even the Directed Acyclic Graph, in which
    a node can have multiple parents.
    This is  desirable feature of the Infogora Hierarchy
    so a solution needs to be found.
    */

  { // class DataTreeModel.

    // Constants.

        private static final String spacyFileSeperator= 
          " "+File.separator+" ";

    // Injected dependency variables.

        private DataRoot theDataRoot;
        private MetaRoot theMetaRoot;
        private MetaFileManager.Finisher theMetaFileManagerFinisher; 
        private Shutdowner theShutdowner;

    // constructor methods.

        public DataTreeModel( 
            DataRoot theDataRoot, 
            MetaRoot theMetaRoot, 
            MetaFileManager.Finisher theMetaFileManagerFinisher,
            Shutdowner theShutdowner
            )
          {
            this.theDataRoot= theDataRoot;
            this.theMetaRoot= theMetaRoot;
            this.theMetaFileManagerFinisher= theMetaFileManagerFinisher;
            this.theShutdowner= theShutdowner;
            }

    // AbstractTreeModel/TreeModel interface methods.

      /* The following getter methods simply delegate to 
        the parentObject, which is assumed to be a DataNode,
        and can perform the operation context-free.
        This is because this TreeModel does no filtering.
        If and when filtering is done then 
        other processing will be needed here.
        */

      public Object getRoot() 
        /* Returns tree root.  
          This is not the Infogora root DataNode.
          It is the parent of the root.
          See DataRoot for more information.
          */
        {
          return theDataRoot.getParentOfRootDataNode();
          }

      public Object getChild( Object parentObject, int IndexI ) 
        /* Returns the Object which is the child of parentObject
          whose index is IndexI.  The child must exist.
          This operation is delegated to parentObject which
          is assumed to satisfy the DataNode interface.
          */
        {
          return ((DataNode)parentObject).getChild( IndexI );
          }

      public boolean isLeaf( Object NodeObject ) 
        /* Returns an indication whether NodeObject is a leaf.
          This operation is delegated to NodeObject which
          is assumed to satisfy the DataNode interface.
          */
        {
          return ((DataNode)NodeObject).isLeaf();
          }

      public int getChildCount( Object parentObject ) 
        /* Returns the number of children of the parentObject.
          This operation is delegated to parentObject which
          is assumed to satisfy the DataNode interface.
          */
        {
          return ((DataNode)parentObject).getChildCount();
          }

      public int getIndexOfChild( 
          Object parentObject, Object childObject 
          ) 
        /* Returns the index of childObject in parentObject.
          This operation is delegated to parentObject which
          is assumed to satisfy the DataNode interface.
          */
        {
          return ((DataNode)parentObject).
            getIndexOfChild( childObject ); 
          }

      public void valueForPathChanged( 
          TreePath theTreePath, Object newValueObject 
          )
        /* Do-nothing stub to satisfy interface.  */
        { }

    // Setter methods which are not part of AbstractTreeModel.

      public void initializeV( 
      		DataNode theInitialDataNode 
      		)
        /* This is code that couldn't [easily] be done at injection time.
          It initializes the DataRoot and 
          tries to load the MetaRoot from external file(s).
          theMetaFileManagerFinisher is used to set a shutdown listener
          in theShutdowner to write MetaRoot back to disk if it was changed.
          */
        {
          theDataRoot.setRootV( theInitialDataNode );
          theMetaRoot.initializeV( );
          theShutdowner.addShutdownerListener( theMetaFileManagerFinisher );
          }

      // Getter methods which are not part of AbstractTreeModel.

      public MetaRoot getMetaRoot()
        {
          return theMetaRoot;
          }

      public JComponent getDataJComponent( TreePath inTreePath )
        /* Returns a JComponent which is appropriate for 
          viewing and possibly manipulating
          the current tree node specified by inTreePath.  
          */
        {
          DataNode InDataNode= // extract...
            (DataNode)  // ...user Object...
            inTreePath.getLastPathComponent();  // ...from the TreePath.
          JComponent ResultJComponent; 
          try { 
            ResultJComponent= 
              InDataNode.getDataJComponent( 
                inTreePath, this 
                );
            }
          catch ( IllegalArgumentException e) {
            ResultJComponent= null;  
            ResultJComponent=  // calculate a blank JLabel with message.
              new TitledTextViewer( inTreePath, this, "getDataJComponent : "+e );
            }
          return ResultJComponent;
          }

      public String getNameString( Object theObject )
        /* Returns a String representing the name of theObject,
          or null if theObject is null.
          This operation is delegated to theObject which
          is assumed to satisfy the DataNode interface.
          */
        {
          String resultString;
          if ( theObject != null )
            resultString= ((DataNode)theObject).getNameString();
            else
            resultString= "NULL-DataTreeModel-Object";
          return resultString;
          }

      public String getLastComponentNameString(TreePath inTreePath)
        /* Returns String representation of the name of 
          the last element of inTreePath.
          */
        {
          DataNode lastDataNode= 
            (DataNode)(inTreePath.getLastPathComponent());
          String TheNameString= 
            lastDataNode.getNameString();
          return TheNameString;
          }

      public String getAbsolutePathString(TreePath inTreePath)
        /* Returns String representation of TreePath inTreePath.  
          ??? Maybe rewrite to be recursive so that 
          java-optimized += String operation can be used,
          maybe with StringBuilder with an initial capacity based on
          previous value of path String being calculated.
          */
        { // GetAbsolutePathString(.)
          String resultString= "";  // Initializing resultString to empty.
          while (true) {  // While more TreePath to process...
            if // Handling detection of illegal null TreePath terminator.
              ( inTreePath == null )
              { resultString+= "NULL-PATH-TERMINATOR";
                break;
                }
            DataNode lastDataNode=  // Getting last path element.
              (DataNode)(inTreePath.getLastPathComponent());
            if // Handling detection of normal TreePath root sentinel.
              ( theDataRoot.getParentOfRootTreePath( ).equals( inTreePath ) )
              { if // Handling illegal sentinel-only TreePath.
                  ( resultString == "" ) 
                  resultString= lastDataNode.getNameString();
                break;
                }
            String headString=  // Making head string be path element name.
              lastDataNode.getNameString();
            if  // Appending separator to headString if needed,...
              ( resultString.length() != 0)  // ...if result String is not empty...
              headString+=  // ...by appending to the head String...
                spacyFileSeperator; // ...the File separator string.
            resultString=  // Prepending...
              headString +  // ...the head String...
              resultString;  // ...to the resultString.
            inTreePath=  // Replacing the TreePath...
              inTreePath.getParentPath();  // ...by its parent TreePath.                
            }
          resultString=  // Prepend...
            "  " +  // ...some space for looks..
            resultString;  // ...to the resultString.
          return resultString;  // Return completed resultString.
          } // GetAbsolutePathString(.)

      public String getInfoString(TreePath inTreePath)
        /* Returns a String representing information about 
          TreePath inTreePath. 
          */
        {
          DataNode lastDataNode= 
            (DataNode)(inTreePath.getLastPathComponent());
          return lastDataNode.getInfoString();
          }

      public void reportingInsertV( 
          DataNode parentDataNode, 
          int indexI, 
          DataNode childDataNode 
          )
        /* This method creates and triggers a single-child TreeModelEvent
          for the insertion a single child DataNode, childDataNode,
          into parentDataNode at position indexI.
          */
        {
          TreePath parentTreePath= // Calculating path of parent. 
          	translatingToTreePath( parentDataNode );
          
          TreeModelEvent theTreeModelEvent= // Construct TreeModelEvent.
            new TreeModelEvent(
            	this, 
            	parentTreePath, 
            	new int[] {indexI}, 
            	new Object[] {childDataNode}
            	);

          fireTreeNodesInserted( theTreeModelEvent ); // Firing insertion event.
          }

      public void reportingRemoveV( 
          DataNode parentDataNode, 
          int indexI, 
          DataNode childDataNode 
          )
      	/* This method creates and triggers a single-child TreeModelEvent
          for the removal of a single child DataNode, childDataNode,
          whose previous position was indexI, into parentDataNode.
          */
        {
          TreePath parentTreePath= // Calculating path of parent. 
          	translatingToTreePath( parentDataNode );
          
          TreeModelEvent theTreeModelEvent= // Constructing TreeModelEvent.
            new TreeModelEvent(
            	this, 
            	parentTreePath, 
            	new int[] {indexI}, 
            	new Object[] {childDataNode}
            	);

          fireTreeNodesRemoved( theTreeModelEvent ); // Firing as removal event.
          }

      public void reportingChangeV( 
          DataNode theDataNode
          )
      	/* This method creates and triggers a single-child TreeModelEvent
          for the change of a single child DataNode, theDataNode,
          whose position is indexI, into parentDataNode.
          */
        {
	        TreePath theTreePath= // Calculating path of the DataNode. 
	          	translatingToTreePath( theDataNode );
	        TreePath parentTreePath= // Calculating path of the parent DataNode. 
	            theTreePath.getParentPath();
	        DataNode parentDataNode= // Calculating parent DataNode.
	        		(DataNode)parentTreePath.getLastPathComponent();
	        int indexI= getIndexOfChild( // Calculating index of the DataNode.
	        		parentDataNode, 
	        		theDataNode 
	        		); 
          
          TreeModelEvent theTreeModelEvent= // Constructing TreeModelEvent.
            new TreeModelEvent(
            	this, 
            	parentTreePath, 
            	new int[] {indexI}, 
            	new Object[] {theDataNode}
            	);

          fireTreeNodesChanged( theTreeModelEvent );  // Firing as change event.
          }

      private TreePath translatingToTreePath( DataNode targetDataNode )
        /* This method returns a TreePath of targetDataNode,
          or null if none can not be found in the DataNode tree.
          It does this with a breadth-first search of 
          the DataNode tree from the root,
          building candidate TreePaths as it goes,
          and returning the first TreePath that ends in targetDataNode.

          This method could be speeded by caching the search result???
          This can be enhanced later to handle duplicate references.
          See Object hashCode() and equals() for HashTable requirements.
          */
        {
          //appLogger.info( "DataTreeModel.translatingToTreePath()." );
      		TreePath resultTreePath= null;  // Defaulting result to null.
      		Queue<TreePath> queueOfTreePath = new LinkedList<TreePath>();
          queueOfTreePath.add(theDataRoot.getParentOfRootTreePath( ));
            // Placing root into the initially empty queue.
          queueScanner: while (true) // Searching queue of parent TreePaths.
            { // Searching one parent TreePath.
          		TreePath parentTreePath = queueOfTreePath.poll(); 
          	  if ( parentTreePath == null) break queueScanner;
          	    // Exiting if queue empty.
            	DataNode parentDataNode= 
            		(DataNode)parentTreePath.getLastPathComponent(); 
              for // Checking children.
                ( int childIndexI=0; ; childIndexI++ ) 
                { 
                	DataNode childDataNode= 
                	  parentDataNode.getChild(childIndexI);
              	  if ( childDataNode == null) break;
              	    // Exiting if no more children.
                  TreePath childTreePath=
                    parentTreePath.pathByAddingChild( childDataNode ); 
                  if // Returning result TreePath if target node found.
                    ( childDataNode == targetDataNode )
                  	{ resultTreePath= childTreePath; break queueScanner; }
                 	queueOfTreePath.add( childTreePath );
                  }
              }
          return resultTreePath;
          }
      
    } // class DataTreeModel.
