package allClasses;

import static allClasses.AppLog.theAppLog;


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
  
    // Platform-dependent new-line code.
  
    public static final String NL= // Defines the new-line String.
        System.getProperty("line.separator");
    
    public static boolean NLTestB(int CI) 
      /* This method returns true if CI is 
        any of the characters in the new-line String, false otherwise.
       */ 
       { return (NL.indexOf(CI)>=0) ; }
  
    
    // Exception handling and logging.
    
	  public static void logAndRethrowAsRuntimeExceptionV( 
	  		String aString, Throwable theThrowable 
	  		)
	    /* Writes an error to the log, then
        Throws a RuntimeException wrapped around theThrowaable.
        This method should never be called, except maybe during debugging.
        */
	    {
		    theAppLog.exception( "Globals.logAndRethrowAsRuntimeExceptionV(..)"
		        + aString + ":" + NL + "  ", theThrowable );
		    throw new RuntimeException( theThrowable );
		    }
  	
  	}
