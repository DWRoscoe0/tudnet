package allClasses.javafx;

import allClasses.Shutdowner;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class DemoStage /// extends EpiStage 
{ 
  
  public static void makeInitializeAndStartV(
      JavaFXGUI theJavaFXGUI,Shutdowner theShutdowner)
    {
      EpiStage theEpiStage= new EpiStage(theShutdowner);
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

      theEpiStage.setScene(theScene);
      theEpiStage.setCommonSettingsV(
        "Demo"
        );
      }

  }