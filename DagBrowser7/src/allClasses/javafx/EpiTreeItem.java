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
      {
        if (! childCacheLoadedB) {
          Iterator<DataNode> theIterator= 
              getValue().getChildIterable().iterator();
          while (theIterator.hasNext())
            super.getChildren().add(
                new EpiTreeItem(theIterator.next())
                );
          childCacheLoadedB= true;
          }
        return super.getChildren();
        }
        
    }
