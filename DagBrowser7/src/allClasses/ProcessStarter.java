package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Arrays;

public class ProcessStarter 

  {

    // Code for defining and starting other processes.
  
    public static Process startProcess(String... inArgStrings)
      /* This method calls a Process built with 
        a ProessBuilder operating on 
        the String argument array inArgStrings.
        It does nothing if inArgStrings is null.
        It returns the built and started process if it succeeds,
        otherwise it returns null. 
  
        It inherits the standard IO streams of the current process.
         */
      {
        Process resultProcess= null;
        if  // Executing an external command if
          ( ( inArgStrings != null )  // command arguments were defined
             && ( inArgStrings.length > 0 ) // and there is at least one.
             )
          try {
              appLogger.info( 
                "ProcessStarter.startProcessV(..):\n  " 
                + Arrays.toString(inArgStrings)
                );
              ProcessBuilder myProcessBuilder= // Build the process. 
                new ProcessBuilder(inArgStrings);
              myProcessBuilder.inheritIO(); // Inherit standard IO streams.
              
              resultProcess= // Start the process.
                  myProcessBuilder.start();
  
              appLogger.info( "ProcessStarter.startProcessV(..): succeeded." ); 
            } catch (IOException e) {
              appLogger.exception( "ProcessStarter.startProcessV(..)",e ); 
            }
        return resultProcess;
        }

}
