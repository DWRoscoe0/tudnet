package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Persistent;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;


public class TreeStuff 
  /* This class does several things.
   * * It stores information about 
   *   a location in the hierarchy for JavaFX Node viewers.
   *   * It stores the location of the subject DataNode.
   *   * It stores the location of the selection DataNode, if any, 
   *     within the subject DataNode.
   * An instance of this class should be updated by 
   * the selection model of the Node's associated viewer.
   * It may be interrogated for location information.
   * 
   * Location is stored both locally, and in the Selections class.
   * 
   * Location is usually represented by 
   * the DataNode of interest at that location.
   * Location can also be represented by the TreePath
   * from the root DataNode to the DataNode of interest,
   * this is not being done now.
   */

  {
    // Injected variables.
    // Some of these are circular parameters used in factory methods only,
    // and may eventually be deleted.
  
    // Initialized by constructor injection.
    private DataNode theSubjectDataNode= null;
      // This is the whole DataNode being displayed by a viewer.
      // It should be the parent of the selected DataNode, if any.
    private DataNode selectedDataNode= null;
      // This should be the selected child DataNode of the subject DataNode.
      // This may be null if there is no selection.
      ///org Maybe bind this to viewer instead of assigning it.
    private Persistent thePersistent;
    private DataRoot theDataRoot;
    private EpiTreeItem theRootEpiTreeItem;
    private Selections theSelections;
    
    // Initialized by setter injection.
    private Node theGuiNode= null;
      // This should be the JavaFX Node used to display the DataNode. 


    public TreeItem<DataNode> toTreeItem(DataNode targetDataNode) 
      {
        return TreeStuff.toTreeItem(
          targetDataNode, 
          theRootEpiTreeItem
          ); 
        }

    public static TreeItem<DataNode> toTreeItem(
        DataNode targetDataNode, TreeItem<DataNode> ancestorTreeItem) 
      /* This translates targetDataNode to the TreeItem that references it
       * by searching for the ancestor DataNode referenced by ancestorTreeItem,
       * then tracing TreeItems back to the target DataNode.
       * This is done recursively to simplify path tracking.  
       * Usually ancestorTreeItem is the root TreeItem.
       * This method returns the target TreeItem or null if translation fails.
       * The returned TreeItem and some of its immediate ancestors
       * might be created if they do not exist before this method is called.
       */
      {
          TreeItem<DataNode> resultTreeItem;
        main: {
          if // Root TreeItem references target DataNode.
            (ancestorTreeItem.getValue() == targetDataNode)
            { resultTreeItem= ancestorTreeItem; break main; } // Exit with root.
          if (null == targetDataNode) // Target is null.
            { resultTreeItem= null; break main; } // Indicate failure with null.
          TreeItem<DataNode> parentTreeItem= // Recursively translate parent. 
              toTreeItem(targetDataNode.getParentNamedList(), ancestorTreeItem);
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
       * This method returns null if moving to the left is not possible.
       */
      {
          DataNode oldSubjectDataNode, oldSelectedDataNode; 
          DataNode newSubjectDataNode, newSelectedDataNode; 
          TreeStuff resultTreeStuff= // Return self if no movement possible.
            this;
        main: {
          oldSubjectDataNode= getSubjectDataNode();
          if (null == oldSubjectDataNode) // If subject not defined 
            break main; // this is a serious problem, so give up.
          oldSelectedDataNode= getSelectionDataNode();
          newSubjectDataNode= // Set new subject to be
              theSelections.chooseSelectionDataNode( // result of choosing from
                  oldSubjectDataNode, oldSelectedDataNode); // old subject.
          if (null == newSubjectDataNode) // If new subject not defined 
            break main; // we can not move right, so give up.
          newSelectedDataNode= // Set new selection to be 
              theSelections.chooseSelectionDataNode( // result of choosing from
                  newSubjectDataNode, null); // new subject.
          resultTreeStuff=  // For the new 
              newSubjectDataNode.makeTreeStuff( // subject node, make TreeStuff
                newSelectedDataNode, // with this as subject's selection 
                thePersistent, // and include a copy of this
                theDataRoot, // and this
                theRootEpiTreeItem, // and this
                theSelections // and this.
                ); 
        } // main:
          purgeSelectionStorageV();
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
        TreeStuff resultTreeStuff= // Return self if no movement possible.
          this;
        DataNode oldSubjectDataNode= getSubjectDataNode();
        DataNode oldParentDataNode= getParentDataNode();
        if (null != oldParentDataNode) { // If there's a parent
          resultTreeStuff= // make new TreeStuff
              oldParentDataNode.makeTreeStuff( // for parent node
                oldSubjectDataNode, // and subject node as selection in parent
                thePersistent, // and this
                theDataRoot, // and this
                theRootEpiTreeItem, // and this
                theSelections // and this.
                ); 
          }
        purgeSelectionStorageV();
        return resultTreeStuff;
        }

    public Node getGuiNode()
      /* Returns the JavaFX GUI Node being used to display 
       * the subject DataNode. 
       */
      {
        return theGuiNode;
        }

    public void setSelectedDataNodeV(DataNode theDataNode)
      /* This method stores theDataNode as the selection of
       * the associated viewer.
       * This includes storing the selection locally 
       * and in the selection history.
       */
      {
        theAppLog.debug(
            "TreeStuff.setSelectedDataNodeV(() "+theDataNode);

        selectedDataNode= theDataNode; // Store selection locally.

        theSelections.recordAndTranslateToMapEpiNode(theDataNode);
          // Store selection in the selection history.
        purgeSelectionStorageV(); // Purge unneeded selection history. 
        }

    private void purgeSelectionStorageV()
      /* This method purges any unneeded selection data from
       * Persistent storage starting from the root of that data.
       */
      {
        theSelections.purgeAndTestB(
            theSelections.getSelectionHistoryMapEpiNode(), // History root.
            theDataRoot.getParentOfRootDataNode() // DataNodes root.
            );
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

          resultDataNode= resultDataNode.getParentNamedList();
          // We now have a the possibly null parent of the subject node.

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
        if (null == theSubjectDataNode) // If null, try calculating it from child.
          theSubjectDataNode= getSelectionDataNode().getParentNamedList();
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
          theSelections.chooseSelectionDataNode(resultDataNode);
        if (null == resultDataNode) // If there is no best sub-selection
          break main; // exit with null.
      } // main:
        return resultDataNode;
      }

    public DataRoot getDataRoot()
      {
        return theDataRoot;
        }

    public static TreeStuff makeWithAutoCompleteTreeStuff(
        DataNode subjectDataNode,
        DataNode selectedDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        EpiTreeItem theRootEpiTreeItem,
        Selections theSelections
        )
      /* This is the factory method for TreeStuff objects.
       * All TreeStuffs are made by a call to this method, 
       * followed by a call to initializeV(theNode) to finish initialization.
       * This method tries to fill in a value for a selectedDataNode
       * within the subjectDataNode.  It might or might not succeed.
       */
      {
        TreeStuff theTreeStuff= new TreeStuff(
            subjectDataNode,
            selectedDataNode,
            thePersistent,
            theDataRoot,
            theRootEpiTreeItem,
            theSelections
            );
        if  // If nothing selected in the TreeStuff
          (null == theTreeStuff.getSelectionDataNode())
          {
            DataNode chosenSelectionDataNode=
              theSelections.chooseSelectionDataNode(subjectDataNode);
            theTreeStuff.setSelectedDataNodeV(chosenSelectionDataNode);
            }
        return theTreeStuff;
        }
          
    public void initializeV(Node theNode)
      /* This does the bit of dependency injection 
       * that can not be done by the constructor. 
       */
      { 
        this.theGuiNode= theNode;
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
