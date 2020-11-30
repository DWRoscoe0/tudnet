package allClasses.javafx;

import allClasses.DataNode;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class EpiTreeItem

  extends TreeItem<DataNode>
  
  {

    boolean childCacheLoadedB= false;

    public EpiTreeItem(DataNode theDataNode) 
      {
        super(theDataNode);
        /// setValue(theDataNode);
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
          /*  ////
          Iterator<DataNode> theIterator= getValue().iterator();
          while (theIterator.hasNext())
            super.getChildren().add(
                new TreeItem<DataNode>(theIterator.next())
                );
          */  ////
          DataNode theDataNode= getValue();
          for 
            (
              int childIndexI= 0; 
              childIndexI < theDataNode.getChildCount();
              childIndexI++
              )
            super.getChildren().add(
                new EpiTreeItem(
                    theDataNode.getChild(childIndexI))
                );
          childCacheLoadedB= true;
          }
        return super.getChildren();
        }
        
    }
