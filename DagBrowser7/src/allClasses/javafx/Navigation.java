package allClasses.javafx;

import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

public class Navigation 

  /* This class is for navigation of the Infogora hierarchy.
   * It's central control is the TreeView. 
   */

  {

    public static void makeStageV(JavaFXGUI theJavaFXGUI)
      {
        Stage theStage= new Stage();
        TreeItem<String> rootTreeItem= 
            new TreeItem<String> ("Naviation-Tree");
        rootTreeItem.setExpanded(true);
        for (int i= 1; i < 6; i++) {
          TreeItem<String> item= new TreeItem<String> ("Item" + i);            
          rootTreeItem.getChildren().add(item);
          }
        TreeView<String> theTreeView= new TreeView<String>(rootTreeItem);        
        Scene theScene= new Scene(theTreeView, 300, 250);
        EpiStage.finishSettingsAndShowV(
          theStage,
          theJavaFXGUI, 
          theScene,
          "Navigation Tree View"
          );
      }

    }
