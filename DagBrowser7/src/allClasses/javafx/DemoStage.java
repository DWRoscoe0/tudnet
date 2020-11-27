package allClasses.javafx;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class DemoStage extends EpiStage 
{ 

  public static DemoStage makeDemoStage(JavaFXGUI theJavaFXGUI)
    {
      DemoStage theDemoStage= new DemoStage(theJavaFXGUI);
      BorderPane theBorderPane = new BorderPane(); // This is the root node.
      theBorderPane.setStyle("-fx-font-family: \"monospace\"; ");
        // Must be first to not override font-size.
      theBorderPane.setStyle("-fx-font-size: 22");
      Scene theScene = new Scene(theBorderPane,400,400);
      theScene.getStylesheets().add(theDemoStage.getClass()
          .getResource("application.css").toExternalForm());
      Label theLabel = 
          new Label("JavaFX sub-Application window!");

      Button theButton = new Button("Who wrote this app?");
      theButton.setOnAction(e -> theLabel.setText(
          "David Roscoe wrote this app!"));
      
      VBox theVBox = new VBox(15.0, theLabel, theButton);
      theVBox.setAlignment(Pos.CENTER);
      
      theBorderPane.setCenter(theVBox);
      
      theDemoStage.setScene(theScene);
      theDemoStage.show();
      theJavaFXGUI.recordOpenWindowV(theDemoStage);
      return theDemoStage;
      }

  public DemoStage(JavaFXGUI theJavaFXGUI)
    {
    }

  }