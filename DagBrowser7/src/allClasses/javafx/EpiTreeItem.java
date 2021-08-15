package allClasses.javafx;

import java.util.ArrayList;
//// import java.util.Iterator;

import allClasses.DataNode;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class EpiTreeItem

  extends TreeItem<DataNode>

  /* This class acts as a kind of bridge between JavaFX TreeItems
   * and DataNodes.
   * It lazily creates child TreeItems, one for each child DataNode,
   * meaning only when the TreeItem children are needed, not before.
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
        super(theDataNode); // Was setValue(theDataNode);
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
       * has been calculated based on the child list in the value DataNode.
       * If not, then it calculates and stores it.
       * Finally it returns the TreeItem's child list.
       * 
       * Note-1: childCacheLoadedB is set before loading is complete.
       * This is done because super.getChildren() called getChildren(),
       * specifically in TreeItem.updateExpandedDescendentCount(boolean reset), 
       * which resulted in a StackOverflowError when 
       * childCacheLoadedB was set after loading was complete.
       * 
       * Note-2: This method originally lazily-evaluated children 
       * and added them to the TreeItem.getChildren() ObservableList 
       * one-at-a-time.
       * This resulted in an unwanted selection when a node was expanded
       * if the selection was below the expanded node.
       * This problem disappeared when the entire collection of children
       * was evaluated and added at once, similar to
       * the way it is done in the TreeItem documentation example of
       * an overridden getChildren() that uses lazy-evaluation. 
       */
      {
        if (! childCacheLoadedB) {
          childCacheLoadedB= true; // See Note-1 above.
          DataNode parentDataNode= getValue();
          ArrayList<EpiTreeItem> childrenListOfTreeItems= // Make empty list. 
              new ArrayList<EpiTreeItem>();
          /*  ////
          Iterator<DataNode> theIterator=
              parentDataNode.getChildListOfDataNodes().iterator();
          while (theIterator.hasNext()) { // See Note-2 above.
            DataNode childDataNode= theIterator.next();
            accumulatorList.add(new EpiTreeItem(childDataNode));
            }
          */  ////
          for // Add all children as TreeItems.  See Note-2 above.
            (DataNode childDataNode : parentDataNode.getChildListOfDataNodes())
            childrenListOfTreeItems.add(new EpiTreeItem(childDataNode));
          super.getChildren().setAll(childrenListOfTreeItems);
          }
        return super.getChildren();
        }
        
    }
