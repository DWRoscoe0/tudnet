package allClasses.javafx;

import allClasses.DataNode;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
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
    private Scene listScene;
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

        // Set button actions, now that Scenes are defined.
        treeButton.setOnAction(e -> {
          ////// define list from tree selection.
          setScene(listScene);
          });
        listButton.setOnAction(e -> { 
          setScene(treeScene);
          });

        setScene(treeScene); // Use tree scene as first one displayed.
        
        finishInitAndStartV("Infogora JavaFX Navigatioo UI");
        }

    private void initializeTreeSceneV()
      {
        EpiTreeItem theEpiTreeItem=
            new EpiTreeItem(theInitialRootDataNode);
        theEpiTreeItem.setExpanded(true);
        TreeView<DataNode> theTreeView= 
            new TreeView<DataNode>(theEpiTreeItem);
        treeButton= new Button("Show List");
        BorderPane treeRootBorderPane= new BorderPane();
        treeRootBorderPane.setTop(treeButton);
        treeRootBorderPane.setCenter(theTreeView);
        treeScene= new Scene(treeRootBorderPane, 300, 500);
        EpiScene.setDefaultsV(treeScene);
        }

    private void initializeListSceneV()
      {
        ListView<DataNode> theListView= 
            new ListView<DataNode>();
        listButton= new Button("Show Tree");
        BorderPane listRootBorderPane= new BorderPane();
        listRootBorderPane.setTop(listButton);
        listRootBorderPane.setCenter(theListView);
        listScene= new Scene(listRootBorderPane, 400, 600);
        EpiScene.setDefaultsV(listScene);
        }

    }
