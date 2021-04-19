package allClasses.javafx;

import javafx.scene.Node;
import javafx.scene.Scene;

public class EpiScene 
  {

    public static void setDefaultsV(Scene theScene)
      {
        Node rootNode= theScene.getRoot();
        JavaFXGUI.setDefaultStyle(rootNode);
        //// rootNode.setStyle(
        ////     "-fx-font-size: 22; -fx-font-family: \"monospace\"; ");
        }
    
    }
