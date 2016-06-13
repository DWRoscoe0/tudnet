package allClasses;

import java.io.PrintWriter;
import java.io.StringWriter;

import static allClasses.Globals.appLogger;  // For appLogger;


/* This file is the root of this application.  
  If you want to understand this application then
  this is where you should start reading.  
 
  This file defines the Infogora class: 
  This is the class that contains the main(..) method
  which is the entry point of the application.
  It constructs the AppFactory, uses it to construct the App,
  and runs that.
  
  Much about the structure of this can be obtained by
  examining the high-level factory classes.  They are:

	* AppFactory: This is the factory for all classes with app lifetime.
	  It wires together the first level of the app.

	* AppGUIFactory: This is the factory for all classes with 
	  app GUI lifetime.  It wires together the second level of the app.

  * UnicasterFactory: This is the factory for all classes with
    lifetimes of a connection.

  The factories above may not be the only factories in the app,
  but they are the top levels.  Factories serve 2 purposes:

  * They contain, or eventually will contain,  all the new-operators, 
    except for 2 uses in the top level Infogora class.
    This will, in theory, make unit testing easier.

  * Their code shows how classes relate to each other in the app
    by showing all dependency injections, usually with
    constructor injection, but occasionally with setter injection.

  * Factory fields are:
    * Singleton variable fields, preferably private.
    * One constructor, which creates all the non-lazy singletons.
    * Lazy singleton getters methods, 
      whose constructions are delayed until needed.
    * Maker methods, which construct new objects each time called.
    ! There should be no non-lazy getters, except for the top level,
      or the helper classes for each scope,
      because non-lazy singletons should be constructor-injected. 
    
  */


class Infogora  // The root of this app.

	/* This class contains 2 methods:

	  * The main(..) method, which is the app's entry point.

	  * setDefaultExceptionHandlerV(), which sets the app's 
	    default handler for uncaught exceptions.

		Both these methods use the new-operator.
		Except for factories, these should [eventually] be 
		the only places where the new-operator is used.
		This can make unit testing much easier.
	  */

	{ // Infogora

	  private static void setDefaultExceptionHandlerV()
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

              appLogger.error( "Uncaught Exception: "+e );

              StringWriter aStringWriter= new StringWriter();
              PrintWriter aPrintWriter= new PrintWriter(aStringWriter);
              e.printStackTrace(aPrintWriter);
              appLogger.info( "Stack trace: "+aStringWriter.toString() );
              }
            }
          );

        //throw new NullPointerException(); // Uncomment to test handler.
        }

	  public static void main(String[] argStrings)
			/* This method is the app's entry point.  It does the following:
	
			  * It sets a default Exception handler.
			  * It creates the AppFactory object.
			  * It uses the AppFactory to create the App object.
			  * It calls the App object's runV() method.
	
				See the AppFactory for information about 
				this app's high-level structure.
			  */
      { // main(..)
	      appLogger.info("Infogora.main() beginning.");

	      setDefaultExceptionHandlerV(); // Preparing for exceptions 
	        // before doing anything else.
	
	      AppFactory theAppFactory=  // Constructing AppFactory.
	        new AppFactory(argStrings);
	      App theApp=  // Getting the App from the factory.
      		theAppFactory.getApp();
	      theApp.runV();  // Running the app until it finishes.
	      
	      // At this point, this thread should be the only non-daemon running.
	      // When it ends, it should trigger a JVM shutdown,
	      // unless a JVM shutdown was triggered already, and terminate the app.
	      // Unfortunately the app doesn't terminate, so we call exit(0).

	      appLogger.info("Infogora.main() calling exit(0).");
	      System.exit(0); // Will kill any remaining unknown threads running??
	      } // main(..)
	
		} // Infogora

// End of file.
