package allClasses.javafx;

import java.util.List;

import allClasses.DataNode;
import javafx.collections.ObservableList;
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
        treeRootBorderPane.setTop(treeButton);
        treeRootBorderPane.setCenter(theTreeView);
        treeScene= new Scene(treeRootBorderPane);
        EpiScene.setDefaultsV(treeScene);
        }

    private void initializeListSceneV()
      {
        theListView= new ListView<DataNode>();
        listButton= new Button("Show Tree");
        BorderPane listRootBorderPane= new BorderPane();
        listRootBorderPane.setTop(listButton);
        listRootBorderPane.setCenter(theListView);
        listScene= new Scene(listRootBorderPane);
        EpiScene.setDefaultsV(listScene);
        }
    
    private void setButtonActionsV()
      /* This method defines what actions will be taken when
       * the user activates the available buttons.
       */
      {
        treeButton.setOnAction(e -> {
          ////// define list from tree selection.
          TreeItem<DataNode> theTreeItemOfDataNode=
            theTreeView.getSelectionModel().getSelectedItem();
          if (null != theTreeItemOfDataNode) {
            DataNode theDataNode= theTreeItemOfDataNode.getValue();
            ObservableList<DataNode> childObservableList= 
                theDataNode.getChildObservableListOfDataNodes();
            theListView.setItems(childObservableList);
            }
          setScene(listScene); // Switch to list scene.
          });
        listButton.setOnAction(e -> { 
          setScene(treeScene); // Switch to tree scene.
          });
        }

    }
