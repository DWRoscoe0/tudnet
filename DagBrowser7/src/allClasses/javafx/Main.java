package allClasses.javafx;
	

public class Main extends Object 

  /* This class is used to launch 
   * the JavaFX part of an application.
   */
  {
    
    public static void main(String[] args) 
    
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
              JavaFXApp.main(args); // Runs launcher.
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
