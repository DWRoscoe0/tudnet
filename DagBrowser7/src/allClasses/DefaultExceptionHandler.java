package allClasses;

import static allClasses.Globals.appLogger;


public class DefaultExceptionHandler {

  public static void setDefaultExceptionHandlerV()
    /* This helper method for the main(..) method
      sets the default handler for uncaught exceptions for all threads.
      The purpose of this is to guarantee that every Exception
      will be handled by this application and
      at least produce a log message.
      The handler sends a message about the exception to
      both the log file and to the console.
      */
    {
      Thread.setDefaultUncaughtExceptionHandler(
        new Thread.UncaughtExceptionHandler() {
          @Override 
          public void uncaughtException(Thread t, Throwable e) {
            String headString= "DefaultExceptionHandler.uncaughtException(..): "
                + "Uncaught Exception in thread "+t.getName();
            System.out.println( headString + e);
            appLogger.exception( headString, e );
            appLogger.closeFileIfOpenB(); // Close log for exit.
            }
          }
        );

      appLogger.info( "setDefaultExceptionHandlerV() run." );
      //throw new NullPointerException(); // Uncomment to test handler.
      }

}
