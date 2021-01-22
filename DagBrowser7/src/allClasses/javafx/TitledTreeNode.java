package allClasses.javafx;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
        theTreeView= new TreeView<DataNode>(theRootEpiTreeItem);

        TreeItem<DataNode> selectionTreeItemOfDataNode= 
            TreeStuff.toTreeItem(selectionDataNode,theRootEpiTreeItem);
        theTreeView.getSelectionModel().select(selectionTreeItemOfDataNode);

        this.theTreeStuff= theTreeStuff;
        Label titleLabel= new Label( // Set label to be name of root node.
          theTreeStuff.getDataRoot().toString());
        setTop(titleLabel); // Adding it to main Node.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);
        setCenter(theTreeView);

        theTreeView.addEventHandler(
          KeyEvent.KEY_PRESSED, 
          (theKeyEvent) -> keyEventHandlerV(theKeyEvent)
          );
        setSelectionEventHandlerV();

        Platform.runLater( () -> theTreeView.requestFocus() );
        }

    private void keyEventHandlerV(KeyEvent theKeyEvent)
      /* This method sets a handler for the right arrow key
       * so that the best child Node selection is made.
       */
      {
        System.out.println("TitledTreeNode key handler.");
        KeyCode keyCodeI = theKeyEvent.getCode(); // Get code of key pressed.
        switch (keyCodeI) {
          case RIGHT:  // right-arrow.
            DataNode subselectionDataNode= // Calculate best sub-selection. 
              theTreeStuff.getSubselectionDataNode();
            if (null == subselectionDataNode) // Exit if no sub-selection. 
              break;
            theTreeView.getSelectionModel().select( // Select it.
                theTreeStuff.toTreeItem(subselectionDataNode));
            theKeyEvent.consume();
            break;
          default: 
            break;
          }
        }

    private void setSelectionEventHandlerV()
      /* This method sets a handler so that TreeNode selection changes
       * are sent to the TreeStuff where it does its normal processing.
       * 
       * WARNING: left-arrow causes a temporary selection of null,
       * so that condition is ignored below.
       */
      {
        MultipleSelectionModel<TreeItem<DataNode>> theMultipleSelectionModel=
            theTreeView.getSelectionModel();
        ReadOnlyObjectProperty<TreeItem<DataNode>> selectedItemProperty=
            theMultipleSelectionModel.selectedItemProperty();
        selectedItemProperty.addListener(
          (observableValueOfDataNode,oldDataNode,newDataNode) 
          -> 
          { /// System.out.println("TitledTreeNode selection change:"
            ///   +oldDataNode+","+newDataNode);
            TreeItem<DataNode> newSelectedTreeItem= selectedItemProperty.get();
            if (null != newSelectedTreeItem) // Ignore temporary null selection. 
              theTreeStuff.setSelectedDataNodeV( // Inform our TreeStuff.
                newSelectedTreeItem.getValue());
            }
          );
        }

    }
