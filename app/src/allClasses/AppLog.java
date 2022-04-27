package allClasses;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;

import allClasses.LockAndSignal.Input;
import allClasses.epinode.MapEpiNode;

import static allClasses.AppLog.LogLevel.*;
import static allClasses.SystemSettings.NL;

public class AppLog 

  extends Thread

  {

    /* The purpose of this class to log information from application programs.

      A logger is one of the most important components of an application.
      It is an almost indispensable tool for debugging.  
      Because of this, it tends to be one of the first components of an app 
      to be constructed and initialized, and 
      one of the last components to be finalized and destroyed. 

      This logger was designed to provide the following features:
      * Logs strings provided by the application.
      * It is thread-safe so any app thread may log.
      * Log entries by all threads are appended to the same file.
      * The log entries from different threads are interleaved by time.
      * Each entry contains the name of the thread that logged it.
      * It is process-safe so multiple app process instances may log.
        Log entries by all processes are appended to the same file.
        Process-safety has not been completely tested.
      * The log entries from different processes are interleaved by time.
      * Each entry contains a session number, which is unique for each process.
      * Log entries are stamped with the relative times since
        the previous entry of the same process session.
  
      Thread-safety is achieved using synchronized Java code.
      Process-safety is achieved using file locking.
      ///tst : process safety needs to be [better] tested.
      
      If any errors occur during logging then they will be reported:
      * As an Exception occurrence reported to the err stream
      * As an error count later to the log, if possible.
  
      Executing logging-intensive parts of the app can cause
      the app to run slowly because of:
      * Microsoft Anti-malware Service Executable  
      * Microsoft Windows Search Indexer
      * File io.
      The app can run much slower when the log file is long.
      This might be because of the above-mentioned processes.
      Apparently they scan the log.txt file after every file close.
      When the file is short it causes little delay,
      but if file is big it slows progress of the program.
      This has been fixed some by the use of buffered mode,
      which doesn't close the log file after every log entry
      but buffered mode is still a bit of a kludge.
  
      Note, to output several lines iteratively to this log's PrintWriter
      in a way that will not be interrupted by output from other threads,
      use the method getPrintWriter() to get the PrintWriter,
      and put all of the code inside a block synchronized on this log object.
      Like all synchronized code, this code should complete quickly.
      See the method TUDNet.logThreadsV() for an example of doing this.

      ///ano: On 2022-03-25 an anomaly occurred in which
        the TUDnet app failed during startup with a NullPointerException.
        It happened in the JavaFXApp class while trying to execute the code:
          static { // Added to log when this class is loaded.
              theAppLog.info("JavaFXApp loaded.");
              }
        indicating that the JavaFXApp class was being loaded
        before AppLog.theAppLog had been initialized.
  !     It appeared that classes were being loaded in the wrong order.
        Statement ordering in TUDNet.main(.) should have prevented this.
        
        Troubleshooting was interrupted when a dialog appeared displaying
          Eclipse Update
            Requirements Update
              The installation does not satisfy the requirements list below.
          ...
        This appeared to be a large, mandatory update to the Eclipse IDE.
        The update could not be skipped and was allowed to proceed.
        Eventually it finished, but the anomaly remained.

        The NullPointerException was reproduced reliably many times, 
        and seemed permanent.

        AppLog initialization was then single-stepped to try find the cause 
        of the problem, but was completed without triggering the problem.
        When app execution was resumed at normal speed, it worked perfectly.  
        Further, the app was restarted and the NullPointerException 
        no longer occurred.  The problem was gone.

        Some changes were made to remove potential uses of AppLog
        before its initialization was complete,
        uses found during the above-mentioned single-stepping,
        but these seemed unrelated to the use by JavaFXApp
        which had caused the NullPointerException.
        
      ///enh Make more modular around the following logging destinations:
       * log file, the normal destination of log entries
       * console, used if the user requests it,
         or if the log file has permanently failed 
       * anomaly dialogs, used for events needing immediate user attention,
         and which presently require the JavaFX runtime for output
       * heap memory, used for temporary storage when 
         the log file or anomaly dialogs are temporarily unavailable

      ///enh: Transition to a logger that can be 
       changed and dependency-injected.
       * Allow both static and injected-non-static, at least for a while.
       * Divide logger into modifiable settings part, and the data engine part.
       * Have methods which 
         * return a new cloned settings logger 
         * do settings changes
  
      ///fix  Sometimes when the app is terminated from 
        the Windows Task Manager, the log file is truncated,
        though judging from Console output, the app exits normally.
  
      ///tst? MakeMultiprocessSafe:  Ready for testing, with TUDNetStarter. 
        ///fix: When the presently kludgy buffered mode is enabled,
          interleaving of log entries might not work correctly
          when there are two running instances on the same computer,
          as during updates, etc.  Might need to use separate files
          which are combined and interleaved later.
        ///fix? It might fail if multiple app instances 
          try to log simultaneously.
          Make log file be share-able in case two app instances
          try to write to it at the same time.  See createOrAppendToFileV(..).

      ///enh: Eliminate thread blocking caused by pausing after closing file.
        This presently allows other processes to open the log file
        and log if they are quick enough.
        It might be necessary to use alternating temporary output files,
        double buffering, to prevent blocking.

      ///enh: To make method calls more self-documenting and less ambigious, 
        replace toConsoleB and similar simple-type flags with enums, 
        like LogLevel.
        
      ///enh: Make the logging routines more orthogonal, in the following
        dimensions:
        * label on output: info/debug/error/exception
        * output includes/does-not-include exception
        * copy log file output to console, or not, except time-stamp
        * re-throw exception and return vs. not returning
       
      ///enh: To limit unwanted logging, it might make sense to have 
        logging methods associated with classes of interest whose purpose is 
        to limit logging associated with those classes by instance.
        Some support code would be required in this class,
        but much of it would be in the target classes themselves.  For example:
        * DataNode: done.
          logging is controlled by a DataNode LogLevel variable
          in ancestor DataNode Lit instances.  
          Methods are in DataNode and subclass NamedList.
        * EpiThread: 
          * If logging is only for EpiThreads then 
            it can be controlled by an EpiThread variable.
          * If logging is to work for any Threads then 
            it the control variables must be in a table or map,
            probably in this class.
   
      */


    // Variable through which other classes can access this logger.
    public static AppLog theAppLog;


    // Dependency injection variables.

    private File appDirectoryFile; // Directory which contains the log file.
      // This is normally the app's directory.

    private String processIDString= ""; // Helpful for IDing app processes.
      // This isn't used much now.

    private MapEpiNode persistentLogMapEpiNode= null;
      // For Persistent log conditions.


    // Other variables.

    private boolean initializationStartedB= false; // Prevents re-entry.

    // File variables.
    private File logFile;  // Name of log file.
    private PrintWriter thePrintWriter = null; // non-null means file open.
      // Open means buffered mode enabled.
      // Closed means buffered mode disabled.
    private boolean clearLogFileB= false;  // true;
      // If this is set, the log file is cleared when a new session is started.


    // Logging level control variables.

    // Logging levels, from least logging to most logging.
    public enum LogLevel {
      UNDEFINED,
      OFF,
      FATAL,
      ERROR, ///ano
      WARNING, ///ano
      INFO,
      DEBUG,
      TRACE
      }  

    public static final LogLevel defaultMaxLogLevel= 
        DEBUG; // For testing.
        // INFO;  // For production.
    
    private static LogLevel maxLogLevel= defaultMaxLogLevel; /*
      The app may create and use their own maximum variables,
      or set this variable and call methods which use it.
      Calls to those methods must use this level or less for log output.
      */


    // Auto-close thread variables.
    private LockAndSignal theLockAndSignal= // For use by auto-closer thread. 
        new LockAndSignal();
    private long openedAtMsL; // Time file was last opened.
    private long appendedAtMsL; // Time file received its last output.
    private boolean bufferedModeB= true; // Initially buffering.
    private int openSleepDelayMsI= 0;


    // Session variables.
    private int theSessionI= -1;  // App session counter.
    private long lastMillisL= 0; // Last time measured.


    // Multiple process variables.
    private FileLock theLogFileLock= null; // For locking while file is open.
      // This facilitates multiple processes logging to the same file.


    /* Debug Flags.  These are added or removed as needed during debugging
    when calls to logging methods should be called in 
    only very limited conditions.  //? 
     */
    public static boolean testingForPingB= false;
    private boolean consoleCopyModeB= false; // When true, logging goes to 
      // console as well as log file.
      ///enh change to Enum for generality and better self-documentation.
    private boolean closeLoggingB= false;


    // Construction, dependency injection, and initialization.

    public AppLog(File appDirectoryFile)  // Constructor injector.
      {
        super( "AppLog"); // Set auto-closer Thread name. 
        this.appDirectoryFile= appDirectoryFile;
        }

    public void setIDProcessV( String processIDString ) // Setter injector.
      // This is used much now.
      { 
        this.processIDString= processIDString; 
        }

    public synchronized void setPersistentV( // Setter injector.
        MapEpiNode theSettingsMapEpiNode)
      /* Used to define the MapEpiNode which stores log conditions. 
       * This is gotten from thePersistent.
       */
      {
        this.persistentLogMapEpiNode= 
          theSettingsMapEpiNode.getMapEpiNode("Logging");
        }

    private synchronized void initializeIfNeededV()
      /* This method does initialization, 
        but only if it hasn't been done yet.
        */
      { 
        if (! initializationStartedB) 
          { 
            initializationStartedB= true; // Prevent re-initialization. 
            initializeV(); // Do actual initialization. 
            }
        }
    
    private void initializeV()
      /* This method does initialization, unconditionally.
        It should be called only once.  It includes the following:
        * Determining the session number.
        * Starting this the auto-closer thread.
        */
      {
        logFile=  // Identify log file name.
            new File(appDirectoryFile, "log.txt" );

        theSessionI= getSessionI();  // Get app session number.
        if (theSessionI == 0)  // If this is session 0...
          { // Reset various stuff.
            closeFileAndSleepIfOpenV(); // Be certain log file is closed.
            logFile.delete();  //...then empty log file by deleting it.
            logV(
              "=== THIS LOGGER WAS RECENTLY RESET FOR DEBUGGING PURPOSES ===");
            lastMillisL= 0; // Do this so absolute time is displayed first.
            }

        { // Initialize auto-closer thread.
          setDaemon( true ); // Make thread the daemon type. 
          start();  // Start the associated thread. 
          }

        // openFileWithRetryDelayIfClosedV(); // Open file for use.
        logHeaderLinesV(); // Append the session header lines.

        }


    // Auto-closer thread
    
    public void run() // Auto-closer thread code.
      /* This method closes the log file at times appropriate for
       * achieving the following goals:
       * 
       * * keeping the file mostly open while the app is producing log entries,
       * * closing the file if the app is NOT producing log entries,
       * * not leaving the log file open too long, so other apps can access it.
       * 
       * It does the above with the following approximate delays:
       * 
       * * 300 ms maximum time file is open with or without output.
       * * 200 ms maximum time file is open while there is no output.
       * * 10 ms minimum time file is closed after
       *   it is closed for any reason.  See closeFileAndDelayV().
       * * 1 ms between file open retries.  See openWithDelayFileWriter().
       *
         */
      {
        // Do pre-loop initialization.
        openedAtMsL= appendedAtMsL= System.currentTimeMillis();
        LockAndSignal.Input theInput= Input.NONE;
        long delayMsL= 0; // Time to next time-out.

        loop: while (true) {
          decodeInput: synchronized (this) { // Must be fast.
            if ( theInput == Input.INTERRUPTION ) // If termination requested
              break loop; // exit thread loop.
            delayMsL= Long.MAX_VALUE; // Assume maximum/infinite wait time.
            if (! bufferedModeB) { // If not buffering.
              break decodeInput; } // go do maximum wait.
            if  (thePrintWriter == null) { // If the log file is closed
              break decodeInput; } // go do maximum wait.
            long nowMsL= System.currentTimeMillis(); // Measure present time.
            long timeFromLastOutputTimeOutMsL= 
                Config.LOG_PAUSE_TIMEOUT - (nowMsL - appendedAtMsL);
            if (timeFromLastOutputTimeOutMsL <= 0) { // log file output timeout.
                /// debug("run() closing log file because of pause in output.");
                closeFileAndSleepV(); // Give other processes a chance.
                break decodeInput; }
            delayMsL= Math.min(delayMsL, timeFromLastOutputTimeOutMsL);
            long timeFromLastOpenTimeOutMsL= 
                Config.LOG_OUTPUT_TIMEOUT - (nowMsL - openedAtMsL);
            if (timeFromLastOpenTimeOutMsL <= 0) { // log file open timeout.
                /// debug("run() closing log file because of excessive output.");
                closeFileAndSleepV(); // Give other processes a chance.
                break decodeInput; }
            delayMsL= Math.min(delayMsL, timeFromLastOpenTimeOutMsL);
            } // synchronized decodeInput:
          theInput= // Wait for next significant event or timeout. 
            theLockAndSignal.waitingForInterruptOrDelayOrNotificationE(
              delayMsL);
          } // loop:
        } // run()


    // Other methods.
    
    public synchronized boolean isEnabledForLoggingB(String conditionString)
      /* Returns true if the logger condition in Persistent storage
       * specified by conditionString is true.
       * Returns false otherwise.
       * All conditions are considered false if 
       * thePersistent has not yet been injected.
       */
      { 
        boolean resultB= false; // Assume default result of logging disabled.
        if (null != persistentLogMapEpiNode) // If conditions map is defined
          resultB= // override result with value from map. 
            persistentLogMapEpiNode.isTrueB(conditionString);
        return resultB; // Return condition. 
        }

    public boolean getAndEnableConsoleModeB()
      {    
        System.out.println("AppLog.getAndEnableConsoleModeB(..) begins.");
        boolean tmpB= consoleCopyModeB;
        consoleCopyModeB= true;
        System.out.println("AppLog.getAndEnableConsoleModeB(..) end.");
        return tmpB;
        }

    public void restoreConsoleModeV( boolean oldConsoleEnabledB )
      { 
        consoleCopyModeB= oldConsoleEnabledB; 
        }

    public synchronized void enableCloseLoggingV( boolean enabledB ) 
      /* This method controls whether 
        the closing of the log file will be logged.
        If enabledB is true, log file closes will be logged.
        If enabledB is false, they will not.
        */
      {
        info("enableCloseLoggingV(" + enabledB + ")");
        closeLoggingB= enabledB;
        }
    
    public synchronized void setBufferedModeV( boolean desiredBufferedModeB ) 
      /* If buffered mode is true then it means that the log file is open and
       * it will not be immediately closed after a log entry is written.
       * 
       * If buffered mode is false then it means that the log file is closed and
       * if a log entry is written, the log file will first be opened,
       * next the entry will be written, and next 
       * the file will be immediately closed.
       * 
       * File closing is followed by a brief sleep
       * to allow output by other processes.
       * ///opt Eliminate outside callers of this?
       */
      {
        initializeIfNeededV();
        String bufferedModeLogString= "AppLog.setBufferedModeV(..), ";
        boolean actualBufferedModeB= bufferedModeB;
        bufferedModeLogString+= // Calculate whether mode is changing.
            ( desiredBufferedModeB == actualBufferedModeB )
            ? "already " // not changing
            : "being "; // is changing
        if ( desiredBufferedModeB )
          { 
            openFileWithRetryDelayIfClosedV();
            info(bufferedModeLogString+"enabled.");
            }
          else
          { 
            info(bufferedModeLogString+"disabled.");
            closeFileAndSleepIfOpenV();
            }
        bufferedModeB= desiredBufferedModeB;
        theLockAndSignal.notifyingV(); // Notify auto-closer thread. 
        }

    private void logHeaderLinesV()
      /* This initialization helper method is used to output header lines 
        at the beginning of a new log file session.
        It is the last part of the initialization.
        */
      {
        logV( 
          "<--< First time is an absolute time.  Later times are relative times."
          ); 
        logV( "" ); // Blank line.
        logV( "" ); // Blank line.
        logV( 
            "======== LOG FILE SESSION #"
            + theSessionI
            + " at " + Misc.dateString(lastMillisL)
            + " BEGINS ========"
            );
        logV( "" ); // Blank line.
        logV( "" ); // Blank line.
        }

    private int getSessionI()
      /* This method calculates what the app session number should be,
        records it in the file session.txt,
        and returns that session number.
        If either the session file or the log file do not exist
        then the session number will be 0.
        Otherwise the session number will be 
        the number stored in the session file incremented by one.
        */
      { 
        String sessionNameString= "session";
        int sessionI= Confingleton.getValueI(sessionNameString);
        if (sessionI < 0) 
          sessionI= 0; // If no or bad value, change to zero
          else 
          sessionI++; // otherwise, increment gotten value.
        if (clearLogFileB) // If debugging with clean log file,
          sessionI= 0;  // reset session to 0.  This will reset log also.
        Confingleton.putValueV(sessionNameString, sessionI);
        return sessionI;
        }

    public void setLevelLimitV( LogLevel limitLogLevel )
      // This method is used to override the default log level at run time.
      { maxLogLevel= limitLogLevel; }



    // Delegating logging methods.


    // Special conditional log methods.

    public boolean testAndLogDisabledB(boolean disabledB, String logString)
      /* This method is used to log a special message about disabled code.
        If disabledB is true then it logs a DEBUG message that
        code identified with the string logString is disabled.
        It returns the value of disabledB.
        If disabled is false then it logs nothing.
        
        ///fix  This probably belongs in a different class.
          It could be changed to get the disabledB parameter from
          a configuration file.
        */
      {
        if (disabledB)
          debug(logString + ": DISABLED");
        return disabledB;
        }


    // Tracing methods.
    
    public void trace(String inString)
      /* This method is for tracing.  It writes only inString.
        */
      { 
        logB( TRACE, false, null, inString );
        }


    // Debug methods.
  
    public void debug(String conditionString, String inString)
      /* This method writes a debug String inString to a log entry
        but not to the console,
        and only if the condition named by conditionString is enabled.
        */
      { 
        if (isEnabledForLoggingB(conditionString))
          debug(inString);
        }

    public void debug(String inString)
      /* This method writes a debug String inString to a log entry.
        It is tagged as for debugging.
        */
      { 
        debug(false, inString);
        }

    public void debugToConsole(String conditionString, String inString)
      /* This method writes a debug String inString to a log entry
        with a copy going to the console,
        but only if the condition named by conditionString is enabled.
        It is tagged as for debugging.
        */
      { 
        if (isEnabledForLoggingB(conditionString))
          debug(true, inString);
        }

    public void debugToConsole(String inString)
      /* This method writes a debug String inString to a log entry
        with a copy going to the console.
        It is tagged as for debugging.
        */
      { 
        debug(true, inString);
        }

    public void debug(boolean toConsoleB, String inString)
      /* This method writes a debug String inString to a log entry,
        and to the console if either toConsoleB is true.
        It is tagged as for debugging.
        */
      {
        logB( DEBUG, toConsoleB, null, inString );
        }


    // Information methods.

    public void info(boolean toConsoleB, String inString)
      /* This method writes an information String inString to a log entry
        and optionally to the console.
        */
      { 
        info(toConsoleB, null, inString); 
        }

    public void info(Throwable theThrowable, String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        If theThrowable is not null then it displays that also.
        */
      { 
        info(false, theThrowable, inString ); 
        }

    public void info(String conditionString, String inString)
      /* This method writes an information String inString to a log entry
        but not to the console,
        and only if the condition named by conditionString is enabled.
        */
      { 
        if (isEnabledForLoggingB(conditionString))
          info(inString); 
        }

    public void info(String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        */
      { 
        info(false, null, inString); 
        }

    public void info( // (toConsoleB, theThrowable, inString)
        boolean toConsoleB, Throwable theThrowable, String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        If theThrowable is not null then it displays that also.
        If toConsoleB==true then out ismade to console also.
        */
      { 
        logB(INFO, toConsoleB, theThrowable, inString);
        }


    // Exception methods.
    
    public void exceptionWithRethrowV(String inString, Exception e)
      throws Exception
      /* This method writes a Java exception String inString to a log entry
        and also to the console error stream for Exception e,
        then it throws the exception again.
        
        //tmp  This method is not usable because of 
        Exception type incompatibilities.
        */
      { 
        exception(inString, e);
        throw e;
        }
    
    public void exception(String inString, Throwable e)
      /* This method writes a Java exception String inString to a log entry
        and also to the console error stream for Exception e.
        This is for exceptions that must be caught,
        but for which there is nothing that can be or needs to be done.
        
        Note, console output is all done to System.out, not System.err.
        This is done to avoid console output which is out of order.
        ///enh Make this display a StackTrace with exception.

        It also throws, and catches, a DebugException, 
        for use in determining the causes of anomalies.
        */
      { 
        String wholeString= "EXCEPTION: " + inString + " :" + NL + "  " + e ;

        synchronized(this) { // Must synchronize on AppLog object so 
          System.out.println(wholeString); // intro string and
          e.printStackTrace(System.out); // stack trace
          } // are together on console.

        synchronized(this) { // Must synchronized on AppLog object so 
          logB( ERROR, true, e, wholeString); // log entry introduction and
          doStackTraceV(e); // stack trace
          } // are together in log file.

        try { // Throw an exception that Eclipse IDE can use to suspend thread.
            throw new DebugException();
          } catch (DebugException theDebugException) {
            ; 
          }
        }


    /* Error methods.  
     * They all eventually call the error(.) method with the most arguments.
     * See that method for documentation.
     */
    
    public void error(String summaryIDLineString)
      {
        error( summaryIDLineString, (Throwable)null); 
        }
    
    public void error(String summaryIDLineString,String detailsString)
      {
        error(summaryIDLineString, detailsString, null); 
        }

    public void error(String summaryIDLineString, Throwable theThrowable)
      {
        error(summaryIDLineString, null, theThrowable); 
        }

    public void error(
        String summaryIDLineString, 
        String detailsString, 
        Throwable theThrowable
        )
      /* This method writes an error String inString to a log entry,
        and also to the console stream.
        and to an Anomaly dialog.
        An error is something with which the app should not have to deal.
        Response to an error is usually to either retry or terminate.
        It also includes a stack trace.

        It also throws, catches, and ignores a DebugException.
        This can be used with the IDE in determining the causes of anomalies.

        The arguments to this function are as follows:
        * summaryIDLineString is for a hopefully short description of the error,
          or at least an error types, that will serve as a cache key
          for Dialog windows so the windows can be reused.  
        * detailsString is for a longer string with details about the error.
        * Throwable theThrowable is for an exception that produced the error.
        Any of these parameters can be null.
        Reasonable attempts will be made to provide values in these cases.

        ///ano Maybe not all errors should be reported as anomalies.
        Maybe only a subset should be, 
        and should go through the Anomalies class.
        */
      { 
        synchronized(this) { // Synchronized on AppLog object for coherence. 
          logV( 
              ERROR, 
              summaryIDLineString, 
              detailsString, 
              theThrowable, 
              true // Send to console also.
              );
          doStackTraceV(theThrowable);
          }

        try { // Throw an exception that Eclipse IDE can use to suspend thread.
            throw new DebugException();
          } catch (DebugException theDebugException) {
            ; // Catch and ignore the exception if the IDE doesn't process it.
          }
        }



    /* Warning methods.
     * They all eventually call the warning(.) method with the most arguments.
     * See that method for documentation.
     */
    

    public void warning(String summaryIDLineString)
      {
        warning(summaryIDLineString, null);
        }

    public void warning(String summaryIDLineString, String detailsString)
      /* This method writes a warning String to a log entry
        and to an Anomaly dialog.
        A warning is something with which the app should not have to deal.
        Response to a warning is usually to either ignore it and continue.

        The arguments to this function are as follows:
        * summaryIDLineString is for a hopefully short description of the error,
          or at least an error types, that will serve as a cache key
          for Dialog windows so the windows can be reused.  
        * detailsString is for a longer string with details about the error.
        * Throwable theThrowable is for an exception that produced the error.
        Any of these parameters can be null.
        Reasonable attempts will be made to provide values in these cases.

        ///ano Maybe not all errors should be reported as anomalies.
        Maybe only a subset should be, 
        and should go through the Anomalies class.
        */
      {
        synchronized(this) { // Synchronized on AppLog object for coherence. 
          logV( 
              WARNING, 
              summaryIDLineString, 
              detailsString, 
              null, // No associated Exception/Trhowable.
              false // Nothing to console.
              );
          /// doStackTraceV(theThrowable);
          }
        }


    // Helper methods.

    public void doStackTraceV(Throwable theThrowable)
      /* This method might log a stack trace, if logStackTraceB is true.
        If it does, it does it as follows:
        If theThrowable is NOT null, it logs a stack trace of it.
        If theThrowable IS null, it creates and assigns a new Throwable to it.
        Then it logs a stack trace of theThrowable.
       */
      {
        boolean logStackTraceB= true; // false; // Change this to control stack trace.
        if (logStackTraceB ) reallyDoStackTraceV(theThrowable);
        }

    public void reallyDoStackTraceV(Throwable theThrowable)
      // See doStackTrace(..).
      {
        if (theThrowable == null)
          theThrowable= new Throwable("Throwable created to display stack trace");
        appendToOpenFileV(NL);  // Go to new line.
        theThrowable.printStackTrace(getPrintWriter());
        }

    public static String glyphifyString(String theString)
      {
        StringBuilder resultStringBuilder= new StringBuilder();
        for (int indexI = 0; indexI < theString.length(); indexI++) {
          char C= theString.charAt(indexI);
          if (Character.isISOControl(C)) {
            resultStringBuilder.append(String.format("\\u%04x", (int)C));
            resultStringBuilder.append(C);
            }
          else
            resultStringBuilder.append(C);
          }
        return resultStringBuilder.toString();
        }

    public void consoleInfo(String inString, boolean debugB)
      /* This method writes an error String inString to a log entry
        and also to the console error stream.
        */
      { 
        String wholeString= "CONSOLE-INFO: "+inString; // Build error output string.

        System.err.println(wholeString);  // Send one copy to error console.

        logV( null, null, wholeString, null, debugB );  // Send one copy to log. 
        }

    
    /* LogB(..) and logV(..) family methods.

      * logB( theLogLevel ) returns true if theLogLevel is less than maxLogLevel.
        This displays nothing.  It is used to control what is displayed.
        If it returns true, logging should be done.
      * logB( theLogLevel, ... ) returns true and creates a log entry with
         theLogLevel displayed in the entry and the remaining parameters 
         if theLogLevel is less than maxLogLevel.  
         Otherwise it just returns false.
      * logV( ... ) creates a log entry from its parameters unconditionally.

      The following methods take various combinations of parameters 
      from the following set:
      * LogLevel theLogLevel: used for filtering and is displayed. 
      * boolean toConsoleB: controls whether a copy of the entry 
        goes to the console.
      * Throwable theThrowable: an exception to be displayed, if it is not null.
      * String inString: message to be displayed.
        
      */

    public synchronized boolean logB(
        LogLevel theLogLevel, 
        boolean toConsoleB,
        Throwable theThrowable, 
        String inString
        )
      {
        boolean loggingB= logB(theLogLevel);
        if ( loggingB )
            logV( theLogLevel, null, inString, theThrowable, toConsoleB );
        return loggingB;
        }

    public synchronized boolean logB( LogLevel theLogLevel )
      /* This method doesn't actually log anything, but
        it is used to decide whether something should be logged.
        It returns true if theLogLevel is less than or equal to maxLogLevel,
        meaning that the associated logging should be done.
        It returns false otherwise. 
        */
      {
        return ( theLogLevel.compareTo( maxLogLevel ) <= 0 );
        }

    public boolean logB( LogLevel theLogLevel, String inString )
      {
        boolean loggingB= logB(theLogLevel);
        if ( loggingB )
            logV( theLogLevel, inString );
        return loggingB;
        }
    
    public void logV( LogLevel theLogLevel, String inString )
      { 
        logV(theLogLevel, null, inString, null, false); 
        }

    public void logV( String inString )
      { 
        logV(null, null, inString, null, false); 
        }


    // Non-delegating logging methods.

    public synchronized void logV(
        LogLevel theLogLevel,
        String summaryIDLineString,
        String detailsString,
        Throwable theThrowable, 
        boolean toConsoleB )
      /* The buck stops here.  
        This logging method does not delegate to another method in the family.

        This is also the most capable logging method.
        It receives and processes all possible parameters.
        All the above logging methods in the family eventually call this one.

        This method creates one log entry and appends it to the log file.
        The entry might contain multiple lines if any of the parameters
        evaluate to Strings which contain newlines.

        This method also sends a copy of the log entry
        to the console if toConsoleB is true.

        The method might also output to a Dialog box in the case of Anomalies.

        The log entry always begins with
        * the app session number,
        * processIDString, if any,
        * milliseconds since the previous entry,
        * theLogLevel if not null,
        * the thread name,
        * theThrowable if not null.

        ///opt Replace String appends with StringBuilder appends, for speed?

        ///enh Add stackTraceB to cause the displays of a stacktrace,
          on console and in log, if true.
          Or divide theThrowable into 2, 1 with and 1 without, a stacktrace.
        */
    { 
      initializeIfNeededV();
      logTriggeredPollerV(); 

      long nowMillisL= System.currentTimeMillis(); // Save present time.

      String entryHeadString= ""; // Initialize head String to empty, then append
      entryHeadString+= NL; // a line terminator to start a new line,
      entryHeadString+= theSessionI;  // the session number,
      entryHeadString+= processIDString;
      entryHeadString+= ":";  // and a separator,
      entryHeadString+= String.format(  // time since last output,
          "%1$5d", nowMillisL - lastMillisL);
      entryHeadString+= " ";  // a space,
      if (toConsoleB || consoleCopyModeB) // a console flag if called for 
        entryHeadString+= "CON ";
      if ( theLogLevel != null ) { // and if the log level is present
        entryHeadString+= theLogLevel; // the log level,
        entryHeadString+= " ";  // and a space,
        }
      entryHeadString+= "["+Thread.currentThread().getName()+"]"; // the thread name,
      entryHeadString+= " ";  // and a space.

      String entryBodyString= ""; // Set content string to empty, then append
      if (null == detailsString)
        detailsString= "[no details available]";
      entryBodyString+= detailsString; // the details string from the caller,
      entryBodyString+= " ";  // and a space
      if ( theThrowable != null ) // and the Throwable if present. 
        entryBodyString+= theThrowable;

      if (null != theLogLevel) // Display Anomalies Dialog window if needed.
        switch (theLogLevel) {
          case WARNING: 
          case ERROR: 
            String dialogErrorString= // Display info as a dialog first.
              Anomalies.displayDialogReturnString(
                summaryIDLineString, 
                "Severity: " + theLogLevel + "\n" + entryBodyString
                );
              entryBodyString= EpiString.combine1And2With3String(
                dialogErrorString, entryBodyString, ": ");
          default: // Common actions for above and any other LogLevel.
            // None.
          }
      entryBodyString= EpiString.combine1And2With3String(
          summaryIDLineString, entryBodyString, ":\n");
      
      { // Append to log file.
        boolean closeFileWhenDoneB=
          ( ! bufferedModeB ) // Buffered mode disabled
          && ( thePrintWriter == null ); // and file is closed.
        openFileWithRetryDelayIfClosedV();
        appendToOpenFileV(entryHeadString + entryBodyString);  // Both pieces.
        if (closeFileWhenDoneB) closeFileV();
        }

      if (toConsoleB || consoleCopyModeB) // Append to console if called for
        System.out.print(entryHeadString + entryBodyString);

      lastMillisL= nowMillisL; // Save present time as new last time.
      }
    
    public synchronized PrintWriter getPrintWriter()
      /* Returns an open PrintWriter for outputting to the log file.  
        This method is for temporary use during debugging.
        It does not preserve buffered mode.  
        */
      {
        openFileWithRetryDelayIfClosedV();
        return thePrintWriter;
        }


    private boolean pollingB= false; // Make true to enable polling.

    private void logTriggeredPollerV()
      /* This method provides the means to call 
        a developer-provided method every time something is logged.
        It can be useful for finding when 
        particular events occur relative to other logged events.
        The developer-provided method typically
        examines one or more program variables
        and logs information when conditions are met.
        */
      {
        if (pollingB) // Prevent [recursive] calls to poller.
          {
            pollingB= false;
            //debug("Call to polling method goes here.");
            pollingB= true;
            }
      } 

    private long previousDebugClockOutTimeMsL= 0;
    public synchronized void debugClockOutV(String theString)
      /* This method appends theString to the log file and to the console.
       * It also outputs the number of ms since the previous similar output
       * if that number is more than 0.
       * It is meant for temporary use during debugging.
       * It is anticipated that this method will output mostly
       * high frequency, short length strings.
       */
      {
        long nowTimeMsL= System.currentTimeMillis();
        long differenceTimeMsL= nowTimeMsL - previousDebugClockOutTimeMsL;
        if (0 != differenceTimeMsL)
          theString= differenceTimeMsL + " " + theString;
        previousDebugClockOutTimeMsL= nowTimeMsL;
        theString= "["+theString+"]";
        System.out.print(theString);
        appendToFileV(theString);
        }
    
    public synchronized void appendToFileV(String inString)
      /* This method writes to thePrintWriter, 
        which may be open or closed.
        It is for temporary logging of short strings during debugging.   
        because it does not respect buffering protocols.
        It always leaves the log file open.
        */
      { 
        openFileWithRetryDelayIfClosedV();
        appendToOpenFileV(inString);
        // Note, log file remains open.
        }

    public synchronized void appendToOpenFileV(String inString)
      /* This method writes to thePrintWriter, which must be open.  */
      { 
        thePrintWriter.print( inString );  // Append inString to file.
        appendedAtMsL= System.currentTimeMillis(); // Record time of append.
        theLockAndSignal.notifyingV(); // Inform auto-closer thread. 
        }

    private synchronized void openFileWithRetryDelayIfClosedV()
      /* This method opens thePrintWriter and 
        everything else associated with the log file.
        If the log file is already open then it does nothing.
        */
      {
        if (thePrintWriter == null) {  // Opening file if closed.
          try {
              thePrintWriter =  // Prepare...
                new PrintWriter(  // ...a character output stream..
                  new BufferedWriter(  // ...with buffering to...
                    openWithRetryDelayFileWriter()  // ...a FileWriter...
                    ) // ...to opened log file.
                  );
              openedAtMsL= System.currentTimeMillis(); // Record time of open.
            } catch (IOException e) {
              System.err.println("AppLog error opening PrintWriter...: "+e);
            }
          if (openSleepDelayMsI != 0) 
            { // Log any open failures.
              debug("openFileIfClosedV() opened after "
                +openSleepDelayMsI
                +" retries after 1 ms sleep each");
              openSleepDelayMsI= 0; // Reset for later.
              }
            else
            ; /// debug("openFileIfClosedV() opened log file.");
          }
        }

    private synchronized Writer openWithRetryDelayFileWriter()
      throws IOException
      /* This method opens a FileWriter for the log file unconditionally.
        It should be called only if the log file is closed.  
        If the open fails, it sleeps for 1 ms, and tries again.
        It repeats until the open succeeds.
        It returns the open FileWriter.
        */
      { 
        boolean interruptedB= false;
        FileOutputStream theFileOutputStream= null;
        Writer resultWriter= null;
        FileChannel theFileChannel= null;
        while (true) { // Keep trying to open until it succeeds.
          try {
              theFileChannel= null;
              theFileOutputStream= null;
              resultWriter= null;
              theLogFileLock= null;
              theFileOutputStream= new FileOutputStream( // Open for writing...
                  logFile,   // ...log file with this name...
                  true  // ...and write to end of file, not the beginning.
                  );
              theFileChannel= theFileOutputStream.getChannel();
              theLogFileLock= theFileChannel.lock();
              theFileChannel.position(theFileChannel.size()); // Set for append.
              resultWriter= new OutputStreamWriter(theFileOutputStream);
              break; // Exit if open succeeded.
            } catch (IOException e) {
              if ( e instanceof FileLockInterruptionException ) {
                Thread.interrupted(); // Clear interrupt
                interruptedB= true; // but record it for restoring later.
                }
              else {
                System.err.println("openWithRetryDelayFileWriter() common exception "+e);
                Closeables.closeWithoutErrorLoggingB(resultWriter);
                if (theLogFileLock!=null) theLogFileLock.release();
                Closeables.closeWithoutErrorLoggingB(theFileChannel);
                Closeables.closeWithoutErrorLoggingB(theFileOutputStream);
                interruptibleSleepB( 1 ); // Pause 1 ms.
                openSleepDelayMsI++; //* Count the pause and the time.
                }
            }
          } // while (true)
        if ( interruptedB ) // If an interruption happened
          Thread.currentThread().interrupt(); // reestablish interrupt status.
        return resultWriter;
        }

    public static boolean interruptibleSleepB( long msL )
      /* This method works like Thread.sleep( msI ),
        causing the current thread to sleep for msI milliseconds,
        except that it does not throw an InterruptedException 
        if it was interrupted while sleeping.
        Also if msL is less than 0 it returns immediately
        instead of producing an illegal argument exception.
        Instead, if that happens then the method returns immediately with
        the thread's interrupt status set and the sleep delay incomplete.
        The interrupt status can be sensed and processed by the caller.
        Also it returns true if the sleep was interrupted, false otherwise,
        but because the interrupt status can be sensed,
        this is probably not very useful.
        */
      {
        boolean interruptedB= false;
        
        try {
          if ( msL >= 0 ) // Skip if less than 0.
            Thread.sleep( msL ); // Try to sleep for desired time.
          } 
        catch( InterruptedException ex ) { // Handling interruption.
          Thread.currentThread().interrupt(); // Reestablish interrupted.
          interruptedB= true; // Changing return value to indicate it.
          }
        
        return interruptedB;
        }

    public synchronized void closeFileAndSleepIfOpenV()
      /* This method is like closeFileAndDelayV() 
        but acts only if the log file is open.
        */
      { 
        if (closeFileIfOpenB())  // Closing file if open. 
          postCloseSleepV();
        }

    public synchronized boolean closeFileIfOpenB()
      /* This method is like closeFileAndSleepV() 
        but acts only if the log file is open.
        */
      { 
        boolean wasOpenB= (thePrintWriter != null); 
        if (wasOpenB)  // Closing file if open. 
          closeFileAndSleepV(); 
        return wasOpenB;
        }

    private synchronized void closeFileAndSleepV()
      /* This method closes thePrintWriter if it's open, 
        which closes everything associated with the log file.
        Then it sleeps for a while.
        Note, because the sleep happens in a synchronized method,
        so it will prevent any output by this app to the log file 
        for that period, allowing other processes to output to the log file.
        */
      { 
        closeFileV();
        postCloseSleepV();
        }

    private void postCloseSleepV()
      /*
        Used to give other processes time to open for their output.
        It is called after some log file closings. 
         */
      {
        interruptibleSleepB(Config.LOG_MIN_CLOSE_TIME); // Block and pause.
        }

    private synchronized void closeFileV()
      /* This method closes thePrintWriter unconditionally,
        which closes everything associated with the log file.
        It should be called only if the log file is open.  
        */
      {
        if (closeLoggingB)
          info("closeFileV() flushing, unlocking, and closing log file.");
        thePrintWriter.flush();  // Flush buffers to file.
        if (theLogFileLock != null) // Unlock file if locked.
          try {
            theLogFileLock.release(); // Unlock first.
            theLogFileLock= null;
          } catch (IOException e){
            System.out.println("closeFileV() during release(), "+e);
          }
        thePrintWriter.close(); // Close file.
        thePrintWriter= null; // Indicate file is closed.
        }
      

    }  
