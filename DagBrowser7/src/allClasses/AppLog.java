package allClasses;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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

    When executing logging-intensive parts of the can cause
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
			  in ancestor DataNode Lit instances.  Subclasses:
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

    	public static final LogLevel defaultMaxLogLevel= INFO;
    	private static LogLevel maxLogLevel= defaultMaxLogLevel;
    	  // The app may create and use their own maximum variables,
    	  // or set this variable and call methods which use it.
    	  // Calls to those methods must use this level or less for log output.
    	
    	private volatile boolean thereHasBeenOutputB= false;
    	
    	private File logFile;  // Name of log file.
      private int theSessionI= 0;  // App session counter.
      private long lastMillisL; // Last time measured.
      private PrintWriter thePrintWriter = null; // non-null means file open.

    /* Debug Flags.  These are added or removed as needed during debugging
      when calls to logging methods should be called in 
      only very limited conditions.  //? 
     	*/
      public static boolean testingForPingB= false;
      private boolean debugEnabledB= true;
      private boolean consoleModeB= false;

    static // static/class initialization for logger.
      // Done here so all subsection variables are created.
      {
        try 
          { 
	        	AppLog.getAppLog().logFileInitializeV(); 
	          }
        catch(IOException e)
          { System.err.println("\nIn AppLog initialization:"+e); }
        }

    public void run()
      /* This method closes the file periodically so that
        new output is visible to developers for debugging and testing.
       	*/
    	{
    	  while (true) {
  	  	  if ( thePrintWriter != null ) // Temporarily close file if open
  	  	  	if (thereHasBeenOutputB) // and there has been new output.
	  		    	synchronized(this) { // Act only when it's safe. 
    	  	  		closeFileV(); // This will cause flush.
		  		    	openFileV(); // Prepare for next output.
				    	  }
    	  	  try {
    	  	    	Thread.sleep(5000); // 5 second pause.
		    	  	} catch(InterruptedException ex) {
		    	  	  Thread.currentThread().interrupt();
		    	  	} finally {
		    	  		closeFileV();
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

    public synchronized void setBufferedModeV( 
    		boolean desiredBufferedModeB 
    		) 
    	/* This method opens the file for buffered mode,
    	  and closes it for non-buffered mode.
    	  */
	    {
	    	if ( desiredBufferedModeB )
		    	{ openFileV();
		    	  info("AppLog.setBufferedModeV(..), enabled.");
		    		}
		    	else
		      { info("AppLog.setBufferedModeV(..), disabled.");
		    	  closeFileV();
		      	}
	    	}
    
    private void logFileInitializeV()
      throws IOException  // Do-nothing put-off.
      {
        logFile=  // Identify log file name.
        		Config.makeRelativeToAppFolderFile( "log.txt" );
        theSessionI= getSessionI();  // Get app session number.
        if (theSessionI == 0)  // If this is session 0...
          logFile.delete();  //...then empty log file by deleting.
        logV( 
        		"<--< This is an absolute time.  Later ones are relative times."
        		); 
        logV( "" ); 
        logV( 
        		"=================== LOG FILE SESSION #"
        		+ theSessionI
        		+ " BEGINS ==================="
        		);
        logV( "" ); 
        }
  
    private int getSessionI()
      throws IOException  // Do-nothing put-off.
      /* This method calculates what the app session number should be,
        records it in the file session.txt,
        and returns that session number.
        If either the session file or the log file do not exist
        then the session number will be 0.
        Otherwise the session number will be 
        the number stored in the session file incremented by one.
        
        ?? This method is messy and long.  Fix.
        */
      { 
        //logFile.delete(); // DO THIS TO CLEAR LOG AND RESET SESSION COUNT!
        
        File sessionFile=  // Identify session file name.
        		Config.makeRelativeToAppFolderFile( "session.txt" );
        if ( ! logFile.exists() )  // If log file doesn't exist...
          sessionFile.delete();  // ...delete session file also.
        int sessionI= 0;
        String sessionString = "0";

        if ( ! sessionFile.exists() )  // If session file doesn't exist...
          {
            sessionI= 0;  // Start at session # 0.
            }
          else
          try // Get session number from file and increment it.
            {
              FileReader sessionFileReader = 
                new FileReader(sessionFile);
              char[] chars = new char[(int) sessionFile.length()];
              sessionFileReader.read(chars);
              sessionString = new String(chars);
              sessionFileReader.close();
              sessionI= Integer.parseInt(sessionString)+1;
            } catch (IOException e) {
                e.printStackTrace();
                }
        FileWriter theFileWriter = null;
        sessionString= sessionI + "";  // Convert int to string.
        try { // Write [new] session # string to session file.
            theFileWriter = new FileWriter(sessionFile);
            theFileWriter.write(sessionString);
        }catch (IOException e) {
            System.err.println(e);
        }finally{
            if(theFileWriter != null){
                theFileWriter.close();
            }
        }
        return sessionI;
        }

    public void setLevelLimitV( LogLevel limitLogLevel )
    	{ maxLogLevel= limitLogLevel; }
  
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

      * logB( theLogLevel ) returns true if theLogLevelis less than maxLogLevel.
        This displays nothing.  It is used to control what is displayed.
      * logB( theLogLevel, ... ) returns true and creates a log entry with
       	theLogLevel and the remaining parameters if theLogLevel 
       	is less than maxLogLevel.  Otherwise it just returns false.
      * logV( ... ) creates a log entry from the parameters unconditionally.
   
      Various combinations of parameters are possible from the following set:
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
      /* This is the most capable logging methods.
        It contains all possible parameters.
        All the logging methods eventually call this one.

        This method create one log entry and appends it to the log file.
        It could be multiple lines if any of the parameters
        evaluate to Strings which contain newlines.
        The log entry always begins with
        * the app session number,
        * milliseconds since the previous entry,
        * theLogLevel if not null, 
        * the thread name,
        * theThrowable if not null.

        This method also sends a copy to the console if consoleB is true.

        ///enh Replace String appends by StringBuilder appends, for speed?
        ///enh Add stackTraceB which displays stack,
          on console and in log, if true.
        */
      { 
    		long nowMillisL= System.currentTimeMillis(); // Saving present time.

        String aString= ""; // Initialize String to empty, then append...
        aString+= theSessionI;  //...the session number,...
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

      	AppLog.getAppLog().appendV( aString );  // Append it to log file.
        
   	  	if (consoleB || consoleModeB) // Append to console if called for... 
        	System.err.print(aString);

        lastMillisL= nowMillisL; // Saving present time as new last time.
        }

    
    // Raw log file manipulation methods.
    
    public synchronized void appendV(String inString)
      /* Appends a raw string to the log file.
        It can be used to append as little as a single character.
        If inString is a log entry then it must be complete,
        including session #, time-stamp, and newlines.
        If the file doesn't exist or exists but is closed
        then it calls createOrAppendToFileV(..).
        If the file exists and is open then it calls writeToOpenFileV(..).
        */
      { 
    	  if ( thePrintWriter == null ) // Acting based on whether file is open.
    	  	createOrAppendToFileV( inString ); // File not open.
    	  	else
          writeToOpenFileV( inString ); // File open.
    	  }
      
    private synchronized void createOrAppendToFileV( String inString )
      /* This method creates the log file if it doesn't exist.
        Then it appends inString to the file.
        If the file already exists then it must be closed.
        The file will be closed when the method exits.
        
        If there is an error appending to the file then it is supposed to 
        insert an error message into the file before writing inString.  
        For example, this might happen if another program,
        such as another instance of this app, is accessing the file.
        I don't recall ever seeing this happen.

        ///ehn MakeMultiprocessSafe: Maybe it is now.  
          If not then it should be made multiprocess safe so 
          concurrent apps can access the log file.
        */
      {
	    	openFileV();
        writeToOpenFileV( inString ); // Appending string to output.
      	closeFileV();
        }

    private void openFileV()
      /* This method opens thePrintWriter and 
        everything else associated with the log file.  
        */
    {
      if (thePrintWriter == null) {  // Opening file if closed.
	      try {
          thePrintWriter =  // Prepare...
            new PrintWriter(  // ...a character output stream..
              new BufferedWriter(  // ...with buffering to...
                new FileWriter(  // ...a writable file...
                  logFile,   // ...with this name...
                  true  // ...and write to end of file, not the beginning.
                  )
                )
              );
        } catch (IOException e) {
          System.err.println("AppLog error opening file: "+e);
          }
        }
      }

    private void closeFileV()
      /* This method closes thePrintWriter if it's open, 
        which closes everything associated with the log file.  
        */
	    { 
	      if (thePrintWriter != null) {  // Closing file if open.
	        thePrintWriter.close(); // Close file.
	        thePrintWriter= null; // Indicate file is closed.
	        thereHasBeenOutputB= false; // Reset unflushed output flag.
	        }
	      }

    private void writeToOpenFileV(String inString)
      /* This method writes to thePrintWriter, which must be open.  */
	    { 
	    	thePrintWriter.print( inString );  // Append inString to file.
	    	thereHasBeenOutputB= true;
	      }

  }  
