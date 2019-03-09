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

import allClasses.LockAndSignal.Input;

import static allClasses.AppLog.LogLevel.*;

public class AppLog extends EpiThread

  /* This class is for logging information from application programs.  
    It is of a special design to provide the following features:
    * Logs strings provided by the application.
    * Thread-safe any app thread may log.
      There entries are appended to the same file
      and their log entries will be interleaved.
      Entries contain the name of the thread that logged it.
    ? Multiple app process instances may log.
      There entries are appended to the same file
      and their log entries will be interleaved.
      Entries contain a session number,
      which should be unique for each logging process.
    * Log entries are stamped with the relative times since
      the previous entry of the same process session.

    Multiple thread safety is achieved using synchronized Java code.
    Multiple process safety is achieved using file locking.
    ///tst : process safety needs to be [better] tested.
    
		If any errors occur during logging they will be reported:
		* As an Exception occurrence reported to the err stream
		* As an error count later to the log, if possible.

    Executing logging-intensive parts of the app can cause
    the app to run slowly because of:
    * Anti-malware Service Executable  
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

    ///fix  Sometimes when the app is terminated from 
      the Windows Task Manager, the log file is truncated,
      though judging from Console output, the app exits normally.

		///enh: Begin the transition to a logger 
      that doesn't need to be a static singleton and can be injected.
      Allow both static and injected-non-static, at least for a while.

    ///tst? MakeMultiprocessSafe:  Ready for testing, with InfogoraStarter. 
	    ///fix: When the presently kludgy buffered mode is enabled,
	      interleaving of log entries might not work correctly
	      when there are two running instances on the same computer,
	      as during updates, etc.  Might need to use separate files
	      which are combined and interleaved later.
	    ///fix? It might fail if multiple app instances try to log simultaneously.
	      Make log file be share-able in case two app instances
	      try to write to it at the same time.  See createOrAppendToFileV(..).

    ///enh: Eliminate thread blocking caused by pausing after closing file.
      This presently allows other processes to open the log file
      and log if they are quick enough.
      It might be necessary to use alternating temporary output files 
      to receive logging data to prevent blocking.
      
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

  {
  
    // Singleton code.
    
      private AppLog()  // Private constructor prevents external instantiation.
        {
      	  super( "AppLog");
        	}

      private static final AppLog theAppLog=  // Internal singleton builder.
        new AppLog();

      static { 
      	theAppLog.setDaemon( true ); // Make thread the daemon type. 
      	theAppLog.start();  // Start the associated thread. 
      	}
      
      public static AppLog getAppLog()   // Returns singleton logger.
        { return theAppLog; }

    // Variables.
    	
    	// Logging levels.

      public enum LogLevel {
      	UNDEFINED,
	    	OFF,
	    	FATAL,
	    	ERROR,
	    	WARN,
	    	INFO,
	    	DEBUG,
	    	TRACE
	  		}	

    	public static final LogLevel defaultMaxLogLevel= DEBUG; // INFO;
    	private static LogLevel maxLogLevel= defaultMaxLogLevel;
    	  // The app may create and use their own maximum variables,
    	  // or set this variable and call methods which use it.
    	  // Calls to those methods must use this level or less for log output.
    	
      private boolean initializationStartedB= false;
    	private LockAndSignal theLockAndSignal= new LockAndSignal();
    	
    	private File logFile;  // Name of log file.
      private int theSessionI= -1;  // App session counter.
      private long lastMillisL= 0; // Last time measured.
      private PrintWriter thePrintWriter = null; // non-null means file open.
        // Open means buffered mode enabled.
        // Closed means buffered mode disabled.
      private FileLock theLogFileLock= null; // For locking while file is open.
      private long openedAtMsL; // Time file was last opened.
      private long appendedAtMsL; // Time file received it last output.

      private boolean bufferedModeB= true; // Initially buffering.
      private int openSleepDelayMsI= 0;
      private String processIDString= "";

    /* Debug Flags.  These are added or removed as needed during debugging
      when calls to logging methods should be called in 
      only very limited conditions.  //? 
     	*/
      public static boolean testingForPingB= false;
      private boolean debugEnabledB= true;
      private boolean consoleCopyModeB= false; // When true, logging goes to 
        // console as well as log file.
        ///ehn change to Enum for generality and better self-documentation.
      public LogLevel packetLogLevel= DEBUG;  // INFO; // DEBUG; 

    public void setIDProcessV( String processIDString )
    { this.processIDString= processIDString; }

    public void run() // Auto-closer thread code.
      /* This method closes the log file at times appropriate for
        achieving the following goals:
        * keeping the file mostly open when the app is producing log entries,
        * closing it if not being used,
        * not leaving it open too long, so other apps can access log file.
        It does this with the following approximate delays:
        * 1 ms between open file retries.  See openWithDelayFileWriter().
        * 10 ms minimum time file it is closed after 
          it is closed for any reason.  See closeFileAndDelayV().
        * 200 ms maximum time file is open with no output.
        * 300 ms maximum time file is open with or without output.

       	*/
    	{
        // closedAtNsL= 
        openedAtMsL= appendedAtMsL= System.currentTimeMillis();
        LockAndSignal.Input theInput= Input.NONE;
        long delayMsL= 0; // Time to next time-out.
    	  loop: while (true) {
      	  decodeInput: synchronized (this) { // Must be fast.
            if ( theInput == Input.INTERRUPTION ) // Signal to terminate.
              break loop; // Exit thread loop.
            delayMsL= Long.MAX_VALUE; // Set maximum/infinite wait time.
            if (! bufferedModeB) { // Not buffering.
              break decodeInput; } // Go do maximum wait.
            if  (thePrintWriter == null) { // Log file closed
              break decodeInput; } // Go do maximum wait.
            long nowMsL= System.currentTimeMillis(); // Measure present time.
            long timeFromLastOutputTimeOutMsL= 
                Config.LOG_PAUSE_TIMEOUT - (nowMsL - appendedAtMsL);
            if (timeFromLastOutputTimeOutMsL <= 0) { // log file output timeout.
                debug("run() closing log file because of pause in output.");
                closeFileAndSleepV(); // Give other processes a chance.
                break decodeInput; }
            delayMsL= Math.min(delayMsL, timeFromLastOutputTimeOutMsL);
            long timeFromLastOpenTimeOutMsL= 
                Config.LOG_OUTPUT_TIMEOUT - (nowMsL - openedAtMsL);
            if (timeFromLastOpenTimeOutMsL <= 0) { // log file open timeout.
                debug("run() closing log file because of excessive output.");
                closeFileAndSleepV(); // Give other processes a chance.
                break decodeInput; }
            delayMsL= Math.min(delayMsL, timeFromLastOpenTimeOutMsL);
            } // synchronized decodeInput:
          theInput= // Wait for next significant event or timeout. 
            theLockAndSignal.waitingForInterruptOrDelayOrNotificationE(
              delayMsL);
          } // loop:
    		} // run()
    
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

    public synchronized void setBufferedModeV( boolean desiredBufferedModeB ) 
    	/* This method opens the file for buffered mode,
    	  and closes it for non-buffered mode.
        File-open means buffering will happen on any output.
        File-closed means any buffer has been flushed.
        File closing is followed by a brief sleep 
        to allow output by other processes.
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
	    	theLockAndSignal.notifyingV(); 
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
        */
      {
        logFile=  // Identify log file name.
        		Config.makeRelativeToAppFolderFile( "log.txt" );
        theSessionI= getSessionI();  // Get app session number.
        if (theSessionI == 0)  // If this is session 0...
          { // Reset various stuff.
            closeFileAndSleepIfOpenV(); // Be certain log file is closed.
            logFile.delete();  //...then empty log file by deleting it.
            logV(
              "=== THIS LOGGER WAS RECENTLY RESET FOR DEBUGGING PURPOSES ===");
            lastMillisL= 0; // Do this so absolute time is displayed first.
            }
        // openFileWithRetryDelayIfClosedV(); // Open file for use.
        logHeaderLinesV(); // Append the session header lines.
        }

    private void logHeaderLinesV()
      /* This initialization helper method is used to output header lines 
        at the beginning of a new log file session.
        It is the last part of the initialization.
        */
      {
        logV( 
          "<--< This is an absolute time.  Later times are relative times."
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
        if (Config.clearLogFileB) // If debugging with clean log file,
          sessionI= 0;  // reset session to 0.  This will reset log also.
        Confingleton.putValueV(sessionNameString, sessionI);
        return sessionI;
        }

    public void setLevelLimitV( LogLevel limitLogLevel )
    	{ maxLogLevel= limitLogLevel; }


    // Actual log methods start here.

    // Non-delegating logging methods.
    
    public void trace(String inString)
      /* This method is for tracing.  It writes only inString.
        */
      { 
				logB( TRACE, false, null, inString );
        }
  
    public void debug(String inString)
      /* This method writes a debug String inString to a log entry
        but not to the console.
        It is tagged as for debugging.
        */
      { 
    		if (debugEnabledB) 
    			logB( DEBUG, consoleCopyModeB, null, inString );
        }

    public void info(boolean debugB, String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        If theThrowable is not null then it displays that also.
        */
      { 
        info(debugB, null, inString); 
        }

    public void info(Throwable theThrowable, String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        If theThrowable is not null then it displays that also.
        */
      { 
    		info(false, theThrowable, inString ); 
        }

    public void info(String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        */
      { 
        info(false, null, inString); 
        }

    public void info(
        boolean consoleCopyEntryB, Throwable theThrowable, String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        If theThrowable is not null then it displays that also.
        If consoleCopyEntryB==true then out ismade to console also.
        */
      { 
				logB(INFO, consoleCopyEntryB, theThrowable, inString);
        }
    
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
    
    public void exception(String inString, Exception e)
      /* This method writes a Java exception String inString to a log entry
        and also to the console error stream for Exception e.
        This is for exceptions that must be caught,
        but for which there is nothing that can be or needs to be done.
        */
      { 
        String wholeString= "EXCEPTION: " + inString + " :\n  " + e ;

        System.err.println(wholeString);  // Send to error console.
        e.printStackTrace();

        error( wholeString );
        }
    
    public void error(String inString)
      { 
        error( null, inString); 
        }
    
    public void error(Throwable theThrowable, String inString)
      /* This method writes an error String inString to a log entry,
        and also to the console error stream?  Disabled temporarily.
        An error is something with which the app should not have to deal.
        Response is to either retry or terminate.
        */
      { 
    		logB( ERROR, true, theThrowable, inString);
        }
    
    public void warning(String inString)
      // This method writes a severe error String inString to a log entry.
      { 
    		logB( WARN, false, null, inString );
        }
    
    public void consoleInfo(String inString, boolean debugB)
      /* This method writes an error String inString to a log entry
        and also to the console error stream.
        */
      { 
        String wholeString= "CONSOLE-INFO: "+inString; // Build error output string.

        System.err.println(wholeString);  // Send one copy to error console.

        logV( null, wholeString, null, debugB );  // Send one copy to log. 
        }


    /* LogB(..) and logV(..) family methods.

      * logB( theLogLevel ) returns true if theLogLevel is less than maxLogLevel.
        This displays nothing.  It is used to control what is displayed.
      * logB( theLogLevel, ... ) returns true and creates a log entry with
       	theLogLevel and the remaining parameters if theLogLevel 
       	is less than maxLogLevel.  Otherwise it just returns false.
      * logV( ... ) creates a log entry from the parameters unconditionally.

      The following methods take various combinations of parameters 
      from the following set:
    	* LogLevel theLogLevel: used for filtering and is displayed. 
      * boolean consoleCopyEntryB: controls whether a copy of the entry goes to console.
      * Throwable theThrowable: an exception to be displayed, of not null.
    	* String inString: message to be displayed.
    		
      */

    public synchronized boolean logB(
    		LogLevel theLogLevel, 
    		boolean consoleCopyEntryB,
    		Throwable theThrowable, 
        String inString
    		)
	    {
	  		boolean loggingB= logB(theLogLevel);
	  		if ( loggingB )
	      		logV( theLogLevel, inString, theThrowable, consoleCopyEntryB );
	  		return loggingB;
      	}

    public synchronized boolean logB( LogLevel theLogLevel )
      /* This method doesn't actually log anything, but
        it is used to decide whether anything should be logged.
        It returns true if logging should be done, false otherwise. 
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
    		logV(theLogLevel, inString, null, false); 
    		}

    public void logV( String inString )
    	{ 
    		logV(null, inString, null, false); 
    		}

    // Non-delegating logging methods.
    
    public synchronized void logV(
    		LogLevel theLogLevel, 
    		String inString, 
    		Throwable theThrowable, 
    		boolean consoleCopyEntryB )
      /* The buck stops here.  
        This logging method does not delegate to another method is the family.

        This is also the most capable logging method.
        It can process all possible parameters.
        All the above logging methods in the family eventually call this one.

        This method creates one log entry and appends it to the log file.
        It could be multiple lines if any of the parameters
        evaluate to Strings which contain newlines.
        The log entry always begins with
        * the app session number,
        * processIDString, if any,
        * milliseconds since the previous entry,
        * theLogLevel if not null, 
        * the thread name,
        * theThrowable if not null.

        This method also sends a copy of the log entry
        to the console if consoleCopyEntryB is true.

        ///opt Replace String appends by StringBuilder appends, for speed?
        ///enh Add stackTraceB which displays stack,
          on console and in log, if true.
        */
    { 
      initializeIfNeededV(); 
      if  // Acting based on whether file is open (buffered) or closed (not).
        ( ( ! bufferedModeB ) // Buffered mode disabled.
          &&( thePrintWriter == null ) // and file is closed.
          )
        { 
          openFileWithRetryDelayIfClosedV();
          logToOpenFileV(theLogLevel,consoleCopyEntryB,theThrowable,inString);
          closeFileV();
          }
        else // Buffered mode enabled or file is open.
        {
          openFileWithRetryDelayIfClosedV(); ///opt  needed?
          logToOpenFileV(theLogLevel,consoleCopyEntryB,theThrowable,inString);
          }
      }

    public synchronized void logToOpenFileV(
        LogLevel theLogLevel, 
        boolean consoleCopyEntryB,
        Throwable theThrowable, 
        String inString
        )
      {
    		long nowMillisL= System.currentTimeMillis(); // Saving present time.

        String aString= ""; // Initialize String to empty, then append to it...
        aString+= theSessionI;  //...the session number,...
        aString+= processIDString;
        aString+= ":";  //...and a separator.
        aString+= String.format(  // ...time since last output,...
        		"%1$5d", nowMillisL - lastMillisL
        		);
        aString+= " ";  //...a space,...
        
   	  	if (consoleCopyEntryB || consoleCopyModeB) // ...a console flag if called for... 
   	  		aString+= "CON ";
   	  	
        if ( theLogLevel != null ) { //..and the log level if present... 
	   	  	aString+= theLogLevel; // ...the log level,...
	        aString+= " ";  //...a space,...
	        }
   	  	
   	  	aString+= Thread.currentThread().getName(); // the name of thread,
        aString+= " ";  //...a space,...
   	    aString+= inString; //...the string to log,...
        aString+= " ";  //...and a space...

        if ( theThrowable != null ) { //..and the exception if present... 
          aString+= theThrowable;
        	}
        
      	aString+= "\n";  //...and a final line terminator.

      	appendToOpenFileV(aString);  // Append it to log file.
        
   	  	if (consoleCopyEntryB || consoleCopyModeB) // Append to console if called for... 
   	  	  System.out.print(aString);

        lastMillisL= nowMillisL; // Saving present time as new last time.
        }

    private void appendToOpenFileV(String inString)
      /* This method writes to thePrintWriter, which must be open.  */
      { 
        thePrintWriter.print( inString );  // Append inString to file.
        appendedAtMsL= System.currentTimeMillis(); // Record time of close.
        theLockAndSignal.notifyingV(); 
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
	        debug("openFileIfClosedV() opened log file.");
        }
      }
    
    private synchronized Writer openWithRetryDelayFileWriter()
      throws IOException
      /* This method opens a FileWriter for the log file.
        If the open fails, it sleeps for 1 ms, and tries again.
        It repeats until the open succeeds.
        It returns the open FileWriter.
        */
      { 
        FileOutputStream theFileOutputStream= null;
        Writer resultWriter= null;
        while (true) { // Keep trying to open until it succeeds.
          try {
              theFileOutputStream= null;
              resultWriter= null;
              theLogFileLock= null;
              theFileOutputStream= new FileOutputStream( // Open for writing...
                  logFile,   // ...log file with this name...
                  true  // ...and write to end of file, not the beginning.
                  );
              FileChannel theFileChannel= theFileOutputStream.getChannel();
              theLogFileLock= theFileChannel.lock();
              theFileChannel.position(theFileChannel.size()); // Set for append.
              resultWriter= new OutputStreamWriter(theFileOutputStream);
              break; // Exit if open succeeded.
            } catch (IOException e) { // Open failed.
              System.out.println("open... 6 catch begin");
              if (theLogFileLock!=null) theLogFileLock.release();
              Closeables.closeWithoutErrorLoggingB(theFileOutputStream);
              Closeables.closeWithoutErrorLoggingB(resultWriter);
              uninterruptableSleepB( 1 ); // Pause 1 ms.
              openSleepDelayMsI++; //* Count the pause and the time.
              System.out.println("open... 7 catch end");
            }
          }
        return resultWriter;
        }

    private synchronized void closeFileAndSleepIfOpenV()
      /* This method is like closeFileAndDelayV() 
        but acts only if the log file is open.
        */
	    { 
	      if (thePrintWriter != null)  // Closing file if open. 
	        closeFileAndSleepV(); 
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
        uninterruptableSleepB(Config.LOG_MIN_CLOSE_TIME); // Block and pause.
        }

    private synchronized void closeFileV()
      /* This method closes thePrintWriter, 
        which closes everything associated with the log file.  
        */
      {
        debug("closeFileV() flushing, unlocking, and closing log file.");
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
