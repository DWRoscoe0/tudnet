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
    private DataNode selectedChildDataNode= null;
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


    public static TreeItem<DataNode> toTreeItem(
        DataNode targetDataNode, TreeItem<DataNode> ancestorTreeItem) 
      /* This translates targetDataNode to the TreeItem that references it
       * by searching for the ancestor DataNode referenced by ancestorTreeItem,
       * Usually ancestorTreeItem is the rott TreeItem.
       * then tracing TreeItems back to the target DataNode.
       * This is done recursively to simplify path tracking.  
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
          oldSelectedDataNode= getSelectedChildDataNode();
          newSubjectDataNode= // Set new subject to be
              theSelections.chooseSelectedDataNode( // result of choosing from
                  oldSubjectDataNode, oldSelectedDataNode); // old subject.
          if (null == newSubjectDataNode) // If new subject not defined 
            break main; // we can not move right, so give up.
          newSelectedDataNode= // Set new selection to be 
              theSelections.chooseSelectedDataNode( // result of choosing from
                  newSubjectDataNode, null); // new subject.
          resultTreeStuff=  // For the new 
              newSubjectDataNode.makeTreeStuff( // subject node, make TreeStuff
                newSelectedDataNode, // with this as subject's selection 
                thePersistent, // and include a copy of this
                theDataRoot, // and this
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

        selectedChildDataNode= theDataNode; // Store selection locally.

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
            theSelections.getSelectionHistoryMapEpiNode(),
            theDataRoot.getParentOfRootDataNode()
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
          theSubjectDataNode= getSelectedChildDataNode().getParentNamedList();
        return theSubjectDataNode;
        }

    public DataNode getSelectedChildDataNode()
      /* Returns selected child DataNode.  There is only one way to do this.
       * It simply returns the stored value.
       */
      {
        return selectedChildDataNode;
        }

    public DataRoot getDataRoot()
      {
        return theDataRoot;
        }

    public static TreeStuff makeWithAutoCompleteTreeStuff(
        DataNode subjectDataNode,
        DataNode selectedChildDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        Selections theSelections
        )
      /* This is the factory method for TreeStuff objects.
       * All TreeStuffs are made with this, 
       * followed by a call to initializeV(theNode) to finish initialization.
       */
      { 
        TreeStuff resultTreeStuff= new TreeStuff(
            subjectDataNode,
            selectedChildDataNode,
            thePersistent,
            theDataRoot,
            theSelections
            );
        if  // If nothing selected in the TreeStuff
          (null == resultTreeStuff.selectedChildDataNode) 
          resultTreeStuff.selectedChildDataNode= // try selecting first child. 
            resultTreeStuff.theSubjectDataNode.getChild(0);
        resultTreeStuff.setSelectedDataNodeV(
            resultTreeStuff.selectedChildDataNode);
        return resultTreeStuff;
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
        DataNode selectedChildDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        Selections theSelections
        )
      { 
        this.theSubjectDataNode= subjectDataNode;
        this.selectedChildDataNode= selectedChildDataNode;
        this.thePersistent= thePersistent;
        this.theDataRoot= theDataRoot;
        this.theRootEpiTreeItem= theRootEpiTreeItem;
        this.theSelections= theSelections;
        }

    }
