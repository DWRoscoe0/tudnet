package allClasses.javafx;

import allClasses.DataNode;
import javafx.scene.Scene;
import javafx.scene.control.TreeView;

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
        EpiTreeItem rootEpiTreeItem= 
            new EpiTreeItem(theInitialRootDataNode);
        rootEpiTreeItem.setExpanded(true);
        TreeView<DataNode> theTreeView= 
            new TreeView<DataNode>(rootEpiTreeItem);        
        Scene theScene= new Scene(theTreeView, 300, 250);
        theEpiStage.finishSettingsAndShowV(
          theEpiStage,
          theScene,
          "Navigation Tree View"
          );
      }

    }
