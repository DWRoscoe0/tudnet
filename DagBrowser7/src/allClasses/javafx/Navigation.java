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
    private Scene treeScene; 
    private Button treeButton;
    private TreeView<DataNode> theTreeView; 
    private Scene listScene;
    private ListView<DataNode> theListView; 
    private Button listButton;
    private BorderPane listRootBorderPane;
            
    public Navigation(JavaFXGUI theJavaFXGUI, DataNode theInitialRootDataNode)
      {
        super(theJavaFXGUI);
        this.theInitialRootDataNode= theInitialRootDataNode;
        }
    
    public void initializeAndStartV()
      /* This method initializes and starts the Navigation Stage, 
       * then returns.  
       */
      {
        initializeTreeSceneV();

        initializeListSceneV();

        setButtonActionsV(); // Okay to do now that Scenes are defined.

        setScene(treeScene); // Use tree scene as first one displayed.
        
        finishInitAndStartV("Infogora JavaFX Navigatioo UI");
        }

    private void initializeTreeSceneV()
      {
        EpiTreeItem theEpiTreeItem=
            new EpiTreeItem(theInitialRootDataNode);
        theEpiTreeItem.setExpanded(true);
        theTreeView= new TreeView<DataNode>(theEpiTreeItem);
        treeButton= new Button("Show List");
        BorderPane treeRootBorderPane= new BorderPane();
        treeRootBorderPane.setCenter(theTreeView);
        //// treeRootBorderPane.setTop(treeButton);
        treeRootBorderPane.setBottom(treeButton);
        treeScene= new Scene(treeRootBorderPane);
        EpiScene.setDefaultsV(treeScene);
        }

    private void initializeListSceneV()
      {
        listRootBorderPane= new BorderPane();
        theListView= new ListView<DataNode>();
        listRootBorderPane.setCenter(theListView);
        listButton= new Button("Show Tree");
        listRootBorderPane.setBottom(listButton);
        listScene= new Scene(listRootBorderPane);
        EpiScene.setDefaultsV(listScene);
        }
    
    private void setButtonActionsV()
      /* This method defines what actions will be taken when
       * the user activates one of the available buttons.
       */
      {
        treeButton.setOnAction(e -> {
          TreeItem<DataNode> theTreeItemOfDataNode=
            theTreeView.getSelectionModel().getSelectedItem();
          if (null != theTreeItemOfDataNode) {
            DataNode theDataNode= theTreeItemOfDataNode.getValue();
            TreePath theTreePath= theDataNode.getTreePath();
            Node listNode= theDataNode.getJavaFXNode(theTreePath, null);
            listRootBorderPane.setCenter(listNode);
            }
          setScene(listScene); // Switch to list scene.
          });
        listButton.setOnAction(e -> { 
          setScene(treeScene); // Switch to tree scene.
          });
        }

    }
