
package allClasses;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class AppLog 

  /* This class is for logging information from an app.  
    This class needs some work.
    
    ?? MakeMultiprocessSafe: 
      It might fail if multiple app instances try to log simultaneously.
      Make log file be share-able in case two app instances
      try to write to it at the same time.  See createOrAppendToFileV(..).

    ?? Slowing might happen because of  
      * Anti-malware Service Executable  
      * Microsoft Windows Search Indexer
      App can run much slower when the log file is long.
      This might be because of these tasks.
      Apparently it scans the log.txt file after every file close
      because when the file is short it causes little delay,
      but if file is big it slows progress of the program.
      Change to close less often?
    */

  {
  
    // Singleton code.
    
      private AppLog()  // Private constructor prevents external instantiation.
        {}

      private static final AppLog theAppLog=  // Internal singleton builder.
        new AppLog();

      public static AppLog getAppLog()   // Returns singleton.
        { return theAppLog; }

    // Variables.
      private File logFile;  // Name of log file.
      private int theSessionI= 0;  // App session counter.
      long startedAtMillisL;  // Time when initialized.
      long lastMillisL; // Last time measured.

    static // static/class initialization for logger.
      // Done here so all subsection variables are created.
      {
        try 
          { 
	        	AppLog.getAppLog().logFileInitializeV(); 
	          }
        catch(IOException e)
          { System.out.println("\nIn AppLog initialization:"+e); }
        }

    /* Debug Flags.  These are added or removed as needed during debugging
      when calls to logging methods should be called in 
      only very limited conditions. 
     */
    
    static boolean testingForPingB= false;
    static boolean focusChangeCheckingB= true;

    
    private void logFileInitializeV()
      throws IOException  // Do-nothing put-off.
      {
        startedAtMillisL=  // Save time when we started.
          System.currentTimeMillis();
        logFile=  // Identify log file name.
          AppFolders.resolveFile( "log.txt" );
        theSessionI= getSessionI();  // Get app session number.
        if (theSessionI == 0)  // If this is session 0...
          logFile.delete();  //...then empty log file by deleting.
        appendEntry( "<--< This is an absolute time.  Later ones are relative."); 
        appendEntry( ""); 
        appendEntry( "=================== NEW LOG FILE SESSION ===================");
        appendEntry( ""); 
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
        try {
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
  
    public void warning(String inString)
      // This method writes a severe error String inString to a log entry.
      { 
        String wholeString= "WARNING: "+inString;

        appendEntry( wholeString );  // Send to log. 
        }

    public synchronized void appendEntry( String inString )
      /* This appends to the log file a new log entry.
        It contains the app session number,
        milliseconds since the previous entry, the thread name,
        and finally inString.
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

        AppLog.getAppLog().appendRawV( aString );  // Append it to log file.

        lastMillisL= nowMillisL; // Saving present time as new last time.
        }

    Component savedFocusedComponent= null;
    
    @SuppressWarnings("unused") 
    private void appendAnyFocusChangeV()
	    {
	    	if (focusChangeCheckingB)
		    	{
	          Component focusedComponent= // get Component owning the focus.
	              KeyboardFocusManager.
	                getCurrentKeyboardFocusManager().getFocusOwner();
	          if ( savedFocusedComponent != focusedComponent )
	          	{
	          		AppLog.getAppLog().appendRawV( 
	          				"-----DEBUG: FOCUS CHANGE FROM: "
	          			  +Misc.componentInfoString(savedFocusedComponent)
	          				+ " TO: "
	          			  +Misc.componentInfoString(focusedComponent)
	          				+"-----\n" 
	          				);  // Append it to log file.
	              savedFocusedComponent= focusedComponent;
	          		}
		    		}
	      }

    public void appendRawV(String inString)
      /* Appends a raw string to the log file.
        It can be used to append as little as a single character.
        */
      { createOrAppendToFileV( inString ); }
      
    private synchronized void createOrAppendToFileV( String inString )
      /* This method creates the log file if it doesn't exist.
        Then it appends inString to the file.
        
        If there is an error appending to the file then
        it will insert an error message into the file before
        writing inString.  This might happen if another program,
        such as another instance of this app, is accessing the file.

        ?? MakeMultiprocessSafe: Maybe it is now.  
          If not then it should be made multiprocess safe so 
          concurrent apps can access the log file.
        */
      {
    	  int errorCountI= 0;
        PrintWriter thePrintWriter = null;
        do {
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
	            if (errorCountI > 0) // Handling any errors.
		            { // Appending error count to file.
			            thePrintWriter.print( 
			            		"ERRORS ("+errorCountI+") writing following to log file."
			            		);
			            errorCountI= 0;
		            	}
	            thePrintWriter.print( inString );  // Append inString to file.
	          } catch (IOException e) {
	            System.err.println("AppLog error: "+e);
	            errorCountI++;
	          } finally {
	            if (thePrintWriter != null) {  // Closing file if open.
	                thePrintWriter.close();  
	                }
	            }
        	} while (errorCountI > 0);
        }

  }  
