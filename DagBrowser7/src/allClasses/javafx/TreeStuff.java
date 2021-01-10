package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Persistent;
import allClasses.epinode.EpiNode;
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
    // Injected fields.
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
    
    // Calculated fields.
    private MapEpiNode selectionHistoryMapEpiNode;
      // This is where selection history is stored to enable
      // easily visiting previously visited children.


    public TreeStuff( // Constructor.
        DataNode subjectDataNode,
        DataNode selectedChildDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot
        )
      { 
        this.theSubjectDataNode= subjectDataNode;
        this.selectedChildDataNode= selectedChildDataNode;
        this.thePersistent= thePersistent;
        this.theDataRoot= theDataRoot;

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
                thePersistent, // and include a copy of this
                theDataRoot // and this.
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
                theDataRoot // and this.
              ); 
          }
        purgeAndTestB(
            selectionHistoryMapEpiNode,
            theDataRoot.getParentOfRootDataNode()
            );
        return resultTreeStuff;
        }

    public static TreeStuff makeWithAutoCompleteTreeStuff(
        DataNode subjectDataNode,
        DataNode selectedChildDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot
        )
      { 
        TreeStuff resultTreeStuff= new TreeStuff(
            subjectDataNode,
            selectedChildDataNode,
            thePersistent,
            theDataRoot
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
          parentMapEpiNode= // Recurse to get parent EpiNode 
              recordAndTranslateToMapEpiNode(
                  theDataNode.getParentNamedList()); // from parent DataNode
        String keyString= theDataNode.getNameString(); // Get DataNode name.
        dataMapEpiNode= // Get or make MapEpiNode associated with that Node name. 
            parentMapEpiNode.getOrMakeMapEpiNode(keyString); 
        parentMapEpiNode.moveToEndOfListV(keyString); // Move it to end of list.
        return dataMapEpiNode; // Return resulting MapEpiNode.
        }

    private boolean purgeAndTestB(MapEpiNode theMapEpiNode,DataNode theDataNode) //////
      /* This method tries to purge and test purge-ability of theMapEpiNode.
        The purpose is to remove all map entries which contain
        no useful, meaning non-default, information
        in either themselves or their descendants.

        This method is recursively called on all of the child map entries.
        It removes child entries that
        * had true returned from the recursive call, and
        * whose DataNode was NOT child 0, the first child,
          which is the child used for default selection, and
        * whose map entry is not the last one in the map,
          which means it is the most recent selection.
        It treats as purge successes data that is out of spec,
        assuming that such data is best deleted at a higher level.

        This method can not and does not remove 
        the map entry containing theMapEpiNode.
        That MAY be done by the caller if this method returns true.

        This method returns true if none of the children remain.
        It returns false otherwise, meaning that 
        at least one child and possibly some if its descendants survived.
        */
      {
          boolean allPurgedB= true; // Default result for all children purged.
        toReturn: {
          if (null == theMapEpiNode) break toReturn; // No map child entries.
          Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= 
              theMapEpiNode.getLinkedHashMap().entrySet();
          List<Map.Entry<EpiNode,EpiNode>> theListOfMapEntrys= 
              new ArrayList<Map.Entry<EpiNode,EpiNode>>(theSetOfMapEntrys);
                // Copy to avoid ConcurrentModificationException.
          int lastEntryIndexI= theListOfMapEntrys.size()-1;
          Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= 
              theListOfMapEntrys.iterator();
        childLoop: while(true) { // Iterate through child entries.
          if (! entryIterator.hasNext()) break childLoop; // No more children.
        success: { failure: { 
          Map.Entry<EpiNode,EpiNode> childMapEntry= entryIterator.next();
          EpiNode valueEpiNode= childMapEntry.getValue(); // Try to get value.
        removeAndSuccess: {
          if (null == valueEpiNode) break success; // Success if no value.
          MapEpiNode valueMapEpiNode= valueEpiNode.tryMapEpiNode();
          if (null == valueMapEpiNode) break success; // Success if not map.
          String keyString= childMapEntry.getKey().toString();
          DataNode childDataNode= theDataNode.getNamedChildDataNode(keyString);
          if (null == childDataNode) break success; // Success if name failed.
          boolean childPurgeAbleB= // Try recursive child purge and purge test.
            purgeAndTestB(valueMapEpiNode,childDataNode); 
          if (! childPurgeAbleB) break failure; // Failure if test failed.
          if (0 == theDataNode.getIndexOfNamedChild(keyString)) // If is child 0
            break removeAndSuccess; // remove with success.
          if (theListOfMapEntrys.get(lastEntryIndexI) != childMapEntry)
            break removeAndSuccess; // Remove with success if not last entry.
          break failure; // so purge failure.
        } // removeAndSuccess:
          theMapEpiNode.removeEpiNode( // Remove purge-able entry from map
              childMapEntry.getKey().toString()); // by name.
          break success; // Tests passed.  Child removed.  Treat as success.
        } // failure: Something caused child purge to fail.
          allPurgedB= false; // so entire parent map fails purge also.
        } // success: This child is done.
        } // childLoop: Continue with next child entry.
        } // toReturn: We're done.
          return allPurgedB; // Return whether purge of all children succeeded.
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
