package allClasses;

import static allClasses.AppLog.theAppLog;


public class DefaultExceptionHandler {

  /* This class contains a method, called by the app's main(..) method,
    which sets the default handler for uncaught exceptions.
    The purpose of this is to guarantee that every Exception
    will be handled by this application and create a record of it.

    The handler attempts to do the following:
    * It constructs message Strings which describe the exception that occurred.  
      The message Strings include:
      * The name of the exception handler method.
      * The fact that an uncaught exception occurred.
      * The name of the thread in which the exception occurred.
      * The name and description of the exception that occurred.
    * It outputs this information to the following places:
      * The Console
      * The application log file.
      * An interactive dialog window if the JavaFX runtime is active.
        If it's not active then an additional exception will be logged. 

    The handler set by this method is an anonymous subclass of
    the Thread.UncaughtExceptionHandler class.

    Notes:

    * This method sets the default uncaught exception handler
      for the widest possible scope, the application scope.
      These handlers can be set for smaller scopes also.  
      The available scopes, in order of decreasing size, are:
      * application (handler can be set)
      * class ThreadGroup (handler can be overridden by a ThreadGroup subclass)
      * class Thread (handler can be set)
      The scope used to handle an uncaught exception 
      at a particular code location is the smallest scope 
      for which a handler is defined.
      For more information, see the documentation for 
      the Thread.UncaughtExceptionHandler interface at
        https://docs.oracle.com/javase/7/docs/api/java/lang/Thread.UncaughtExceptionHandler.html
      The lead for this note came from
        https://www.bugsnag.com/blog/error-handling-on-android-part-1

    * An uncaught exception handler setting can be changed,
      or removed by setting a value of null, at any time.
      This can happen repeatedly during the life of an application.

    * ///que : Default exception handlers are not called in Eclipse debug mode.
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
            System.out.println( headString + e); // Output to console.
            theAppLog.exception( headString, e ); // Output to log file,
              // and an interactive dialog if the JavaFX runtime is active.
            theAppLog.closeFileIfOpenB(); // Close log in case app will exit.
            }

          }
        );

      theAppLog.info( "setDefaultExceptionHandlerV() run." );

      // throw new NullPointerException(); // Uncomment to test handler.
      // ((Object)null).notify(); // A another way to test handler.
      }

}
