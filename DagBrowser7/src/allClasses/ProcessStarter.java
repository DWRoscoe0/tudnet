package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Arrays;

public class ProcessStarter 

  {

    // Code for defining and starting other processes and ending this one.
  
    public static String[] argStrings = null; // Command to be executed at exit.
      // If null then no command is to be executed.
  
    public static void setCommandV( String... inArgStrings )
      /* This method sets to inArgStrings the array of Strings which
        defines the command Process to be created and executed 
        at shut-down time by ProcessBuilder.
        If at shutdown time inArgStrings is null 
        then no command will be executed.
        */
      {
        appLogger.info( 
          "ProcessStarter.setCommandV(..): " 
          + Arrays.toString(inArgStrings)
          );
        
        argStrings = inArgStrings; 
        }
  
    public static Process startProcess(String... inArgStrings)
      /* This method calls a Process built with 
        a ProessBuilder operating on 
        the String argument array inArgStrings.
        It does nothing if inArgStrings is null.
  
        ?? This could use some work.
        In previous version it redirected 
        the Process's stdout and stderr to 
        this Process's stdout.
        Until this redirection ended it could cause an access violation
        which would prevent replacement of the file from which 
        this Process was loaded!
        */
      {
        Process resultProcess= null;
        if  // Executing an external command if
          ( ( inArgStrings != null )  // command arguments were defined
             && ( inArgStrings.length > 0 ) // and there is at least one.
             )
          try {
              appLogger.info( 
                "ProcessStarter.startAProcessV(..) w: " 
                + Arrays.toString(inArgStrings)
                );
              ProcessBuilder myProcessBuilder= // Build the process. 
                new ProcessBuilder(inArgStrings);
              
              resultProcess= // Start the process.
                  myProcessBuilder.start();
  
              appLogger.info( "ProcessStarter.startAProcessV(..): succeeded." ); 
            } catch (IOException e) {
              appLogger.exception( "ProcessStarter.startAProcessV(..)",e ); 
            }
        return resultProcess;
        }

}
