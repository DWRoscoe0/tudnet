package allClasses.javafx;

import allClasses.DataNode;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;
//// import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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
        VBox treeRootVBox = new VBox(0.0,
            treeButton,
            theTreeView
            );
        Scene treeScene= 
            new Scene(treeRootVBox, 400, 600);

        ListView<DataNode> theListView= 
            new ListView<DataNode>();
        Button listButton= new Button("Show Tree");
        VBox listRootVBox= new VBox(0.0,
            listButton,
            theListView
            );
        Scene listScene= 
            new Scene(listRootVBox, 400, 600);

        // Set button actions, not that Scenes are defined.
        treeButton.setOnAction(e -> {
          //// treeButton.setText("List Boo!");
          theEpiStage.setScene(listScene);
          });
        listButton.setOnAction(e -> { 
          //// listButton.setText("Tree Boo!");
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
