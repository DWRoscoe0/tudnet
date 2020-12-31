package allClasses.javafx;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.layout.BorderPane;

import allClasses.DataNode;

public class TitledListNode 
  extends BorderPane
  
  /* This class is used for displaying Nodes that
   * can be displayed as lists.
   */

  {

    DataNode theSubjectDataNode= null; // DataNode containing our list.
      //////opt This should be eliminated and theTreeStuff used instead.
    
    ListView<DataNode> theListView= // List view GUI Node. 
        new ListView<DataNode>();

    private TreeStuff theTreeStuff;
    
    public TreeStuff getTreeStuff()
      { 
        return theTreeStuff;
        }

    public static TreeStuff makeTreeStuff(
                DataNode subjectDataNode,
                DataNode selectedDataNode
                )
    { 
      //// TreeStuff theTreeStuff= new TreeStuff(
      TreeStuff theTreeStuff= TreeStuff.makeWithAutoCompleteTreeStuff(
          subjectDataNode,
          selectedDataNode
          );
      TitledListNode theTitledListNode= new TitledListNode( 
        subjectDataNode,
        theTreeStuff
        );
      theTreeStuff.initializeV(theTitledListNode);
      return theTreeStuff;
      }

    public TitledListNode( 
        DataNode subjectDataNode,
        TreeStuff theTreeStuff
        )
      {
        this.theTreeStuff= theTreeStuff;
        Label titleLabel= new Label(
          subjectDataNode.toString());
        setTop(titleLabel); // Adding it to main JPanel.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);

        theSubjectDataNode= subjectDataNode;  //////
        ObservableList<DataNode> childObservableList= 
            theSubjectDataNode.getChildObservableListOfDataNodes();
        theListView.setItems(childObservableList);
        setCenter(theListView);
        theListView.getSelectionModel().
          select(theTreeStuff.getSelectedChildDataNode());
        Platform.runLater( // Request focus later after being added to Scene.
          new Runnable() {
            @Override
            public void run() {
                theListView.requestFocus();
                }
            }
          );
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
