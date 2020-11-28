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
   * 
   * The JavaFX app launching and life-cycle are NOT elegant.
   * They involve a lot of Java reflection and static methods.
   * To deal with this fact, this class is a Singleton.
   */

  {

    // Injected dependencies.

    private Shutdowner theShutdowner;
    private DataNode theInitialRootDataNode;

    // Other variables

    private Map<Window,Boolean> windowMap= // Stores showing windows. 
        new HashMap<Window,Boolean>();
  
    private static JavaFXGUI theJavaFXGUI= null; // The 1 instance.


    // Methods

    public static JavaFXGUI getInstanceJavaFXGUI()
      /* This is the instance getter method.  */
      {
        if (null == theJavaFXGUI) {
          throw new IllegalStateException(
              "JavaFXGUI Instance not constructed yet!");
          }
        return theJavaFXGUI;
        }

    public static JavaFXGUI getJavaFXGUI(
          DataNode theInitialRootDataNode,
          Shutdowner theShutdowner
          )
      /* This method constructs and initializes which will be
       * a single instance of the JavaFXGUI object.
       */
    {
      if (null != theJavaFXGUI)
        theAppLog.error("JavaFXGUI.getJavaFXGUI(.) "
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
    
    public void recordOpenWindowV(Window theWindow)
      /* This method records an open (showing) window.  */
      {
        windowMap.put(theWindow, true); // Record it in map.
        }
    
    public void startJavaFXLaunchV()
    
      /* This method creates and starts a thread 
       * which launches the JavaFX sub-Application.
       * It queues this job on the JavaFX runtime queue,
       * then it returns.
       * 
       * If the launch(.) method returns, it means 
       * the JavaFX GUI and JavaFX part of the app has closed,
       * so a complete shutdown is requested
       * which will shutdown the Swing GUI also.
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
       * It should be run only on the JavaFX application thread. 
       * It creates some temporary demonstration windows
       * and an actual useful one.
       */
      {
      
        TreeStage.makeStage(this); // Create tree demonstration.
        
        DemoStage.makeStage(this); // Create button demonstration.

        Navigation.makeStageV( // Create Navigation Stage.
            this, theInitialRootDataNode);

        // This method will now return to Application.start(Stage).
        // After Application.start(Stage) returns, the launch will be complete.
        }

    public void finalizeV()
      /* This method finalizes the JavaFX GUI.
       * It does this by closing (hiding) all open (showing) JavaFX windows.
       * This will allow the app to terminate 
       * if other termination conditions are satisfied.  
       */
      {
        for (Window theWindow : windowMap.keySet())
          theWindow.hide();
        }
    
    }
