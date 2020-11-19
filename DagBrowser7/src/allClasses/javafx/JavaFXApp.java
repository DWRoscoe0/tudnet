package allClasses.javafx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class JavaFXApp extends Application {
  
  @Override
  public void start(Stage primaryStage) {
      try {
        BorderPane theBorderPane = new BorderPane();
        Scene theScene = new Scene(theBorderPane,400,400);
        theScene.getStylesheets().add(getClass()
            .getResource("application.css").toExternalForm());
        Label theLabel = 
            new Label("JavaFX sub-Application window!");

        Button theButton = new Button("Who wrote this app?");
        theButton.setOnAction(e -> theLabel.setText(
            "David Roscoe wrote this app!"));
        
        VBox theVBox = new VBox(15.0, theLabel, theButton);
        theVBox.setAlignment(Pos.CENTER);
        
        theBorderPane.setCenter(theVBox);
        primaryStage.setScene(theScene);
        primaryStage.show();
        JavaFXWindows.recordOpenWindowV(primaryStage);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  
  /*  ////
  public static void main(String[] args) {
      Application.launch(args);
      }
  */  ////

  }
