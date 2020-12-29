package allClasses.javafx;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.layout.BorderPane;
import javax.swing.tree.TreePath;

import allClasses.DataNode;
import allClasses.DataTreeModel;

public class TitledListNode 
  extends BorderPane
  
  /* This class is used for displaying Nodes that
   * can be displayed as lists.
   */

  {

    DataNode parentDataNode= null; // DataNode containing our list.
    
    ListView<DataNode> theListView= // List view GUI Node. 
        new ListView<DataNode>();

    private TreeStuff theTreeStuff;
    
    public TreeStuff getTreeStuff()
      { 
        return theTreeStuff;
        }

    public static TreeStuff makeTreeStuff(
                TreePath theTreePath, 
                DataTreeModel theDataTreeModel 
                )
    { 
      TreeStuff theTreeStuff= new TreeStuff();
      TitledListNode theTitledListNode= new TitledListNode( 
        theTreePath, 
        theDataTreeModel,
        theTreeStuff
        );
      theTreeStuff.initializeV(theTitledListNode);
      return theTreeStuff;
      }

    public TitledListNode( 
        TreePath theTreePath, 
        DataTreeModel inDataTreeModel, 
        TreeStuff theTreeStuff
        )
      {
        this.theTreeStuff= theTreeStuff;
        Label titleLabel= new Label(
          ((DataNode)(theTreePath.getLastPathComponent())).toString());
        setTop(titleLabel); // Adding it to main JPanel.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);

        parentDataNode= (DataNode)theTreePath.getLastPathComponent();
        ObservableList<DataNode> childObservableList= 
            parentDataNode.getChildObservableListOfDataNodes();
        theListView.setItems(childObservableList);
        setCenter(theListView);
        setEventHandlersV();
        }

    private void setEventHandlersV()
      {
        MultipleSelectionModel<DataNode> theMultipleSelectionModel=
            theListView.getSelectionModel();
        ReadOnlyObjectProperty<DataNode> selectedItemProperty=
            theMultipleSelectionModel.selectedItemProperty();
        selectedItemProperty.addListener(
          (observableValueOfDataNode,oldDataNode,newDataNode) 
          -> 
          { System.out.println("TitledListNode selection changed."); 
            getTreeStuff().setSelectedDataNodeV(selectedItemProperty.get());
            }
          );

        }

    }
