package allClasses;

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

  * Factories contain, or eventually will contain,  all the new-operators, 
    except for 2 or 3 uses in the top level Infogora class,
    immutable constants, and initially empty containers.
    This will, in theory, make unit testing easier.

  * Factory code shows how objects relate to each other in the app
    by showing all dependencies, usually with constructor injection, 
    but occasionally with setter injection.

  * All factory fields are one of the following.
    * Singleton variable fields, preferably private.
    * The factory's one constructor, which creates all the non-lazy singletons.
    * Lazy singleton getter methods, 
      whose constructions are delayed until needed.
    * Maker methods, which construct a new object each time called.
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
		Except for factories, immutable constants, 
		and initially empty containers,
		these should [eventually] be the only places 
		where the new-operator is used.
		This can make unit testing much easier.
	  */

	{ // Infogora

	  public static void main(String[] argStrings)
			/* This method is the app's entry point.  It does the following:
	
			  * It sets a default Exception handler.
			  * It creates the AppFactory object.
			  * It uses the AppFactory to create the App object.
			  * It calls the App object's runV() method.
	
				See the AppFactory for information about 
				this app's high-level structure.
	      
	      At the end of this method, 
	      this thread should be the only non-daemon running.
	      When it returns, it should trigger a JVM shutdown,
	      unless a JVM shutdown was triggered already, and terminate the app.
	      Unfortunately the app doesn't terminate, 
	      so we call exit(0) to terminate.
	      ///fix so exit() doesn't need to be called.
	        As first step, list all remaining active ,,threads.

			  */
      { // main(..)
	      //// appLogger.getAndEnableConsoleModeB(); //// For debugging.
        appLogger.setBufferedModeV(false); //// Disabling for debugging.
        appLogger.setBufferedModeV(true); // Enabling fast buffered logging.
        DefaultExceptionHandler.setDefaultExceptionHandlerV(); 
          // Preparing for exceptions before doing anything else.
	      appLogger.info("Infogora.main() beginning. ======== APP IS STARTING ========");
	      System.out.println(
	        "Infogora.main() beginning. ======== APP IS STARTING ========");
	
        CommandArgs theCommandArgs= new CommandArgs(argStrings);
	      AppFactory theAppFactory=  // Constructing AppFactory.
	        new AppFactory(theCommandArgs);
	      App theApp=  // Getting the App from the factory.
      		theAppFactory.getApp();
	      theApp.runV();  // Running the app until it has shutdown.
	        // This might not return if shutdown began in the JVM. 

        System.out.println(
          "Infogora.main() calling exit(0). ======== APP IS ENDING ========");
        appLogger.info("Infogora.main() calling exit(0). ======== APP IS ENDING ========");
    		appLogger.setBufferedModeV( false ); // Final close of log file.
	      System.exit(0); // Will kill any remaining unknown threads running??
	      } // main(..)
	
		} // Infogora

// End of file.
