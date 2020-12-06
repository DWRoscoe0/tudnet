package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import java.util.HashMap;
import java.util.Map;

import allClasses.DataNode;
import allClasses.Shutdowner;
import javafx.stage.Window;

public class JavaFXGUI

  /* This class is used to manage JavaFX operations,
   * including launching and windows.
   * It might superficially resemble a Singleton because
   * it has a static method to return the only instance of this class.
   * That is because the JavaFX app launching and life-cycle are NOT elegant,
   * involve a lot of Java reflection and some static methods,
   */

  {

    // Injected dependencies.

    private Shutdowner theShutdowner;
    private DataNode theInitialRootDataNode;

    // Other variables

    private Map<Window,Boolean> windowMap= // Stores showing windows. 
        new HashMap<Window,Boolean>();
  
    private static JavaFXGUI theJavaFXGUI=  // The 1 instance of this class. 
        null;


    // Methods

    public static JavaFXGUI initializeJavaFXGUI(
          DataNode theInitialRootDataNode,
          Shutdowner theShutdowner
          )
      /* This method constructs, initializes, and returs
       * what will become the only instance of this class.
       */
    {
      if (null != theJavaFXGUI)
        theAppLog.error("initializeJavaFXGUI.getJavaFXGUI(.) "
            + "Instance already constructed!");
        else
        { // Create instance and store injected dependencies.
          theJavaFXGUI= new JavaFXGUI();
          theJavaFXGUI.theShutdowner= theShutdowner;
          theJavaFXGUI.theInitialRootDataNode= theInitialRootDataNode;
          }
      return theJavaFXGUI;
      }

    private JavaFXGUI() {} // private constructor guarantees a single instance.

    public static JavaFXGUI getInstanceJavaFXGUI()
      /* This is the instance getter method.
       * This method should not be called until 
       * initializeJavaFXGUI(.) has been called.
       * It exists mainly so that JavaFXApp can call back to this class.
       */
      {
        if (null == theJavaFXGUI) { // If not instantiated yet
          throw new IllegalStateException( // throw exception.
              "JavaFXGUI Instance not constructed yet!");
          }
        return theJavaFXGUI;
        }
    
    public void startJavaFXLaunchV()
    
      /* This method creates and starts a thread 
       * which launches the JavaFX sub-Application.
       * It must not be called until initialization method
       * initializeJavaFXGUI(.) is called.
       * 
       * It queues this job on the JavaFX runtime queue,
       * then it returns.
       * 
       * This method works by creating a Runnable containing a call to
       * the Application.launch(.) method, makes a Thread from that,
       * then starts the thread.
       * If the launch(.) method returns, it means 
       * the JavaFX GUI and JavaFX part of the app has closed,
       * so a complete shutdown is requested
       * which will shutdown the Swing GUI also.
       * 
       * This method initiates what is normally shown as being done by
       * the main(.) method in example JavaFX applications.
       * This method was created so that a JavaFX GUI 
       * could be developed in parallel to an existing Java Swing GUI,
       * both being part of the same app. 
       */
      {
        Runnable javaFXRunnable= // Create launcher Runnable. 
          new Runnable() {
            @Override
            public void run() {
              JavaFXApp.launch( // Launch sub-App as JavaFX Application.
                  JavaFXApp.class, (String[])null);
              theShutdowner.requestAppShutdownV();
              }
            };
        Thread javaFXLauncherThread= // Create launcher thread from Runnable.
          new Thread(
            javaFXRunnable,
            "JavaFXLauncher" // Thread name.
            );
        javaFXLauncherThread.start(); // Start launcher thread.
        }
    
    public void continueStartV()
      /* This method continues the launch begun by 
       * the Application subclass start(Stage) method.
       * It creates some GUI Stages for the application, then exits.
       * 
       * This method should be run only on the JavaFX application thread. 
       */
      {
        TreeStage.makeStage(this); // Create temporary tree demonstration.
        DemoStage.makeStage(this); // Create temporary button demonstration.

        Navigation.makeStageV( // Create application Navigation Stage.
            this, theInitialRootDataNode);

        // This method will now return to the Application.start(Stage) method.
        // After Application.start(Stage) returns, the launch will be complete.
        }
    
    public void recordOpenWindowV(Window theWindow)
      /* This method records an opening (showing) of theWindow.  */
      {
        windowMap.put(theWindow, true); // Record it in map.
        }

    public void finalizeV()
      /* This method finalizes the JavaFX GUI
       * in preparation for app exit.  It is called during app shutdown.
       * 
       * This method works by hiding (closing) all showing JavaFX windows.
       * This is a signal to JavaFX to stop all GUI operations 
       * assuming other termination conditions are satisfied.  
       */
      {
        for (Window theWindow : windowMap.keySet())
          theWindow.hide();
        }
    
    }
