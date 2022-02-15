package allClasses.javafx;

import java.util.ArrayList;

import allClasses.DataNode;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class EpiTreeItem

  extends TreeItem<DataNode>

  /* The EpiTreeItems class acts as a bridge between 
   * the JavaFX user interface and DataNodes.
   * each instance contains a reference to a value DataNode and
   * a list of child EpiTreeItems.
   * 
   * The reference to the value DataNode does not change,
   * but the DataNode content might not be fully evaluated at first.
   * And after being evaluated, the content of the DataNode might change.
   * 
   * The reference to the list of child EpiTreeItems does not change.
   * The contents of the list is lazily evaluated, triggered when it is gotten.
   * Before evaluation the list is empty.
   * After evaluation it might or might not be empty.
   *
   * ///org I was going to try to make this class generic,
   * and remove the dependency on the DataNode class,
   * but some tree node functionality is needed,
   * so I might need to define a tree interface and extend that for 
   * the type parameter.
   */

  {

    boolean childCacheLoadedB= false; // Whether TreeItem children are defined.

    public EpiTreeItem(DataNode theDataNode) // Constructor. 
      {
        super(theDataNode); // Set the value.
        }

    @Override 
    public boolean isLeaf()
      // This is a leaf if value DataNode is a leaf.
      {
        return getValue().isLeaf();
        }

    @Override 
    public ObservableList<TreeItem<DataNode>> getChildren()
      /* This method returns the TreeItem's child list.
       * It first checks to see whether the TreeItem child list 
       * has been evaluated based on the child list in the value DataNode.
       * If not, then it evaluates and stores it.
       * Finally it returns list.
       * 
       * Note-1: For a while, childCacheLoadedB was set 
       *   before loading is complete!
       *   This was done because super.getChildren() called getChildren(),
       *   specifically in 
       *     TreeItem.updateExpandedDescendentCount(boolean reset), 
       *   which resulted in a StackOverflowError when 
       *   childCacheLoadedB was set after loading was complete.
       *   Later this was no longer true, so that assignment was moved down.
       * 
       * Note-2: This method originally lazily-evaluated individual children 
       *   and added them to the TreeItem.getChildren() ObservableList 
       *   one-at-a-time.
       *   This resulted in an unwanted selection when a node was expanded
       *   if the selection was below the expanded node.
       *   This problem disappeared when the entire collection of children
       *   was evaluated and added at once, similar to
       *   the way it is done in the TreeItem documentation example of
       *   an overridden getChildren() that uses lazy-evaluation.
       *   ///opt Figure out a way of evaluating children one at a time. 
       */
      {
        if (! childCacheLoadedB) // Load cache if not loaded yet.
          { // Load cache.
            // childCacheLoadedB= true; // Moved down.  See Note-1 above.
            DataNode parentDataNode= getValue();
            ArrayList<EpiTreeItem> childrenListOfTreeItems= // Make empty list. 
                new ArrayList<EpiTreeItem>();
            for // Add all children as TreeItems.  See Note-2 above.
              ( DataNode childDataNode : 
                parentDataNode.getChildrenAsListOfDataNodes() 
                )
              childrenListOfTreeItems.add(new EpiTreeItem(childDataNode));
            super.getChildren().setAll(childrenListOfTreeItems);
            childCacheLoadedB= true; // See Note-1 above.
            }
        return super.getChildren();
        }
        
    }
