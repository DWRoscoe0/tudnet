package allClasses.javafx;

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

  {
    
    /* This class is a JavaFX Node that is used for 
     * displaying DataNodes that can be displayed as lists.
     * It includes a title, which is the name of the DataNode,
     * and a list of other DataNodes, 
     * which are the children of the main DataNode.
     */


    // Variables.

    private ListView<DataNode> theListView= // List view GUI Node. 
        new ListView<DataNode>(); // It is initially empty.

    private TreeStuff theTreeStuff;


    // Methods.
    
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
    /* This method makes and returns
     * a TreeStuff appropriate for a TitledListNode.
     */
    { 
      TreeStuff theTreeStuff= // Construct the appropriate TreeStuff. 
        TreeStuff.makeWithAutoCompleteTreeStuff(
          subjectDataNode,
          selectedDataNode,
          thePersistent,
          theDataRoot,
          theRootEpiTreeItem,
          theSelections
          );
      TitledListNode theTitledListNode= // Calculate the UI Node to use.
          new TitledListNode(subjectDataNode,theTreeStuff);
      theTreeStuff.initializeV(theTitledListNode); // Store the UI Node.
      return theTreeStuff;
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
          { 
            DataNode newSelectedDataNode= selectedItemProperty.get();
            getTreeStuff().setSelectedDataNodeV(newSelectedDataNode);
            }
          );

        }

    public TitledListNode( // Constructor.
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
            subjectDataNode.getChildrenAsObservableListOfDataNodes();
        theListView.setItems(childObservableList);
        setCenter(theListView);
        //// Platform.runLater( 
        JavaFXGUI.runLaterV( "TitledListNode( // Constructor.",
            () -> theListView.requestFocus() );
        setEventHandlersV(); // Needed for initial selection which follows.
        theListView.getSelectionModel().
          select(theTreeStuff.getSelectionDataNode());
        }

    }
