package allClasses.javafx;
	

public class Main extends Object 

  /* This class creates a thread with which to launch
   * a JavaFX sub-Application.
   * It could do other stuff, before of after,
   * but it doesn't do that here.
   */
  {
    
    public static void main(String[] args) {
      new Thread(
        new Runnable() {
          @Override
          public void run() {
            JavaFXApp.main(args);
            }
          },
        "JavaFXLauncher" // Thread name.
        ).start();
      }

    }
