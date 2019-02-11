package allClasses;

import static allClasses.Globals.appLogger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class DefaultExceptionHandler {

  public static void setDefaultExceptionHandlerV()
    /* This helper method for the main(..) method
      sets the default handler for uncaught exceptions.
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
            System.out.println( "Uncaught Exception, "+t.getName()+", "+e);

            appLogger.setBufferedModeV( false ); // Disabling buffering.
            appLogger.error( "Uncaught Exception: "+e );
            StringWriter aStringWriter= new StringWriter();
            PrintWriter aPrintWriter= new PrintWriter(aStringWriter);
            e.printStackTrace(aPrintWriter);
            appLogger.info( "Stack trace: "+aStringWriter.toString() );
            }
          }
        );

      appLogger.info( "setDefaultExceptionHandlerV() done." );
      //throw new NullPointerException(); // Uncomment to test handler.
      }

}
