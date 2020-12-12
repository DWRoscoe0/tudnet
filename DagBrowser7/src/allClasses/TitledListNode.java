package allClasses;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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
  
    public TitledListNode( TreePath theTreePath, DataTreeModel inDataTreeModel )
      {
        Label titleLabel= new Label(
          //"TEST-TITLE"
          ((DataNode)(theTreePath.getLastPathComponent())).toString()
          );
        setTop(titleLabel); // Adding it to main JPanel.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);

        ListView<DataNode> theListView= new ListView<DataNode>();
        DataNode theDataNode= (DataNode)theTreePath.getLastPathComponent();
        ObservableList<DataNode> childObservableList= 
            theDataNode.getChildObservableListOfDataNodes();
        theListView.setItems(childObservableList);
        setCenter(theListView);
        }
  
    }
