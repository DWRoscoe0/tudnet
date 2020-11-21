package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import java.util.HashMap;
import java.util.Map;
import javafx.stage.Window;

public class JavaFXWindows

  /* This class is used to manage JavaFX operations,
   * including launching and windows.
   * 
   * The JavaFX app launching and life-cycle are NOT elegant.
   * They involve a lot of Java reflection and static methods.
   * To deal with this fact, this class is being made 
   * a Singleton with static access methods.

00000000001111111111222222222233333333334444444444555555555566666666667777777777
01234567890123456789012345678901234567890123456789012345678901234567890123456789

   */

  {
  
    private static JavaFXWindows theJavaFXWindows= null; // The 1 instance.
    
    public static JavaFXWindows getJavaFXWindows() 
      // This is the instance getter method.
      {
        if (null == theJavaFXWindows) {
          theAppLog.error("JavaFXWindows.getJavaFXWindows() "
              + "Instance not constructed yet!");
          theJavaFXWindows= initializeJavaFXWindows();
          }
        return theJavaFXWindows;
        }

    public static JavaFXWindows initializeJavaFXWindows()
      // This is the initializer and dependency injector.
    {
      if (null != theJavaFXWindows)
        theAppLog.error("JavaFXWindows.initializeJavaFXWindows() "
            + "Instance already constructed!");
        else
        theJavaFXWindows= new JavaFXWindows();
      return theJavaFXWindows;
      }

    private Map<Window,Boolean> windowMap= // Stores showing windows. 
        new HashMap<Window,Boolean>();

    private JavaFXWindows() {} // private constructor guarantees single instance.
    
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
       */
      {
        Runnable javaFXRunnable= // Create launcher Runnable. 
          new Runnable() {
            @Override
            public void run() {
              JavaFXApp.launch(JavaFXApp.class, args);
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