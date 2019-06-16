package allClasses;

import static allClasses.Globals.appLogger;  // For appLogger;

import java.io.File;
import java.util.Set;


/* This file is the root of this application.  
  If you want to understand this application then
  this is where you should start reading.
  It by using, to a large extent, by using 
  the Dependency-Injection software pattern.  
 
  This file defines the Infogora class: 
  This class contains a main(..) method
  which is the entry point of the application.
  Other classes might have main methods, InfogoraStarter for example.
  
  The main(..) method constructs the AppFactory, 
  uses it to construct the single App instance, 
  and runs a method in that instance.
  
  Much about the structure of this can be obtained by
  examining the high-level factory classes.  They are:

	* AppFactory: This is the factory for all classes with app lifetime.
	  It wires together the first level of the app.

	* AppGUIFactory: This is the factory for all classes with 
	  app GUI lifetime, which is shorter than app lifetime.  
	  It wires together the second level of the app.

  * UnicasterFactory: This is the factory for all classes with
    lifetimes of a connection.

  The factories above may not be the only factories in the app,
  but they are the top levels.  Factories have the following attributes:

  * Factories contain, or eventually will contain
    all the new-operators, except for new-operators that create 
    * objects in the top level static Infogora.main(..) method,
    * immutable constant objects, and 
    * initially empty container objects.
    This will, in theory, make unit testing easier.

  * Factory code, and code in the main(..) method,
    shows how objects relate to each other in the app
    by concentrating and documenting all dependencies, 
    usually with constructor injection, but occasionally with setter injection.
    This will, in theory, make code easier to understand.

  * All factory fields are one of the following.
    * Non-lazy final variable singleton fields, preferably private.
    * The factory's one constructor, which creates all the non-lazy singletons.
    * Lazy singleton getter methods, each of which 
      constructs its singleton only when first needed.
      There should be no non-lazy getters, except one at the top level,
      or the helper classes for each factory scope,
      because non-lazy singletons should be constructor-injected. 
    * Maker methods, which construct a new object each time called.
    
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
	        List all remaining active threads seems to show that
	        the only non-daemon threads are Java or UI related,
	        except possibly EventQueue-1 which might be or be related to
	        the TracingEventQueueMonitor thread.

        Legal switches input on the command line from the InfogoraStarter are;
        -starterPort : followed by port number to be used
          to send messages back to the InfogoraStarter process.
        -userDir : see InfogoraStarter.
        -tempDir : see InfogoraStarter.

        Legal switches output from a parent Infogora app process
        to a new child Infogora app process:
        -otherAppIs : followed by the full path to 
          the parent processes initiator file.
        -starterPort : passed through, see above.

        Legal switches output by an Infogora app instance which is starting
        to the InstancePort of an already running Infogora app instance:
        -otherAppIs : followed by the full path to 
          the starting apps initiator file.

        Legal switches output from an descendant Infogora app to 
        the TCP socket -starterPort of an its ancestor InfogoraStarter app:
        -delegatorExiting : indicates that this descendant process which
          delegated its job to its own descendant, is exiting.
          Its descendant might still need the temporary directory to exist. 
        -delegateeExiting : indicates that a descendant process which
          did not delegate its job to its own descendant, is exiting.
          Therefore it chould be safe for the ancestor InfogoraStarter app
          to exit, which would cause the temporary directory to be deleted.

			  */
      { // main(..)
	      appLogger= new AppLog(new File( // Constructing logger.
	          new File(System.getProperty("user.home") ),Config.appString));
	      // AppLog should now be able to do logging.
        /// appLogger.getAndEnableConsoleModeB(); ///dbg
	      /// Config.clearLogFileB= true; ///dbg
	      DefaultExceptionHandler.setDefaultExceptionHandlerV(); 
          // Preparing for exceptions before doing anything else.

        appLogger.info(true,
	          "Infogora.main() beginning. ======== APP IS STARTING ========");
	      CommandArgs theCommandArgs= new CommandArgs(argStrings);
        AppSettings.initializeV(Infogora.class, theCommandArgs);
	      AppFactory theAppFactory= new AppFactory(theCommandArgs);
	      App theApp= theAppFactory.getApp();  // Getting App from factory.
	      theApp.runV();  // Running the app until shutdown.
	        // This might not return if a shutdown is initiated by the JVM. 

        logThreadsV(); // Record threads that are still active.
	      appLogger.info(true,
          "Infogora.main() ======== APP IS ENDING ========"
          + "\n    by closing log file and exiting the main(..) method.");
        appLogger.closeFileIfOpenB(); // Close log for exit.
	      //// System.exit(0) is no longer needed.
	      } // main(..)

    private static void logThreadsV()
      /* Logs active threads, of which there should be very few,
        because when this method is called,
        all non-daemon app threads should have been terminated,
        and all active windows should have been dispose()-ed.             
        
        This method was based on code from a web article.
        
        Although the output from this method might contain 
        some still active Normal threads other than "main", for example 
          TIMED_WAITING  5  Normal  AWT-Shutdown
          WAITING        6  Normal  AWT-EventQueue-1
        their termination in the near future should be quite certain. 
        
        ///fix Prevent log entry fragmentation because it presently uses
          multiple calls to appLogger.
        */
      {
        synchronized (appLogger) { // Output thread list as single log entry.
          appLogger.info("Infogora.logThreadsV(), remaining active threads:"); 
          Set<Thread> threadSet= Thread.getAllStackTraces().keySet();
          for (Thread t : threadSet) {
              Thread.State threadState= t.getState();
              int priorityI= t.getPriority();
              String typeString= t.isDaemon() ? "Daemon" : "Normal";
              String nameString= t.getName();
              appLogger.getPrintWriter().printf("    %-13s %2d  %s  %-25s  \n", 
                  threadState, priorityI, typeString, nameString);
              }
          }
        }

		} // Infogora

// End of file.
