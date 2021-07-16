package allClasses.javafx;

import allClasses.Shutdowner;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;


public class TreeStage //// extends EpiStage

  { 

    public static void makeInitializeAndStartV(
        JavaFXGUI theJavaFXGUI,Shutdowner theShutdowner)
      {
        //// EpiStage theEpiStage= EpiStage.makeEpiStage(theJavaFXGUI);
        //// EpiStage theEpiStage= new EpiStage(theJavaFXGUI,theShutdowner);
        EpiStage theEpiStage= new EpiStage(theShutdowner);
        TreeItem<String> rootTreeItem= new TreeItem<String> ("Inbox");
        rootTreeItem.setExpanded(true);
        for (int i= 1; i < 6; i++) {
          TreeItem<String> item= new TreeItem<String> ("Message" + i);            
          rootTreeItem.getChildren().add(item);
          }
        TreeView<String> theTreeView= new TreeView<String>(rootTreeItem);        
        Scene theScene= new Scene(theTreeView, 300, 250);
        theEpiStage.setScene(theScene);
        theEpiStage.finishStateInitAndStartV(
          "Tree View Sample"
          );
      }
    }
