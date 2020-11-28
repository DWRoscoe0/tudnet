package allClasses.javafx;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

class DemoStage extends EpiStage 
{ 
  
  private DemoStage() {}
  
  public static Stage makeStage(JavaFXGUI theJavaFXGUI)
    {
      EpiStage theEpiStage= EpiStage.prepareEpiStage(theJavaFXGUI);
      BorderPane theBorderPane = new BorderPane(); // This is the root node.
      Scene theScene = new Scene(theBorderPane,400,400);
      Label theLabel = 
          new Label("JavaFX sub-Application window!");

      Button theButton = new Button("Who wrote this app?");
      theButton.setOnAction(e -> theLabel.setText(
          "David Roscoe wrote this app!"));
      
      VBox theVBox = new VBox(15.0, theLabel, theButton);
      theVBox.setAlignment(Pos.CENTER);
      
      theBorderPane.setCenter(theVBox);

      theEpiStage.finishSettingsAndShowV(
        theEpiStage,
        theScene,
        "Demo"
        );
      return theEpiStage;
      }

  }