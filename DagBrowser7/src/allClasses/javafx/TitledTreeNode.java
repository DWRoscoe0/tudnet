package allClasses.javafx;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.TreeView;
import javafx.scene.control.Label;
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
    private TreeView<DataNode> theTreeView= // Tree view GUI Node. 
        new TreeView<DataNode>();

    //// @SuppressWarnings("unused") ////
    //// private EpiTreeItem theRootEpiTreeItem;

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
      TitledTreeNode theTitledTreeNode= new TitledTreeNode( 
        //// subjectDataNode,
        selectedDataNode,
        theRootEpiTreeItem,
        theTreeStuff
        );
      theTreeStuff.initializeV(theTitledTreeNode);
      return theTreeStuff;
      }

    public TitledTreeNode( // Constructor. 
        DataNode selectionDataNode,
        EpiTreeItem theRootEpiTreeItem,
        TreeStuff theTreeStuff
        )
      {
        //// theRootEpiTreeItem= new EpiTreeItem(theRootDataNode);
        //// theRootEpiTreeItem= new EpiTreeItem(
        ////   theTreeStuff.toTreeItem(theTreeStuff.getDataRoot().getRootDataNode()));
        //// theRootEpiTreeItem.setExpanded(true);
        theTreeView= new TreeView<DataNode>(theRootEpiTreeItem);
        //// treeContentBorderPane= new BorderPane();
        //// treeContentBorderPane.setCenter(theTreeView);
        //// treeContentBorderPane.setBottom(theSwitchButton);
        //// theTreeScene= new Scene(treeContentBorderPane);
        //// EpiScene.setDefaultsV(theTreeScene);

        TreeItem<DataNode> selectionTreeItemOfDataNode= 
            TreeStuff.toTreeItem(selectionDataNode,theRootEpiTreeItem);
        theTreeView.getSelectionModel().select(selectionTreeItemOfDataNode);

        this.theTreeStuff= theTreeStuff;
        Label titleLabel= new Label( // Set label to be name of root node.
          //// subjectDataNode.toString());
          theTreeStuff.getDataRoot().toString());
        setTop(titleLabel); // Adding it to main Node.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);

        //// ObservableList<DataNode> childObservableList= 
        ////     subjectDataNode.getChildObservableListOfDataNodes();
        //// theTreeView.setItems(childObservableList);
        setCenter(theTreeView);
        setEventHandlersV(); // Needed for initial selection which follows.
        //// theTreeView.getSelectionModel().
        ////   select(theTreeStuff.getSelectedChildDataNode());
        Platform.runLater( () -> theTreeView.requestFocus() );
        }

    //// @SuppressWarnings("unused") ////
    private void setEventHandlersV()
      /* This method sets a handler so that TreeNode selection changes
       * are sent to the TreeStuff where it does its normal processing.
       */
      {
        MultipleSelectionModel<TreeItem<DataNode>> theMultipleSelectionModel=
            theTreeView.getSelectionModel();
        ReadOnlyObjectProperty<TreeItem<DataNode>> selectedItemProperty=
            theMultipleSelectionModel.selectedItemProperty();
        selectedItemProperty.addListener(
          (observableValueOfDataNode,oldDataNode,newDataNode) 
          -> 
          { System.out.println("TitledTreeNode selection changed.");
            TreeItem<DataNode> newSelectedTreeItem= selectedItemProperty.get();
            //// getTreeStuff().setSelectedDataNodeV(newSelectedTreeItem.getValue());
            theTreeStuff.setSelectedDataNodeV(newSelectedTreeItem.getValue());
            }
          );

        }

    }
