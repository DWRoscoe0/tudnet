package allClasses;

//import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class AppLog 

  /* This class is for logging information from an app.  
    This class needs some work.
    
    MadeMultiprocessSafe: ??? It might fail if multiple app instances
      try to log simultaniously.

    Windows8AntiMalwareSlowsItDown: ???
      It can run much slower when the file is long.
      This might be because of the Windows8 AntiMalware task.
      Apparently it scans the log.txt file after every close
      because when the file is short it causes little delay,
      but if file is big it slows progress of the program.
      Change to close less often?
    */

  {

    /* ??? Make log file be shareable in case two app instances
      try to write to it at the same time.  See createOrAppendToFileV(..).
      */
  
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

    static // static/class initialization for logger.
      // Done here so all subsection variables are created.
      {
        try 
          { AppLog.getAppLog().logFileInitializeV(); }
        catch(IOException e)
          { System.out.println("\nIn AppLog initialization:"+e); }
        }
    
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
        
        ??? This method is messy and long.  Fix.
        */
      { 
        //logFile.delete();  // !!! DO THIS TO CLEAR ESSION ???
        
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
  
    public void debug(String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        It is tagged as for debugging.
        */
      { 
        appendEntry( "DEBUG: "+inString ); 
        }
  
    public void info(String inString)
      /* This method writes an information String inString to a log entry
        but not to the console.
        */
      { 
        appendEntry( "INFO: "+inString ); 
        }
  
    public void error(String inString)
      /* This method writes an error String inString to a log entry
        and also to the console error stream.
        */
      { 
        String wholeString= "ERROR: "+inString;
        System.err.println(wholeString);  // Send to error console.
        appendEntry( wholeString );  // Send to log. 
        }
  
    public void severe(String inString)
      /* This method writes a severe error String inString to a log entry
        and also to the console error stream.
        */
      { 
        String wholeString= "SEVERE: "+inString;
        System.err.println(wholeString);  // Send to error console.
        appendEntry( wholeString );  // Send to log. 
        }

    public /* static */ void appendEntry( String InString )
      /* This appends to the log file a new log entry.
        It contains the app session,
        the entry counters, which is incremented, 
        followed by InString.
        */
      { 
        String aString= ""; // Initialize String to empty, then append...
        aString+= AppLog.getAppLog().theSessionI;  //...the session number,...
        aString+= ":";  //...and a seperator.
        aString+= System.currentTimeMillis(); //...present real time,...
        aString+= ": ";  //...a seperator and space,...
        aString+= InString; //...the string to log,...
        aString+= "\n";  //...and a line terminator.

        AppLog.getAppLog().appendRawV( aString );  // Append it to log file.
        }

    public void appendRawV(String inString)
      /* Appends a raw string to the log file.
        It can be used to append as little as a single character.
        */
      { createOrAppendToFileV( inString ); }
      
    private void createOrAppendToFileV( String inString )
      /* This method creates the log file if it doesn't exist.
        Then it appends inString to the file.

        ??? MadeMultiprocessSafe: Needs to be made multiprocess safe so 
          concurrent apps can access the log file.
        */
      {
        PrintWriter thePrintWriter = null;
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
            thePrintWriter.print( inString );  // Append inString to file.
          } catch (IOException e) {
            System.err.println(e);
          } finally {
            if (thePrintWriter != null) {  // File is (still) open.
                thePrintWriter.close();  // Close it.
                }
            }
        }

  }
