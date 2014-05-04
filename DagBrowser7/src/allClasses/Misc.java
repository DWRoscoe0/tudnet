package allClasses;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

//import java.util.ArrayList;
//import java.util.Collections;

//import javax.swing.tree.TreePath;

public class Misc
  /* This class contains miscellaneous useful code
    for debugging and logging.
    */
  { // class Misc 
  
    // Singleton code.
    
      private Misc()  // Private construtor prevents external instantiation.
        {}

      private static final Misc theMisc =  // Internal singleton builder.
        new Misc();

      public static Misc getMisc()   // Returns singleton.
        { return theMisc; }
  
    // Miscellaneous variables.

      public static final boolean ReminderB= false;  // true;

    // Logger class simulator code.
      /* ??? Make log file be shareable in case two app instances
        try to write to it at the same time.
        */
    
      private int theSessionI= 0;
      private File logFile;
      
      private void logFileInitializeV()
        throws IOException  // Do-nothing put-off.
        {
          logFile=  // Identify log file.
            AppFolders.resolveFile( "log.txt" );
          theSessionI= getSessionI();
          boolean appendB=  // Controls whether to start with empty file.
            true;
          createOrAppendToFileV( // Create new or append to log file.
            appendB, ""  // false means new file, true means append.
            );
          dbgOut( ""); 
          dbgOut( ""); 
          dbgOut( "------------------- NEW LOG FILE SESSION -------------------");
          //dbgOut("log.txt created.");            
          }
    
      private int getSessionI()
        throws IOException  // Do-nothing put-off.
        /* This method calculates what the log session number should be,
          records it in the file session.txt,
          and returns the session number.
          */
        { 
      		int sessionI= 0;
          String sessionString = "0";
      		File sessionFile=  // Identify session file.
            AppFolders.resolveFile( "session.txt" );
          if ( ! logFile.exists() )  // If log file doesn't exist...
            sessionFile.delete();  // ...delete session file also.
          if ( ! sessionFile.exists() )  // If session file doesn't exist...
            {
              logFile.delete();  // ...delete session file also.
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
          //System.out.println("sessionString is: "+sessionString);
          FileWriter theFileWriter = null;
          sessionString= sessionI + "";
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
    
      public void info(String inString)
        { dbgOut( "INFO:"+inString ); }
    
      public void severe(String inString)
        { dbgOut( "SEVERE:"+inString ); }

      public void log(String inString) // Appends to log file.
        { createOrAppendToFileV( true, inString ); }
        
      private void createOrAppendToFileV( boolean appendB, String inString )
        /* This method creates a new log file,
          or empties an existing log file if appendB == false.
          In any case it appends inString to the file.
          ??? Needs analysis of close.
          */
        {
          if ( ! appendB)  // If not appending then...
            logFile.delete();  // ...delete the file first.
          PrintWriter thePrintWriter = null;
          try {
              thePrintWriter = new PrintWriter(
                //new BufferedWriter(new FileWriter(logFile, appendB))
                new BufferedWriter(new FileWriter(logFile, true))
                );
              thePrintWriter.print( inString );
          }catch (IOException e) {
              System.err.println(e);
          }finally{
              if(thePrintWriter != null){
                  thePrintWriter.close();
              }
          }
        }

    /* Debugging code.
      Now this code is called by Logger code.
      Later the roles will be reversed and it will call Logger code.
      */

      private static int dbgCountI= 0;  // Output counter.
  
      public static void noOp( ) {} // Convenience for setting breakpoints.

      public static void dbgProgress() 
        /* Use this method line to display a single character
          to verify that code is being executed.  
          */
        { 
          Misc.getMisc().log( "!" );  // Debug. 
          }
      
      public static String componentInfoString( Component InComponent )
        /* This method returns a string with 
          useful information about a component.
          */
        {
          if ( InComponent == null )
            return " null ";
            
          String ResultString= "";
          ResultString+= " " + InComponent.hashCode(); 
          ResultString+= " " + InComponent.getClass().getName();
          if ( InComponent.getName() != null )
            ResultString+= " name:"+InComponent.getName(); 
          return ResultString;
          }
      
      private static boolean dbgEventDoneB= false;  // Set true to output Debug.txt.
      
      public static void dbgEventDone()
        /* This is a convenient place to output state information
          after a user input event is processed or 
          after any other significant event occurs.
          This is useful because the Java event loop is not accessible.
          */
        { 
        if ( dbgEventDoneB )
          {
            dbgOut( "Writing Debug.txt" ); 
            MetaFile.writeDebugFileV( MetaRoot.getRootMetaNode( ));
            noOp( );
            }
          }
      
      public static void dbgConversionDone()
        /* This is called after a conversion from IDNumber to MetaNode.  */
        { 
          //dbgOut( "Conv." ); 
          //MetaFile.writeDebugState( );  // ??? Display for debugging.
          noOp( );
          }
      
      public static void dbgOut( String InString )
        /* This outputs to the console a new line containing a counter, 
          which is incremented, followed by InString.
          */
        { 
          String wholeString=
            (Misc.getMisc().theSessionI + ":")
            + dbgCountI++ 
            + ": "
            + InString 
            + "\n"
            ;
          Misc.getMisc().log( wholeString );

          noOp( );
          }

    static // static/class initialization.
      // Done here so all subsection variables are created.
      {
        try 
          { Misc.getMisc().logFileInitializeV(); }
        catch(IOException e)
          { System.out.println("\nIn Misc initialization:"+e); }
        }

    } // class Misc 
