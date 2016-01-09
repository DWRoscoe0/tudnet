package allClasses;


public class Globals 
  /* This class is used for convenient access to
    things the app needs to access from many different source files. 

    It is used as the argument for "import static" to reduce
    the need for fully qualified names, for example:
      import static allClasses.Globals.*;  // For appLogger;

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

    public static <T> T fastFailNullCheckT( T testT )
      // Throws a NullPointerException if testT == null, 
      // Otherwise returns testT.
	    {
			  if ( testT == null ) // Doing fast-fail construction check.
				  {
			  		appLogger.error("nullCheckV():");
				  	throw new NullPointerException();
				  	}
			  return testT;
	      }

    public static void logAndRethrowAsRuntimeExceptionV( 
    		String aString, Throwable aThrowable 
    		)
	    /* Writes an error to the log, then
        Throws a RuntimeException wrapped around aThrowaable.
        */
	    {
		    appLogger.error( aString + ": " + aThrowable );
		    throw new RuntimeException( aThrowable );
		    }

    }
