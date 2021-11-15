package allClasses;

import static allClasses.AppLog.theAppLog;


public class DefaultExceptionHandler {

  /* This class contains a method, called by the apps main(..) method,
    which sets the default handler for uncaught exceptions.
    The purpose of this is to guarantee that every Exception
    will be handled by this application and at least produce a log message.

    The handler set by this method is an anonymous subclass of
    the Thread.UncaughtExceptionHandler class.
    The handler outputs information about the exception to
    both the log file and to the console.

    Notes:

    * This method sets the default uncaught exception handler
      for the widest possible scope, the application scope.
      These handlers can be set for smaller scopes also.  
      The available scopes, in order of increasing size, are:
      * class Thread (handler can be set)
      * class ThreadGroup (handler can be overridden by a ThreadGroup subclass)
      * application (handler can be set)
      The scope used to handle an uncaught exception 
      at a particular code location is the smallest scope 
      for which a handler is defined.
      For more information, see documentation for 
      the Thread.UncaughtExceptionHandler interface at
        https://docs.oracle.com/javase/7/docs/api/java/lang/Thread.UncaughtExceptionHandler.html
      The lead for this note came from
        https://www.bugsnag.com/blog/error-handling-on-android-part-1

    * Uncaught exception handler settings can be changed,
      or removed by setting a value of null, at any time.
      This can happen repeatedly during the life of an application.

    * ///que : Default exception handler is not called in Eclipse debug mode.
      Under Eclipse, the set exception handler is called when expected
      when Eclipse is in regular mode, but not when Eclipse is in debug mode, 
      though I have no exception breakpoints set.  Why is this?
      * ///enh Add a default exception handler tester to SystemsMonitor?
        It could be done with a Listener?

    * ///enh ? Restart some Threads that throw uncaught exceptions.
      It's possible for a default exception handler 
      to restart its thread, or to start an identical replacement thread.  
      This might be worth trying if strange exceptions cause threads to crash.

    */

  public static void setDefaultExceptionHandlerV()
    {
      Thread.setDefaultUncaughtExceptionHandler(
        new Thread.UncaughtExceptionHandler() {

          @Override 
          public void uncaughtException(Thread t, Throwable e) {
            String headString= "DefaultExceptionHandler.uncaughtException(..): "
                + "Uncaught Exception in thread "+t.getName();
            System.out.println( headString + e);
            theAppLog.exception( headString, e );
            theAppLog.closeFileIfOpenB(); // Close log for exit.
            }

          }
        );

      theAppLog.info( "setDefaultExceptionHandlerV() run." );
      //throw new NullPointerException(); // Uncomment to test handler.
      }

}
