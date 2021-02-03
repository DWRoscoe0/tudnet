package allClasses.javafx;

import java.util.ListIterator;
import java.util.Map;

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
   * 
   * ///org At some point this will probably be divided into:
   *   * Selection
   *   * Hierarchy/Attributes
   */

  {
    final String selectionPathKeyString= "SelectionPath";
  
    // Injected variables.
    private Persistent thePersistent;
    private DataRoot theDataRoot;
    
    // Calculated variables.
    private MapEpiNode hierarchyRootMapEpiNode;
      // This is where selection history and other attributes are stored 
      // to enable easily visiting previously visited children.
      // The first and only element of the map at this level
      // is the map entry associated with the root DataNode.

    public Selections( // Constructor.
        Persistent thePersistent,
        DataRoot theDataRoot
        )
      { 
        this.thePersistent= thePersistent;
        this.theDataRoot= theDataRoot;

        this.hierarchyRootMapEpiNode= // Calculate root MapEpiNode.
            this.thePersistent.getOrMakeMapEpiNode("HierarchyMetaDataRoot");
        }

    public boolean purgeAndTestB(
        MapEpiNode subjectMapEpiNode,DataNode subjectDataNode)
      /* 
        This method tries to purge and test 
        the purge-ability of subjectMapEpiNode.

        The purpose of this method is to save space by removing all map entries 
        in the tree rooted at theMapEpiNode which contain no useful information.
        Not useful information is information which causes behavior 
        which is identical to the behavior if the information was absent.
        At this time the behavior of interest is only
        the selection of previously visited data nodes,
        but this might change.

        The method works by recursively traversing 
        * the MapEpiNode tree rooted at theMapEpiNode, and
        * the DataNode tree rooted at subjectDataNode
        MapEpiNodes may be deleted but DataNodes are not.
        The DataNode tree is used only to provide context needed for
        deciding what is useful information in the MapEpiNode tree.

        This method is recursively called on all of the child map entries
        and removes what can be deleted.
        It keeps a child map entry, meaning it does not remove one,  
        * that had false returned from the recursive call to this method, or
        * whose associated DataNode, the child DataNode with the same name, 
          is child 0, the first child DataNode, which is used as 
          the default selection if the selection 
          is not specified by a map entry, or
        * whose map entry is the last one in the map, because that one
          is used to represent the most recent selection.

        Any data that is out of specification considered purge-able.

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
          if (null == subjectMapEpiNode) // Map missing. 
            break toReturn; // Considering it a purge success.
          MapEpiNode childrenAttributeMapEpiNode= // Get Children attribute map. 
              getOrMakeChildrenMapEpiNode(subjectMapEpiNode);
          //// int childCountI= childrenAttributeMapEpiNode.getSizeI();
          ListIterator<Map.Entry<EpiNode,EpiNode>> childIterator=
              childrenAttributeMapEpiNode.getListIteratorOfEntries();

        childLoop: while(true) { // Iterate through child map entries.
          if (! childIterator.hasNext()) break childLoop; // No more entries.
        toChildDone: { toKeepChild: { 
          Map.Entry<EpiNode,EpiNode> childMapEntry= childIterator.next();
        toRemoveChild: {
          EpiNode childValueEpiNode= childMapEntry.getValue();
          if (null == childValueEpiNode) break toRemoveChild; // No child value.
          MapEpiNode childValueMapEpiNode= childValueEpiNode.tryMapEpiNode();
          if (null == childValueMapEpiNode) break toKeepChild; // Keep non-map.
          String childKeyString= childMapEntry.getKey().toRawString(); // Name.
          DataNode childDataNode= // Get DataNode for this child key name. 
              subjectDataNode.getNamedChildDataNode(childKeyString);
          if (null == childDataNode) break toRemoveChild; // No DataNode.
          boolean childPurgeAbleB= // Try recursive child entry purge and test.
            purgeAndTestB(childValueMapEpiNode,childDataNode); 
          if (childPurgeAbleB) // Child is purge-able.
            break toRemoveChild; // Go remove it. 
          //// if (! childPurgeAbleB) // Some sub-entries survived. 
          ////   break toKeepChild;
          //// if // If empty map entry is associated with DataNode child 0
          ////   (0 == subjectDataNode.getIndexOfNamedChild(childKeyString))
          ////   break toRemoveChild; // remove it because it's a not needed default.
          //// if // If empty map entry is not last one in parent map
          ////   (childCountI != childIterator.nextIndex()) // If not last entry 
          ////   break toRemoveChild; // remove it because it's not selected now.
          break toKeepChild; // Keep child because all purge options failed.

        } // toRemoveChild:
          childrenAttributeMapEpiNode.removeEpiNode( // Remove child from map
              childMapEntry.getKey().toRawString()); // by name.
          break toChildDone; // Tests passed, child removed, done, with success.

        } // toKeepChild: Map entry is needed for some reason, so leave it.
          allPurgedB= false; // Record that some of parent map purge failed.

        } // toChildDone: processing of this map entry is done.

        } // childLoop: Continue with next child entry.
          if (0 != subjectMapEpiNode.getSizeI()) // If anything survived
            allPurgedB= false; // return purge failure.

        } // toReturn: Everythng's done.
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
          MapEpiNode childrenAttributeMapEpiNode= // Get Children attribute map. 
            getOrMakeChildrenMapEpiNode(scanMapEpiNode);
          String childString= // Get name of next-level candidate selection
            childrenAttributeMapEpiNode.getKeyString(
              childrenAttributeMapEpiNode.getSizeI()-1); // at end of map.
          if (null == childString) break loop; // No name, no selection, exit.
          DataNode childDataNode= // Try getting same-name child DataNode. 
              scanDataNode.getNamedChildDataNode(childString);
          if (null == childDataNode) break loop; // No child DataNode, exit.
          MapEpiNode childMapEpiNode= // Try getting next-level child map. 
              childrenAttributeMapEpiNode.getMapEpiNode(childString);
          if (null == childMapEpiNode) break loop; // No next level map, exit.
          // At this point, we have all data needed to go to next level.  Do it.
          scanDataNode= childDataNode;
          scanMapEpiNode= childMapEpiNode;
        } // loop: 
          return scanDataNode; // Return last valid DataNode, the selection.
        }

    public DataNode chooseSelectionDataNode(
        DataNode subjectDataNode, DataNode selectedDataNode)
      /* This method returns a DataNode to be used as the selection within
       * the subjectDataNode.  
       * It returns selectedDataNode if it is not null.
       * Otherwise it tries to find an appropriate child DataNode
       * that may be used as the selection.
       * If it finds one then it returns it.
       * Otherwise it returns null.
       */
      {
        main: {
          if (null != selectedDataNode) // Selection was provided as parameter
            break main; // so return it.
          selectedDataNode= // Choose an appropriate selection.
              chooseSelectionDataNode(subjectDataNode); 
        } // main: 
          return selectedDataNode;
      }

    public DataNode chooseSelectionDataNode(DataNode subjectDataNode)
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
          if (null != selectedDataNode) // Got previous selection from history.
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
        MapEpiNode childrenAttributeMapEpiNode= 
            getOrMakeChildrenMapEpiNode(subjectMapEpiNode);
        String selectionNameString= // Get the name of most recent selection,
          childrenAttributeMapEpiNode.getKeyString(
            childrenAttributeMapEpiNode.getSizeI()-1); // the most recent entry.
        DataNode selectedDataNode= // Try getting child by name from child list. 
            subjectDataNode.getNamedChildDataNode(selectionNameString);
        return selectedDataNode;
        }

    public MapEpiNode recordPathTowardRootAndGetMapEpiNode(  //// new.
        DataNode subjectDataNode)
      /* This method adjusts PathAttributes to store a selection path
       * from subjectDataNode to the root.
       * It add new path information and removes old path information.
       * It works recursively.
       * It returns the attribute MapEpiNode associated with subjectDataNode.
       */
      {
          //// /*  //////
          MapEpiNode subjectMapEpiNode; // Used for returned result. 
          MapEpiNode parentMapEpiNode;
          MapEpiNode parentsChildrenMapEpiNode;
  
        goRecord: {
          if (subjectDataNode.isRootB()) { // DataNode is the root node.
            subjectMapEpiNode= hierarchyRootMapEpiNode; // So use root MapEpiNode.
            break goRecord;
            }
          parentMapEpiNode= recordPathTowardRootAndGetMapEpiNode( // Recurse
            subjectDataNode.getParentNamedList()); // with parent of subject.
          String subjectKeyString= subjectDataNode.getNameString(); // Get name.
          parentsChildrenMapEpiNode=
              getOrMakeChildrenMapEpiNode(parentMapEpiNode);
          subjectMapEpiNode= 
              parentsChildrenMapEpiNode.getOrMakeMapEpiNode(subjectKeyString);
          parentsChildrenMapEpiNode.moveToEndOfListV(subjectKeyString);
            /// This might be deprecated.
        } // goRecord:
          subjectMapEpiNode.putV( // Record this node part of selected path.
              selectionPathKeyString,"NOW");
          return subjectMapEpiNode; // Return resulting MapEpiNode.
          //// */  //////
          //// return null;
        }

    public void removeSelectionPathV(MapEpiNode subjectMapEpiNode)  ////// new.  Not used yet.
      /* This method adjusts PathAttributes to deactivate 
       * the present selection path starting with subjectMapEpiNode.  
       * It works recursively.
       */
      {
          final String pathKeyString= "SelectionPath";
          /// final String pathNewValueString= "NOW";
          final String pathOldValueString= "LAST";
        toReturn: {
          String pathValueString= subjectMapEpiNode.getString(pathKeyString);
          if (null == pathValueString) // Exit immediately if key not present. 
            break toReturn;
          MapEpiNode childrenAttributeMapEpiNode= // Get Children attribute map. 
              getOrMakeChildrenMapEpiNode(subjectMapEpiNode);
          ListIterator<Map.Entry<EpiNode,EpiNode>> childIterator=
              childrenAttributeMapEpiNode.getListIteratorOfEntries();
        childLoop: while(true) { // Iterate through child map entries.
          if (! childIterator.hasNext()) break childLoop; // No more entries.
        toChildDone: { 
          Map.Entry<EpiNode,EpiNode> childMapEntry= childIterator.next();
          EpiNode childValueEpiNode= childMapEntry.getValue();
          if (null == childValueEpiNode) break toChildDone; // No child value.
          MapEpiNode childValueMapEpiNode= childValueEpiNode.tryMapEpiNode();
          if (null == childValueMapEpiNode) break toChildDone; // Not a map.
          removeSelectionPathV(childValueMapEpiNode); // Recursive call with child.
        } // toChildDone: processing of this map entry is done.
        } // childLoop: Continue with next child entry.
          subjectMapEpiNode.putV( // Replace new path value with old value.
              pathKeyString, pathOldValueString);
        } // toReturn:
          return;
        }

    @SuppressWarnings("unused")
    private MapEpiNode getChildMapEpiNode(String keyString,String valueString)
    {
      /*  ////
          MapEpiNode childMapEpiNode;
        toReturn: {
          if (null == childMapEpiNode) // Exit if key not present. 
            break goReturn;
          if (! childMapEpiNode.testKeyForValueB(keyString,valueString))
            break toReturn; // Exit if value not present.
        } // toReturn:
      */  ////
          return null; //////
      }

    public MapEpiNode OLDrecordAndTranslateToMapEpiNode(DataNode subjectDataNode)
      /* This method translates subjectDataNode to 
       * the MapEpiNode at the location in Persistent storage
       * associated with that DataNode.
       * If it needs to create that MapEpiNode,
       * or any others between it and the root of Persistent storage,
       * then it does so.
       * It returns the resulting MapEpiNode.  It never returns null.
       * This is done recursively to simplify path tracking.
       * Execution time is O*d where d is the tree depth traversed.
       * This method is used both for recording selection path information
       * and for looking up selection path information MapEpiNodes
       * associated with DataNodes.
       */
      {
        MapEpiNode subjectMapEpiNode; 
        MapEpiNode parentMapEpiNode;
        MapEpiNode parentsChildrenMapEpiNode;

        if (subjectDataNode.isRootB()) // DataNode is the root node.
          parentMapEpiNode= // So use root as parent EpiNode.
            hierarchyRootMapEpiNode; 
          else  // DataNode is not Root.
          parentMapEpiNode= // Recurse to get parent EpiNode 
              recordAndTranslateToMapEpiNode(
                  subjectDataNode.getParentNamedList()); // from parent DataNode
        String subjectKeyString= subjectDataNode.getNameString(); // Get name.
        parentsChildrenMapEpiNode= 
            getOrMakeChildrenMapEpiNode(parentMapEpiNode);
        subjectMapEpiNode= 
            parentsChildrenMapEpiNode.getOrMakeMapEpiNode(subjectKeyString); 
        parentsChildrenMapEpiNode.moveToEndOfListV(subjectKeyString);
        return subjectMapEpiNode; // Return resulting MapEpiNode.
        }

    public MapEpiNode recordAndTranslateToMapEpiNode(DataNode subjectDataNode)
      /* This method translates subjectDataNode to 
       * the MapEpiNode at the location in Persistent storage
       * associated with that DataNode.
       * If it needs to create that MapEpiNode,
       * or any others between it and the root of Persistent storage,
       * then it does so.
       * It returns the resulting MapEpiNode.  It never returns null.
       * This is done recursively to simplify path tracking.
       * Execution time is O*d where d is the tree depth traversed.
       * This method is used both for recording selection path information
       * and for looking up selection path information MapEpiNodes
       * associated with DataNodes.
       */
      {
          MapEpiNode subjectMapEpiNode; // Value to be returned. 
          MapEpiNode parentMapEpiNode;
          MapEpiNode parentsChildrenMapEpiNode;

        toReturn: {
          if (subjectDataNode.isRootB()) { // subjectDataNode is the root node.
            subjectMapEpiNode= hierarchyRootMapEpiNode; // Return it.
            break toReturn;
            }
          DataNode parentDataNode= subjectDataNode.getParentNamedList();
          parentMapEpiNode= // Recursing to translate parent. 
                recordAndTranslateToMapEpiNode(parentDataNode);
          String subjectKeyString= subjectDataNode.getNameString(); // Get name.
          parentsChildrenMapEpiNode= 
              getOrMakeChildrenMapEpiNode(parentMapEpiNode);
          subjectMapEpiNode= 
              parentsChildrenMapEpiNode.getOrMakeMapEpiNode(subjectKeyString); 
          parentsChildrenMapEpiNode.moveToEndOfListV(subjectKeyString);
        } // toReturn:
          return subjectMapEpiNode;
        }

    public MapEpiNode getHierarchyAttributesMapEpiNode() 
      { 
        return hierarchyRootMapEpiNode; 
        }

    private MapEpiNode getOrMakeChildrenMapEpiNode(MapEpiNode parentMapEpiNode)
      /* This method get the "Children" attribute value from parentMapEpiNode.
       */
      { 
        return parentMapEpiNode.getOrMakeMapEpiNode("Children");
        }

    }