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
   * It makes it possible for a user to quickly return to
   * many different previously visited locations in the DataNode hierarchy.
   * The information is stored in Persistent storage at shutdown
   * so the selection state can be restored during app restart.
   * 
   * ///org At some point this class will probably be divided into:
   *   * Selection
   *   * Hierarchy/Attributes
   */

  {
    final String pathKeyString= "SelectionPath"; // Name of path attribute.
      // Selection path information is stored in this attributes.
    // The following are the 3 levels of path attributes, 
    // from highest to lowest.
    final String pathPresentString= "PRESENT"; // Value indicating that
      // this sibling is part of the active path.
    final String pathLastString= "LAST"; // Value indicating that
      // this sibling was the last one that was part of the path.
    final String pathEarlierString= "EARLIER"; // Value indicating that
      // this sibling was part of the path, but it is not the last one.
      // Maybe change this to indicate its function as a path holder
      // for children.

    //////
    // The above is sufficient to encode path information at app shutdown
    // for reactivation at app restart.
    // But it does not store all the information needed for
    // selection of a previously selected node when executing a move-right.
    // Move-right is presently handled by selecting the most-recently 
    // accessed child, recorded in the linked list in the LinkedHashMap
    // of the MapEpiNode class.
    // To do this without the LinkedHashMap it might be necessary to
    // go to a 3-valued attribute, as was done in PathAttributeMetaTool,
    // which uses path attribute values: "IS", "WAS", and "OLD".
    // Recommended values are: 
    // * "PRESENT" this sibling is presently part of the path.
    // * "LAST" this sibling was the last one that was part of the path.
    // * "EARLIER"  this sibling was part of path but wasn't the last one.
  
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
        ///org This might be eliminated if 
        customized and incremental purging is done.

        This method tries to purge and test 
        the purge-ability of the subtree rooted at subjectMapEpiNode.
        subjectDataNode provides context.
        This method is meant to be used recursively from the root
        to purge the entire hierarchy metadata tree.

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

    @SuppressWarnings("unused") ////// incomplete.
    private boolean purgePathChildrenB(MapEpiNode childrenMapEpiNode)
      /*
        This method tries to purge children 
        that have no useful path information from
        the map at childrenMapEpiNode.
        */
      { return false; }

    private boolean purgeEmptyAttributesB(MapEpiNode attributesMapEpiNode) //////
      /*
        This method tries to purge empty attributes in attributesMapEpiNode.
        All attributes in the map are examined.
        This includes the "Children" attribute, 
        but it doesn't examine individual children 
        for internal purge opportunities.  This should have been done earlier.
        Attributes are kept if their values are scalars or
        if their values are maps that are not empty.
        This method returns true if no attribute remains, false otherwise.
        */
      {
          boolean allPurgedB= true; // Default result meaning complete purge.
        toReturn: {
          if (null == attributesMapEpiNode) // Map is missing.   
            break toReturn; // This shouldn't happen, but consider it a purge.
          ListIterator<Map.Entry<EpiNode,EpiNode>> attributeIterator=
              attributesMapEpiNode.getListIteratorOfEntries();

        attributeLoop: while(true) { // Iterate through attribute map entries.
          if (! attributeIterator.hasNext()) // Exit if no more attributes. 
            break attributeLoop;
        toAttributeDone: { toKeepAttribute: { 
          Map.Entry<EpiNode,EpiNode> attributeMapEntry= 
            attributeIterator.next();
        toRemoveAttribute: {
          EpiNode attributeValueEpiNode= attributeMapEntry.getValue();
          if (null == attributeValueEpiNode) // No value. 
            break toRemoveAttribute; // So remove it.
          MapEpiNode attributeValueMapEpiNode= 
              attributeValueEpiNode.tryMapEpiNode();
          if (null == attributeValueMapEpiNode) // Not a map, must be a Scalar.
            break toKeepAttribute; // So keep it.
          if (0 == attributeValueMapEpiNode.getSizeI()) // It is an empty map. 
            break toRemoveAttribute; // So remove it. 
          break toKeepAttribute; // It's a non-empty map, so keep it.

        } // toRemoveAttribute:
          attributesMapEpiNode.removeEpiNode( // Remove attribute from map
              attributeMapEntry.getKey().toRawString()); // by name.
          break toAttributeDone;

        } // toKeepAttribute: Attribute is needed for some reason, so keep it.
          allPurgedB= false; // Record that at least one entry purge failed.

        } // toAttributeDone: processing of this map entry is done.

        } // attributeLoop: Continue with next attribute entry.
          //// if (0 != attributesMapEpiNode.getSizeI()) // If anything survived
          ////   allPurgedB= false; // return purge failure.

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
       * This method is used at app start up to restore 
       * the selection that existed when the app shut down.
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
          String childString= // Get name of next-level candidate selection.
            getChildSelectionCandidateString(childrenAttributeMapEpiNode);
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

    public DataNode getByAttributeChildMapEpiNode()
      /* This method returns the DataNode representing 
       * the last selection displayed by the app,
       * which is recorded in the selection history.
       * It works by following the path of 
       * recent selections in the Persistent storage tree
       * while following an equivalent path in the DataNode tree,
       * starting each from its respective root.
       * It returns the last DataNode in the DataNode path.
       * This method is used at app start up to restore 
       * the selection that existed when the app shut down.
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
          String childString= // Get name of next-level candidate selection.
            getChildSelectionCandidateString(childrenAttributeMapEpiNode);
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

    private String getChildSelectionCandidateString(
        MapEpiNode childrenAttributeMapEpiNode)
      /* This method returns the name of the child in 
       * childrenAttributeMapEpiNode which has an active path.
       * There should be only one.
       * Returns the name if found, null otherwise.
       * 
       * ///opt use new search-by-attribute method.
       */
      {
          String childNameString= null; // Default of child not found.
        toReturn: {
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
          String pathValueString= childValueMapEpiNode.getString(pathKeyString);
          if (! pathPresentString.equals(pathValueString))// Not active path.
            break toChildDone;
          childNameString= childMapEntry.getKey().toRawString(); // Get name.
          break toReturn; // Exit child loop with child name.
        } // toChildDone: processing of this map entry is done.
        } // childLoop: Continue with next child entry.
        } // toReturn:
          return childNameString;
        }

    private String getByAttributeChildString(   ////////////////////
        MapEpiNode childrenMapEpiNode,String desiredAttributeValueString)
      /* This method returns the name of the child
       * which has a path attribute value of desiredAttributeValueString.
       * If there is no such child then it returns null.
       * This method works by searching until it finds what should be
       * the first and only child with the desired attribute value.
       * 
       * This is potentially a very useful method.
       * It can be used for find path candidates,
       * and for demoting path elements.
       */
      {
          String childString= null; // Set default result for not found.
        toReturn: {
          ListIterator<Map.Entry<EpiNode,EpiNode>> childrenIterator=
              childrenMapEpiNode.getListIteratorOfEntries();
        childLoop: while(true) { // Iterate through child map entries.
          if (! childrenIterator.hasNext()) break childLoop; // No more entries.
        toChildDone: {
          Map.Entry<EpiNode,EpiNode> childMapEntry= 
              childrenIterator.next(); // Get next entry.
          EpiNode childValueEpiNode= childMapEntry.getValue();
          if (null == childValueEpiNode) break toChildDone; // No child value.
          MapEpiNode childValueMapEpiNode= childValueEpiNode.tryMapEpiNode();
          if (null == childValueMapEpiNode) break toChildDone; // Not a map.
          String pathValueString= // Get value of attribute.  Might be null. 
             childValueMapEpiNode.getString(pathKeyString);
          if // Not desired value.
            (! desiredAttributeValueString.equals(pathValueString))
            break toChildDone;
          childString= // Use name of present child as result. 
              childMapEntry.getKey().toRawString();
          break toReturn; // Exit child loop with child name.
        } // toChildDone: processing of this map entry is done.
        } // childLoop: Continue with next child entry.
        } // toReturn:
          return childString;
        }

    public DataNode chooseChildDataNode(
        DataNode subjectDataNode, DataNode preferedDataNode)
      /* This method returns a child DataNode within the subjectDataNode 
       * to be used as the next selection.  
       * This is used by the move-right command.  
       * It returns preferedDataNode if it is not null.
       * Otherwise it tries to find an appropriate child DataNode
       * that may be used as the selection.
       * If it finds one then it returns it.
       * Otherwise it returns null.
       */
      {
          DataNode resultDataNode;
        main: {
          if (null != preferedDataNode) // Preferred selection was provided
            { resultDataNode= preferedDataNode; break main; } // so use it.
          resultDataNode= // Choose an appropriate selection.
              chooseChildDataNode(subjectDataNode); 
        } // main: 
          return resultDataNode;
      }


    public DataNode chooseChildDataNode(DataNode subjectDataNode)
      /* This method returns a child DataNode within the subjectDataNode 
       * to be used as the next selection.  
       * This is used by the move-right command.  
       * First it tries to find the most recent selected child
       * from the selection history.
       * If there is none then it tries to return the subject's first child.
       * If there are no children then it returns null.
       */
      {
          DataNode selectedDataNode; // For result selection.
        main: {
          selectedDataNode= chooseChildFromHistoryDataNode(subjectDataNode);
          if (null != selectedDataNode) // Got previous selection from history.
            break main; // Return it.
          selectedDataNode= // Try getting first child from node's child list. 
            subjectDataNode.getChild(0);
          // At this point, null means there will be no selection,
          // non-null means result selection is first child of subject.
        } // main: 
          return selectedDataNode;
        }


    public DataNode chooseChildFromHistoryDataNode(DataNode subjectDataNode)
      /* This method returns the subjectDataNode's child DataNode 
       * to be used as the selection.  
       * It tries to find the most recently selected child
       * from the selection history for that node.
       * The last child in the selection history is assumed to be that child. 
       * If there is one then it returns that child, otherwise it returns null.
       */
      {
        MapEpiNode subjectMapEpiNode= // Get subject's MapEpiNode from history.
          recordAndTranslateToMapEpiNode(subjectDataNode);
        MapEpiNode childrenAttributeMapEpiNode= // Get map of it's children.
            getOrMakeChildrenMapEpiNode(subjectMapEpiNode);
        String selectionNameString= // Get the name of child which is
          //// childrenAttributeMapEpiNode.getKeyString(
          ////   childrenAttributeMapEpiNode.getSizeI()-1); // the most recent entry.
          getByAttributeChildString(
              childrenAttributeMapEpiNode,pathLastString);
        DataNode selectedDataNode= // Try getting child DataNode by name. 
            subjectDataNode.getNamedChildDataNode(selectionNameString);
        return selectedDataNode;
        }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
    public MapEpiNode recordPathTowardRootAndGetMapEpiNode(
        DataNode subjectDataNode)
      /* This method adjusts PathAttributes to store a selection path.
       * It deactivates the present selection path,
       * and activates a new path from subjectDataNode to the root.
       * It works recursively.
       * It returns the attribute MapEpiNode associated with subjectDataNode.
       */
      {
          MapEpiNode subjectAttributesMapEpiNode; // Used for returned result. 
  
        goRecord: {
          if (subjectDataNode.isRootB()) { // DataNode is the root node.
            subjectAttributesMapEpiNode= hierarchyRootMapEpiNode; // Use root.
            demotePathInChildrenV(subjectAttributesMapEpiNode);
            break goRecord;
            }
          MapEpiNode parentsAttributesMapEpiNode= // Recurse with parent node.
            recordPathTowardRootAndGetMapEpiNode(
              subjectDataNode.getParentNamedList());
          MapEpiNode parentsChildrenMapEpiNode=
            getOrMakeChildrenMapEpiNode(parentsAttributesMapEpiNode);
          String subjectNameKeyString= subjectDataNode.getNameString();
          subjectAttributesMapEpiNode= 
            parentsChildrenMapEpiNode.getOrMakeMapEpiNode(subjectNameKeyString);
          parentsChildrenMapEpiNode.moveToEndOfListV(subjectNameKeyString);
            /// This might be deprecated.
        } // goRecord:
          //// demotePathInChildrenV(subjectAttributesMapEpiNode);
          subjectAttributesMapEpiNode.putV( // [Re]activate this node in path.
              pathKeyString,pathPresentString);
          purgeEmptyAttributesB(subjectAttributesMapEpiNode);
          return subjectAttributesMapEpiNode; // Return resulting MapEpiNode.
        }

    public void OLDdemotePathInChildrenV( //////
        MapEpiNode subjectsAttributesMapEpiNode)
      /* This method deactivates the primary path 
       * in any child for which the path is active.
       * 
       * //// ???? It does this using iteration of 
       * the children in subjectsAttributesMapEpiNode.
       * 
       * This has been changed to be multiple-level recursive.
       * 
       * ////////// It does not properly handle 
       * the 3 different attribute values.
       * What is should do is 
       * * replace PRESENT with LAST
       * * replace LAST with EARLIER if a new LAST was created.
       */
      {
          MapEpiNode childrenMapEpiNode=
            getOrMakeChildrenMapEpiNode(subjectsAttributesMapEpiNode);
          ListIterator<Map.Entry<EpiNode,EpiNode>> childIterator=
              childrenMapEpiNode.getListIteratorOfEntries();
        childLoop: while(true) { // Iterate through child map entries.
          if (! childIterator.hasNext()) break childLoop; // No more entries.
        toChildDone: {
          Map.Entry<EpiNode,EpiNode> childMapEntry= childIterator.next();
          EpiNode childAttributesEpiNode= childMapEntry.getValue();
          if (null == childAttributesEpiNode) break toChildDone; // No value.
          MapEpiNode childAttributesMapEpiNode= 
              childAttributesEpiNode.tryMapEpiNode();
          if (null == childAttributesMapEpiNode) break toChildDone; // Not map.
          if // Try demoting paths in this child. 
            (tryDemotingPathAttributesInB( // If success in subject's attribute
                childAttributesMapEpiNode))
            {
              demotePathInChildrenV( // recurse in subject's children.
                  childAttributesMapEpiNode);
              purgeEmptyAttributesB(childAttributesMapEpiNode);
              }
        } // toChildDone: processing of this child's map entry is done.
        } // childLoop: Continue with next child entry.
          return;
        }

    public void demotePathInChildrenV( ////// new
        MapEpiNode subjectsAttributesMapEpiNode)
      /* This recursive method deactivates the primary path 
       * in any child for which the path is active,
       * starting with the children in subjectsAttributesMapEpiNode.
       *
       * It does this by replacing selection values, as follows:
       * * replace PRESENT with LAST
       * * replace LAST with EARLIER if a new LAST was created.
       */
      {
        MapEpiNode childrenMapEpiNode=
          getOrMakeChildrenMapEpiNode(subjectsAttributesMapEpiNode);
        MapEpiNode presentPathChildMapEpiNode= // Look for path in children. 
          getChildWithSelectionMapEpiNode(
            childrenMapEpiNode,pathPresentString);
        if (null != presentPathChildMapEpiNode) // Present path found.
          { // Demote path in this node and its descendants.
            demotePathInChildrenV( // First recurse in child.
                presentPathChildMapEpiNode);
            MapEpiNode lastPathChildMapEpiNode= getChildWithSelectionMapEpiNode(
                childrenMapEpiNode,pathLastString); // Look for last path.
            if (null != lastPathChildMapEpiNode) // If found, replace it.
              presentPathChildMapEpiNode.putV(pathKeyString,pathEarlierString);
            presentPathChildMapEpiNode.putV(pathKeyString,pathLastString);
            }
        }

    private MapEpiNode getChildWithSelectionMapEpiNode(
        MapEpiNode childrenMapEpiNode,String desiredValueString)
      /* This method searches the children in childrenMapEpiNode for 
       * a child with a selection attribute with value of desiredValueString.
       * It returns the attributes MapEpiNode of the found child,
       * or null if not found. 
       */
      {
          MapEpiNode childAttributesMapEpiNode;
          ListIterator<Map.Entry<EpiNode,EpiNode>> childIterator=
              childrenMapEpiNode.getListIteratorOfEntries();
        childLoop: while(true) { // Iterate through child map entries.
          if (! childIterator.hasNext()) // Exit if no more child entries.
            { childAttributesMapEpiNode= null; break childLoop; }
        toChildDone: {
          Map.Entry<EpiNode,EpiNode> childMapEntry= childIterator.next();
          EpiNode childAttributesEpiNode= childMapEntry.getValue();
          if (null == childAttributesEpiNode) break toChildDone; // No value.
          childAttributesMapEpiNode= childAttributesEpiNode.tryMapEpiNode();
          if (null == childAttributesMapEpiNode) break toChildDone; // Not map.
          String pathValueString= // Get the path attribute value, if any. 
            childAttributesMapEpiNode.getString(pathKeyString);
          if (desiredValueString.equals(pathValueString)) // Exit loop if found.
            break childLoop;
        } // toChildDone: processing of this child's map entry is done.
        } // childLoop: Continue with next child entry.
          return childAttributesMapEpiNode;
        }
    
    public boolean tryDemotingPathAttributesInB(
        MapEpiNode subjectAttributesMapEpiNode)
      /* This method tries to demote the path attribute one level, 
       * if the attribute is present.
       * It returns true if demotion was successful, false otherwise.
       * 
       * Presently this demotes either PRESENT or LAST.
       * This is not usable as is.
       */
      {
        boolean resultB= true; // Assume an attribute substitution will be made.
        String pathValueString= // Get the present path attribute value, if any. 
            subjectAttributesMapEpiNode.getString(pathKeyString);

        if (pathPresentString.equals(pathValueString)) // Try 1st substitution.
          subjectAttributesMapEpiNode.putV(pathKeyString,pathLastString);
        else if (pathLastString.equals(pathValueString)) // Try 2nd.
          subjectAttributesMapEpiNode.putV(pathKeyString,pathEarlierString);
        else // Indicate that both substitutions failed.
          resultB= false;

        return resultB;
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
      /* This method gets or makes 
       * the "Children" attribute value from parentMapEpiNode.
       */
      {
        return parentMapEpiNode.getOrMakeMapEpiNode("Children");
        }

    }