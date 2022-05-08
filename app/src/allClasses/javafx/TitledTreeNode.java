package allClasses.javafx;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Label;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;

import static allClasses.AppLog.theAppLog;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Persistent;

public class TitledTreeNode

  extends BorderPane
  
  /* This class is used for displaying Nodes in the form of a tree.
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
      TreeStuff theTreeStuff= TreeStuff.makeTreeStuff(
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
      theTreeStuff.initializeGUINodeV(theTitledTreeNode);
      return theTreeStuff;
      }

    public TitledTreeNode( // Constructor. 
        DataNode selectionDataNode,
        EpiTreeItem theRootEpiTreeItem,
        TreeStuff theTreeStuff
        )
      {
        theTreeView= new TreeView<DataNode>(theRootEpiTreeItem);

        this.theTreeStuff= theTreeStuff;
        Label titleLabel= // Set label to indicate tree view. 
            new Label("Tree View");
        setTop(titleLabel); // Adding it to main Node.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);
        setCenter(theTreeView);

        theTreeView.addEventHandler(
          KeyEvent.KEY_PRESSED, 
          (theKeyEvent) -> keyEventHandlerV(theKeyEvent)
          );
        setSelectionEventHandlerV();

        JavaFXGUI.runLaterQuietV( 
            "TitledTreeNode Constructor",
            () -> {
              selectV(selectionDataNode);
              theTreeView.requestFocus();
              } );
        }

    private void keyEventHandlerV(KeyEvent theKeyEvent)
      /* This method handles KeyEvents so that right arrow key is processed
       * so that the best child Node selection is made.
       */
      {
        System.out.println("[ttnkh]");
        boolean keyProcessedB= true;
        KeyCode keyCodeI= theKeyEvent.getCode(); // Get code of key pressed.
        switch (keyCodeI) {
          case RIGHT: // right-arrow.
            tryGoingRightV(); break;

          default: keyProcessedB= false; break; // Being here means no key processed.
          }
        if (keyProcessedB) theKeyEvent.consume();
        JavaFXGUI.runLaterQuietV( "keyEventHandlerV(KeyEvent theKeyEvent)",
          () -> { // After or later key processing done
            scrollToSelectionV(); // scroll selection into view. 
            });
        }

    private void tryGoingRightV()
      {
        main: {
          DataNode selectionDataNode= theTreeStuff.getSelectionDataNode();
          if (null == selectionDataNode) break main; // Exit if no selection. 
          TreeItem<DataNode> selectionTreeItem= // Get its TreeItem. 
              theTreeStuff.toTreeItem(selectionDataNode);
          if (null == selectionTreeItem) break main; // Exit if no TreeItem.
          if (! selectionTreeItem.isExpanded()) // If not already expanded
            { selectionTreeItem.setExpanded(true); // expand TreeItem.
              break main; // Do nothing else for this key.
              }
          DataNode subselectionDataNode= // Calculate best sub-selection. 
            theTreeStuff.getSubselectionDataNode();
          if (null == subselectionDataNode) // Exit if no sub-selection. 
            break main;
          selectV(subselectionDataNode);
        } // main:
          return;
        }

    private void selectV(DataNode theDataNode)
      /* This method selects theDataNode in the TreeView.
       * It also records the position in Selections.
       * 
       * This is all done on the JavaFX Application Thread.
       */
      {
        JavaFXGUI.runLaterQuietV( 
          "TitledTreeNode.selectV("+theDataNode+")",
          () -> {
            TreeItem<DataNode> theTreeItem= 
                theTreeStuff.toTreeItem(theDataNode);
            theTreeView.getSelectionModel().select( // Select the child.
                theTreeItem);
            int selectedI= // Get its index. 
                theTreeView.getSelectionModel().getSelectedIndex();
            theTreeView.scrollTo(selectedI); // Bring into view.
              /// might not be needed.
            /// layout(); // kludge said to fix scrollTo(.) bug, doesn't.
            theAppLog.debug("TitledTreeNode.selectV(.) "
              +theDataNode+" "+selectedI+" "+theTreeItem);
            } );
        }

    private void scrollToSelectionV()
      /* This method scrolls the present selection into view.  */
      {
        int selectedI= // Get its index. 
            theTreeView.getSelectionModel().getSelectedIndex();
        theTreeView.scrollTo(selectedI);
        theAppLog.debug("TitledTreeNode.scrollToSelectionV() "+selectedI);
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
