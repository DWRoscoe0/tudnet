package allClasses;

import java.io.File;

import allClasses.javafx.JavaFXGUI;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

/* This file is the root of this application.  
  If you want to understand how this application works,
  then you should start reading here.  That reading should include:
  * The main(.) method in this file.
  * The factory classes, starting with the AppFactory constructed by main(.).
  * Anomalies, which are explained in more detail 
    by documentation in file Anomalies.java.  ///ano  

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
    It makes all classes with app [OuterApp] lifetime.
    It wires together the top level of the app.
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
    
  ///ano The main(.) method deals with 2 anomalies.
  For more information, see the main(.) method in this file
  and the file Anomalies.java.

  */


class TUDNet

  { // TUDNet

    public static void main(String[] argStrings) // This is app's entry point.

      /* This method is the app's entry point.  
        It does the following actions in the following order:

        * It prepares the AppLog logger for use first, 
          because many modules use it to log significant events.
        * It constructs and initializes the Persistent data module
          so modules can access app settings.
        * ///ano It starts the JavaFX runtime next, so other modules can 
          use its GUI to report early anomalies to the user.
        * ///ano It creates and activates the app's BackupTerminator in case
          normal termination fails at shutdown.
        * It sets a default Exception handler for unhandled exceptions.
        * 
        * It constructs the AppFactory object.
        * It uses the AppFactory object to construct the App object.
        * It calls the App.runV() method,
          which does most of the work of this app.
          This method might or might not return, depending on
          how shutdown is done, but usually it returns.
        * If App.runV() does return, then main(.) does these additional actions:
          * It does some final logging about app termination.
          * It finalizes the Persistent data module, 
            which saves changed data to non-volatile storage.
          * ///ano It triggers the BackupTerminator timer to force termination 
            in case normal termination doesn't happen after main(.) exits.  
            See BackupTerminator for details.
          * It returns to the JVM, which should then itself shut down,
            terminating the entire app and the process containing it.

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
        See BackupTerminator for details about this.

        Command line switches in method parameter argStrings
        are used to pass information from one app instance to another.  
        Legal switches are listed below.
        Ignore anything related to TUDNetStarter, 
        an old module that is not presently being used.
        * Legal switches input on the command line from the TUDNetStarter are;
          -starterPort : followed by port number to be used
            to send messages back to the TUDNetStarter process.
          -userDir : see TUDNetStarter.
          -tempDir : see TUDNetStarter.
        * Legal switches output from a parent TUDNet app process
          to a new child TUDNet app process:
          -otherAppIs : followed by the full path to 
            the parent processes initiator file.
          -starterPort : passed through, see above.
        * Legal switches output by an TUDNet app instance 
          which is starting to the InstancePort of an already running 
          TUDNet app instance:
          -otherAppIs : followed by the full path to 
            the starting apps initiator file.
        * Legal switches output from a descendant TUDNet app to 
          the TCP socket -starterPort of an its ancestor TUDNetStarter app:
          -delegatorExiting : indicates that this descendant process which
            delegated its job to its own descendant, is exiting.
            Its descendant might still need the temporary directory to exist. 
          -delegateeExiting : indicates that a descendant process which
            did not delegate its job to its own descendant, is exiting.
            Therefore it should be safe for the ancestor TUDNetStarter app
            to exit, which would cause the temporary directory to be deleted.

        */

      { // Beginning of the body of main(.).

        theAppLog= new AppLog( // Prepare logger.
          new File(new File(System.getProperty("user.home")),Config.appString));
        DefaultExceptionHandler.setDefaultExceptionHandlerV();
          // ((String)null).charAt(0); // Test above with NullPointerException.
        Persistent thePersistent= (new Persistent()).initializePersistent();
        theAppLog.setPersistentV(thePersistent); // Add for conditional logging.
        String javaFXStartAnomalyString= ///ano Save to later report result of
          JavaFXGUI.startRuntimeAndReturnString(); // start of JavaFX runtime.
        BackupTerminator theBackupTerminator= ///ano 
            BackupTerminator.makeBackupTerminator(); ///ano

        theAppLog.info(true,"TUDNet.main() ======= APP IS STARTING =======");

        if (null != javaFXStartAnomalyString) ///ano Report any problem with
          //// theAppLog.error(javaFXStartAnomalyString); ///ano runtime startup.
          theAppLog.error( ///ano runtime startup.
              "JavaFX startup anomaly",javaFXStartAnomalyString);

        CommandArgs theCommandArgs= new CommandArgs(argStrings);
        AppSettings.initializeV(TUDNet.class, theCommandArgs);
        AppFactory theAppFactory= new AppFactory(theCommandArgs, thePersistent);
        OuterApp theOuterApp= theAppFactory.getOuterApp();
        theOuterApp.runV();  // Run the app until shutdown.
          // This might not return if a shutdown is initiated by the JVM!

        // If here then shutdown was requested in App.runV() and it returned.
        thePersistent.finalizeV();  // Write any new or changed app properties.
        BackupTerminator.logThreadsV(); ///ano Record threads active now.
        theAppLog.info(true,"TUDNet.main() ======== APP IS ENDING ========"
          + NL + "    by closing log file and exiting the main(.) method.");
        theAppLog.closeFileIfOpenB(); // Close log file for exit.
        theBackupTerminator.setTerminationUnderwayV(); ///ano Start exit timer.
          // while(true) {} ///ano Use this infinite loop to test above line.

        } // End of the body of main(.).

    } // /TUDNet

// End of file.
