package allClasses.javafx;

import java.util.HashMap;
import java.util.Map;
import javafx.stage.Stage;
import javafx.stage.Window;

public class JavaFXWindows

  /* This class is used to manage JavaFX operations,
   * including launching and windows.
   * 
   * JavaFX app launching and life-cycle are NOT elegant.
   * They involve a lot of Java reflection
   * and static methods. 
   */

  {
  
    private static Map<Window,Boolean> windowMap= 
        new HashMap<Window,Boolean>();
    
    public static Stage makeStage() 
      {
        Stage theStage= new Stage(); // Construct Stage.
        recordOpenWindowV(theStage); // Record it in map.
        return theStage;
        }
    
    public static void recordOpenWindowV(Window theWindow)
      /* This method records an open (showing) window.  */
      {
        windowMap.put(theWindow, true); // Record it in map.
        }
    
    public static void closeWindows()
      /* This method closes (hides) 
       * all open (showing) JavaFX windows.
       * This will allow the app to terminate if
       * other termination conditions are satisfied.  
       */
      {
        for (Window theWindow : windowMap.keySet())
          theWindow.hide();
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
              //// JavaFXApp.main(args); // Runs launcher.
              JavaFXApp.launch(JavaFXApp.class,args);
              }
            };
        Thread javaFXLauncherThread= // Create launcher thread.
          new Thread(
            javaFXRunnable,
            "JavaFXLauncher" // Thread name.
            );
        javaFXLauncherThread.start(); // Start launcher thread.
        }
    
    }