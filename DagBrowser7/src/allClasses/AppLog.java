
package allClasses;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class AppLog 

  /* This class is for logging information from an app.  
    It is of a special design to provide the following features:
    * Logs strings provided by the application.
    * Multiple app instances can log to the same file
      and their log entries will be interleaved.
    * Output from different app instances 
      are identified by different session numbers.
    * Log entries are stamped with the relative times since
      the previous entry of the same session.

		If any errors occur during logging they will be reported:
		* As an Exception occurrence reported to the err stream
		* As an error count later to the log, if possible.

    This class is under development.

    ///fix: Slowing of speed-sensitive parts of the app might happen because of:
      * Anti-malware Service Executable  
      * Microsoft Windows Search Indexer
      * File io.
      App can run much slower when the log file is long.
      This might be because of these tasks.
      Apparently it scans the log.txt file after every file close
      because when the file is short it causes little delay,
      but if file is big it slows progress of the program.
      Fix by:
      * Closing less often so most logging would be to buffer.
      * Always write to open files which are copied to main file in background.

    ///enh MakeMultiprocessSafe: 
      It might fail if multiple app instances try to log simultaneously.
      Make log file be share-able in case two app instances
      try to write to it at the same time.  See createOrAppendToFileV(..).
    */

  {
  
    // Singleton code.
    
      private AppLog()  // Private constructor prevents external instantiation.
        {}

      private static final AppLog theAppLog=  // Internal singleton builder.
        new AppLog();

      public static AppLog getAppLog()   // Returns singleton logger.
        { return theAppLog; }

    // Variables.
      private File logFile;  // Name of log file.
      private int theSessionI= 0;  // App session counter.
      private long lastMillisL; // Last time measured.
      private PrintWriter thePrintWriter = null; // non-null means file open.

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

    /* Debug Flags.  These are added or removed as needed during debugging
      when calls to logging methods should be called in 
      only very limited conditions. 
     */
    
    public static boolean testingForPingB= false;

    public synchronized void setBufferedModeV( 
    		boolean desiredBufferedModeB 
    		) 
    	/* This method opens the file for buffered mode,
    	  and closes it for non-buffered mode.
    	  */
	    {
	    	if ( desiredBufferedModeB )
		    	{ openFileV();
		    	  info("setBufferedModeV(..), enabled.");
		    		}
		    	else
		      { info("setBufferedModeV(..), disabled.");
		    	  closeFileV();
		      	}
	    	}
    
    private void logFileInitializeV()
      throws IOException  // Do-nothing put-off.
      {
        logFile=  // Identify log file name.
          AppFolders.resolveFile( "log.txt" );
        theSessionI= getSessionI();  // Get app session number.
        if (theSessionI == 0)  // If this is session 0...
          logFile.delete();  //...then empty log file by deleting.
        appendEntry( 
        		"<--< This is an absolute time.  Later ones are relative times."
        		); 
        appendEntry( "" ); 
        appendEntry( 
        		"=================== NEW LOG FILE SESSION ==================="
        		);
        appendEntry( "" ); 
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
          AppFolders.resolveFile( "session.txt" );
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
  
    private boolean debugEnabledB= true;

    public void debug(String inString)
      /* This method writes a debug String inString to a log entry
        but not to the console.
        It is tagged as for debugging.
        */
      { 
    	  if (debugEnabledB)
    	  	appendEntry( "DEBUG: "+inString ); 
        }
  
    public void info(String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        */
      { 
        appendEntry( "INFO: "+inString ); 
        }
  
    public void exception(String inString, Exception e)
      /* This method writes a Java exception String inString to a log entry
        and also to the console error stream for Exception e.
        */
      { 
        String wholeString= "EXCEPTION: " + inString + " : " + e ;

        System.err.println(wholeString);  // Send to error console.
        e.printStackTrace();

        appendEntry( wholeString );  // Send to log. 
        }
    
    public void error(String inString)
      /* This method writes an error String inString to a log entry
        and also to the console error stream.
        */
      { 
        String wholeString= "ERROR: "+inString; // Build error output string.

        //System.err.println(wholeString);  // Send to error console.

        appendEntry( wholeString );  // Send to log. 
        }
    
    public void consoleInfo(String inString)
      /* This method writes an error String inString to a log entry
        and also to the console error stream.
        */
      { 
        String wholeString= "CONSOLE-INFO: "+inString; // Build error output string.

        System.err.println(wholeString);  // Send one copy to error console.

        appendEntry( wholeString );  // Send one copy to log. 
        }
  
    public void warning(String inString)
      // This method writes a severe error String inString to a log entry.
      { 
        String wholeString= "WARNING: "+inString;

        appendEntry( wholeString );  // Send to log. 
        }

    public void appendEntry( String inString )
      /* This appends to the log file a new log entry line.
        It contains the app session number,
        milliseconds since the previous entry, the thread name,
        and finally inString.
        ///enh Replace String appends by StringBuilder appends, for speed.
        */
      { 
    	  //appendAnyFocusChangeV();
    		long nowMillisL= System.currentTimeMillis(); // Saving present time.

        String aString= ""; // Initialize String to empty, then append...
        aString+= AppLog.getAppLog().theSessionI;  //...the session number,...
        aString+= ":";  //...and a seperator.
        aString+= String.format(  // ...time since last output,...
        		"%1$5d", nowMillisL - lastMillisL
        		);
        aString+= " ";  //...a space,...
        aString+= Thread.currentThread().getName(); // the name of thread,
        aString+= " ";  //...a space,...
        aString+= inString; //...the string to log,...
        aString+= "\n";  //...and a line terminator.

        AppLog.getAppLog().appendV( aString );  // Append it to log file.

        lastMillisL= nowMillisL; // Saving present time as new last time.
        }

    public synchronized void appendV(String inString)
      /* Appends a raw string to the log file.
        It can be used to append as little as a single character.
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

        ?? MakeMultiprocessSafe: Maybe it is now.  
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
      /* This method closes thePrintWriter if it's opn, 
        which closes everything associated with the log file.  
        */
	    { 
	      if (thePrintWriter != null) {  // Closing file if open.
	        thePrintWriter.close(); // Close file.
	        thePrintWriter= null; // Indicate file is closed.
	        }
	      }

    private void writeToOpenFileV(String inString)
      /* This method writes to thePrintWriter, which must be open.  */
	    { 
	    	thePrintWriter.print( inString );  // Append inString to file.
	      }

  }  
