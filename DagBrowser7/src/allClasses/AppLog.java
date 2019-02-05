package allClasses;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static allClasses.AppLog.LogLevel.*;

public class AppLog extends EpiThread

  /* This class is for logging information from an app.  
    It is of a special design to provide the following features:
    * Logs strings provided by the application.
    * Thread-safe so multiple app threads may log to the same file
      and their log entries will be interleaved.
    ? Multiple app process instances may log to the same file
      and their log entries will be interleaved.
      Output from different app instances 
      are identified by different session numbers.
    * Log entries are stamped with the relative times since
      the previous entry of the same session.

		If any errors occur during logging they will be reported:
		* As an Exception occurrence reported to the err stream
		* As an error count later to the log, if possible.

    When executing logging-intensive parts of the app can cause
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

		///enh: Begin the transition to a logger 
      that doesn't need to be a static singleton and can be injected.
      Allow both static and injected-non-static, at least for a while.

    ///fix? MakeMultiprocessSafe: 
	    ///fix: When the presently kludgy buffered mode is enabled,
	      interleaving of log entries might not work correctly
	      when there are two running instances on the same computer,
	      as during updates, etc.  Might need to use separate files
	      which are combined and interleaved later.
	    ///fix? It might fail if multiple app instances try to log simultaneously.
	      Make log file be share-able in case two app instances
	      try to write to it at the same time.  See createOrAppendToFileV(..).
	    The best way to do this might be to make this class to use 
	    a separate temporary open log file 
	    for each thread its own thread for fast logging,
	    but atomically replaces these and copies/merges them together
	    periodically for convenient real-time viewing and debugging.

		///enh: Make the logging routines more orthogonal, in the following
		  dimensions:
		  * label on output: info/debug/error/exception
		  * output includes/does-not-include exception
		  * copy log file output to console, or not, except time-stamp
		  * re-throw exception and return vs. not returning
		 
		///ehn: To limit unwanted logging, it might make sense to have 
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
    	private volatile boolean thereHasBeenOutputB= false;
    	
    	private File logFile;  // Name of log file.
      private int theSessionI= 0;  // App session counter.
      private long lastMillisL; // Last time measured.
      private PrintWriter thePrintWriter = null; // non-null means file open.
        // Open means buffered mode enabled.
        // Closed means buffered mode disabled.
      private int openSleepDelayMsI= 0;
      private String processIDString= "";

    /* Debug Flags.  These are added or removed as needed during debugging
      when calls to logging methods should be called in 
      only very limited conditions.  //? 
     	*/
      public static boolean testingForPingB= false;
      private boolean debugEnabledB= true;
      private boolean consoleModeB= false; // When true, logging goes to console
        // as well as log file.  
      public LogLevel packetLogLevel= DEBUG;  // INFO; // DEBUG; 

    public void setIDProcessV( String processIDString )
    { this.processIDString= processIDString; }

    public void run()
      /* This method closes the file periodically so that
        new output is visible to developers for debugging and testing.
        Initially it will do nothing because thePrintWriter == null. 
        ///fix This is a kludge.
       	*/
    	{
    	  while (true) {
  	  	  if ( thePrintWriter != null ) // Temporarily close file if open
  	  	  	if (thereHasBeenOutputB) // and there has been new output.
	  		    	synchronized(this) { // Act only when it's safe. 
    	  	  		closeFileIfOpenV(); // This will cause flush.
		  		    	openFileIfClosedV(); // Prepare for next output.
				    	  }
  	  	  try {
  	  	    	Thread.sleep(5000); // 5 second pause.
	    	  	} catch(InterruptedException ex) {
	    	  	  Thread.currentThread().interrupt();
	    	  	} finally {
	    	  		closeFileIfOpenV(); ///fix Should this be here?
	    	  	}
    	  	}
    		}
    
    public boolean getAndEnableConsoleModeB()
	    { 
	    	boolean tmpB= consoleModeB; 
		    consoleModeB= true;
		    return tmpB;
	    	}

    public void restoreConsoleModeV( boolean oldConsoleEnabledB )
    	{ 
    		consoleModeB= oldConsoleEnabledB; 
    		}

    public synchronized void setBufferedModeV( boolean desiredBufferedModeB ) 
    	/* This method opens the file for buffered mode,
    	  and closes it for non-buffered mode.
        File-open means buffering will happen on any output.
        File-closed means any buffer has been flushed.
    	  */
	    {
        initializeIfNeededV();
        String bufferedModeLogString= "AppLog.setBufferedModeV(..), ";
        boolean actualBufferedModeB= ( thePrintWriter != null );
        bufferedModeLogString+= // Calculate whether mode is changing.
            ( desiredBufferedModeB == actualBufferedModeB )
            ? "already " // not changing
            : "being "; // is changing
	    	if ( desiredBufferedModeB )
		    	{ 
	    	    openFileIfClosedV();
		    	  info(bufferedModeLogString+"enabled.");
		    		}
		    	else
		      { 
            info(bufferedModeLogString+"disabled.");
		    	  closeFileIfOpenV();
		      	}
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
          logFile.delete();  //...then empty log file by deleting.
        openFileIfClosedV(); // Open file for use.
        logHeaderLinesV(); // Append the session header lines.
        }

    private void logHeaderLinesV()
      /* This initialization helper method outputs header lines 
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
    	  if (Config.clearLogFileB)
    	  	logFile.delete(); // DO THIS TO CLEAR LOG AND RESET SESSION COUNT!

    	  String sessionNameString= "session";
        File sessionFile=  // Identify session file name.
        		Config.makeRelativeToAppFolderFile( sessionNameString+".txt" );
        if ( ! logFile.exists() )  // If log file doesn't exist...
          sessionFile.delete();  // ...delete session file also.
        String sessionString = Confingleton.getValueString(sessionNameString);
        int sessionI= 0; // Default if Confingleton unreadable or unparseable.
        if ( sessionString != null )  // If session file exists...
          try { sessionI= Integer.parseInt(sessionString)+1; }
            catch ( NumberFormatException e ) { /* Ignore, using default. */ }
        sessionString= sessionI + "";  // Convert int to string.
        Confingleton.putValueV(sessionNameString, sessionString);
        return sessionI;
        }

    public void setLevelLimitV( LogLevel limitLogLevel )
    	{ maxLogLevel= limitLogLevel; }


    // Actual log methods start here.

    public void trace(String inString)
      /* This method is for tracing.  It writes only inString.
        */
      { 
				logB( TRACE, inString, null, false );
        }
  
    public void debug(String inString)
      /* This method writes a debug String inString to a log entry
        but not to the console.
        It is tagged as for debugging.
        */
      { 
    		if (debugEnabledB) 
    			logB( DEBUG, inString, null, consoleModeB );
        }

    public void info(String inString, boolean debugB)
      /* This method writes an information String inString to a log entry
        but not to the console.
        If theThrowable is not null then it displays that also.
        */
      { 
        info( inString, null, debugB ); 
        }

    public void info(String inString, Throwable theThrowable)
      /* This method writes an information String inString to a log entry
        but not to the console.
        If theThrowable is not null then it displays that also.
        */
      { 
    		info( inString, theThrowable, false ); 
        }

    public void info(String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        */
      { 
        info( inString, null, false); 
        }

    public void info(String inString, Throwable theThrowable, boolean consoleB)
      /* This method writes an information String inString to a log entry
        but not to the console.
        If theThrowable is not null then it displays that also.
        If consoleB==true then out ismade to console also.
        */
      { 
				logB( INFO, inString, theThrowable, consoleB );
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
        String wholeString= "EXCEPTION: " + inString + " : " + e ;

        System.err.println(wholeString);  // Send to error console.
        e.printStackTrace();

        error( wholeString );
        }
    
    public void error(String inString)
      { 
        error( inString, null); 
        }
    
    public void error(String inString, Throwable theThrowable)
      /* This method writes an error String inString to a log entry,
        and also to the console error stream?  Disabled temporarily.
        An error is something with which the app should not have to deal.
        Response is to either retry or terminate.
        */
      { 
    		logB( ERROR, inString, theThrowable, true);
        }
    
    public void warning(String inString)
      // This method writes a severe error String inString to a log entry.
      { 
    		logB( WARN, inString, null, false );
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
    	* String inString: message to be displayed.
    	* Throwable theThrowable: an exception to be displayed, of not null.
    	* boolean consoleB: controls whether a copy of the entry goes to console.
    		
      */

    public synchronized boolean logB(
    		LogLevel theLogLevel, 
    		String inString, 
    		Throwable theThrowable, 
    		boolean consoleB )
	    {
	  		boolean loggingB= logB(theLogLevel);
	  		if ( loggingB )
	      		logV( theLogLevel, inString, theThrowable, consoleB );
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

    public synchronized void logV(
    		LogLevel theLogLevel, 
    		String inString, 
    		Throwable theThrowable, 
    		boolean consoleB )
      /* The buck stops here.  This method does not delegate to
        another method is the family.

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
        to the console if consoleB is true.

        ///opt Replace String appends by StringBuilder appends, for speed?
        ///enh Add stackTraceB which displays stack,
          on console and in log, if true.
        */
    { 
      if  // Acting based on whether file is open (buffered) or closed (not).
        ( thePrintWriter == null ) // File is closed.
        //// appendToClosedFileV( inString ); // Open, append to it, and close it.
        { 
          openFileIfClosedV();
          //// appendToOpenFileV( inString ); // Appending string to output.
          logToOpenFileV(theLogLevel,inString,theThrowable,consoleB );
          closeFileIfOpenV();
          }
        else // File is open.
        ////appendToOpenFileV( inString ); // Just write to it, leaving it open.
        logToOpenFileV(theLogLevel,inString,theThrowable,consoleB );
      }

    public synchronized void logToOpenFileV( //// convert to use open file.
        LogLevel theLogLevel, 
        String inString, 
        Throwable theThrowable, 
        boolean consoleB )
      {
        initializeIfNeededV(); 
    		long nowMillisL= System.currentTimeMillis(); // Saving present time.

        String aString= ""; // Initialize String to empty, then append to it...
        aString+= theSessionI;  //...the session number,...
        aString+= processIDString;
        aString+= ":";  //...and a separator.
        aString+= String.format(  // ...time since last output,...
        		"%1$5d", nowMillisL - lastMillisL
        		);
        aString+= " ";  //...a space,...
        
   	  	if (consoleB || consoleModeB) // ...a console flag if called for... 
   	  		aString+= "CONSOLE-COPIED ";
   	  	
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

      	//// getAppLog().appendToFileV( aString );  // Append it to log file.
        appendToOpenFileV(aString);  // Append it to log file.
        
   	  	if (consoleB || consoleModeB) // Append to console if called for... 
        	System.err.print(aString);

        lastMillisL= nowMillisL; // Saving present time as new last time.
        }

    
    // Raw log file manipulation methods.
    
    /*  ////
    private synchronized void appendToFileV(String inString)
      /* Appends a raw string to the log file.
        It can be used to append as little as a single character.
        If inString is a log entry then it must be a complete entry,
        including session #, time-stamp, and newlines.


        If the log file is closed on entry, it will be closed again on exit.
        
        If the log file is open on entry, it will still be closed on exit.
        */
    /*  ////
      { 
    	  if  // Acting based on whether file is open (buffered) or closed (not).
    	    ( thePrintWriter == null ) // File is closed.
    	  	appendToClosedFileV( inString ); // Open, append to it, and close it.
    	  	else // File is open.
          appendToOpenFileV( inString ); // Just write to it, leaving it open.
    	  }
    */  ////
      
    /*  ////
    private synchronized void appendToClosedFileV( String inString )
      /* This method appends inString to the closed log file.
        If the log file doesn't exist then it is created first.
        The file will be closed again before the method exits.

        ///enh MakeMultiprocessSafe: Maybe it is now.  
          If not then it should be made multiprocess safe so 
          concurrent apps can access the log file.
        */
    /*  ////
      {
	    	openFileIfClosedV();
        appendToOpenFileV( inString ); // Appending string to output.
      	closeFileIfOpenV();
        }
    */  ////

    private void appendToOpenFileV(String inString)
      /* This method writes to thePrintWriter, which must be open.  */
      { 
        thePrintWriter.print( inString );  // Append inString to file.
        thereHasBeenOutputB= true;
        }

    private void openFileIfClosedV()
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
                openFileWriter()  // ...a FileWriter to opened log file.
                )
            );
        } catch (IOException e) {
          System.err.println("AppLog error opening PrintWriter...: "+e);
          }
	      if (openSleepDelayMsI != 0) { // Log open failures.
	        debug("openFileIfClosedV() opened after "
	          +openSleepDelayMsI
	          +" failures and 1 ms sleeps");
	        openSleepDelayMsI= 0; // Reset for later.
	        }
        }
      }
    
    private FileWriter openFileWriter()
      throws IOException
      /* This method opens a FileWriter for the log file.
        If the open fails, it sleeps for 1 ms, and tries again.
        It repeats until the open succeeds.
        */
      { 
        FileWriter resultFileWriter;
        while (true) {
          try {
              resultFileWriter= new FileWriter(  // Open log file for writing...
                logFile,   // ...with this name...
                true  // ...and write to end of file, not the beginning.
                );
              break; // Exit if open succeeded.
            } catch (IOException e) { // Open failed.
              uninterruptableSleepB( 1 ); // Pause 1 ms.
              openSleepDelayMsI++; //* Count the pause and the time.
            }
          }
        return resultFileWriter;
        }

    private void closeFileIfOpenV()
      /* This method closes thePrintWriter if it's open, 
        which closes everything associated with the log file.  
        If the log file is already closed then it does nothing.
        */
	    { 
	      if (thePrintWriter != null) {  // Closing file if open.
	        thePrintWriter.close(); // Close file.
	        thePrintWriter= null; // Indicate file is closed.
	        thereHasBeenOutputB= false; // Reset unflushed output flag.
	        }
	      }

  }  
