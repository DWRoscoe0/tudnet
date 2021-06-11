package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import javafx.application.Application;
import javafx.stage.Stage;

public class JavaFXApp extends Application

  /* This class defines the JavaFX "Application".
   * It's purpose is to satisfy the requirement for
   * a subclass of javafx.application.Application
   * in the not-elegant JavaFX application Life-Cycle.
   * 
   * The name "Application" by itself is misleading.
   * It includes only the JavaFX GUI part of the application.  
   * It does not include a possible Swing GUI part.
   * It does not include the non-GUI business logic.
   * 
   * The only method that needs to be defined is start(Stage).
   * Other methods and a static code section 
   * were added for debugging purposes.
   */

  {

    static { // Added to log when this class is loaded.
        theAppLog.info("JavaFXApp loaded.");
        }

    public JavaFXApp()
      /* Note, this constructor must be public, otherwise a
       *   java.lang.NoSuchMethodException: allClasses.javafx.JavaFXApp.<init>()
       * will result.
       */
      {
        super();
        theAppLog.debug("JavaFXAppLog","JavaFXApp() constructor called.");
        }

    @Override
    public void init()
        throws Exception
      {
        theAppLog.debug("JavaFXAppLog","JavaFXApp.init() called.");
        }
    
    @Override
    public void start(Stage fromLauncherToBeIgnoredStage)
      /* This is the standard start(.) method.  It does only 2 things:
       * * It dereferences the provided Stage argument 
       *   so it will be garbage collected,
       *   because this app uses customized Stage subclasses.
       *   needs will be constructed elsewhere.
       * * It immediately calls JavaFXGUI().nestedStartV() 
       *   to do the actual start.
       *
       * This method runs on the JavaFX application thread
       * and so it should return quickly. 
       */
      {
        theAppLog.debug("JavaFXAppLog","JavaFXApp.start(Stage) begins, "
            + "calling JavaFXGUI.getInstanceJavaFXGUI().nestedStartV"
            + "().");

        fromLauncherToBeIgnoredStage= null; // Set to null to garbage collect.

        JavaFXGUI.getInstanceJavaFXGUI().nestedStartV();
        theAppLog.debug("JavaFXAppLog","JavaFXApp.start(Stage) "
            + "returned from JavaFXGUI.getInstanceJavaFXGUI().nestedStartV(),"
            + " ending.");
        }
  
    public void stop()
          throws Exception
      {
        theAppLog.debug("JavaFXAppLog","JavaFXApp.stop() called.");
        }


    }
