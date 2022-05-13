package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import allClasses.AppLog.LogLevel;
import allClasses.epinode.MapEpiNode;
import allClasses.javafx.EpiTreeItem;
import allClasses.javafx.Selections;
import allClasses.javafx.TitledListNode;
import allClasses.javafx.TitledTextNode;
import allClasses.javafx.TreeStuff;
import allClasses.multilink.ElementMultiLink;

public abstract class DataNode

  extends Object

  implements ElementMultiLink<DataNode>

  {

    /* This class is the base class of all classes in the DataNode hierarchy.

      Presently it does not support full DAG capabilities.
      A node can have only one parent, so the hierarchy is a tree.  
      Someday this could change.

      Some of the methods in this class are 
      non working stubs and MUST be overridden.
      Other methods in this class work as is,
      but might be inefficient in nodes with large numbers of children, 
      and SHOULD be overridden. 

      Much of the code here came from an earlier version of DataNode.

      ///opt: Remove theMaxLogLevelto reduce node storage, 
        also the methods what use it,
        because this feature hasn't been used for a long time,
        and there are probably better ways to do control logging.

      ///enh Possible methods to add:
  
        ?? Add getEpiThread() which returns the EpiThread associated with
          this DataNode, or null if there is none, which is the default.
          This could be used to standardize and simplify 
          thread starting and stopping.
           
        ?? Add JComponent getSummaryJComponent() which returns 
          a component, such as a Label, which summarizes this DataNode,
          at least with the name, but possibly also with a summary value,
          can be used as a part of its parent DataJComponent.
          See getSummaryString() which returns a String.
          This might be able to be temporary, like a Render component.
  
      */


    // Some sources of useful DataNodes.

    protected static DataNode[] makeEmptyArrayOfDataNodes()
      // This method returns a new empty DataNode array.
      { 
        return new DataNode[]{}; 
        }

    static DataNode dummyDataNode= 
        NamedLeaf.makeNamedLeaf( "DUMMY-DATA-NODE");

    // Constructors and initialization.

    public DataNode() {} // 0-argument constructor.


    // Finalization.

    protected int finalizeThisSubtreeI()
      /* This override-able method finalizes the subtree 
       * rooted at this DataNode, meaning this node and its descendants.
       * This method returns the number of DataNodes finalized.
       *
       * This version of the method assumes that the DataNode has no children, 
       * so it returns the value 1.
       *
       * The main purpose of finalization is
       * to close resources associated with DataNodes.
       * 
       * See NamedList for an override example.
       */
      {
        //theAppLog.debug("DataNode.finalizeDataNodesV() called.");

        return 1; // Return a total of 1 node for just ourself.
        }


    // Code dealing with this DataNode's parent.

    private NamedList treeParentNamedList= null; /* Parent node of this node.
      If there is a parent then the parent must be a list, hence NamedList.

      This variable has multiple uses.
      * It is used for propagation of change notifications towards the root,
        used for lazy evaluation updating of displays.
      * It is used in the first phase of translating
        a DataNode to an associated 
        * MapEpiNode or MetaNode in order to access Swing UI metadata.
        * TreeItem for JavaFX TreeViewer. 

      This variable might be replaced by a list later, 
      possibly a MultiLink instance, multiple parents are required
      if the tree DataNode hierarchy is replaced with a DAG hierarchy.

      ///opt If we eliminate StateList.parentStateList then
      // this would need to be changed to a StateList,
      // unless we allow casting of the variable.
      
      */

    public void setTreeParentToV( NamedList treeParentNamedList )
      /* This method is called when a DataNode is added to a NamedList
        or one of its subclasses.  
        This method stores treeParentNamedList within this node, 
        because every node has a parent and needs access to its parent.

        This method does NOT attempt to unlink from
        an existing parent if there is one. 
        */
      {
        this.treeParentNamedList= treeParentNamedList;
        }

    public NamedList getTreeParentNamedList()
      /* This method returns a reference to this node's tree parent node.
       * 
       * This method might be replaced by multiple methods later,
       * one for trees and one for DAGs.
       */
      {
        return treeParentNamedList;
        }


    /* DataNode hierarchy change notification system, common part.

      This is part of a system that aggregates and processes 
      DataNode change notifications from multiple threads,
      and eventually outputs changed nodes to the GUI.
      
      Communication happens through shared memory consisting of
      a Node variable named "theTreeChange" in each DataNode instance.
      A change to any node means that it and all its ancestors
      might need to be redisplayed to the GUI.
      This is an example of restricted value multitasking communication. 
      No locks are used.
      * Threads that report and aggregate node changes write
        values other than TreeChange.NONE to the variable and
        propagates these changes toward the DataNode root.
      * The thread that processes and outputs node changes to the GUI
        writes the value TreeChange.NONE to the variable
        and propagates those changes toward the leaves.

      In the Java Swing UI, this system is used for deciding when to repaint
      cells representing DataNodes in JTrees and JLists.

      In the JavaFX UI, changes output to the JavaFX UI 
      are not immediately displayed.  Changes are aggregated in
      JavaFX's own capture-and-bubble aggregation system, 
      using JavaFX Node and TreeItem instances.
      Display doesn't actually happen until JavaFX's next Pulse-time.
      ///fix This doesn't work yet.  Make it work.

      */

    public volatile TreeChange theTreeChange= TreeChange.NONE; // Initial state.
    
    public enum TreeChange /* Constants used for aggregation and notification.
        These constants are used to delay and manage the firing of 
        appearance change notification events to GUI components.
        It is based on the theory that if the appearance of a node changes,
        then it might affect the appearance of the node's ancestors,
        and the GUI display of those nodes should happen soon.
  
        Besides NONE and SUBTREE_CHANGED, which would be sufficient for
        node value changes in a tree with unchanging size and shape, 
        there is also STRUCTURE_CHANGED,
        which helps in dealing with changes in a tree's branching structure.
        STRUCTURE_CHANGED is interpreted as a need to reevaluate
        and display the entire subtree that contains it.
  
        ///org To better match the way caches are invalidated,
        theTreeChange field should probably be converted into two fields:
        * a field to indicate subtree changes
        * a field to indicate structure changes
  
        ///org A more scalable solution might be 
        to not propagate general changes 
        which might cause a GUI appearance change,
        but propagate node field change dependencies between nodes,
        then propagate fields locally into the node's GUI appearance.
        This way, invalidations would be limited to only
        attributes in nodes that were actually being displayed. 
        */
      {
        // Minimum values needed for correct operation.

        // Value Name           // Value Description

        NONE,                   // No changes here or in descendants.
                                // The cell is correct as displayed.
                                // This subtree may be ignored.

        STRUCTURE_CHANGED,      // This subtree contains major changes. 
                                // Requires structure a change event.  The node 
                                // and [all] its descendants need redisplay.

        SUBTREE_CHANGED,        // This node and possibly some of 
                                // its descendants have changed 
                                // and should be redisplayed.
                                // Requires child checking and change events.

        ///opt Other values for possible later optimizations.  Unused.
        /*   ///opt
        INSERTED,     // this node or subtree has been inserted
        INSERTED_DESCENDANTS,   // one or more children have been inserted
        CHANGED,

        NODE,         // this node changed
        CHILDREN,     // one or more of this node's children
        REMOVALS,     // one or more children have been removed
        */  ///opt
        }


    /* DataNode hierarchy change notification system, aggregation part. 

      The following code is used to aggregate changes to the
      DataNode hierarchy for later and more efficient display to the UI.
      Nodes are marked when and how they change and
      changes are propagated toward the root until
      a DataNode is encountered that has been sufficiently marked.

      Comments on code that calls these methods might indicate 
      that a call is being made to fire change listeners.
      That is still true except that the listeners 
      are no longer called immediately.
      Changes are aggregated with other changes, 
      and the listeners are fired later.

      These methods do NOT need to be called on 
      the Event Dispatch Thread (EDT) or
      the JavaFX Application Thread (JAT).
      */

    protected void signalChangeOfSelfV()
      /* This method signals that this node has changed.  */
      {
        DataNode.signalChangeOfV( this ); 
        }

    public static void signalChangeOfV( DataNode theDataNode )
      /* This method signals the change of a single DataNode, theDataNode. */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
        DataNode.signalSubtreeChangeV( // Handle as a subtree change. 
            theDataNode );
        }

    public static void signalInsertionV(
        DataNode parentDataNode, 
        int indexI, 
        DataNode childDataNode 
        )
      /* This method signals the insertion of a single child DataNode, 
        childDataNode, into theSubjectDataNode, at child position indexI.

        ///opt Implement as an Insert instead of StructuralChange
        if speed becomes an issue.
        */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
        DataNode.signalStructuralChangeInV( // Handle as a structural change.
            parentDataNode);
        }

    public static void signalRemovalV(
        DataNode parentDataNode, 
        int indexI, 
        DataNode childDataNode 
        )
      /* This method reports the removal of a single child DataNode, 
        childDataNode, from parentDataNode at child position indexI.

        ///opt Implement as a Remove instead of StructuralChange
        if speed becomes an issue.
        */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
        DataNode.signalStructuralChangeInV( // Handle as a structural change.
            parentDataNode);
        }

    static void signalStructuralChangeInV(DataNode parentDataNode)
      /* This method is used to signal
       * a structural change in parentDataNode.  
       * It is used to deal with insertions and deletions.
       */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
    
        switch // Act based on present flag value.
          ( parentDataNode.theTreeChange ) 
          { 
            case NONE: // Node is unmarked, so must mark it.
            case SUBTREE_CHANGED: // Node marked changed, so must override it.
              parentDataNode.theTreeChange= // Mark node structure changed. 
                TreeChange.STRUCTURE_CHANGED;
              signalSubtreeChangeV( // Propagate as subtree change 
                  parentDataNode.getTreeParentNamedList()); // to parent.
              break;
              
            case STRUCTURE_CHANGED: // Already marked as desired
              ; // so no additional marking is needed.
              break;
            }
        }

    static void signalSubtreeChangeV( 
        DataNode theDataNode
        )
      /* This method signals that a change happened in a subtree.
        If it needs to change the marking of theDataNode then
        it will also propagate a change to its parent.
        */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
        if ( theDataNode == null ) // No node to update 
          ; // so do nothing.
        else // Mark as changed this node and its unmarked ancestors.
          switch ( theDataNode.theTreeChange ) {
            case NONE: // Node is unmarked, so mark it and its ancestors.
              theDataNode.theTreeChange= // Mark node as having subtree changed. 
                TreeChange.SUBTREE_CHANGED;
              signalSubtreeChangeV( // Propagate as subtree change to parent. 
                  theDataNode.getTreeParentNamedList() );
              break;
              
            case STRUCTURE_CHANGED: // Already marked with structure change.
            case SUBTREE_CHANGED: // Already marked with subtree change.
              ; // So no additional marking is needed.
              break;
            }
        }


    /* DataNode hierarchy change notification system, output part. 
     *
     * These methods are used to output aggregated DataNode changes
     * to the Swing UI and the JavaFX UI.
     * 
     * Changes output to the Swing UI are displayed immediately.
     *  
     * Changes output to the JavaFX UI are NOT displayed immediately.
     * Changes are aggregated in JavaFX's own capture-and-bubble
     * aggregation system, in Nodes and TreeItems.
     * Display doesn't actually happen until JavaFX's next Pulse-time.
     * ///fix Not working yet.
     * 
     * */

    protected DataTreeModel theDataTreeModel; /* Receives change notifications.

      This variable references the DataTreeModel that is notified when
      displayable DataNodes change for the Java Swing UI.

      This variable also serves as a flag for 
      the method propagateTreeModelIntoSubtreeV(..).
      theDataTreeModel != null means down-propagation into this node
      and all its descendants has already occurred and 
      need not be done again, except for new added children. 
      */
    
    public static void outputPossiblyChangedNodesFromV(
        TreePath parentTreePath, DataNode theDataNode,EpiTreeItem theEpiTreeItem 
        )
      /* If theDataNode has changed, as indicated field by theTreeChange,
        then this method outputs the change and
        it repeats this for any children.
        The TreePath of theDataNode's parent is parentTreePath.
        theEpiTreeItem is the TreeItem whose value is theDataNode
        and is used for JavaFX TreeView display.
        */
      {
        if ( theDataNode == null ) // Nothing to display. 
          ; // Do nothing.
        else { // Check this subtree.
          theAppLog.trace( "DataTreeModel.displayChangedNodesFromV() "
              + theDataNode.getLoggingNodePathString() );
          if (! valueOfTreeItemCheckB(theEpiTreeItem, theDataNode)) return;
          // Display this node and any updated descendants if it's needed.
          switch ( theDataNode.theTreeChange ) {
            case NONE: // This subtree is unmarked.
              ; // So display nothing.
              break;
            case STRUCTURE_CHANGED: // Structure of this subtree changed.
              outputStructuralChangeV( parentTreePath, theDataNode );
              break;
            case SUBTREE_CHANGED: // Something else in this subtree changed.
              outputChangedSubtreeV( 
                  parentTreePath, theDataNode, theEpiTreeItem );
              break;
            }
          }
      }

    private static boolean valueOfTreeItemCheckB(
        EpiTreeItem theEpiTreeItem, DataNode theDataNode)
      {
        boolean successB= false;
        goReturn: {
          if (null == theEpiTreeItem) break goReturn;
          if (null == theDataNode) break goReturn;
          if (theEpiTreeItem.getValue() != theDataNode) break goReturn;
          successB= true;
          } // goReturn:
        if (! successB)
          System.out.print("[valueOfTreeItemCheckB() failed]");
        return successB;
        }

    private static void outputStructuralChangeV(
        TreePath parentTreePath, DataNode theDataNode 
        )
      /* This method displays a structural change of
        a subtree rooted at theDataNode.
        The TreePath of its parent is parentTreePath.
    
        ///dnh?  It is presently assumed that displaying a structural change
        will be handled by displaying the entire subtree.
        If this is not true then it might be necessary
        to report which nodes have changed also,
        probably after the structure change has been reported.
        It shouldn't do any harm, except maybe take extra time.
        */
      {
        resetChangesInSubtreeV( // Do this now so late changes are recorded.
            theDataNode );

        TreePath theTreePath= parentTreePath.pathByAddingChild(theDataNode);
        EDTUtilities.runOrInvokeAndWaitV( () -> { // Do on Swing EDT thread. 
          theDataNode.theDataTreeModel.reportStructuralChangeB( theTreePath );
            // Display by reporting to the listeners.
          });
        
        ///fix  Need to add JavaFX display.
        }

    private static void outputChangedSubtreeV(
      TreePath parentTreePath, 
      DataNode subtreeDataNode, 
      EpiTreeItem theEpiTreeItem
      )
    /* This method displays the subtree rooted at subtreeDataNode,
      a subtree which may have changed,
      It does this by reporting possible changes to the Java GUI.
      The descendants are displayed recursively first.
      The TreeChange status of all the nodes of any subtree displayed is reset.

      parentTreePath is the TreePath of the subtree's parent 
      and is used to identify the tree node to the Swing GUI.
      
      theEpiTreeItem is the TreeItem whose value is theDataNode
      and is used for JavaFX TreeView display.
      ///Fix : This code is not working yet.
      
      ///enh : Calculate childEpiTreeItem using DataNode search instead of
      an indexed get(.).  It's safer.
      */
    {
      theAppLog.trace( "DataTreeModel.displayChangedSubtreeV() "
          + subtreeDataNode.getLoggingNodePathString() );
      subtreeDataNode.theTreeChange= // Unmark the subtree root now, not later,
          TreeChange.NONE; // to prevent loss of changes while displaying.

      TreePath theSubtreeTreePath= 
          parentTreePath.pathByAddingChild(subtreeDataNode); 
      int childIndexI= 0;  // Initialize child scan index.
      while ( true ) // Recursively display any possibly changed descendants.
        { // Process one descendant subtree.
          DataNode childDataNode=  // Get the child, the descendant root.
             subtreeDataNode.getChild( childIndexI );
          if ( childDataNode == null )  // No more DataNode children.
              break;  // so exit while loop.
          EpiTreeItem childEpiTreeItem= // Get the child, the descendant root.
            (EpiTreeItem)theEpiTreeItem.getChildren().get(childIndexI);
          if ( childEpiTreeItem == null )  // No more EpiTreeItem children.
              break;  // so exit while loop.
          DataNode.outputPossiblyChangedNodesFromV( // Display descendants.
              theSubtreeTreePath, childDataNode, childEpiTreeItem );
          childIndexI++;  // Increment index for processing next child.
          }
      
      // Display the subtree root node to UIs.
      EDTUtilities.runOrInvokeAndWaitV( () -> { // Do on Swing EDT thread. 
        subtreeDataNode.theDataTreeModel.reportChangeB( // Display with Swing.
            parentTreePath, subtreeDataNode );
        });
      Platform.runLater(() -> { // Display with JavaFX Application Thread.
        /// theAppLog.debug("DataNode.outputChangedSubtreeV() updating TreeView.");
        /// System.out.print("[dnti]");
        DataNode savedDataNode= theEpiTreeItem.getValue();
        theEpiTreeItem.setValue(null); // Force reference change.
        theEpiTreeItem.setValue(savedDataNode); // Restore original reference.
        }); 
      }

    static void resetChangesInSubtreeV( DataNode theDataNode )
      /* This method resets the change status of 
        all the nodes in the subtree rooted at theDataNode.
        theDataNode's descendants that need resetting are reset 
        recursively first, then the status of this theDataNode is reset.
        It is assumed that if a DataNode's status is already reset,
        then the same is true of all its descendants.
        */
      {
        switch ( theDataNode.theTreeChange ) {

          case NONE: // Status is reset.
            ; // So do nothing, because all descendants should be the same.
            break;

          default: // Status is NOT reset.
            int childIndexI= 0;  // Initialize child scan index.
               while ( true ) // Recursively reset appropriate descendants.
              { // Process one child subtree.
                DataNode childDataNode=  // Get the child.
                   (DataNode)theDataNode.getChild(childIndexI);
                if ( childDataNode == null )  // Null means no more children.
                    break; // so exit while loop.
                resetChangesInSubtreeV( // Recursively reset child subtree. 
                    childDataNode ); 
                childIndexI++;  // Increment index for processing next child.
                }
            theDataNode.theTreeChange= // Reset root node status. 
                TreeChange.NONE;
            break;

          }

        }


    // Getter and tester methods [with equivalents in DataTreeModel].

    public boolean isLeaf()
      /* This method and any of its overridden version
        returns true if this is a leaf node,
        which means it can never have children.
        It returns false otherwise.
        The method of this class always returns false as the default value.
        */
      { 
        return false; 
        }

    public int getChildCount()
      /* This method returns the number of children in this node.
        The method of this class actually scans all the children that are
        visible to the method getChild(..) and counts them.  
        It assumes a functional getChild( IndexI ) method which
        returns null if IndexI is out of range.  
        */
      { // getChildCount()
        int childIndexI= 0;  // Initialize child index.

        while  // Process all children...
          ( getChild( childIndexI ) != null )  // ...returned by getChild(.).
          { // process this child.
          
            childIndexI++;  // increment index.
            } // process this child.
        return childIndexI;  // Return ending index as count.
        } // getChildCount()

    public DataNode getChild( int indexI )
      /* This method returns the child DataNode 
        whose index is indexI if in the range 0 through getChildCount() - 1.
        If IndexI is out in this range then it returns null.
        The method of this class returns null as the default value.  
        */
      {
        return null;
        }

    public int getIndexOfChild( Object inChildObject )
      /* Returns the index of child childObject.  
        If childObject is not one of this node's children then it returns -1.
        */
      { // getIndexOfChild(.)
        int childIndexI= 0;  // Initialize child search index.

        while ( true ) // Search for child.
          { // Check one child.
            Object childObject=  // get the child.
               getChild( childIndexI );

            if ( childObject == null )  // null means no more children.
              { // Exit with failure.
                childIndexI= -1;  // Set index to indicate failure.
                break;  // Exit while loop.
                } // Exit with failure.

            if ( inChildObject.equals( childObject ) )  // Found child.
              break;  // Exit while loop.

            childIndexI++;  // Increment index to check next child.
            } // Check one child.

        return childIndexI;  // Return index as search result.
        } // getIndexOfChild(.)


    // Methods which return DataNode Lists.

    public ObservableList<DataNode> getChildrenAsObservableListOfDataNodes()
      /* Returns an ObservableList containing the children of this object.
       * 
       * This method was added for use with JavaFX.
       */
      { 
        return // Return
          FXCollections.observableArrayList( // an ObservableList version of
            getChildrenAsListOfDataNodes()); // regular List of the children. 
        }

    public List<DataNode> getChildrenAsListOfDataNodes()
      /* Returns a List of the children of this object.
       * 
       * This version builds a list by adding all children to an empty list.
       * This method should be overridden if a non-leaf subclass
       * already contains its children in its own list.
       * 
       * This method was added for use with JavaFX.
       */
      { 
        List<DataNode> theList= // Create empty list. 
            new ArrayList<DataNode>();
        for // Add all the children.
          (
            int childIndexI= 0; 
            childIndexI < getChildCount();
            childIndexI++
            )
          {
            theList.add(getChild(childIndexI));
            } 
        return theList;
        }


    /* Methods which do or are related to returning Strings about this DataNode.
      These may be overridden by subclasses as needed.
      The meaning of each method should be preserved in the overrides.
      */

    public String getLoggingNodePathString()
      /* Recursively calculates and returns a display-able path to this node.
        The path is a comma-separated list of node names
        from the root of the hierarchy to this node.
        It gathers the path elements by following node parent links. 
        It is used only for logging.
       */
      {
        String resultString;
        
        if ( getTreeParentNamedList() == null )
          resultString= getNameString();
        else
          resultString= 
            getTreeParentNamedList().getLoggingNodePathString()
            + ", "
            + getNameString(); 

        Nulls.fastFailNullCheckT(resultString);
        return resultString;
        }

    public String getMetaDataString()
      /* Returns meta-data of this DataNode as a String.
        This is typically a sequence of name:value pair attributes 
        and the names of present value-less attributes.
        Meta-data is generally considered to be all associated attributes except 
        content-type attributes, which tend to be large.
        This default method returns the concatenation of the Name attribute and,
        if its value is non-blank, the Summary attribute.
        Other classes might want to override this method to return
        additional attributes.
        */
      { 
        String resultString= "Name=" + getNameString(); // Assume only Name attribute.
        String summaryString= getSummaryString();
        if (! summaryString.isEmpty()) // Append Summary attribute if value isn't blank
          resultString+= " Summary=" + summaryString;

        return resultString; 
        }

    public String toString()
      /* According the Java documentation, this is supposed to returns 
        a meaningful and useful String representation.
        Typically it returns a possibly very long string which represents 
        the entire object state, including objects referenced in fields,
        So this, in theory, is the most complex of the String-returning methods.
        But the Swing cell-rendering methods use this to get a value suitable
        for display in a cell, including JTree cells.
        So this method is defined to return a limited length string.
        This method returns a decorated cell string.
        */
      { 
        if (isDecoratingB()) // If decoration is enabled
          return decoratedCellString(); // return decorated string
          else // otherwise
          return getCellString(); // return undecorated string.
        }

    public String decoratedCellString()
      /* This method returns String appropriate for display in
        a Component cell which  might include decoration
        with other characters to indicate state information.
        This method returns the default of the undecorated cell String,
        but subclasses could decorate as desired.
        */
      {
        return getCellString();
        }

    public boolean isDecoratingB()
      /* Returns indication of whether node String decoration is enabled.
        By default it is disabled.
        Subclasses that want String decoration should override this method
        with one that returns true.
       */
      {
        return false;
        }

    public String getCellString()  
    /* Returns an undecorated String representation of this node, 
      suitable for use in rendering the node
      in the cell of a JTree, JList, or other Component.
      This version returns the node name followed by
      the Summary attribute value if it is not blank.
      */
      {
        String resultString= getNameString(); // Assume result will be name only.

        String summaryString= getSummaryString();
        if (! summaryString.isEmpty()) // Append value if there is one. 
          resultString+= " : " + summaryString;

        return resultString; 
        }

    public String getSummaryString()
      /* This method returns Summary attribute value String of the node.
        It is a calculated value that is meant to be combined 
        with the node's name and used for rendering in a Component cell.
        The String might be a value, a child count,
        or some other type of summary information.
        This default method returns the first line of 
        the Content attribute value.
        Other classes might want to override this behavior.
        */
      {
        String resultString= getContentString(); // Assume no content trimming is needed.
        int indexOfNewLineI= resultString.indexOf(NL);
        if // Trim extra lines if there are any in content string.
          ( indexOfNewLineI >= 0 )
          resultString= // Replacing string with only its first line. 
            resultString.substring(0,indexOfNewLineI);
        return resultString;
        }

    public String getContentString()
      /* Returns the content of the DataNode as a String.  
        This is potentially a long String, such as the content of a file, 
        and it might consist of multiple lines.
        This method will be overridden.
        */
      {
        return
            "UNDEFINED-CONTENT-OF-"
            + super.toString(); // Object's toString(), equivalent to: 
              // getClass().getName() + '@' + Integer.toHexString(hashCode())
        }

    public String getNameString()
      /* This method returns the name of the DataNode as a String.  
        This should be a unique String among its siblings because
        it is used to distinguish it from those siblings,
        and as part of an identifying tree path-name.  
        However, it need not be unique in 
        all the DataNodes in the DataNode hierarchy.

        This method should be overridden by a subclass method.
        If it is not then something is wrong and
        this method will produce a string similar to
        what Object.toString() produces, which is 
          getClass().getName() + '@' + Integer.toHexString(hashCode())
        but is shorter, simpler, but provides 
        sibling name uniqueness in most cases.
        See the code below.
        */
      {
        String resultString;

        resultString= // Build result String from
          DataNode.class.getSimpleName() // the simple name, it's shorter,
            + '@'  // '@',
            + Integer.toHexString( // and the hash code,
                ((Object)this) // making certain to use the Object superclass.
                  .hashCode()
              );

        return resultString;
        }


    /* Java Swing and JavaFX UI methods.
     * 
     * The app uses both the older Java Swing UI and the newer JavaFX UI.
     * The following 2 method create UI-dependent objects for doing this.
     * 
     * getDataJComponent(.) creates and returns a Java Swing JComponent 
     * that is useful for interacting with this DataNode with the Java Swing UI.
     * The JComponent uses a TreeHelper object useful for dealing with
     * the DataNode hierarchy.
     * 
     * makeTreeStuff(.) creates and returns a TreeStuff object 
     * that is useful for dealing with the DataNode hierarchy,
     * and also can return a JavaFX Node object  
     * that is useful for interacting with this DataNode in the JavaFX UI.
     */

    public JComponent getDataJComponent( 
        TreePath inTreePath, DataTreeModel inDataTreeModel 
        ) 
      /* This method returns a Java Swing UI JComponent 
        that is capable of at least displaying this DataNode.

        inTreePath must be a TreePath to this DataNode. which means
        that this DataNode must be the last element of inTreePath.

        inDataTreeModel may provide context to the JComponent.

        This base class method returns useful defaults, specifically
        * a TextViewer for DataNode leaves and 
        * a ListViewer for DataNode non-leaves.
        This base class method may be overridden if 
        a more specialized JComponent is needed, 
        or to allow the user to edit the DataNode.

        */
      {
        JComponent resultJComponent= null;

        if ( isLeaf() ) // Using TitledTextViewer if node is leaf.
          resultJComponent= // Using TitledTextViewer.
            new TitledTextViewer( 
              inTreePath, 
              inDataTreeModel, 
              getContentString()
              );
          else  // Using TitledListViewer if not a leaf.
          resultJComponent= // Using TitledListViewer.
            new TitledListViewer( inTreePath, inDataTreeModel );

        return resultJComponent;  // Returning result from above.
        }

    public TreeStuff makeTreeStuff( 
        DataNode selectedDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        EpiTreeItem theRootEpiTreeItem,
        Selections theSelections
        )
      /* This method creates a TreeStuff object that will be useful for 
       * displaying and manipulating this DataNode in the JavaFX UI.
       * * selectedDataNode is the DataNode to be the selected child data node,
       *   or null if there is to be no selected child DataNode,
       *   or the selection should come from the selection history.
       * * thePersistent provides access to persistent data storage.
       * * theDataRoot provides access to the root of the data hierarchy.
       * * theSelections provides access to the DataNode selection history.
       * 
       * This implementation creates 1 of 2 default TreeStuffs:
       * * one for displaying leaves as text
       * * one for displaying branches as a list
       * A DataNode subclass may override this method to create TreeStuffs
       * customized for doing tree operations with that particular subclass. 
       */
      {
        TreeStuff resultTreeStuff;
        if ( isLeaf() ) // Create a TreeStuff for text viewers if a leaf.
          resultTreeStuff= TitledTextNode.makeTreeStuff(
                this,
                selectedDataNode,
                getContentString(),
                thePersistent,
                theDataRoot,
                theRootEpiTreeItem,
                theSelections
                );
          else // Create a TreeStuff for list viewers if not a leaf.
          resultTreeStuff= TitledListNode.makeTreeStuff(
                this,
                selectedDataNode,
                theDataRoot,
                theRootEpiTreeItem,
                thePersistent,
                theSelections
                );
        return resultTreeStuff; // Return the view that was created.
        }

    public Color getBackgroundColor( Color defaultBackgroundColor )
      /* This method returns the background color 
        which should be used to display this DataNode.
        The default is input parameter defaultBackgroundColor,
        but this method may be overridden to return any other color 
        which is a function of class, state, or other data.
       */
      {
        return defaultBackgroundColor;
        }


    // Other functions of this DataNode.

    static boolean isAUsableDataNodeB( DataNode inDataNode )
      /* This method returns true if inDataNode is usable,
        which means it is not null and not an UnknownDataNode.
        It returns false otherwise.
        */
      { 
        boolean usableB= true;  // Assume the node is usable.

        toReturn: { // block beginning.
        toUnusable: { // block beginning.
        
          if ( inDataNode == null )  // There is no DataNode reference.
            break toUnusable; // Go return value for unusable.
          if // Node class is not an UnknownDataNode.
            ( ! UnknownDataNode.isOneB( inDataNode ) )
            break toReturn;  // Go return initial default value of usable.

        } // toUnusable
          usableB= false;  // Override default usability value.

        } // toReturn
          return usableB;  // Return final calculated value.

        }
  
    public boolean isRootB()
      /* Returns whether this node represents the root of its hierarchy.
       * This default method returns false.
       * The class which represents the root should return true.
       * 
       * This method provides an easy and low cost way
       * to determine whether a node is the root node,
       * without comparing node references,
       * which would require having access to the known root node.
       * 
       * This is an alternative to comparing null to getTreeParentNamedList().
       */
      { 
        return false; 
        }

    public TreePath getTreePath()
      /* This method returns the TreePath associated with this DataNode.
       * The path always includes at least one element and
       * this DataNode is always the last element of the path.
       * 
       * This method would be simpler if all constructors of TreePath
       * were public.
       * 
       *  ///opt Use cache like 
       *    DataTreeModel.translatingToTreePath( DataNode targetDataNode ).
       */
      { 
        TreePath resultTreePath;
        if ( null == getTreeParentNamedList() ) // If node has no parent then
          resultTreePath= 
            new TreePath(this); // path is only this node.
        else // Node has a parent so recursively
          resultTreePath= // construct path from
            getTreeParentNamedList().getTreePath() // parent's path and
             .pathByAddingChild(this); // then adding this node.
        return resultTreePath;   
        }

    public int getIndexOfNamedChild( String inString )
      /* Returns the index of the child whose name is inString,
        or -1 if this DataNode has no such child. 
        This method works by doing a search of the node's children.
        It assumes a functional getChild(..) method.  
        */
      {
        int childIndexI;  // Storage for search index and return result.

        if ( inString == null )  // Handling null child name.
          childIndexI= -1;  // Indicating no matching child.
          else  // Handling non-null child name.
          for ( childIndexI=0; ; childIndexI++ ) // Searching for child.
            { // Checking one child.
              DataNode childDataNode=  // Getting the child.
                 getChild( childIndexI );
              if ( childDataNode == null )  // Handling end of children.
                { // Exiting with search failure.
                  childIndexI= -1;  // Setting index to indicate failure.
                  break;  // Exiting loop.
                  }
              String childNameString= childDataNode.getNameString();
              // theAppLog.debug("[DataNode.getIndexOfNamedChild(.):"
              //   +this+","+inString+","+childDataNode+","
              //   +childNameString+","+childIndexI+"]");
              if  // Handling child with matching name.
                ( inString.equals( childNameString ) )
                break;  // Exiting while loop.
              }

        return childIndexI;  // Return index as search result.
        }

    public DataNode getNamedChildDataNode( String inNameString )
      /* Returns the child DataNode whose name is inString.
          If no such child exists then it returns null.
          This method is used for reading from the MetaFile
          and translating a name into a DataNode subclass.
          */
      { 
        int childIndexI= // Translate name to index.
          getIndexOfNamedChild( inNameString );

        DataNode childDataNode= // Translate index to DataNode.
          getChild( childIndexI );

        /*  ///
        theAppLog.debug("[DataNode.getNamedChildDataNode(.):"
          +this+","+inNameString+","+childIndexI+","+childDataNode+"]");
        if (null == childDataNode)
          theAppLog.appendToFileV(" [get FAILED!]");
        */  ///
        
        return childDataNode;  // Return DataNode.
        }


    /* Code to do customized logging.

      These methods, along with this.theMaxLogLevel and class AppLog,
      are used to control what is logged from this class and its subclasses.
      this.theMaxLogLevel works in a way similar to AppLog.maxLogLevel,
      to determine which log statements are executed.
      The method propagateLogLevelIntoSubtreeV(..) makes 
      the control of logging of entire subtrees possible.  

      ///opt: Remove theMaxLogLevel to reduce node storage, because
      this feature hasn't been used for a long time.
     */

    protected LogLevel theMaxLogLevel= AppLog.defaultMaxLogLevel;

    protected void propagateLogLevelIntoSubtreeV( LogLevel theMaxLogLevel )
      /* This method propagates theMaxLogLevel into this node.
        It acts only if the present level is different from theMaxLogLevel.
        Subclasses with descendants should propagate into them also.

        Unlike propagateIntoSubtreeV( DataTreeModel ),
        this method is called only when changes to the default logging
        are needed for debugging purposes.
        This method can be called whenever it is determined that
        special logging would be helpful.
        
        ///opt The checking whether a change is needed
          is probably not needed, and should eventually be eliminated.
          Meanwhile, it shouldn't cause any harm. 
        */
      {
        boolean changeNeededB= ( this.theMaxLogLevel != theMaxLogLevel ); 
        if // Make change only if new level limit is different.
          ( changeNeededB )
          {
            this.theMaxLogLevel= theMaxLogLevel;
            }
        }

    protected boolean logB( LogLevel theLogLevel )
      {
        return ( theLogLevel.compareTo( theMaxLogLevel ) <= 0 ) ;
        }

    protected boolean logB( LogLevel theLogLevel, String theLogString )
      {
        return logB( theLogLevel, theLogString, null, false );
        }

    protected void logV( LogLevel theLogLevel, String theLogString )
      {
        logV( theLogLevel, theLogString, null, false );
        }

    protected boolean logB( 
        LogLevel theLogLevel, 
        String theLogString, 
        Throwable theThrowable, 
        boolean consoleB )
      /* This method logs if theLogLevel is 
        lower than or equal to maxLogLevel.
        It returns true if it logged, false otherwise.
        */
      {
        boolean loggingB= logB(theLogLevel);
        if ( loggingB )
          theAppLog.logV( 
              theLogLevel, null, theLogString, theThrowable, consoleB );
        return loggingB;
        }

    protected void logV( 
        LogLevel theLogLevel, 
        String theLogString, 
        Throwable theThrowable, 
        boolean consoleB )
      /* This method logs unconditionally. */
      {
        theAppLog.logV( 
            theLogLevel, null, theLogString, theThrowable, consoleB );
        }
      


    // Miscellaneous methods.

  // Other miscellaneous methods.

    public void propagateTreeModelIntoSubtreeV( 
        DataTreeModel theDataTreeModel )
      /* This method is called to propagate theDataTreeModel
        into the nodes which needed it when 
        a DataNode is added to a NamedList or one of its subclasses.
          
        This method simply stores theDataTreeModel.
        It does nothing else because this is a leaf node 
        and leaves have no subtrees, so the propagation ends here.
        List nodes will override this method.  See NamedList.
        
        This method should be called last because 
        theDataTreeModel is the controlling flag for tree traversal.
        */
      {
        this.theDataTreeModel=  theDataTreeModel;
        }

    protected boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode)
      /* This method is not used for anything now.  It has not been tested.
       * 
       * This method is meant to try processing a MapEpiNode.
       * It is supposed to return true if processing is successful,
       * false otherwise.
       * This version does no processing so it returns false.
       * Methods which override this method will return true in some cases.
       * For an example, see the NamedList class.
       */
      { 
        return false; 
        }



    } // End of class DataNode.
