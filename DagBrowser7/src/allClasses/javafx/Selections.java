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
  
    // The following are definitions used to describe path information.
  
    final String pathKeyString= "SelectionPath"; // Name of path attribute.
      // Selection path information is stored in this attributes.
    
    // The following are the 3 values of path attributes.
    final String pathActiveString= "ACTIVE"; // Value indicating that
      // this sibling is part of the active path.
    final String pathNextString= "NEXT"; // Value indicating that
      // this sibling should become ACTIVE if its parent is ACTIVE 
      // if there is a move from there to an unspecified child.
    final String pathHoldingString= "HOLDING"; // Value indicating that
      // this sibling is holding NEXT or HOLDING path attributes in
      // one or more of its children.

    /* A set of siblings may contain the following types of nodes:
     * * One ACTIVE node, or one NEXT node, or neither, but not both.
     * * Any number of HOLDING nodes.
     * * Any number of null nodes.
     * 
     * For all nodes except the root node:
     * * The parent of an ACTIVE node must be ACTIVE.  
     *   Its children may be of any type.
     * * The parent of a NEXT node must be ACTIVE, NEXT, or HOLDING.
     *   Its children may be of any type.
     * * The parent of a HOLDING node must be ACTIVE, NEXT, or HOLDING.
     *   It must have at least one NEXT or HOLDING child node.
     * * The parent of a null node may be anything.
     *   All of its children must null.
     * The root node is always an ACTIVE node.
     */
    
    /* This above 3-valued attribute system is based loosely on 
     * class PathAttributeMetaTool, which uses path attribute values: 
     * "IS", "WAS", and "OLD".
     *
     * Constraints: The node attribute values are not independent.
     *   When one changes, the values of siblings or children
     *   often must be checked and change also.
     *   * If a node value is ACTIVE then its parent's value 
     *     must also be ACTIVE unless it is the root which has no parent.
     *   * Only one of a set of siblings may be ACTIVE or NEXT.
     *     Each of the remaining siblings may be either
     *     a HOLDING or have no value.
     *   * If a node is HOLDING then at least one of its children
     *     must have an attribute value.
     *     
     * How to maintain these constraints?
     * * Make changes, and make post-change adjustments to restore constraints.
     * * Check conditions before making changes, and make only
     *   those changes that maintain constraints.
     * * A combination of the above.
     * 
     * recordPathTowardRootAndGetMapEpiNode(subjectDataNode) does all this.
     * See its documentation for how.
     * 
     */
    
  
    // Injected variables.
    private DataRoot theDataRoot;
    private Persistent thePersistent;

    // Calculated variables.

    // This is where selection history and other attributes are stored 
    // to enable easily visiting previously visited children.
    private MapEpiNode hierarchyRootParentsAttributesMapEpiNode;

    public Selections( // Constructor.
        Persistent thePersistent,
        DataRoot theDataRoot
        )
      { 
        this.theDataRoot= theDataRoot;
        this.thePersistent= thePersistent;

        hierarchyRootParentsAttributesMapEpiNode=
            thePersistent.getOrMakeMapEpiNode(
                "HierarchyRootsParentsAttributes");
        }


    /* Purge methods.
     * These need work.  Useless path information is not being purged.
     * Specifically, LAST children which are siblings of PRESENT children,
     * are not being purged.
     */

    private void purgeChild1WithUnneededHoldingIn2V(
        String subjectsNameString,MapEpiNode parentsAttributesMapEpiNode)
      /* This method is a path-specific purge method.
       * If the subject child specified by the name subjectsNameString
       * in parentsAttributesMapEpiNode contains HOLDING in its path attribute
       * and that subject has no children with path attributes of their own,
       * then it will remove the HOLDING path attribute from the subject,
       * and if no attributes remain in the subject,
       * it will be removed from the parent's Children list attribute,
       * and if the Children list attribute becomes empty, 
       * the Children list attribute will be removed from 
       * the parent's attribute list.
       */
      {
        toExit: {
          MapEpiNode parentsChildrenMapEpiNode= // Get children from parent.
            getOrMakeChildrenMapEpiNode(parentsAttributesMapEpiNode);
          MapEpiNode subjectsAttributesMapEpiNode= // Get subject attributes.
              parentsChildrenMapEpiNode.getMapEpiNode(subjectsNameString);
        removeOurAttribute: {
          if (null == subjectsAttributesMapEpiNode) // Subject map is missing.   
            break toExit; // This shouldn't happen, so exit.
          String pathValueString= // Get value of subject's path attribute. 
             subjectsAttributesMapEpiNode.getString(pathKeyString);
          if // The value is not HOLDING, which is the only case of interest,
            (! pathHoldingString.equals(pathValueString))
            break toExit; // so exit without doing anything.
          MapEpiNode subjectsChildrenMapEpiNode= // Get subject's children.
              getChildrenMapEpiNode(subjectsAttributesMapEpiNode);
          if (null == subjectsChildrenMapEpiNode) // If subject has no children 
            break removeOurAttribute;// go remove subject's attribute.
          String childNameString= // Search subject's children for one
            getChildNameIn1WithPathValue2String( // with any path attribute.
                subjectsChildrenMapEpiNode,null);
          if (null != childNameString) // Child found with a path attribute
            break toExit; // so exit without removing our path attribute.
        } // removeOurAttribute: We have an unneeded path attribute
          subjectsAttributesMapEpiNode.removeV( // so remove it.
              pathKeyString);
          purgeChild1InAttributes2IfEmptyV(
            subjectsNameString,parentsAttributesMapEpiNode);
        } // toExit: Everythng's done
          return; // so exit.
        }

    private void purgeChild1InAttributes2IfEmptyV(
            String subjectsNameString,MapEpiNode parentsAttributesMapEpiNode)
      /* This method purges a subject and its Children contain, if possible.
       * If no attributes remain in the subject's attributes map then
       * the subject will be removed from the parent's Children map attribute.
       * If the parent's Children map attribute becomes empty, 
       * then the Children map attribute will be removed from 
       * the parent's attribute map.
       */
      {
        toExit: {
          MapEpiNode parentsChildrenMapEpiNode= // Get Children from parent.
            getChildrenMapEpiNode(parentsAttributesMapEpiNode);
        childrenPurge: {
          if (null == parentsChildrenMapEpiNode) // Parent has no Children   
            break toExit; // so exit because there is nothing to purge.
          MapEpiNode subjectsAttributesMapEpiNode= // Get subject attributes.
              parentsChildrenMapEpiNode.getMapEpiNode(subjectsNameString);
          if (null == subjectsAttributesMapEpiNode) // Subject map is missing   
            break childrenPurge; // so go try purging children map now.
          if // Subject map is present but empty
            (0 == subjectsAttributesMapEpiNode.getSizeI())
            parentsChildrenMapEpiNode.removeEpiNode( // so remove from children
              subjectsNameString); // the subject map by name.
        } // childrenPurge:
          if // There are no children in Children attribute map
            (0 == parentsChildrenMapEpiNode.getSizeI())
            parentsAttributesMapEpiNode.removeEpiNode( // so remove from parent.
              "Children"); // 
        } // toExit: Everythng possible has been done
          return; // so exit.
        }

    private boolean purgeAttributesIn1B(MapEpiNode attributesMapEpiNode)
      /*
        This method tries to purge empty attributes in attributesMapEpiNode.
        All attributes in the map are examined.
        This includes the "Children" attribute, 
        but it doesn't examine individual children 
        for internal purge opportunities.  
        This should have been done earlier in path-specific purge code.
        Attributes are kept if their values are scalars or
        if their values are maps that are not empty.
        Attributes are removed if their values are empty maps.
        This method returns true if no attribute remains, false otherwise.
        */
      {
          boolean allPurgedB= true; // Default result meaning complete purge.
        toExit: {
          if (null == attributesMapEpiNode) // Map is missing.   
            break toExit; // This shouldn't happen, but consider it a purge.
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
          if (null == attributeValueMapEpiNode) // Not a map, must be a Scalar,
            break toKeepAttribute; // so keep it.
          if (0 == attributeValueMapEpiNode.getSizeI()) // It is an empty map 
            break toRemoveAttribute; // so remove it. 
          break toKeepAttribute; // It's a non-empty map, so keep it.

        } // toRemoveAttribute:
          attributesMapEpiNode.removeEpiNode( // Remove attribute from map
              attributeMapEntry.getKey().toRawString()); // by name.
          break toAttributeDone;

        } // toKeepAttribute: Attribute is needed for some reason, so keep it.
          allPurgedB= false; // Record that at least one entry failed the purge.

        } // toAttributeDone: processing of this map entry is done.

        } // attributeLoop: Loop to continue with next attribute entry.

        } // toExit: Everythng's done.
          return allPurgedB; // Return whether purge of all entries succeeded.
        }


    // Mutator methods.

    public void adjustForNewSelectionV(DataNode subjectDataNode)
      /* This method adjusts the information in Persistent storage
       * about what was and is selected.
       * The new selection is subjectDataNode.
       * It makes adjustments in this node, nodes closer to the root,
       * and nodes farther from the root.
       */
      { 
        MapEpiNode parentsAttributesMapEpiNode= // Adjust here and toward root.
          recordPathFrom1ToRootAndGetParentsAttributesMapEpiNode(
            subjectDataNode);

        MapEpiNode parentsChildrenMapEpiNode= // Get children from attributes.
          getOrMakeChildrenMapEpiNode(parentsAttributesMapEpiNode);
        
        { // Adjust away from root.
          String subjectsNameKeyString= subjectDataNode.getNameString();
          MapEpiNode subjectsAttributesMapEpiNode=
              parentsChildrenMapEpiNode.getMapEpiNode(subjectsNameKeyString);

          deactivatePathFrom1V(subjectsAttributesMapEpiNode);
          }
        thePersistent.signalDataChangeV(); // Cause save of selection state.
        }

    /// new method.
    public MapEpiNode recordPathFrom1ToRootAndGetParentsAttributesMapEpiNode(
        DataNode subjectDataNode)
      /*
       * This method adjusts PathAttributes to activate a new path
       * from the root to subjectDataNode while deactivating the old path
       * defined by the path attributes in Persistent storage.
       * It also returns the map of the attributes of the subject's parent.
       * This method uses recursion and works in 2 phases:
       * * Recursively calls itself until the root DataNode is reached.
       *   It does this to associate MapEpiNodes with DataNodes.
       * * Returns from previous recursive calls 
       *   while examining the state of the subject's siblings,
       *   and adjusting the path attributes of 
       *   its sibling's subtrees appropriately. 
       *   This return phase is divided into 3 sub-phases:
       *   1: Where the old path node and new path node are the same node.
       *     In this case the subject node is already ACTIVE,
       *     so nothing needs to be changed.
       *   2: Where the old and new paths do not match.
       *     This is the level at which the paths diverge.
       *     It is where the old path is ACTIVE and the new path is not, yet.
       *     In this case the old path must be deactivated.
       *     The ACTIVE node in the old path will become a HOLDING node,
       *     and converted to NEXT in the descendants.
       *     No HOLDING nodes need to change.
       *     A non-ACTIVE node in the new path will become an ACTIVE node.
       *     This piece of path might have a length of 1,
       *     or 0, in which case nothing actually happens.
       *   3: After path divergence, the new part of the desired path
       *     must be activated by storing ACTIVE in the subject node.
       *     Because ACTIVE and NEXT can not be siblings,
       *     any NEXT sibling[s] must be demoted to HOLDING first,
       *     but in this case nothing needs to be done in the subtree[s]. 
       *     This piece of path might have a length of 0.
       */
      {
          MapEpiNode parentsChildrenMapEpiNode;
          MapEpiNode parentsAttributesMapEpiNode; // Used for returned result.
          String subjectNameKeyString= subjectDataNode.getNameString();
  
        recursingOrNot: {
          if (subjectDataNode == theDataRoot.getRootDataNode()) // At root
            { // so use root children, don't call recursively.
              parentsAttributesMapEpiNode= getRootParentsAttributesMapEpiNode();
              break recursingOrNot;
              }
          // We need to do a recursive call.
          DataNode parentDataNode= subjectDataNode.getParentNamedList();
          String parentNameKeyString= parentDataNode.getNameString();
          MapEpiNode grandParentsAttributesMapEpiNode= // Recurse with parent
            recordPathFrom1ToRootAndGetParentsAttributesMapEpiNode(
              parentDataNode); // to get grandparent's attributes map.
          MapEpiNode grandParentsChildrenMapEpiNode=
              getOrMakeChildrenMapEpiNode(grandParentsAttributesMapEpiNode);
          parentsAttributesMapEpiNode=
            grandParentsChildrenMapEpiNode.getOrMakeMapEpiNode(
                parentNameKeyString);
        } // recursingOrNot:
          parentsChildrenMapEpiNode=
              getOrMakeChildrenMapEpiNode(parentsAttributesMapEpiNode);
          MapEpiNode subjectAttributesMapEpiNode= 
            parentsChildrenMapEpiNode.getOrMakeMapEpiNode(subjectNameKeyString);
          String subjectPathValueString= 
              subjectAttributesMapEpiNode.getString(pathKeyString);
        adjustThisLevel: { goActivateSubject: {
          String activeNodeNameString= // Get name of old ACTIVE node, if any.
            getByAttributeChildString(
              parentsChildrenMapEpiNode,pathActiveString);
          if // We have returned past the point of path divergence because
            (null == activeNodeNameString) { // there is no old ACTIVE node.
              demoteSiblingsOf1InAttributes2ToHoldingV( // so check a`nd demote siblings
                  subjectPathValueString,parentsAttributesMapEpiNode);
              break goActivateSubject; // and go make subject node ACTIVE.
              }
          if // We have not yet returned to the point of path divergence because
            (subjectNameKeyString.equals( // the subject node is ACTIVE
                activeNodeNameString))
            break adjustThisLevel; // so exit because no adjustment is needed.
          // We are at the point of divergence
          demoteSiblingsOf1InAttributes2ToHoldingV( // so check and demote sibling[s].
              subjectNameKeyString,parentsAttributesMapEpiNode);
        } // goActivateSubject:
          subjectAttributesMapEpiNode.putV( // [Re]activate this node in path.
              pathKeyString,pathActiveString);
        } // adjustThisLevel:
          purgeAttributesIn1B(subjectAttributesMapEpiNode);
          return parentsAttributesMapEpiNode;
        }

    private void deactivatePathFrom1V(MapEpiNode parentAttributesMapEpiNode)
      /* This method deactivates the active path in 
       * the children of parentAttributesMapEpiNode.
       * It does this by finding and replacing any ACTIVE attribute values
       * with NEXT.
       * 
       * Because this does a simple replacement, no purge checks are needed.
       * 
       * This method is being combined with anddeactivatePathIn1V(.). 
       * ///fix Make it not create "Children" attribute if it doesn't exist.
       s*/
      {
        toExit: {
          if (null == parentAttributesMapEpiNode) // If there are no parent map
            break toExit; // exit because parental access is required.
          MapEpiNode parentsChildrenMapEpiNode=
            getChildrenMapEpiNode(parentAttributesMapEpiNode);
          if (null == parentsChildrenMapEpiNode) // If there are no children
            break toExit; // exit because a path in children requires children.
          MapEpiNode activeSubjectAttributesMapEpiNode= // Look for child 
            getChildIn1WithPathValue2MapEpiNode(
              parentsChildrenMapEpiNode,pathActiveString); // in active path.
          if (null == activeSubjectAttributesMapEpiNode) // Not in Active path
            break toExit; // so exit.
          deactivatePathFrom1V( // First recurse in found child's children.
              activeSubjectAttributesMapEpiNode);
          activeSubjectAttributesMapEpiNode.putV( // In this node, replace 
              pathKeyString,pathNextString); // ACTIVE with NEXT.
        } // toExit:
          return;

        }

    private void demoteSiblingsOf1InAttributes2ToHoldingV(
        String subjectNameString,MapEpiNode parentsAttributesMapEpiNode)
      /* This method demotes siblings of the node 
       * whose name is subjectNameString in the map parentsAttributesMapEpiNode 
       * to the HOLDING state.
       * This method is used when a subject node
       * is going to become either ACTIVE or NEXT
       * from a state that is neither ACTIVE or NEXT.
       * This method demotes to HOLDING any sibling nodes 
       * that are ACTIVE or NEXT, though there should be only one.
       * If it demotes an ACTIVE sibling node then it
       * will also demote to NEXT any ACTIVE descendants of that sibling.
       * 
       * /// Purging: If a node is set to HOLDING,
       * but the node has no children with path attributes,
       * then the HOLDING attribute is removed
       * and the child node becomes eligible for purging.
       */
      {
          MapEpiNode parentsChildrenMapEpiNode=
              getOrMakeChildrenMapEpiNode(parentsAttributesMapEpiNode);
          MapEpiNode childAttributesMapEpiNode;
          ListIterator<Map.Entry<EpiNode,EpiNode>> parentsChildrenIterator=
              parentsChildrenMapEpiNode.getListIteratorOfEntries();
        childLoop: while(true) { // Iterate through child map entries.
          if (! parentsChildrenIterator.hasNext()) // No more child entries 
            break childLoop; // so exit loop.
          Map.Entry<EpiNode,EpiNode> childMapEntry=
              parentsChildrenIterator.next(); // Get next child entry.
          String childKeyNameString= childMapEntry.getKey().toRawString();
        toChildDone: { toDemoteChild: {
          if // This child is the subject node, not one of its siblings
            (childKeyNameString.equals(subjectNameString))
            break toChildDone; // so skip it.
          EpiNode childAttributesEpiNode= childMapEntry.getValue();
          if (null == childAttributesEpiNode) break toChildDone; // No value.
          childAttributesMapEpiNode= childAttributesEpiNode.tryMapEpiNode();
          if (null == childAttributesMapEpiNode) break toChildDone; // Not map.
          String pathValueString= // Get value of path attribute. 
             childAttributesMapEpiNode.getString(pathKeyString);
          if (pathNextString.equals(pathValueString)) // Is a NEXT node
            break toDemoteChild; // so just demote it.
          if (pathActiveString.equals(pathValueString)) // Is an ACTIVE node
            { // so deactivate its descendants
              deactivatePathFrom1V(childAttributesMapEpiNode);
              break toDemoteChild; // and go demote the node.
              }
          break toChildDone; // Ignore child with any other path attribute.
        } // toDemoteChild:
          childAttributesMapEpiNode.putV( // Set as HOLDING.
              pathKeyString, pathHoldingString);
          purgeChild1WithUnneededHoldingIn2V(
              childKeyNameString,parentsAttributesMapEpiNode);
        } // toChildDone: processing of this child's map entry is done.
        } // childLoop: Continue with next child entry.
          return;
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
          String subjectNameKeyString= subjectDataNode.getNameString();
        toExit: {
          if (subjectDataNode.isRootB()) { // subjectDataNode is the root node.
            parentsChildrenMapEpiNode= getRootParentsChildrenMapEpiNode(); // Use root.
            subjectMapEpiNode=
                parentsChildrenMapEpiNode.getOrMakeMapEpiNode(
                    subjectNameKeyString);
            break toExit;
            }
          DataNode parentDataNode= subjectDataNode.getParentNamedList();
          parentMapEpiNode= // Recursing to translate parent. 
                recordAndTranslateToMapEpiNode(parentDataNode);
          parentsChildrenMapEpiNode= 
              getOrMakeChildrenMapEpiNode(parentMapEpiNode);
          subjectMapEpiNode= 
              parentsChildrenMapEpiNode.getOrMakeMapEpiNode(subjectNameKeyString); 
        } // toExit:
          return subjectMapEpiNode;
        }


    // Getters and choosers of selection path information.


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


    public DataNode getByAttributeChildDataNode()
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
       * childrenAttributeMapEpiNode which is part of the active path.
       * There should be only one.
       * Returns the name if found, null otherwise.
       * 
       * ///opt use new search-by-attribute method.
       */
      {
          String childNameString= null; // Default of child not found.
        toExit: {
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
          if (! pathActiveString.equals(pathValueString))// Not active path.
            break toChildDone;
          childNameString= childMapEntry.getKey().toRawString(); // Get name.
          break toExit; // Exit child loop with child name.
        } // toChildDone: processing of this map entry is done.
        } // childLoop: Continue with next child entry.
        } // toExit:
          return childNameString;
        }

 
    private String getByAttributeChildString(
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
        toExit: {
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
          break toExit; // Exit child loop with child name.
        } // toChildDone: processing of this map entry is done.
        } // childLoop: Continue with next child entry.
        } // toExit:
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
          getByAttributeChildString(
              childrenAttributeMapEpiNode,pathNextString);
        DataNode selectedDataNode= // Try getting child DataNode by name. 
            subjectDataNode.getNamedChildDataNode(selectionNameString);
        return selectedDataNode;
        }


    private MapEpiNode getChildIn1WithPathValue2MapEpiNode(
        MapEpiNode childrenMapEpiNode,String desiredValueString)
      /* This method searches the children in childrenMapEpiNode for 
       * a child with a selection attribute with value of desiredValueString,
       * or any value if desiredValueString is null.
       * It returns the attributes MapEpiNode of the found child,
       * or null if such a child is not found. 
       * 
       */
      {
        MapEpiNode childAttributesMapEpiNode= null; // Assume search failure.
        String childNameString= getChildNameIn1WithPathValue2String(
            childrenMapEpiNode,desiredValueString); // Search for name.
        if (null != childNameString) // If got name, translate to MapEpiNode.
          childAttributesMapEpiNode= // Override null with MapEpiNode.
            childrenMapEpiNode.getMapEpiNode(childNameString);
        return childAttributesMapEpiNode;
        }


    private String getChildNameIn1WithPathValue2String(
        MapEpiNode childrenMapEpiNode,String desiredValueString)
      /* This method searches the children in childrenMapEpiNode for 
       * a child with a selection attribute with value of desiredValueString,
       * or any value if desiredValueString is null.
       * It returns the String name of the found child,
       * or null if such a child is not found.
       * It does this by iterating through the children. 
       */
      {
          String childNameString= null; // Set default value of not found.
          MapEpiNode childAttributesMapEpiNode;
          ListIterator<Map.Entry<EpiNode,EpiNode>> childIterator=
              childrenMapEpiNode.getListIteratorOfEntries();
        childLoop: while(true) { // Iterate through child map entries.
          if (! childIterator.hasNext()) // Exit if no more child entries.
            { childAttributesMapEpiNode= null; break childLoop; }
        toChildDone: {
          Map.Entry<EpiNode,EpiNode> childAttributesMapEntry= 
              childIterator.next();
          EpiNode childAttributesEpiNode= childAttributesMapEntry.getValue();
          if (null == childAttributesEpiNode) break toChildDone; // No value.
          childAttributesMapEpiNode= childAttributesEpiNode.tryMapEpiNode();
          if (null == childAttributesMapEpiNode) break toChildDone; // Not map.
          String pathValueString= // Get the path attribute value, if any. 
            childAttributesMapEpiNode.getString(pathKeyString);
          if ( (null == desiredValueString) // If no particular value desired
               || desiredValueString.equals(pathValueString) // or value gotten
               )
            { // return name of child containing the path attribute.
              childNameString= childAttributesMapEntry.getKey().toRawString();
              break childLoop;
              }
        } // toChildDone: processing of this child's map entry is done.
        } // childLoop: Continue with next child entry.
          return childNameString;
        }

    private MapEpiNode getOrMakeChildrenMapEpiNode(MapEpiNode parentMapEpiNode)
      /* This method gets or makes 
       * the "Children" attribute value from parentMapEpiNode.
       * This is useful for methods which deal with child node sets.
       * 
       * ///org Using this method can create empty Children attributes
       * if it is used for condition testing.
       * It should be used only when a child or attribute is being created
       * unless a Children purge is done afterward.
       */
      {
        return parentMapEpiNode.getOrMakeMapEpiNode("Children");
        }

    private MapEpiNode getChildrenMapEpiNode(MapEpiNode parentMapEpiNode)
      /* This method gets the "Children" attribute value from parentMapEpiNode.
       * It returns the associated MapEpiNode if there is one.
       * It returns null if there isn't one.
       * This is useful for methods which deal with child node sets.
       */
      {
        return parentMapEpiNode.getMapEpiNode("Children");
        }

    private MapEpiNode getRootParentsChildrenMapEpiNode()
      /* This returns the children map of the parent of 
       * the root of the path meta-data hierarchy.  
       */
      { 
        return getOrMakeChildrenMapEpiNode(
            getRootParentsAttributesMapEpiNode());
        }

    private MapEpiNode getRootParentsAttributesMapEpiNode()
      /* This returns the attributes map of the parent of 
       * the root of the path meta-data hierarchy.  
       */
      { 
        return hierarchyRootParentsAttributesMapEpiNode; 
        }

    }