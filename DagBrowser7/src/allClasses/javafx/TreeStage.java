package allClasses.javafx;

import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;


public class TreeStage extends EpiStage 
  { 

    public static void makeInitializeAndStartV(JavaFXGUI theJavaFXGUI)
      {
        EpiStage theEpiStage= EpiStage.makeEpiStage(theJavaFXGUI);
        TreeItem<String> rootTreeItem= new TreeItem<String> ("Inbox");
        rootTreeItem.setExpanded(true);
        for (int i= 1; i < 6; i++) {
          TreeItem<String> item= new TreeItem<String> ("Message" + i);            
          rootTreeItem.getChildren().add(item);
          }
        TreeView<String> theTreeView= new TreeView<String>(rootTreeItem);        
        Scene theScene= new Scene(theTreeView, 300, 250);
        theEpiStage.setScene(theScene);
        theEpiStage.finishInitAndStartV(
          "Tree View Sample"
          );
      }
    }
