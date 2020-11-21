package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import java.util.HashMap;
import java.util.Map;

import allClasses.Shutdowner;
import javafx.stage.Window;

public class JavaFXGUI

  /* This class is used to manage JavaFX operations,
   * including launching and windows.
   * 
   * The JavaFX app launching and life-cycle are NOT elegant.
   * They involve a lot of Java reflection and static methods.
   * To deal with this fact, this class is being made 
   * a Singleton with static access methods.
   */

  {
    // Injected dependencies.
    private Shutdowner theShutdowner;
  
    private Map<Window,Boolean> windowMap= // Stores showing windows. 
        new HashMap<Window,Boolean>();
  
    private static JavaFXGUI theJavaFXGUI= null; // The 1 instance.
    
    public static JavaFXGUI getJavaFXGUI() 
      // This is the instance getter method.
      {
        if (null == theJavaFXGUI) {
          theAppLog.error("JavaFXGUI.getJavaFXGUI() "
              + "Instance not constructed yet!");
          theJavaFXGUI= initializeJavaFXGUI(null);
          }
        return theJavaFXGUI;
        }

    public static JavaFXGUI initializeJavaFXGUI(
          Shutdowner theShutdowner
          )
      /* This is the initializer and dependency injector.
       * It doesn't inject any dependencies yet, but this is where they will go.
       */
    {
      if (null != theJavaFXGUI)
        theAppLog.error("JavaFXGUI.initializeJavaFXGUI() "
            + "Instance already constructed!");
        else
        { // Create instance and store dependencies into it.
          theJavaFXGUI= new JavaFXGUI();
          theJavaFXGUI.theShutdowner= theShutdowner;
          }
      return theJavaFXGUI;
      }

    private JavaFXGUI() {} // private constructor guarantees single instance.
    
    /*  ////
      {
        Stage theStage= new Stage(); // Construct Stage.
        recordOpenWindowV(theStage); // Record it in map.
        return theStage;
        }
     */  ////
    
    public void recordOpenWindowV(Window theWindow)
      /* This method records an open (showing) window.  */
      {
        windowMap.put(theWindow, true); // Record it in map.
        }

    public void startJavaFXLaunchV(String[] args) 
    
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
              JavaFXApp.launch(JavaFXApp.class, args);
              JavaFXGUI.getJavaFXGUI().theShutdowner.requestAppShutdownV();
              }
            };
        Thread javaFXLauncherThread= // Create launcher thread.
          new Thread(
            javaFXRunnable,
            "JavaFXLauncher" // Thread name.
            );
        javaFXLauncherThread.start(); // Start launcher thread.
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