package allClasses;

import java.io.File;
import java.util.Set;

import static allClasses.AppLog.LogLevel.WARN;
import static allClasses.AppLog.LogLevel.INFO;
import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

/* This file is the root of this application.  
  If you want to understand this application then
  this is where you should start reading.
  To a large extent it uses the Dependency-Injection software pattern.  

  This file defines the Infogora class. 
  This class contains a main(..) method 
  which is the entry point of the application.
  Other classes also might have main methods, InfogoraStarter for example.

  The main(..) method constructs the AppFactory, 
  uses it to construct the single App instance, 
  and runs a method in that instance.
  
  Much about the structure of this app can be obtained by
  examining the high-level factory classes.  They are:

	* AppFactory: This is the factory for all classes with app lifetime.
	  It wires together the first level of the app.

	* AppGUIFactory: This is the factory for all classes with 
	  app GUI lifetime, which is shorter than app lifetime.  
	  It wires together the second level of the app.

  * UnicasterFactory: This is the factory for all classes with
    lifetimes of a Unicaster network connection.

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

	      * It prepares the AppLog logger for use.
	      * It creates and activates the app BackupTerminator.
			  * It sets a default Exception handler.
			  * It creates the AppFactory object.
			  * It uses the AppFactory to create the App object.
			  * It calls the App object's runV() method.
			  * IT does some progress logging.
    
				See the AppFactory for information about 
				this app's high-level structure.
	      
	      Exiting this method should cause process termination because
	      all remaining threads should be either terminate-able daemon threads,
	      or normal threads which will soon terminate themselves.
	      When all threads terminate, it should trigger a JVM shutdown,
	      unless a JVM shutdown was triggered already, and terminate the app.
	      Unfortunately the app doesn't always terminate, for unknown reasons, 
	      so we use BackupTerminator to handle this possibility.

	      ///fix so exit() in BackupTerminator doesn't need to be called.
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
	      theAppLog= new AppLog(new File( // Constructing logger.
	          new File(System.getProperty("user.home") ),Config.appString));
	      // AppLog should now be able to do logging.
	      theAppLog.enableCloseLoggingV( false );
	      DefaultExceptionHandler.setDefaultExceptionHandlerV(); 
          // Preparing for exceptions before doing anything else.
	      // ((String)null).charAt(0); // Test DefaultExceptionHandler.
	      BackupTerminator theBackupTerminator= BackupTerminator.makeBackupTerminator();

        theAppLog.info(true,
	          "Infogora.main() ======== APP IS STARTING ========");
	      CommandArgs theCommandArgs= new CommandArgs(argStrings);
        AppSettings.initializeV(Infogora.class, theCommandArgs);
	      AppFactory theAppFactory= new AppFactory(theCommandArgs);
	      App theApp= theAppFactory.getApp();  // Getting App from factory.
	      theApp.runV();  // Running the app until shutdown.
	        // This might not return if a shutdown is initiated by the JVM!

        logThreadsV(); // Record threads that are still active.
	      theAppLog.info(true,
          "Infogora.main() ======== APP IS ENDING ========"
          + NL + "    by closing log file and exiting the main(..) method.");
        theAppLog.closeFileIfOpenB(); // Close log for exit.
        theBackupTerminator.setTerminationUnderwayV(); // In case termination fails.
        // while(true) ; // Uncomment this line to test BackupTerminator.
	      } // main(..)

	  
	  private static class BackupTerminator extends Thread
	  
	    /* This class is used to force termination of the app
	      if it doesn't terminate on its own after exiting main(..).

	      Forced termination by this class happened successfully in 2 tests:
	      * I temporarily inserted an infinite loop just before 
	        the end of main(..).
	      * When the standard folder app failed to terminate
	        when starting a new TCPCopierStaging app.

	        I examined the Thread list at the main(..) exit 
	        and BackupTerminator exit.  The following are
	        all the differences and all threads that were Normal (non-daemon).

          ? AWT-EventQueue-1 is WAITING 6 Normal first and later.
          * BackupTerminator is WAITING 5 Daemon first, 
            RUNNABLE 5 Daemon later.
          ? AWT-Shutdown is TIMED_WAITING 5 Normal first and later.
          * main is RUNNABLE 5 Normal first, then missing.
          * DestroyJavaVM is missing first, then RUNNABLE 5 Normal later.
            Apparently this is what waits for other non-daemon threads
            to terminate.
            
          Note the two AWT threads.  Research indicates that
          continuing AWT activity (GUI, events, etc.) might be preventing
          these non-daemon threads terminating.
          All windows have been disposed, so something strange 
          is happening.  ///fix  
	      */
	  
  	  {
  	    
  	    boolean terminationUnderwayB=false; // This flag is used 
  	      // to prevent spurious wake ups.
  
  	    public static BackupTerminator makeBackupTerminator()
  	      // This method makes and returns a ready and running BackupTerminator.
    	    {
    	      BackupTerminator theBackupTerminator= new BackupTerminator();
            theBackupTerminator.setName("BackupTerminator");
            theBackupTerminator.setDaemon(true); // Don't prevent termination ourselves.
            theBackupTerminator.start(); // Start its thread.
    	      return theBackupTerminator;
    	      }
  	    
  	    public synchronized void run() 
    	    {
    	      
            while (true) { // Wait for signal that termination is underway.
              if (terminationUnderwayB) break; // Done waiting.
              waitV(0);
              } // while(true)
            
            int secondsI= 5;
            theAppLog.logB( INFO, true, null,
                "run() Starting "+secondsI+"-second backup exit timer");
            while (secondsI-- > 0) { // Count down, time and, display progress.
              waitV(1000); // Waiting 1 second.
              System.out.print(".");
    	        }
    	      
    	      // If we got this far, time has expired, 
            // and termination probably failed.
    	      
            synchronized(theAppLog) { // Log the following block as an indivisible block.
              theAppLog.logB( WARN, true, null,
                  "run() ======== FORCING LATE APP TERMINATION ========");
              theAppLog.doStackTraceV(null);
              logThreadsV(); // Record threads that are still active.
              theAppLog.debug("run() closing log file and executing System.exit(1)." );
              theAppLog.closeFileIfOpenB(); // Close log before exit.
              }
            System.exit(1); // Force termination with an error code of 1.
    	      }
    
  	    private void waitV(long msI)
  	      /* This helper method waits and handles any InterruptedException. 
  	        Waits for msI milliseconds, or an interrupt, or a notification,
  	        which ever happens first.
  	        */
  	      {
            try {
              wait(msI);
              }
            catch (InterruptedException e) {
              theAppLog.debug("BackupTerminator.waitV() wait(..), interrupted."); 
              }
  	      
  	      }
  	    
          public synchronized void setTerminationUnderwayV() 
            /* This method signals the thread that termination should
              be imminent.  See run() for what happens next.
               */
          {
            terminationUnderwayB= true;
            notify(); // Unblock the thread.
            }
            
  	    }

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
        synchronized (theAppLog) { // Output thread list as single log entry.
          theAppLog.info("Infogora.logThreadsV(), remaining active threads:"); 
          Set<Thread> threadSet= Thread.getAllStackTraces().keySet();
          for (Thread t : threadSet) {
              Thread.State threadState= t.getState();
              int priorityI= t.getPriority();
              String typeString= t.isDaemon() ? "Daemon" : "Normal";
              String nameString= t.getName();
              theAppLog.getPrintWriter().printf(NL+ "    %-13s %2d  %s  %-25s  ", 
                  threadState, priorityI, typeString, nameString);
              }
          }
        }

		} // Infogora

// End of file.
