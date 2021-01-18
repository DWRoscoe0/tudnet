package allClasses.javafx;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.TreeView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Persistent;

public class TitledTreeNode

  extends BorderPane
  
  /* This class is used for displaying Nodes inn the form of a tree.
   * It uses the TreeView class.
   */

  {

    private TreeView<DataNode> theTreeView= // List view GUI Node. 
        new TreeView<DataNode>();

    @SuppressWarnings("unused") ////
    private EpiTreeItem theRootEpiTreeItem;

    private TreeStuff theTreeStuff;
    
    public TreeStuff getTreeStuff()
      { 
        return theTreeStuff;
        }

    public static TreeStuff makeTreeStuff(
        DataNode subjectDataNode,
        DataNode selectedDataNode,
        DataRoot theDataRoot,
        Persistent thePersistent,
        Selections theSelections
        )
    { 
      TreeStuff theTreeStuff= TreeStuff.makeWithAutoCompleteTreeStuff(
          subjectDataNode,
          selectedDataNode,
          thePersistent,
          theDataRoot,
          theSelections
          );
      TitledListNode theTitledListNode= new TitledListNode( 
        subjectDataNode,
        theTreeStuff
        );
      theTreeStuff.initializeV(theTitledListNode);
      return theTreeStuff;
      }

    public TitledTreeNode( 
        DataNode subjectDataNode,
        TreeStuff theTreeStuff
        )
      {
        /*  ////
        //// theRootEpiTreeItem= new EpiTreeItem(theRootDataNode);
        theRootEpiTreeItem= new EpiTreeItem(
          theTreeStuff.toTreeItem(theTreeStuff.getDataRoot().getRootDataNode()));
        theRootEpiTreeItem.setExpanded(true);
        theTreeView= new TreeView<DataNode>(theRootEpiTreeItem);
        treeContentBorderPane= new BorderPane();
        treeContentBorderPane.setCenter(theTreeView);
        treeContentBorderPane.setBottom(theSwitchButton);
        theTreeScene= new Scene(treeContentBorderPane);
        EpiScene.setDefaultsV(theTreeScene);

        this.theTreeStuff= theTreeStuff;
        Label titleLabel= new Label(
          subjectDataNode.toString());
        setTop(titleLabel); // Adding it to main Node.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);

        ObservableList<DataNode> childObservableList= 
            subjectDataNode.getChildObservableListOfDataNodes();
        theTreeView.setItems(childObservableList);
        setCenter(theTreeView);
        setEventHandlersV(); // Needed for initial selection which follows.
        ////// theTreeView.getSelectionModel().
        //////   select(theTreeStuff.getSelectedChildDataNode());
        Platform.runLater( () -> theTreeView.requestFocus() );
        */  ////
        }

    @SuppressWarnings("unused") ////
    private void setEventHandlersV()
      {
        MultipleSelectionModel<TreeItem<DataNode>> theMultipleSelectionModel=
            theTreeView.getSelectionModel();
        ReadOnlyObjectProperty<TreeItem<DataNode>> selectedItemProperty=
            theMultipleSelectionModel.selectedItemProperty();
        selectedItemProperty.addListener(
          (observableValueOfDataNode,oldDataNode,newDataNode) 
          -> 
          { //// System.out.println("TitledListNode selection changed.");
            TreeItem<DataNode> newSelectedDataNode= selectedItemProperty.get();
            ////// getTreeStuff().setSelectedDataNodeV(newSelectedDataNode);
            }
          );

        }

    }
