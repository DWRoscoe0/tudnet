package allClasses.javafx;

import javafx.application.Application;
import javafx.stage.Stage;

public class JavaFXApp extends Application

  /* This class defines the JavaFX "Application".
   * It's only purpose is to satisfy the requirement for
   * a subclass of javafx.application.Application
   * in the not-elegant JavaFX Life-Cycle.
   * 
   * The name "Application" is misleading.
   * It includes only the JavaFX part of the GUI.  
   * It does not include a possible Swing part.
   * It does not include the non-GUI business logic.
   * 
   * The only method defined is start(Stage),
   */

  {
    
    @Override
    public void start(Stage fromLauncherToBeIgnoredStage)
      /* This method does only 2 things:
       * * It dereferences the Stage argument so it will be garbage collected.
       *   The Stages this app needs will be constructed elsewhere.
       * * It immediately calls back to JavaFXGUI to finish what needs doing
       *   in the launch process.
       *
       * This method runs on the JavaFX application thread. 
       */
      {
        fromLauncherToBeIgnoredStage= null; // Set to null to garbage collect.
        
        JavaFXGUI.getInstanceJavaFXGUI().continueStartV();
        }
  
    }
