package allClasses;

import static allClasses.Globals.appLogger;

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
      String aString=
        "InfogoraStarter.main() beginning. ======== STARTER IS STARTING ========";
      appLogger.info(aString);
      System.out.println(aString);

      //// setDefaultExceptionHandlerV(); // Preparing for exceptions 
        // before doing anything else.
  
      //// CommandArgs theCommandArgs= new CommandArgs(argStrings);
  
      System.out.println(
        "Infogora.main() calling exit(0). ======== STARTER IS ENDING ========");
      appLogger.info("Infogora.main() calling exit(0). ======== STARTER IS ENDING ========");
      appLogger.setBufferedModeV( false ); // Disabling buffered logging.
      System.exit(0); // Will kill any remaining unknown threads running??
      } // main(..)

  }
