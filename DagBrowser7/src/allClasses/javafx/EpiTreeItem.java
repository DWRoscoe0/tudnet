package allClasses.javafx;

import java.util.Iterator;

import allClasses.DataNode;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class EpiTreeItem

  extends TreeItem<DataNode>

  /* This class acts as a kind of bridge between JavaFX TreeItems
   * and DataNodes.
   * It lazily creates child TreeItems, one for each child DataNode,
   * meaning only when the TreeItem children are needed, not before.
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
       */
      {
        if (! childCacheLoadedB) {
          DataNode parentDataNode= getValue();
          Iterator<DataNode> theIterator=
              parentDataNode.getChildListOfDataNodes().iterator();
          while (theIterator.hasNext()) {
            DataNode childDataNode= theIterator.next();
            super.getChildren().add(
                new EpiTreeItem(childDataNode)
                );
            }
          childCacheLoadedB= true;
          }
        return super.getChildren();
        }
        
    }
