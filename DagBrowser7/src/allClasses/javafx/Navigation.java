package allClasses.javafx;

import javax.swing.tree.TreePath;

import allClasses.DataNode;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

public class Navigation extends EpiStage

  /* This class is, or eventually will be,
   * for navigation of the Infogora hierarchy.
   * It's central control is the TreeView. 
   */

  {

    // Injected dependencies.
    private final DataNode theInitialRootDataNode;

    // Other variables.
    //// private TreePath locationTreePath;
    
    private EpiTreeItem theRootEpiTreeItem;
    private Button theTreeShowItemButton;
    private Scene theTreeScene; 

    private TreeView<DataNode> theTreeView; 
    private Scene theItemScene;
    private ListView<DataNode> theListView; 
    private Button theItemShowTreeButton;
    private BorderPane itemRootBorderPane;
            
    public Navigation(JavaFXGUI theJavaFXGUI, DataNode theInitialRootDataNode)
      {
        super(theJavaFXGUI);
        this.theInitialRootDataNode= theInitialRootDataNode;
        }
    
    public void initializeAndStartV()
      /* This method initializes and starts the Navigation Stage, then returns.
       * The Stage alternates between a Scene which displays
       * the hierarchy as a tree,,
       * and a Scene which displays a single node within the tree.
       * Each Scene has a button to switch to the other.
       */
      {
        //// locationTreePath= theInitialRootDataNode.getTreePath();
        theRootEpiTreeItem= new EpiTreeItem(theInitialRootDataNode);

        theTreeShowItemButton= new Button("Show Item");
        theItemShowTreeButton= new Button("Show Tree");

        theTreeScene= makeTreeScene(theTreeShowItemButton, theRootEpiTreeItem);

        theItemScene= makeItemScene();

        //// setButtonActionsV(); // Okay to do now that Scenes are defined.
        theTreeShowItemButton.setOnAction(e -> doTreeShowItemButtonActionV());
        theItemShowTreeButton.setOnAction(e -> doItemShowTreeButtonActionV());

        setScene(theTreeScene); // Use tree scene as first one displayed.
        finishStateInitAndStartV("Infogora JavaFX Navigatioo UI");
        }

    private Scene makeTreeScene(
        Button theSwitchButton, EpiTreeItem rootEpiTreeItem)
      /* Creates and returns the initial Scene to display a tree. 
       * It will be reused so as to reuse tree browsing context.  
       */ 
      {
        rootEpiTreeItem.setExpanded(true);
        theTreeView= new TreeView<DataNode>(rootEpiTreeItem);
        theTreeView.getSelectionModel().select(rootEpiTreeItem);
        BorderPane treeRootBorderPane= new BorderPane();
        treeRootBorderPane.setCenter(theTreeView);
        treeRootBorderPane.setBottom(theSwitchButton);
        Scene theScene= new Scene(treeRootBorderPane);
        EpiScene.setDefaultsV(theScene);
        return theScene;
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
    
    /*  ////
    private void setButtonActionsV()
      /* This method defines what actions will be taken when
       * the user activates one of the available buttons.
       * It does the tree button and the list button.
       */
    /*  ////
      {
        theTreeShowItemButton.setOnAction(e -> doItemButtonActionV());
        theItemShowTreeButton.setOnAction(e -> doTreeButtonActionV());
        }
    */  ////

    private void doTreeShowItemButtonActionV()
      /* This method sets the Scene to display
       * the TreeItem presently displayed by the TreeView.
       */
      {
        TreeItem<DataNode> theTreeItemOfDataNode=
          theTreeView.getSelectionModel().getSelectedItem();
        if (null != theTreeItemOfDataNode) { // Process if item selected.
          DataNode theDataNode= theTreeItemOfDataNode.getValue();
          TreePath theTreePath= theDataNode.getTreePath();
          Node itemNode= theDataNode.getJavaFXNode(theTreePath, null);
          itemRootBorderPane.setCenter(itemNode);
          }
        setScene(theItemScene); // Switch to item scene.
        }

    private void doItemShowTreeButtonActionV()
      /* This method sets the Scene to display the TreeView.
       */
      {
        setScene(theTreeScene); // Switch [back] to tree scene.
        }
    
    }
