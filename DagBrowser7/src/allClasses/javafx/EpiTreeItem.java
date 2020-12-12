package allClasses.javafx;

import java.util.Iterator;

import allClasses.DataNode;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class EpiTreeItem

  extends TreeItem<DataNode>
  
  {

    boolean childCacheLoadedB= false;

    public EpiTreeItem(DataNode theDataNode) 
      {
        super(theDataNode); // Was setValue(theDataNode);
        }

    @Override 
    public boolean isLeaf() 
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
