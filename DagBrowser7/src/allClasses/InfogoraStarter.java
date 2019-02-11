package allClasses;

import static allClasses.Globals.appLogger;

import java.io.File;

public class InfogoraStarter 

{

  public static void main(String[] argStrings)
    /* This method is the app starter's entry point.  It does the following:
  
      * It sets a default Exception handler.
  
      */
    { // main(..)
      appLogger.setIDProcessV("Starter");
      appLogger.setBufferedModeV( true ); // Enabling buffered logging.
      ////appLogger.setBufferedModeV( false ); // Slower disabled buffered logging.
      DefaultExceptionHandler.setDefaultExceptionHandlerV(); 
        // Preparing for exceptions before doing anything else.
      String aString=
        "InfogoraStarter.main() beginning. ======== STARTER IS STARTING ========";
      appLogger.info(aString);
      System.out.println(aString);
  
      Shutdowner theShutdowner= new Shutdowner();
      theShutdowner.initializeV();
      String [] commandOrArgStrings= new String[] {
          ( // Path of java command in array.
            "." +
            File.separator +
            "bin" +
            File.separator + 
            "java.exe"
            ),
          "-cp", // java.exe -cp (classpath) option.
          "Infogora.jar", // Path of .jar file to run
          "allClasses.InfogoraStarter" // entry point.
          };
      ///fix? add argStrings at end?
      theShutdowner.setCommandV(  // Setting String as command to run later.
        commandOrArgStrings
        );
      appLogger.debug("InfogoraStarter.main() starting Infogora process.");
      Process theProcess= theShutdowner.startProcess(commandOrArgStrings);
      if (theProcess == null)
        appLogger.error("InfogoraStarter.main() start failed.");
        else 
        { appLogger.debug("InfogoraStarter.main() waiting for process termination.");
          try {theProcess.waitFor();} catch (InterruptedException e) {}
          }
      
      System.out.println(
        "InfogoraStarter.main() calling exit(0). ======== STARTER IS ENDING ========");
      appLogger.info("InfogoraStarter.main() calling exit(0). ======== STARTER IS ENDING ========");
      appLogger.setBufferedModeV( false ); // Disabling buffered logging.
      System.exit(0); // Will kill any remaining unknown threads running??
      } // main(..)

  }
