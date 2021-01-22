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
import allClasses.DataRoot;
import allClasses.Persistent;

public class TitledListNode 
  extends BorderPane
  
  /* This class is used for displaying Nodes that
   * can be displayed as lists.
   */

  {

    ListView<DataNode> theListView= // List view GUI Node. 
        new ListView<DataNode>();

    private TreeStuff theTreeStuff;
    
    public TreeStuff getTreeStuff()
      { 
        return theTreeStuff;
        }

    public static TreeStuff makeTreeStuff(
                DataNode subjectDataNode,
                DataNode selectedDataNode,
                DataRoot theDataRoot,
                EpiTreeItem theRootEpiTreeItem,
                Persistent thePersistent,
                Selections theSelections
                )
    { 
      TreeStuff theTreeStuff= TreeStuff.makeWithAutoCompleteTreeStuff(
          subjectDataNode,
          selectedDataNode,
          thePersistent,
          theDataRoot,
          theRootEpiTreeItem,
          theSelections
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
        setTop(titleLabel); // Adding it to main Node.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);

        ObservableList<DataNode> childObservableList= 
            subjectDataNode.getChildObservableListOfDataNodes();
        theListView.setItems(childObservableList);
        setCenter(theListView);
        Platform.runLater( () -> theListView.requestFocus() );
        setEventHandlersV(); // Needed for initial selection which follows.
        theListView.getSelectionModel().
          select(theTreeStuff.getSelectionDataNode());
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
          { //// System.out.println("TitledListNode selection changed.");
            DataNode newSelectedDataNode= selectedItemProperty.get();
            getTreeStuff().setSelectedDataNodeV(newSelectedDataNode);
            }
          );

        }

    }
