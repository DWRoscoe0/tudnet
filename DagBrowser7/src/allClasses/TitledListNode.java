package allClasses;

import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javax.swing.tree.TreePath;

public class TitledListNode extends BorderPane  

  /* This class is used for displaying Nodes that
   * can be displayed as lists.
   * 
   * ///fix Though the name of this class includes "Titled",
   * it does not yet display a title.
   */

  {
  
    public TitledListNode( TreePath inTreePath, DataTreeModel inDataTreeModel )
      {
        ListView<DataNode> theListView= new ListView<DataNode>();
        DataNode theDataNode= (DataNode)inTreePath.getLastPathComponent();
        ObservableList<DataNode> childObservableList= 
            theDataNode.getChildObservableListOfDataNodes();
        theListView.setItems(childObservableList);
        setCenter(theListView);
        }
  
    }
