package allClasses;

import java.io.File;

import allClasses.javafx.JavaFXGUI;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

/* This file is the root of this application.  
  If you want to understand this application then
  this is where you should start reading.
  Look for 2 things:
  * The main(.) method: This method is the top-level time sequencer of the app.
  * The factory classes: The main(..) method constructs the AppFactory,
  *   uses it to construct the single instance of the App class,
  *   and runs a method in that instance.  
  *   Factories of other types are created as needed.
  *
  * Some of the more important high-level factory classes are:
  *   * AppFactory: This is the top level factory.
  *     It makes all classes with app lifetime.
  *     It wires together the first level of the app.
  *   * AppGUIFactory: This is the factory for all classes with
  *     app GUI lifetime, which is shorter than app lifetime.
  *     It wires together the second level of the app.
  *   * UnicasterFactory: This is the factory for all classes with
  *     lifetimes of a Unicaster network connection.
  *     
  * Factories have the following attributes:
    * Factories contain, or eventually will contain
      all the new-operators, except for new-operators that create 
      * some objects in the top level static Infogora.main(..) method,
      * immutable constant objects, and 
      * initially empty container objects.
      This will, in theory, make unit testing easier.
    * Factory code, and code in the main(..) method,
      shows how objects relate to each other in the app
      by concentrating and documenting all dependencies, 
      usually with constructor injection, 
      but occasionally with setter injection.
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


class Infogora  // The class is the root of this app.

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

			/* This method is the app's entry point.  
			  It does the following actions in the following order:

        * It starts the JavaFX runtime [new]
          so other elements can use it, for example to report errors.
	      * It prepares the AppLog logger for use.
	      * It creates and activates the app's BackupTerminator.
			  * It sets a default Exception handler for unhandled exceptions.
			  * It creates the AppFactory object.
			  * It uses the AppFactory to create the App object.
			  * It calls the App.runV() method.
			  * It may or may not do not some actions as part of app shutdown 
			    if runV() returns, which depends on how shutdown is initiated.  
			    Those optional actions include:
			    * Some additional logging.
			    * ///mys ///klu Triggering the BackupTerminator timer 
			      to force process termination in case
			      termination doesn't happen after this method exits.  
			    * Returning from this method to the JVM.

	      Exiting this method should cause process termination because
	      all remaining threads should be either terminate-able daemon threads,
	      or normal threads which will soon terminate themselves.
	      When all threads terminate, it should trigger a JVM shutdown,
	      unless a JVM shutdown was triggered already, and terminate the app.

	      ///mys ///klu Unfortunately the app doesn't always terminate, 
	      for unknown reasons, so we use BackupTerminator 
	      to force termination if that happens.
        The List of active threads just before 
        forcing termination with exit(.) seems to show that
        the only non-daemon threads are Java JVM or UI related,
        except possibly EventQueue-1 which might be related to
        this app's TracingEventQueueMonitor thread.

        Command line switches are one way used to pass information
        from one app instance to another.  Legal switches are listed below.
        Ignore anything related to InfogoraStarter, 
        an old module that is not presently being used.
        * Legal switches input on the command line from the InfogoraStarter are;
          -starterPort : followed by port number to be used
            to send messages back to the InfogoraStarter process.
          -userDir : see InfogoraStarter.
          -tempDir : see InfogoraStarter.
        * Legal switches output from a parent Infogora app process
          to a new child Infogora app process:
          -otherAppIs : followed by the full path to 
            the parent processes initiator file.
          -starterPort : passed through, see above.
        * Legal switches output by an Infogora app instance which is starting
          to the InstancePort of an already running Infogora app instance:
          -otherAppIs : followed by the full path to 
            the starting apps initiator file.
        * Legal switches output from a descendant Infogora app to 
          the TCP socket -starterPort of an its ancestor InfogoraStarter app:
          -delegatorExiting : indicates that this descendant process which
            delegated its job to its own descendant, is exiting.
            Its descendant might still need the temporary directory to exist. 
          -delegateeExiting : indicates that a descendant process which
            did not delegate its job to its own descendant, is exiting.
            Therefore it should be safe for the ancestor InfogoraStarter app
            to exit, which would cause the temporary directory to be deleted.

			  */

	    { // main(..)
        String javaFXStartAnomalyString= ///ano Save now for reporting later.
            JavaFXGUI.startJavaFXAndReturnString(); // Start JavaFX runtime.
	      theAppLog= new AppLog(new File( // Construct logger.
	          new File(System.getProperty("user.home") ),Config.appString));
	      theAppLog.enableCloseLoggingV( false );
	      DefaultExceptionHandler.setDefaultExceptionHandlerV(); 
	      // ((String)null).charAt(0); // For testing DefaultExceptionHandler.
	      BackupTerminator theBackupTerminator= ///ano 
	          BackupTerminator.makeBackupTerminator(); ///ano

        theAppLog.info(true,
	          "Infogora.main() ======== APP IS STARTING ========");
        if (null != javaFXStartAnomalyString) ///ano
          theAppLog.error(javaFXStartAnomalyString); ///ano
        CommandArgs theCommandArgs= new CommandArgs(argStrings);
        AppSettings.initializeV(Infogora.class, theCommandArgs);
	      AppFactory theAppFactory= new AppFactory(theCommandArgs);
	      App theApp= theAppFactory.getApp();  // Getting App from factory.
	      theApp.runV();  // Running the app until shutdown.
	        // This might not return if a shutdown is initiated by the JVM!

	      // If here then shutdown was initiated in App.runV() and it returned.
        BackupTerminator.logThreadsV(); // Record threads that are still active.
	      theAppLog.info(true,
          "Infogora.main() ======== APP IS ENDING ========"
          + NL + "    by closing log file and exiting the main(..) method.");
        theAppLog.closeFileIfOpenB(); // Close log for exit.
        theBackupTerminator.setTerminationUnderwayV(); ///ano Start exit timer.
        // while(true) ; ///ano Uncomment this line to test BackupTerminator.

	      } // main(..)

		} // Infogora

// End of file.
