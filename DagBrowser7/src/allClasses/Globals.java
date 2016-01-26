
package allClasses;

public class Globals 
  /* This class is used for convenient access to
    things the app needs to access from many different source files. 

    It is used as the argument for "import static" to reduce
    the need for fully qualified names, for example:
      import static allClasses.Globals.*;  // For appLogger;

    Often groups of methods start here, but are eventually move
    to their own class.

    WARNING: Globals (public statics) should be used as little as possible,
    global storage anyway.
    Dependency Injection should be used instead.
    When globals are used, they should be for things which
    do not change the state of the program, such as logging.
    */
  {
		//public static Logger appLogger= Logger.getAnonymousLogger();
    //public static Misc appLogger= Misc.getMisc();  // Emulate Logger subset.
    public static AppLog appLogger= AppLog.getAppLog();

	  public static void logAndRethrowAsRuntimeExceptionV( 
	  		String aString, Throwable theThrowable 
	  		)
	    /* Writes an error to the log, then
        Throws a RuntimeException wrapped around theThrowaable.
        */
	    {
		    appLogger.error( aString + ": " + theThrowable );
		    throw new RuntimeException( theThrowable );
		    }
  	
  	}
