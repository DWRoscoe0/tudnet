package allClasses;

import static allClasses.Globals.appLogger;

//// import java.io.File;

public class InfogoraStarter 

  {

    public static void main(String[] argStrings)
      /* This method is the app starter's entry point.  It does the following:
        * It sets a default Exception handler.
        * It starts the Infogora app.
        * It waits for the Infogora app to terminate.
        * It waits for anything spawned by the Infogora app terminate.

        */
      { // main(..)
        Process theProcess= null;
        appLogger.setIDProcessV("Starter");
        appLogger.setBufferedModeV( true ); // Enabling fast buffered logging.
        DefaultExceptionHandler.setDefaultExceptionHandlerV(); 
          // Preparing for exceptions before doing anything else.
        String aString=
          "InfogoraStarter.main() beginning. ======== STARTER IS STARTING ========";
        appLogger.info(aString);
        System.out.println(aString);
    
        //// Shutdowner theShutdowner= new Shutdowner();
        //// theShutdowner.initializeV();
        String [] commandOrArgStrings= new String[] {
            ( // Path of java command in array.
              "jre1.8.0_191\\bin\\java.exe"
              /*   //// "." +
              File.separator +
              "bin" +
              File.separator + 
              "java.exe" */   ////
              ),
            "-cp", // java.exe -cp (classpath) option.
            "Infogora.jar", // Path of .jar file to run
            "allClasses.Infogora" // entry point.
            };
        ///fix? add argStrings at end?
        ProcessStarter.setCommandV(  // Setting String as command to run later.
          commandOrArgStrings
          );
        appLogger.debug("InfogoraStarter.main() starting Infogora process.");
        theProcess= ProcessStarter.startProcess(commandOrArgStrings);
        if (theProcess == null)
          appLogger.error("InfogoraStarter.main() app start failed.");
          else 
          { appLogger.debug(
              "InfogoraStarter.main() waiting for app process termination.");
            try {theProcess.waitFor();} catch (InterruptedException e) {}
            }
        
        System.out.println(
          "InfogoraStarter.main() calling exit(0). ======== STARTER IS ENDING ========");
        appLogger.info("InfogoraStarter.main() calling exit(0). ======== STARTER IS ENDING ========");
        appLogger.setBufferedModeV( false ); // Disabling buffered logging.
        System.exit(0); // Will kill any remaining unknown threads running??
        } // main(..)

  }
