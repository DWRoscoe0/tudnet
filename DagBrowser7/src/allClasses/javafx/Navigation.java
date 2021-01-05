package allClasses.javafx;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

import allClasses.DataNode;
import allClasses.Persistent;

public class Navigation extends EpiStage

  /* This class is, or eventually will be,
   * for navigation of the Infogora hierarchy.
   * It's central control is the TreeView. 
   */

  {

    // Injected dependencies.
    private final DataNode theInitialRootDataNode;
    private final Persistent thePersistent;

    // Other variables.
    
    private Scene theTreeScene; 
    private BorderPane treeRootBorderPane;
    private TreeView<DataNode> theTreeView; 
    private Button theTreeShowItemButton;
    private EpiTreeItem theRootEpiTreeItem;
    private TreeStuff itemTreeStuff;

    private Scene theItemScene;
    private BorderPane itemRootBorderPane;
    private ListView<DataNode> theListView; 
    private Button theItemShowTreeButton;


    // Constructors.

    public Navigation(
        JavaFXGUI theJavaFXGUI, 
        DataNode theInitialRootDataNode,
        Persistent thePersistent
        )
      {
        super(theJavaFXGUI);
        this.theInitialRootDataNode= theInitialRootDataNode;
        this.thePersistent= thePersistent;

        }
    
    public void initializeAndStartV()
      /* This method initializes and starts the Navigation Stage, then returns.
       * The Stage alternates between a Scene which displays
       * the hierarchy as a tree,
       * and a Scene which displays a single node within the tree.
       * Each Scene has a button to switch to the other.
       */
      {
        theTreeShowItemButton= new Button("Show Item");
        theItemShowTreeButton= new Button("Show Tree");

        theTreeScene= makeTreeScene(
            theTreeShowItemButton, theInitialRootDataNode);
        setTreeSelectionFromDataNodeV(theInitialRootDataNode);

        theItemScene= makeItemScene();
        setItemRootFromDataNodeV(theInitialRootDataNode);

        setEventHandlersV(); // Okay to do now that above definitions are done.

        // setScene(theTreeScene); // Use tree scene as first one displayed.
        setScene(theItemScene); // Use item scene as first one displayed.

        finishStateInitAndStartV("Infogora JavaFX Navigation UI");
        }

    private Scene makeTreeScene(
        Button theSwitchButton, DataNode rootDataNode)
      /* Creates and returns the initial Scene to display a tree. 
       * It will be reused so as to reuse tree browsing context.  
       */ 
      {
        theRootEpiTreeItem= new EpiTreeItem(theInitialRootDataNode);
        theRootEpiTreeItem.setExpanded(true);
        theTreeView= new TreeView<DataNode>(theRootEpiTreeItem);
        treeRootBorderPane= new BorderPane();
        treeRootBorderPane.setCenter(theTreeView);
        treeRootBorderPane.setBottom(theSwitchButton);
        Scene theScene= new Scene(treeRootBorderPane);
        EpiScene.setDefaultsV(theScene);
        return theScene;
        }

    private void setTreeSelectionFromDataNodeV(DataNode theDataNode) 
      /* If theDataNode is null then this method does nothing.
       * Otherwise it calculates an appropriate GUI Node for theDataNode
       * and stores at the center of the item Pane to be displayed.
       */
      { 
        if (null != theDataNode) { // Process DataNode if present.
          TreeItem<DataNode> theTreeItem= 
              toTreeItem(theDataNode,theRootEpiTreeItem); 
          theTreeView.getSelectionModel().select(theTreeItem);
          }
        }

    private TreeItem<DataNode> toTreeItem(
        DataNode targetDataNode, TreeItem<DataNode> rootTreeItem) 
      /* This translates targetDataNode to the TreeItem that references it
       * by searching for the ancestor DataNode referenced by rootTreeItem,
       * then tracing TreeItems back to the target DataNode.
       * This is done recursively to simplify path tracking.  
       * Returns the target TreeItem or null if the translation fails.
       */
      {
          TreeItem<DataNode> resultTreeItem;
        main: {
          if // Root TreeItem references target DataNode.
            (rootTreeItem.getValue() == targetDataNode)
            { resultTreeItem= rootTreeItem; break main; } // Exit with root.
          TreeItem<DataNode> parentTreeItem= // Recursively translate parent. 
              toTreeItem(targetDataNode.getParentNamedList(), rootTreeItem);
          if (null == parentTreeItem) // Parent translation failed.
            { resultTreeItem= null; break main; } // Indicate failure with null.
          for // Search for target DataNode in translated parent's children.
            ( TreeItem<DataNode> childTreeItem : parentTreeItem.getChildren() )
            {
              if  // Exit with child TreeItem if it references target DataNode.
                (childTreeItem.getValue() == targetDataNode)
                { resultTreeItem= childTreeItem; break main; }
              }
          // If here then no child referenced target DataNode.
          resultTreeItem= null; // Indicate failure with null.
        } // main:
          return resultTreeItem;
        }

    private Scene makeItemScene()
      /* Creates and returns a Scene displaying a single item. */ 
      {
        itemRootBorderPane= new BorderPane();
        theListView= new ListView<DataNode>(); // Empty ListView.
        itemRootBorderPane.setCenter(theListView);
        itemRootBorderPane.setBottom(theItemShowTreeButton);
        Scene itemScene= new Scene(itemRootBorderPane);
        EpiScene.setDefaultsV(itemScene);
        return itemScene;
        }
    
    private void setEventHandlersV()
      /* This method registers EventHandlers for various events.
       */
      {
        theTreeShowItemButton.setOnAction(
            theActionEvent -> doTreeShowItemButtonActionV() );
        theItemShowTreeButton.setOnAction(
            theActionEvent -> doItemShowTreeButtonActionV() );

        theItemScene.addEventHandler(
            KeyEvent.KEY_PRESSED, e -> doItemKeyV(e) );
        }

    private void doItemKeyV(javafx.scene.input.KeyEvent theKeyEvent)
      {
        KeyCode keyCodeI = theKeyEvent.getCode(); // Get code of key pressed.
        switch (keyCodeI) {
          case RIGHT:  // right-arrow.
            System.out.println("Right-arrow typed.");
            //// setItemRootFromDataNodeV(itemTreeStuff.getSelectedChildDataNode());
            setItemRootFromTreeStuffV(itemTreeStuff.moveRightAndMakeTreeStuff());
            break;
          case LEFT:  // left-arrow.
            System.out.println("Left-arrow typed.");
            //// setItemRootFromDataNodeV(itemTreeStuff.getParentDataNode());
            setItemRootFromTreeStuffV(itemTreeStuff.moveLeftAndMakeTreeStuff());
            break;
          default: 
            break;
          }
        }
    
    private void doTreeShowItemButtonActionV()
      /* This method sets the Scene to display
       * the TreeItem presently displayed by the TreeView.
       */
      {
        TreeItem<DataNode> theTreeItemOfDataNode=
          theTreeView.getSelectionModel().getSelectedItem();
        if (null != theTreeItemOfDataNode) { // Process if item selected.
          DataNode theDataNode= theTreeItemOfDataNode.getValue();
          setItemRootFromDataNodeV(theDataNode);
          }
        setScene(theItemScene); // Switch to item scene.
        }

    private void setItemRootFromTreeStuffV(TreeStuff theTreeStuff)
      /* If theTreeStuff is null then this method does nothing.
       * Otherwise it calculates an appropriate GUI Node for theTreeStuff
       * and stores at the center of the item Pane to be displayed.
       */
      {
        if (null != theTreeStuff) { // Process TreeStuff if present.
          itemTreeStuff= theTreeStuff; // Save in instance field.
          Node itemNode= itemTreeStuff.getGuiNode();
          itemRootBorderPane.setCenter(itemNode);
          }
        }

    private void setItemRootFromDataNodeV(DataNode theDataNode)
      /* If theDataNode is null then this method does nothing.
       * Otherwise it calculates an appropriate GUI Node for theDataNode
       * and stores at the center of the item Pane to be displayed.
       */
      {
        if (null != theDataNode) { // Process DataNode if present.
          itemTreeStuff= theDataNode.makeTreeStuff(
              null, // no known selection at this point
              thePersistent
              );
          Node itemNode= itemTreeStuff.getGuiNode();
          itemRootBorderPane.setCenter(itemNode);
          }
        }

    private void doItemShowTreeButtonActionV()
      /* This method sets the Scene to display the TreeView.
       */
      {
        //// DataNode selectedDataNode= itemTreeStuff.getSelectedChildDataNode();
        DataNode parentOfSelectedDataNode= ////// aka subjectDataNode.
        ////    selectedDataNode.getParentNamedList();
            itemTreeStuff.getSubjectDataNode();
        TreeItem<DataNode> theTreeItemOfDataNode= 
            toTreeItem(parentOfSelectedDataNode,theRootEpiTreeItem);
        theTreeView.getSelectionModel().select(theTreeItemOfDataNode);
        requestFocusLaterInV(theTreeView);
        setScene(theTreeScene); // Switch [back] to tree scene.
        }

    private void requestFocusLaterInV(Node theNode)
      /* This method queues a request to focus theNode later.
       */
      {
        Platform.runLater( // Later
          new Runnable() {
            @Override
            public void run() { // run a request to
                theNode.requestFocus(); // focus the Node.
                }
            }
          );
        }

    }
