package allClasses.javafx;

import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;


public class TreeStage extends EpiStage 
  { 

    public static Stage makeStage(JavaFXGUI theJavaFXGUI)
      {
        EpiStage theEpiStage= EpiStage.prepareEpiStage(theJavaFXGUI);
        TreeItem<String> rootTreeItem= new TreeItem<String> ("Inbox");
        rootTreeItem.setExpanded(true);
        for (int i= 1; i < 6; i++) {
          TreeItem<String> item= new TreeItem<String> ("Message" + i);            
          rootTreeItem.getChildren().add(item);
          }
        TreeView<String> theTreeView= new TreeView<String>(rootTreeItem);        
        Scene theScene= new Scene(theTreeView, 300, 250);
        theEpiStage.finishSettingsAndShowV(
          theEpiStage,
          theScene,
          "Tree View Sample"
          );
        return theEpiStage;
      }
    }
