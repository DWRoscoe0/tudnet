package allClasses.javafx;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javax.swing.tree.TreePath;

import allClasses.DataNode;
import allClasses.DataTreeModel;

public class TitledListNode extends BorderPane  

  /* This class is used for displaying Nodes that
   * can be displayed as lists.
   */

  {
  
    public TitledListNode( TreePath theTreePath, DataTreeModel inDataTreeModel )
      {
        Label titleLabel= new Label(
          ((DataNode)(theTreePath.getLastPathComponent())).toString());
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
