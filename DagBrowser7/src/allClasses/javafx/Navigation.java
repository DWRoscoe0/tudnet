package allClasses.javafx;

import allClasses.DataNode;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
//// import javafx.scene.layout.Region;
//// import javafx.scene.layout.VBox;

public class Navigation 

  /* This class is, or eventually will be,
   * for navigation of the Infogora hierarchy.
   * It's central control is the TreeView. 
   */

  {

    public static void makeStageV(
        JavaFXGUI theJavaFXGUI, DataNode theInitialRootDataNode)
      {
        EpiStage theEpiStage= EpiStage.prepareEpiStage(theJavaFXGUI);

        EpiTreeItem theEpiTreeItem=
            new EpiTreeItem(theInitialRootDataNode);
        theEpiTreeItem.setExpanded(true);
        TreeView<DataNode> theTreeView= 
            new TreeView<DataNode>(theEpiTreeItem);
        Button treeButton= new Button("Show List");
        BorderPane treeRootBorderPane= new BorderPane();
        treeRootBorderPane.setTop(treeButton);
        treeRootBorderPane.setCenter(theTreeView);
        Scene treeScene= 
            new Scene(treeRootBorderPane, 300, 500);
        EpiScene.setDefaultsV(treeScene);

        ListView<DataNode> theListView= 
            new ListView<DataNode>();
        Button listButton= new Button("Show Tree");
        BorderPane listRootBorderPane= new BorderPane();
        listRootBorderPane.setTop(listButton);
        listRootBorderPane.setCenter(theListView);
        Scene listScene= 
            new Scene(listRootBorderPane, 400, 600);
        EpiScene.setDefaultsV(listScene);

        // Set button actions, now that Scenes are defined.
        treeButton.setOnAction(e -> {
          theEpiStage.setScene(listScene);
          });
        listButton.setOnAction(e -> { 
          theEpiStage.setScene(treeScene);
          });

        //// theEpiStage.setMaximized(true);
        theEpiStage.finishSettingsAndShowV(
          //// theEpiStage,
          treeScene,
          "Infogora JavaFX UI"
          );
      }

    }
