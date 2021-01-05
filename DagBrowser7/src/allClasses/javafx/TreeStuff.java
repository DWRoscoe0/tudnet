package allClasses.javafx;

import allClasses.DataNode;
import allClasses.Persistent;
import allClasses.epinode.MapEpiNode;
import javafx.scene.Node;


public class TreeStuff 
  /* This class stores information about 
   * a location in the hierarchy for JavaFX Node viewers.
   * It stores both the location of the Node,
   * and the location of the selection, if any, within the Node.
   * It should be updated by the selection model of
   * the Node's associated viewer.
   * It may be interrogated for location information.
   * Location can be expressed by either
   * a TreePath or the DataNode of interest that terminates it. 
   * A TreePath can be calculated from a DataNode
   * by following the links to parent DataNodes.
   */

  {
    private Node theGuiNode= null;
      // This should be the JavaFX Node used to display the DataNode. 
    private DataNode subjectDataNode= null;
      // This is the whole DataNode being displayed.
      // It should be the parent of the selected DataNode, if any.
    private DataNode selectedChildDataNode= null;
      // This should be the selected child DataNode of the subject DataNode.
      // This may be null if there is not selection.
      ///org Maybe bind this to viewer instead of assigning it.
    private Persistent thePersistent;

    //// @SuppressWarnings("unused") ////
    private MapEpiNode selectionHistoryMapEpiNode;
      // This is where selection history is stored to enable
      // easily visiting previously visited children.

    public TreeStuff( // Constructor.
        DataNode subjectDataNode,
        DataNode selectedChildDataNode,
        Persistent thePersistent
        )
      { 
        this.subjectDataNode= subjectDataNode;
        this.selectedChildDataNode= selectedChildDataNode;
        this.thePersistent= thePersistent;
        
        this.selectionHistoryMapEpiNode= // Calculate map of subscribees.
            this.thePersistent.getOrMakeMapEpiNode("SelectionHistory");
        }

    public TreeStuff moveRightAndMakeTreeStuff()
      /* Returns a TreeStuff for the location to the right of the present one,
       * hopefully the most recently visited child, or the first child,
       * or null if moving to the right in this way is not possible.
       */
      {
        TreeStuff theTreeStuff= null;
        DataNode childDataNode= getSelectedChildDataNode();
        if (null != childDataNode) { // If there's a child,
          theTreeStuff= childDataNode.makeTreeStuff( // make TreeStuff from it.
              null, // No selection within child specified yet.
              thePersistent
              ); 
          }
        return theTreeStuff;
        }

    public TreeStuff moveLeftAndMakeTreeStuff()
      /* Returns a TreeStuff for the location to the left of the present one,
       * hopefully the parent,
       * or null if moving to the left in this way is not possible.
       */
      {
        TreeStuff theTreeStuff= null;
        DataNode parentDataNode= getParentDataNode();
        if (null != parentDataNode) { // If there's a parent
          theTreeStuff= parentDataNode.makeTreeStuff( // make TreeStuff from it.
              null, // No selection within child specified yet.
              thePersistent
              ); 
          }
        return theTreeStuff;
        }

    public static TreeStuff makeWithAutoCompleteTreeStuff(
        DataNode subjectDataNode,
        DataNode selectedChildDataNode,
        Persistent thePersistent
        )
      { 
        TreeStuff resultTreeStuff= new TreeStuff(
            subjectDataNode,
            selectedChildDataNode,
            thePersistent
            );

        if  // If nothing selected
          (null == resultTreeStuff.selectedChildDataNode) 
          resultTreeStuff.selectedChildDataNode= // try selecting first child. 
            resultTreeStuff.subjectDataNode.getChild(0);

        return resultTreeStuff;
        }
          
    public void initializeV(Node theNode)
      { 
        this.theGuiNode= theNode;
        }


    public Node getGuiNode()
      /* Returns the JavaFX GUI Node being used to display the DataNode. */
      {
        return theGuiNode;
        }


    /*  ////
    public TreeStuff makeSelectedChildTreeStuff()
      /* This method makes and returns a TreeStuff appropriate for
       * the presently selected child.
       */
    /*  ////
      { 
        DataNode theSelectedDataNode= getSelectedChildDataNode();
        TreeStuff theTreeStuff= theSelectedDataNode.makeTreeStuff(theSelectedDataNode);
        TitledListNode theTitledListNode= new TitledListNode( 
          subjectDataNode,
          theTreeStuff
          );
        theTreeStuff.initializeV(theTitledListNode);
        return theTreeStuff;
        }
    */  ////

    ////// TreeStuff.getSelectedChildDataNode());
    ////// TreeStuff.getParentDataNode());
    
    public void setSelectedDataNodeV(DataNode theDataNode)
      ////////////////////////
      {
        selectedChildDataNode= theDataNode;
        
        recordSelectionPathV(theDataNode);
        }

    private void recordSelectionPathV(DataNode theDataNode)
      /* Records locationDataNode in Persistent storage.
       * This is done recursively to simplify path tracking.  
       */
      {
          selectionHistoryMapEpiNode.getOrMakeMapEpiNode(
              theDataNode.getNameString()); ////// Do this later in appropriate place.

        /*  ////
          TreeItem<DataNode> resultTreeItem;
        main: {
          if // Root TreeItem references target DataNode.
            (rootTreeItem.getValue() == targetDataNode)
            { resultTreeItem= rootTreeItem; break main; } // Exit with root.
          TreeItem<DataNode> parentTreeItem= // Recursively translate parent. 
              toTreeItem(targetDataNode.getParentNamedList(), rootTreeItem);
          if (null == parentTreeItem) // Parent translation failed.
            { resultTreeItem= null; break main; } // Indicate failure with null.
          for // Search for target DataNode in translated parent's children.
            ( TreeItem<DataNode> childTreeItem : parentTreeItem.getChildren() )
            {
              if  // Exit with child TreeItem if it references target DataNode.
                (childTreeItem.getValue() == targetDataNode)
                { resultTreeItem= childTreeItem; break main; }
              }
          // If here then no child referenced target DataNode.
          resultTreeItem= null; // Indicate failure with null.
        } // main:
          return resultTreeItem;
        */  ////
        }

    public DataNode getParentDataNode()
      /* Tries to return the parent of the subject node.
       * Returns null if there is none.
       */
      {
        DataNode resultDataNode;
        goReturn: {

          //// resultDataNode= selectedChildDataNode;
          //// if (null == resultDataNode) break goReturn;
          //// // We now have the non-null selected child node.

          //// resultDataNode= resultDataNode.getParentNamedList();
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
        if (null == subjectDataNode) // If null, try calculating it from child.
          subjectDataNode= getSelectedChildDataNode().getParentNamedList();
        return subjectDataNode;
        }

    public DataNode getSelectedChildDataNode()
      /* Returns selected child DataNode.  There is only one way to do this.
       * It simply returns the stored value.
       */
      {
        return selectedChildDataNode;
        }

    }
