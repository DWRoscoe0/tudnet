package allClasses.javafx;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static allClasses.AppLog.theAppLog;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class TreeStage extends EpiStage 
  { 

    public TreeStage(JavaFXGUI theJavaFXGUI)
      /* This method finishes the launch begun by the Application subclass. 
       * It should run only on the JavaFX application thread. 
       */
      {
        Stage primaryStage= this;
        try {
          //// final Node rootIcon = new ImageView(
          ////   new Image(getClass().getResourceAsStream("folder_16.png"))
          ////   ); // Produces NullPointerException.
          primaryStage.setTitle("Tree View Sample");        
      
          //// TreeItem<String> rootItem = new TreeItem<String> ("Inbox", rootIcon);
          TreeItem<String> rootItem = new TreeItem<String> ("Inbox");
          rootItem.setExpanded(true);
          for (int i = 1; i < 6; i++) {
              TreeItem<String> item = new TreeItem<String> ("Message" + i);            
              rootItem.getChildren().add(item);
          }        
          TreeView<String> tree = new TreeView<String> (rootItem);        
          StackPane root = new StackPane();
          root.getChildren().add(tree);
          primaryStage.setScene(new Scene(root, 300, 250));
          primaryStage.show();

          theJavaFXGUI.recordOpenWindowV(this);
        } catch(Exception e) {
          //// e.printStackTrace();
          theAppLog.error("TreeStage.TreeStage(.) "+e);
        }
      }
    }
