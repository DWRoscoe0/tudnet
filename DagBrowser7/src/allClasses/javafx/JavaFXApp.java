package allClasses.javafx;

import javafx.application.Application;
import javafx.stage.Stage;

public class JavaFXApp extends Application

  /* This class defines the JavaFX "Application".
   * It's only purpose is to satisfy the requirement for
   * a subclass of javafx.application.Application    * in the JavaFX Life-Cycle.
   * 
   * The name "Application" is a little misleading.
   * It includes only the JavaFX part of the GUI.  
   * It does not include a possible Swing part.
   * It does not include the non-GUI business logic.
   * 
   * The only method defined is start(Stage),
   * and it does nothing but delegate to a JavaFXGUI method.
   * Start runs only on the JavaFX application thread. 
   */

  {
    
    @Override
    public void start(Stage fromLauncherToBeIgnoredStage)
      {
        fromLauncherToBeIgnoredStage= null; // Set to null to garbage collect
          // the ignored and unused Stage.  We will construct our own Stages.
        
        JavaFXGUI.getJavaFXGUI().continueLaunchV();
        }
  
    }
