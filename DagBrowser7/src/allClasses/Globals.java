
package allClasses;

public class Globals 
  /* This class is used for convenient access to
    things the app needs to access from many different source files. 

    It is used as the argument for "import static" to reduce
    the need for fully qualified names, for example:
      import static allClasses.Globals.*;  // For appLogger;

    Often methods start here, but are eventually moved to their own classes.

    WARNING: Use of globals (public statics) should be minimized,
    global storage anyway.  Dependency Injection should be used instead.
    When globals are used, they should be for things which
    do not change the state of the program, such as logging.
    */
  {
    public static AppLog appLogger;

	  public static void logAndRethrowAsRuntimeExceptionV( 
	  		String aString, Throwable theThrowable 
	  		)
	    /* Writes an error to the log, then
        Throws a RuntimeException wrapped around theThrowaable.
        This method should never be called, except maybe during debugging.
        */
	    {
		    appLogger.error( "Globals.logAndRethrowAsRuntimeExceptionV(..)"
		        + aString + ":\n  " + theThrowable );
		    throw new RuntimeException( theThrowable );
		    }
  	
  	}
