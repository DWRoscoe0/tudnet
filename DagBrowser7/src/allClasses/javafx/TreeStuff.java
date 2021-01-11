package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import allClasses.DataNode;
import allClasses.DataRoot;
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
    // Injected variables.
    // Some of these are circular parameters used in factory methods only,
    // and may eventually be deleted.
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
    private DataRoot theDataRoot;
    private Selections theSelections;
    
    // Calculated variables.
    private MapEpiNode selectionHistoryMapEpiNode;
      // This is where selection history is stored to enable
      // easily visiting previously visited children.


    public TreeStuff( // Constructor.
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
        this.theSelections= theSelections;

        this.selectionHistoryMapEpiNode= // Calculate history root MapEpiNode.
            this.thePersistent.getOrMakeMapEpiNode("SelectionHistory");
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
              theSelections.chooseSelectedDataNode( // result of choosing a selection from
                  oldSubjectDataNode, oldSelectedDataNode); // old subject.
          if (null == newSubjectDataNode) // If new subject not defined 
            break main; // we can not move right, so give up.
          newSelectedDataNode= // Set new selection to be 
              theSelections.chooseSelectedDataNode( // result of choosing a selection from
                  newSubjectDataNode, null); // new subject.
          resultTreeStuff=  // For the new 
              newSubjectDataNode.makeTreeStuff( // subject node, make TreeStuff
                newSelectedDataNode, // with this as subject's selection 
                thePersistent, // and include a copy of this
                theDataRoot, // and this
                theSelections // and this.
                ); 
        } // main:
          return resultTreeStuff;
        }
    
    public TreeStuff moveLeftAndMakeTreeStuff()
      /* Returns a TreeStuff for the location to the left of the present one,
       * hopefully the parent, or null if moving to the left is not possible.
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
        theSelections.purgeAndTestB(
            selectionHistoryMapEpiNode,
            theDataRoot.getParentOfRootDataNode()
            );
        return resultTreeStuff;
        }

    public static TreeStuff makeWithAutoCompleteTreeStuff(
        DataNode subjectDataNode,
        DataNode selectedChildDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        Selections theSelections
        )
      { 
        TreeStuff resultTreeStuff= new TreeStuff(
            subjectDataNode,
            selectedChildDataNode,
            thePersistent,
            theDataRoot,
            theSelections
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

    public void setSelectedDataNodeV(DataNode theDataNode)
      {
        theAppLog.debug(
            "TreeStuff.setSelectedDataNodeV(() "+theDataNode);

        selectedChildDataNode= theDataNode;
        
        theSelections.recordAndTranslateToMapEpiNode(theDataNode);
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

    }
