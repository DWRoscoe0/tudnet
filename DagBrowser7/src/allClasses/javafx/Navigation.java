package allClasses.javafx;

import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class Navigation 

  /* This class is, or eventually will be,
   * for navigation of the Infogora hierarchy.
   * It's central control is the TreeView. 
   */

  {

    public static void makeStageV(JavaFXGUI theJavaFXGUI)
      {
        EpiStage theEpiStage= EpiStage.prepareEpiStage(theJavaFXGUI);
        TreeItem<String> rootTreeItem= 
            new TreeItem<String> ("Naviation-Tree");
        rootTreeItem.setExpanded(true);
        for (int i= 1; i < 6; i++) {
          TreeItem<String> item= new TreeItem<String> ("Item" + i);            
          rootTreeItem.getChildren().add(item);
          }
        TreeView<String> theTreeView= new TreeView<String>(rootTreeItem);        
        Scene theScene= new Scene(theTreeView, 300, 250);
        theEpiStage.finishSettingsAndShowV(
          theEpiStage,
          theScene,
          "Navigation Tree View"
          );
      }

    }
