package allClasses;

import java.io.File;

import allClasses.javafx.JavaFXGUI;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

/* This file is the root of this application.  
  If you want to understand how this application works,
  then you should start reading here.  That reading should include:
  * The main(.) method in this class.
  * The factory classes, starting with the AppFactory constructed by main(.).
  * Anomalies, which are explained in more detail 
    in by documentation in the Anomalies class.  ///ano  

  The main(.) method in this class is the entry point of this app.
  It acts as the top-level time sequencer of the app.

  One of the things main(.) does is construct a single AppFactory object,
  use it to construct a single App class object,
  and run the runV(.) method in that object.
  Factories of other types are created as needed.

  The object factory classes are useful for understanding app structure.
  They construct most of the objects in the app and link them together.
  Factory classes are distinguished by the lifetimes of the objects they create. 
  Some important factory classes are:
  * AppFactory: This is the top level factory.
    It makes all classes with app lifetime.
    It wires together the first level of the app.
  * AppGUIFactory: This is the factory for all classes with
    app GUI lifetime, which is shorter than app lifetime.
    It wires together the second level of the app.
    [ Actually, since the addition of the JavaFX GUI,
    whose runtime is started immediately,
    this class should probably be called AppSwingGUIFactory. ]
  * UnicasterFactory: This is the factory for all classes with
    lifetimes of a Unicaster network connection.

  Object factories have the following noteworthy characteristics:
  * Factories contain most of the new-operators in the app.  Exceptions are:
    * a small number of objects constructed by main(.),
    * immutable constant objects, and 
    * initially empty container objects.
    Doing this, in theory, makes future unit testing easier.
  * Factory code, because it constructs most of the service objects,
    is good for documenting module dependencies and app structure. 
  * All factory class fields are one of the following.
    * Non-lazy final variable singleton fields, usually private,
      some of which are dependency injections, 
      and some of which are computed internally.
    * The factory's one constructor, which creates all the non-lazy singletons.
    * Lazy singleton getter methods, each of which 
      constructs its singleton only when first needed.
      There should be no non-lazy getters, except one at the top level,
      or the helper classes for each factory scope,
      because non-lazy singletons should be constructor-injected. 
    * Maker methods, which construct a new object each time called.
    
  The main(.) method deals with 2 anomalies.
  For more information about anomalies, see the Anomalies class.

  */


class Infogora  // The class is the root of this app.

	{ // Infogora

	  public static void main(String[] argStrings)

			/* This method is the app's entry point.  
			  It does the following actions in the following order:

        * It starts the JavaFX runtime [new]
          so other elements can use it, for example to report errors.
	      * It prepares the AppLog logger for use.
	      * ///ano It creates and activates the app's BackupTerminator for
	        possible use during shutdown.
			  * It sets a default Exception handler for unhandled exceptions.
			  * It constructs the AppFactory object.
			  * It uses the AppFactory object to construct the App object.
			  * It calls the App.runV() method,
			    which does most of the work of this app.
			    This method might or might not return, depending on
			    how shutdown is done.  Usually it returns.
			  * If App.runV() returns then main(.) does these additional actions:
			    * It does some additional logging.
			    * ///ano It triggers the BackupTerminator timer to force termination 
			      in case normal termination doesn't happen.  
			      See BackupTerminator for details.
			    * It returns to the JVM, which should itself shut down,
			      and terminate the entire app.

	      Exiting this method should cause process termination because
	      all remaining threads should be either daemon threads
	      which do not need to be terminated,
	      or normal threads which should be in the process of terminating.
	      When all normal threads have terminated, 
	      it should trigger a JVM shutdown, unless that happened earlier.
	      When the JVM shuts down, the app's process terminates,
	      and app termination is complete.
	      
	      ///ano Unfortunately, for unknown reasons, 
	      this app doesn't always terminate as it should. 
	      Termination is forced in that case.
	      See BackupTerminator for details of this.

        Command line switches in method parameter argStrings
        are used to pass information from one app instance to another.  
        Legal switches are listed below.
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

	    { // main(.)

        theAppLog= // Construct logger immediately because everything uses it.
            new AppLog(new File(new File(
                System.getProperty("user.home") ),Config.appString));
	      theAppLog.enableCloseLoggingV( false ); // Close it when not in use.

        String javaFXStartAnomalyString= ///ano Save result for reporting later.
            JavaFXGUI.startRuntimeAndReturnString(); // Start JavaFX runtime.

	      DefaultExceptionHandler.setDefaultExceptionHandlerV(); 
	      // ((String)null).charAt(0); // This tests with a NullPointerException.

	      BackupTerminator theBackupTerminator= ///ano 
	          BackupTerminator.makeBackupTerminator(); ///ano

        theAppLog.info(true,
	          "Infogora.main() ======== APP IS STARTING ========");

        if (null != javaFXStartAnomalyString) ///ano Report any JavaFX anomaly.
          //// theAppLog.error(javaFXStartAnomalyString); ///ano
          Anomalies.displayDialogV(javaFXStartAnomalyString); ///ano

        CommandArgs theCommandArgs= new CommandArgs(argStrings);
        AppSettings.initializeV(Infogora.class, theCommandArgs);
	      AppFactory theAppFactory= new AppFactory(theCommandArgs);
	      App theApp= theAppFactory.getApp();  // Getting App from factory.
	      theApp.runV();  // Running the app until shutdown.
	        // This might not return if a shutdown is initiated by the JVM!

	      // If here then shutdown was initiated in App.runV() and it returned.
        BackupTerminator.logThreadsV(); // Record threads that are active now.
	      theAppLog.info(true,
          "Infogora.main() ======== APP IS ENDING ========"
          + NL + "    by closing log file and exiting the main(.) method.");
        theAppLog.closeFileIfOpenB(); // Close log for exit.

        theBackupTerminator.setTerminationUnderwayV(); ///ano Start exit timer.
        // while(true) {} ///ano Uncomment this line to test BackupTerminator.

	      } // main(.)

		} // Infogora

// End of file.
