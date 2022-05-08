package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Nulls;
import allClasses.Persistent;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import static allClasses.SystemSettings.NL;


public class TreeStuff 

  {
    /* This class is the JavaFX equivalent of 
     * the class TreeHelper which is used with Swing.
     *
     * This class does several things.
     * * It stores information about 
     *   a location in the hierarchy for JavaFX Node viewers.
     *   * It stores the location of the parent subject DataNode.
     *   * It stores the location of the child selection DataNode, if any, 
     *     within the subject DataNode.  null means there is no selection.
     *
     * This class performs various operations that are useful
     * to the JavaFX UI, tracking locations in the hierarchy, etc.
     * For example, this class may be interrogated for location information.
     * Location information is duplicated in the Selections class object.
     * 
     * An instance of this class should be updated by 
     * the selection model of the Node's associated viewer.
     *
     * Location is represented by the DataNode of interest at the location.
     *
     * Location can also be represented by the TreePath
     * from the root DataNode to the DataNode at the location of interest,
     * though this is not being done now.
     * ///org Representing location with a TreePath might be necessary 
     * when and if TUDNet supports DataNode DAGs instead of trees.
     */

  
    // Variables injected by constructor.
  
    ///opt? Some of these are circular parameters used in factory methods only,
    // and may eventually be deleted.
  
    private DataNode theSubjectDataNode= null;
      // This is the whole DataNode being displayed by a viewer.
      // It should be the parent of the selected DataNode, if any.

    private DataNode selectedDataNode= null;
      // This should be the selected child DataNode of the subject DataNode.
      // This may be null if there is no selection.
      ///org Maybe bind this to viewer instead of assigning it.

    private Persistent thePersistent; // Persistent data storage.

    private DataRoot theDataRoot; // Root of DataNode hierarchy.

    private EpiTreeItem theRootEpiTreeItem; // The root of TreeItem tree.

    private Selections theSelections; // Other DataNode selections and history. 


    // Variables initialized by setter injection after construction.

    private Node theUINode= null; // JavaFX Node that displays subjectDataNode.


    // Methods.

    public TreeItem<DataNode> toTreeItem(DataNode targetDataNode) 
      /* This method is equivalent to 
       *   toTreeItem(targetDataNode, subtreeTreeItem)
       * with subtreeTreeItem set to the root of the TreeItem tree.
       */
      {
        return TreeStuff.toTreeItem(
          targetDataNode, 
          theRootEpiTreeItem
          ); 
        }

    public static TreeItem<DataNode> toTreeItem(
        DataNode targetDataNode, TreeItem<DataNode> subtreeTreeItem) 
      /* This recursive method translates targetDataNode to 
       * the TreeItem that references it as its value.
       * 
       * This method works by searching ancestor DataNodes until it finds 
       * the one that is the value DataNode of subtreeTreeItem,
       * then tracing simultaneously the DataNode tree
       * and the TreeItems tree back to targetDataNode.
       * Usually subtreeTreeItem is the root TreeItem.
       * 
       * This method creates new TreeItems in the TreeItem tree
       * if they did not exist before this method is called.
       * 
       * This method returns the TreeItem whose value is targetDataNode,
       * or null if the translation fails for any reason.
       */
      {
          TreeItem<DataNode> resultTreeItem;
        main: {
          if // Root TreeItem references target DataNode.
            (subtreeTreeItem.getValue() == targetDataNode)
            { resultTreeItem= subtreeTreeItem; break main; } // Exit with root.
          if (null == targetDataNode) // Target is null.
            { resultTreeItem= null; break main; } // Indicate failure with null.
          TreeItem<DataNode> parentTreeItem= // Recursively translate parent. 
              toTreeItem(
                  targetDataNode.getTreeParentNamedList(), subtreeTreeItem);
          if (null == parentTreeItem) // Parent translation failed.
            { resultTreeItem= null; break main; } // Indicate failure with null.
          for // Search for target DataNode in translated parent's children.
            ( TreeItem<DataNode> childTreeItem : parentTreeItem.getChildren() )
            {
              if  // Exit with child TreeItem if it references target DataNode.
                (childTreeItem.getValue().equals(targetDataNode))
                { resultTreeItem= childTreeItem; break main; }
              }
          // If here then no child referenced the target DataNode.
          resultTreeItem= null; // Indicate failure with null.
        } // main:
          Nulls.testAndLogIfNullB(resultTreeItem);
          return resultTreeItem;
        }

    public TreeStuff moveRightAndMakeTreeStuff()
      /* This method is called when a viewer is given a command to
       * move to the right, which usually means moving to an appropriate child
       * of the DataNode the viewer is presently displaying. 
       * This method returns a TreeStuff appropriate for displaying
       * a DataNode to the right of the present one.
       * This TreeStuff will include the subject and selected child DataNodes,
       * and a JavaFX Node of a viewer appropriate for 
       * displaying the new subject DataNode, with the viewer initialized
       * with the proper selection.
       * This method returns null if moving to the right is not possible.
       */
      {
          DataNode oldSubjectDataNode, oldSelectedDataNode; 
          DataNode newSubjectDataNode, newSelectedDataNode; 
          TreeStuff resultTreeStuff= // Return null if movement impossible.
            null;
        main: {
          oldSubjectDataNode= getSubjectDataNode();
          theAppLog.appendToFileV(NL+"[tsmr1:"+oldSubjectDataNode+"]");
          if (null == oldSubjectDataNode) // If subject not defined 
            break main; // this is a serious problem, so give up.
          oldSelectedDataNode= getSelectionDataNode();
          newSubjectDataNode= // Set new subject to be
              theSelections.chooseChildDataNode( // result of choosing from
                  oldSubjectDataNode, oldSelectedDataNode); // old subject.
          theAppLog.appendToFileV(
              NL+"[tsmr2:"+oldSelectedDataNode+";"+newSubjectDataNode+"]");
          if (null == newSubjectDataNode) // If new subject not defined 
            break main; // we can not move right, so give up.
          newSelectedDataNode= // Set new selection to be 
              theSelections.chooseChildDataNode( // result of choosing from
                  newSubjectDataNode, null); // new subject.
          theAppLog.appendToFileV(NL+"[tsmr3:"+newSelectedDataNode+"]");
          resultTreeStuff=  // For the new 
              newSubjectDataNode.makeTreeStuff( // subject node, make TreeStuff
                newSelectedDataNode, // with this as subject's selection 
                thePersistent, // and include a copy of this
                theDataRoot, // and this
                theRootEpiTreeItem, // and this
                theSelections // and this.
                ); 
        } // main:
          theAppLog.appendToFileV("[tsmrx]");
          return resultTreeStuff;
        }
    
    public TreeStuff moveLeftAndMakeTreeStuff()
      /* This method is called when a viewer is given a command to
       * move to the left, which means moving to the parent
       * of the DataNode the viewer is presently displaying. 
       * This method returns a TreeStuff appropriate for displaying
       * a DataNode to the left of the present one.
       * This TreeStuff will include the subject and selected child DataNodes,
       * and a JavaFX Node of a viewer appropriate for 
       * displaying the new subject DataNode, with the viewer initialized
       * with the proper selection.
       * This method returns null if moving to the left is not possible.
       */
      {
          TreeStuff resultTreeStuff= null; // Return null if anything fails.
        main: { 
          DataNode oldSubjectDataNode= getSubjectDataNode();
          if (oldSubjectDataNode.isRootB()) break main; // Fail if at root.
          DataNode oldParentDataNode= getParentDataNode();
          if (null == oldParentDataNode) break main; // Fail if no parent
          resultTreeStuff= // make new TreeStuff
              oldParentDataNode.makeTreeStuff( // for parent node
                oldSubjectDataNode, // and subject node as selection in parent
                thePersistent, // and this
                theDataRoot, // and this
                theRootEpiTreeItem, // and this
                theSelections // and this.
                ); 
        } // main: 
          return resultTreeStuff;
        }

    public Node getGuiNode()
      /* Returns the JavaFX GUI Node being used to display 
       * the subject DataNode. 
       */
      {
        return theUINode;
        }

    public void setSelectedDataNodeV(DataNode theDataNode)
      /* This method stores theDataNode as the selection of
       * the associated viewer.
       * This includes storing the selection locally 
       * and in the selection history.
       */
      {
        /// theAppLog.debug(
        ///     "TreeStuff.setSelectedDataNodeV() to "+theDataNode);

        selectedDataNode= theDataNode; // Store selection locally.

        // Store selection in the selection history.
        theSelections.adjustForNewSelectionV(theDataNode);
        }

    public DataNode getParentDataNode()
      /* Tries to return the parent of the subject node.
       * Returns null if there is none.
       */
      {
        DataNode resultDataNode;
        goReturn: {

          resultDataNode= getSubjectDataNode();
          if (null == resultDataNode) break goReturn;
          // We now have the non-null subject node.

          resultDataNode= resultDataNode.getTreeParentNamedList();
          // We now have the possibly null parent of the subject node.

        } // goReturn:
        
        return resultDataNode; // Return the possibly null result.
        }

    public DataNode getSubjectDataNode()
      /* Returns subject DataNode.  
       * Tries to calculate it as parent of selected child DataNode
       * if immediate value is null.
       * Returns null if all attempts to calculate it fail. 
       */
      {
        if (null == theSubjectDataNode) // If null, try calculating from child.
          theSubjectDataNode= getSelectionDataNode().getTreeParentNamedList();
        return theSubjectDataNode;
        }

    public DataNode getSelectionDataNode()
      /* Returns selected child DataNode.  There is only one way to do this.
       * It simply returns the stored value.
       * It might return null, if there is no selection.
       */
      {
        return selectedDataNode;
        }

    public DataNode getSubselectionDataNode()
      /* Returns the best candidate for a DataNode to be
       * a selected child within the present selection.
       * The best candidate either comes from the selection history,
       * or if there is none, child 0, or if there is none, null.
       */
      {
        DataNode resultDataNode=  // Get present selection.
            getSelectionDataNode();
      main: {
        if (null == resultDataNode) // If there is no present selection 
          break main; // exit with null.
        resultDataNode= // Get best sub-selection. with present selection.
          theSelections.chooseChildDataNode(resultDataNode);
        if (null == resultDataNode) // If there is no best sub-selection
          break main; // exit with null.
      } // main:
        return resultDataNode;
      }

    public DataRoot getDataRoot()
      {
        return theDataRoot;
        }

    public static TreeStuff makeTreeStuff(
        DataNode subjectDataNode,
        DataNode selectedDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        EpiTreeItem theRootEpiTreeItem,
        Selections theSelections
        )
      /* This method makes TreeStuff objects.
       * All TreeStuffs are made by a call to this method, 
       * followed by a call to initializeGUINodeV(.) to finish initialization.
       * This method tries to fill in a value for a selectedDataNode
       * within the subjectDataNode.  It might or might not succeed.
       */
      {
        TreeStuff theTreeStuff= new TreeStuff( // Construct TreeStuff.
            subjectDataNode,
            selectedDataNode,
            thePersistent,
            theDataRoot,
            theRootEpiTreeItem,
            theSelections
            );
        if  // If nothing selected in the TreeStuff
          (null == theTreeStuff.getSelectionDataNode())
          { // Try to make a selection based on available context.
            DataNode chosenSelectionDataNode=
              theSelections.chooseChildDataNode(subjectDataNode);
            theTreeStuff.setSelectedDataNodeV(chosenSelectionDataNode);
            }
        return theTreeStuff;
        }

    public void initializeGUINodeV(Node theUINode)
      /* This does the bit of dependency injection 
       * that can not be done by the constructor,
       * in this case storing the JavaFX UI Node. 
       */
      { 
        this.theUINode= theUINode;
        }

    private TreeStuff( // Constructor.
        DataNode subjectDataNode,
        DataNode selectedDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        EpiTreeItem theRootEpiTreeItem,
        Selections theSelections
        )
      { 
        this.theSubjectDataNode= subjectDataNode;
        this.selectedDataNode= selectedDataNode;
        this.thePersistent= thePersistent;
        this.theDataRoot= theDataRoot;
        this.theRootEpiTreeItem= theRootEpiTreeItem;
        this.theSelections= theSelections;
        }

    }
