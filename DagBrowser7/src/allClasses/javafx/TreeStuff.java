package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import allClasses.DataNode;
import allClasses.Persistent;
import allClasses.epinode.MapEpiNode;
import javafx.scene.Node;


public class TreeStuff 
  /* This class stores information about 
   * a location in the hierarchy for JavaFX Node viewers.
   * It stores both the location of the subject DataNode,
   * and the location of the selection DataNode, if any, 
   * within the subject DataNode.
   * It should be updated by the selection model of
   * the Node's associated viewer.
   * It may be interrogated for location information.
   * Location can be expressed by either a TreePath 
   * or the DataNode of interest that terminates that path. 
   * A TreePath can be calculated from a DataNode
   * by following the links to parent DataNodes.
   */

  {
    private Node theGuiNode= null;
      // This should be the JavaFX Node used to display the DataNode. 
    private DataNode theSubjectDataNode= null;
      // This is the whole DataNode being displayed by a viewer.
      // It should be the parent of the selected DataNode, if any.
    private DataNode selectedChildDataNode= null;
      // This should be the selected child DataNode of the subject DataNode.
      // This may be null if there is no selection.
      ///org Maybe bind this to viewer instead of assigning it.
    private Persistent thePersistent;
    private MapEpiNode selectionHistoryMapEpiNode;
      // This is where selection history is stored to enable
      // easily visiting previously visited children.

    public TreeStuff( // Constructor.
        DataNode subjectDataNode,
        DataNode selectedChildDataNode,
        Persistent thePersistent
        )
      { 
        this.theSubjectDataNode= subjectDataNode;
        this.selectedChildDataNode= selectedChildDataNode;
        this.thePersistent= thePersistent;
        
        this.selectionHistoryMapEpiNode= // Calculate history root MapEpiNode.
            this.thePersistent.getOrMakeMapEpiNode("SelectionHistory");
        }

    public TreeStuff OLDmoveRightAndMakeTreeStuff() ////
      /* This method is called when a viewer is given a command to
       * move to the right, which usually means move to an appropriate child
       * of the DataNode the viewer is presently displaying. 
       * This method returns a TreeStuff appropriate for displaying
       * the DataNode at the new location.
       * This will include the both the subject and selected child DataNodes,
       * and a JavaFX Node for viewing appropriate for 
       * displaying the subject DataNode, with the viewer initialized
       * with the proper selection.
       */
      {
          TreeStuff resultTreeStuff= // Return self if no movement possible.
            this;
          DataNode childDataNode= getSelectedChildDataNode();
          if (null != childDataNode) { // If there's a child,
            resultTreeStuff= childDataNode.makeTreeStuff( // make TreeStuff from it.
                null, // No selection within child specified yet.
                thePersistent
                ); 
            }
          return resultTreeStuff;
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
              chooseSelectedDataNode( // result of choosing a selection from
                  oldSubjectDataNode, oldSelectedDataNode); // old subject.
          if (null == newSubjectDataNode) // If new subject not defined 
            break main; // we can not move right, so give up.
          newSelectedDataNode= // Set new selection to be 
              chooseSelectedDataNode( // result of choosing a selection from
                  newSubjectDataNode, null); // new subject.
          resultTreeStuff=  // For the new 
              newSubjectDataNode.makeTreeStuff( // subject node, make TreeStuff
              newSelectedDataNode, // with this as subject's selection 
              thePersistent // and include a copy of this.
              ); 
        } // main:
          return resultTreeStuff;
        }

    private DataNode chooseSelectedDataNode(
        DataNode subjectDataNode, DataNode selectedDataNode)
      /* This method returns a DataNode to be used as the selection within
       * the subjectDataNode.  
       * It returns selectedDataNode if it is not null.
       * Otherwise it tries to find the most recent selected child
       * from the selection history.
       * If there is none then it tries to return the first child.
       * If there are no children then it returns null.
       */
      {
        main: {
          if (null != selectedDataNode) // Selection was provided.
            break main; // Return it.
          MapEpiNode subjectMapEpiNode= // Get subject MapEpiNode from history.
            recordAndTranslateToMapEpiNode(subjectDataNode);
          String selectionNameString= // Get name of previous selection,
              subjectMapEpiNode.getKeyString(
                  subjectMapEpiNode.getSizeI()-1); // the most recent entry.
          selectedDataNode= // Try getting child by that name from child list. 
              subjectDataNode.getNamedChildDataNode(selectionNameString);
          if (null != selectedDataNode) // Got child.
            break main; // Return it.
          selectedDataNode= // Try getting first child from list. 
              subjectDataNode.getChild(0);
          // At this point, null means there will be no selection,
          // non-null means result selection is first child of subject.
        } // main: 
          return selectedDataNode;
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
            resultTreeStuff.theSubjectDataNode.getChild(0);

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
          theSubjectDataNode,
          theTreeStuff
          );
        theTreeStuff.initializeV(theTitledListNode);
        return theTreeStuff;
        }
    */  ////

    ////// TreeStuff.getSelectedChildDataNode());
    ////// TreeStuff.getParentDataNode());
    
    public void setSelectedDataNodeV(DataNode theDataNode)
      {
        theAppLog.debug(
            "TreeStuff.setSelectedDataNodeV(() "+theDataNode);

        selectedChildDataNode= theDataNode;
        
        recordAndTranslateToMapEpiNode(theDataNode);
        }

    private MapEpiNode recordAndTranslateToMapEpiNode(DataNode theDataNode)
      /* This method translates theDataNode to 
       * the MapEpiNode at the location in Persistent storage
       * associated with that DataNode.
       * If it needs to create that MapEpiNode,
       * or any others between it and the root of Persistent storage,
       * then it does so.
       * It returns the resulting MapEpiNode.  It never returns null.
       * This is done recursively to simplify path tracking.
       */
      {
        MapEpiNode dataMapEpiNode; // MapEpiNode associated with DataNode name. 
        MapEpiNode parentMapEpiNode; // MapEpiNode which is above's parent. 

        if (theDataNode.isRootB()) // DataNode is the root node.
          parentMapEpiNode= // So use root as parent EpiNode.
            selectionHistoryMapEpiNode; 
          else  // DataNode is not Root.
          parentMapEpiNode= // Recurse to get parent EpiNode. 
              recordAndTranslateToMapEpiNode( 
                  theDataNode.getParentNamedList()); // from parent DataNode
        dataMapEpiNode= // Get or make MapEpiNode associated with DataNode name. 
            parentMapEpiNode.getOrMakeMapEpiNode(
                theDataNode.getNameString()); 

        return dataMapEpiNode; // Return resulting MapEpiNode.
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

    }
