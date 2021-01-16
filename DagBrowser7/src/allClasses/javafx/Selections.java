package allClasses.javafx;

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

public class Selections

  /* This class stores and retrieves DataNode selection history.
   * It makes it possible for a user to quickly return close to
   * many different previously visited selection locations 
   * in the DataNode hierarchy.
   * 
   * This system is not complete because,
   * though it stores past selections, 
   * which is very useful for developing,
   * it does not store the final selection path at shutdown,
   * which would be expected by a user who is not a developer.
   */

  {

    // Injected variables.
    private Persistent thePersistent;
    private DataRoot theDataRoot;
    
    // Calculated variables.
    private MapEpiNode selectionHistoryMapEpiNode;
      // This is where selection history is stored to enable
      // easily visiting previously visited children.
      // The first and only element of the map at this level
      // is the map entry associated with the root DataNode.

    public Selections( // Constructor.
        Persistent thePersistent,
        DataRoot theDataRoot
        )
      { 
        this.thePersistent= thePersistent;
        this.theDataRoot= theDataRoot;

        this.selectionHistoryMapEpiNode= // Calculate history root MapEpiNode.
            this.thePersistent.getOrMakeMapEpiNode("SelectionHistory");
        }
    
    public boolean purgeAndTestB(MapEpiNode theMapEpiNode,DataNode theDataNode)
      /* 
        This method tries to purge and test purge-ability of theMapEpiNode.

        The purpose of this method is to save space by removing all map entries 
        in the tree rooted at theMapEpiNode which contain no useful information.
        Not useful information is information which causes behavior 
        which is identical to the behavior if the information was absent.
        At this time the behavior of interest is only
        the selection of previously visited data nodes,
        but this might change.

        The method works by recursively traversing 
        * the MapEpiNode tree rooted at theMapEpiNode, and
        * the DataNode tree rooted at theDataNode
        MapEpiNodes may be deleted but DataNodes are not.
        The DataNode tree is used only to provide context needed for
        deciding what is useful information in the MapEpiNode tree.

        This method is recursively called on all of the child map entries
        and removed what can be deleted.
        It keeps a child map entry, meaning it does not remove one,  
        * that had false returned from the recursive call to this method, or
        * whose associated DataNode, the child DataNode with the same name, 
          is child 0, the first child DataNode, which is used as 
          the default selection if the selection 
          is not specified by a map entry, or
        * whose map entry is the last one in the map, because that one
          is used to represent the most recent selection.

        Any data that is out of spec is removed or treated as purge-able.

        This method can not and does not remove 
        the parent map entry containing theMapEpiNode.
        That MAY be done by the caller if this method returns true.

        This method returns true to indicate a purge success,
        meaning that none of the child map entries remain,
        and the parent map entry MAY be removed.
        It returns false to indicate failure, meaning that 
        at least one child map entry and possibly some if its descendants 
        survived the purge.
        */
      {
          boolean allPurgedB= true; // Default result meaning successful purge.
        toReturn: {
          if (null == theMapEpiNode) // Map missing. 
            break toReturn; // Considering it a purge success.
          Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= 
              theMapEpiNode.getLinkedHashMap().entrySet();
          List<Map.Entry<EpiNode,EpiNode>> theListOfMapEntrys= 
              new ArrayList<Map.Entry<EpiNode,EpiNode>>(theSetOfMapEntrys);
                // Copy to avoid ConcurrentModificationException.
          int lastEntryIndexI= theListOfMapEntrys.size()-1;
          Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= 
              theListOfMapEntrys.iterator();

        entryLoop: while(true) { // Iterate through child map entries.
          if (! entryIterator.hasNext()) break entryLoop; // No more entries.
        entryDone: { keepEntry: { 
          Map.Entry<EpiNode,EpiNode> childMapEntry= entryIterator.next();
        removeEntry: {
          EpiNode valueEpiNode= childMapEntry.getValue(); // Try to get value.
          if (null == valueEpiNode) break removeEntry; // No value.
          MapEpiNode valueMapEpiNode= valueEpiNode.tryMapEpiNode();
          if (null == valueMapEpiNode) break removeEntry; // Value not a map.
          String keyString= childMapEntry.getKey().toRawString(); // Get name.
          DataNode childDataNode= // Get DataNode associated with key name. 
              theDataNode.getNamedChildDataNode(keyString);
          if (null == childDataNode) break removeEntry; // DataNode missing.
          boolean childPurgeAbleB= // Try recursive child entry purge and test.
            purgeAndTestB(valueMapEpiNode,childDataNode); 
          if (! childPurgeAbleB) break keepEntry; // Some sub-entries survived.
          if // If empty map entry is associated with DataNode child 0
            (0 == theDataNode.getIndexOfNamedChild(keyString))
            break removeEntry; // remove it because it is a not needed default.
          if // If empty map entry is not last one in parent map
            (theListOfMapEntrys.get(lastEntryIndexI) != childMapEntry)
            break removeEntry; // remove it because it's not present selection.
          break keepEntry; // so purge failure.

        } // removeEntry:
          theMapEpiNode.removeEpiNode( // Remove purge-able entry from map
              childMapEntry.getKey().toRawString()); // by name.
          break entryDone; // Tests passed.  Child removed.  Done, with success.

        } // keepEntry: Map entry is needed for some reason, so leave it.
          allPurgedB= false; // Record that entire parent map purge failed.

        } // entryDone: processing of this map entry is done.

        } // entryLoop: Continue with next child entry.

        } // toReturn: We're done.
          return allPurgedB; // Return whether purge of all entries succeeded.
        }

    public DataNode getPreviousSelectedDataNode()
      /* This method returns the DataNode representing 
       * the last selection displayed by the app,
       * which is recorded in the selection history.
       * It works by following the path of 
       * recent selections in the Persistent storage tree
       * while following an equivalent path in the DataNode tree,
       * starting each from its respective root.
       * It returns the last DataNode in the DataNode path.
       */
      {
          DataNode scanDataNode= theDataRoot.getRootDataNode();
          MapEpiNode scanMapEpiNode= // Get map of root DataNode. 
            recordAndTranslateToMapEpiNode(scanDataNode);
              // Forces root to be in selection path history.
        loop: while(true) { // Loop to follow selection history path to its end.
          // At this point, we  have a valid [partial] selection.
          String testString= // Get name of next-level candidate selection
              scanMapEpiNode.getKeyString(
                  scanMapEpiNode.getSizeI()-1); // at end of map.
          if (null == testString) break loop; // No name, no selection, exit.
          DataNode testDataNode= // Try getting same-name child DataNode. 
              scanDataNode.getNamedChildDataNode(testString);
          if (null == testDataNode) break loop; // No DataNode, exit.
          MapEpiNode testMapEpiNode= // Try getting next-level map. 
              scanMapEpiNode.getMapEpiNode(testString);
          if (null == testMapEpiNode) break loop; // No next level map, exit.
          // At this point, we have all data needed to go to next level.  Do it.
          scanDataNode= testDataNode;
          scanMapEpiNode= testMapEpiNode;
        } // loop: 
          return scanDataNode; // Return last valid DataNode, the selection.
        }

    public DataNode chooseSelectedDataNode(
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
            break main; // Use it.
          selectedDataNode= // Choose selection from selection history. 
              chooseSelectedDataNode(subjectDataNode); 
        } // main: 
          return selectedDataNode;
      }

    public DataNode chooseSelectedDataNode(
        DataNode subjectDataNode)
      /* This method returns a DataNode to be used as the selection within
       * the subjectDataNode.  
       * It tries to find the most recent selected child
       * from the selection history.
       * If there is none then it tries to return the subject's first child.
       * If there are no children then it returns null.
       */
      {
          DataNode selectedDataNode; // For result selection.
        main: {
          selectedDataNode= chooseFromHistoryDataNode(subjectDataNode);
          if (null != selectedDataNode) // Got child.
            break main; // Return it.
          selectedDataNode= // Try getting first child from node's child list. 
            subjectDataNode.getChild(0);
          // At this point, null means there will be no selection,
          // non-null means result selection is first child of subject.
        } // main: 
          return selectedDataNode;
        }

    public DataNode chooseFromHistoryDataNode(DataNode subjectDataNode)
      /* This method returns a DataNode to be used as the selection within
       * the DataNode subjectDataNode.  
       * It tries to find the most recent selected child
       * from the selection history for that node.
       * If there is none then it returns null.
       */
      {
        MapEpiNode subjectMapEpiNode= // Get subject MapEpiNode from history.
          recordAndTranslateToMapEpiNode(subjectDataNode);
        String selectionNameString= // Get the name of most previous selection,
            subjectMapEpiNode.getKeyString(
                subjectMapEpiNode.getSizeI()-1); // the most recent entry.
        DataNode selectedDataNode= // Try getting child by that name from child list. 
            subjectDataNode.getNamedChildDataNode(selectionNameString);
        return selectedDataNode;
        }

    public MapEpiNode recordAndTranslateToMapEpiNode(DataNode theDataNode)
      /* This method translates theDataNode to 
       * the MapEpiNode at the location in Persistent storage
       * associated with that DataNode.
       * If it needs to create that MapEpiNode,
       * or any others between it and the root of Persistent storage,
       * then it does so.
       * It returns the resulting MapEpiNode.  It never returns null.
       * This is done recursively to simplify path tracking.
       * Execution time is O*d.
       * This method is used both for recording selection path information
       * and for looking up selection path information MapEpiNodes
       * associated with DataNodes.
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
        dataMapEpiNode= // Get or make MapEpiNode associated with Node name. 
            parentMapEpiNode.getOrMakeMapEpiNode(keyString); 
        parentMapEpiNode.moveToEndOfListV(keyString); // Move it to end of list.
        return dataMapEpiNode; // Return resulting MapEpiNode.
        }

    public MapEpiNode getSelectionHistoryMapEpiNode() 
      { 
        return selectionHistoryMapEpiNode; 
        }
  }
