package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class InfogoraStarter

  /* This class is used as a process to start the Infogora app process.
    It is in the same jar file as the Infogora class.
    Both this class and the Infogora class have their own main(..) methods.
    
    This cless was created because of the characteristics of the
    7Zip SFX Modules used as an decompressor and launcher.
    It decompresses the app into a temporary directory,
    starts the specified app process, waits for that process to terminate,
    and then deletes the temporary directory and its contents.
    It doesn't understand that the process that it starts
    might start other processes just as dependent on that temporary directory.
    This class, by waiting for all child processes to terminate
    before terminating itself, guarantees that the temporary directory
    is deleted only when it is safe to do so.  
    
    ///enh Maybe generalize to use different termination conditions.
      Instead of using -delegateeExiting and -delegatorExiting,
      use -StartingProcess and -processExiting, and keep a count.
      When the count reaches 0, this process may exit.
   */

  {

    private static Lock theReentrantLock= new ReentrantLock();
      /* This is Lock variable is used for thread safety.  */
    private static LoopbackMonitorThread theLoopbackMonitorThread;

    public static void main(String[] argStrings)
      /* This method is the app starter's entry point.  
        It does the following, in the following order:
        * It sets a default Exception handler.
        * It starts the Infogora app as another process.
        * It waits for that Infogora app process to terminate.
        * It waits for all of the Infogora app's child processes to terminate.
        * It terminates itself.

        */
      { // main(..)
        System.out.println(
            "main(..) DISCOVER CAUSE OF DISPLAY OF CONSOLE WINDOW.");
        
        Config.clearLogFileB= true;
        Process theProcess= null;
        appLogger.setIDProcessV("Starter");
        appLogger.setBufferedModeV( true ); // Enabling fast buffered logging.
        
        DefaultExceptionHandler.setDefaultExceptionHandlerV(); 
        appLogger.info(true,
            "InfogoraStarter.main() beginning. ======== STARTER IS STARTING ========");
        SystemState.initializeV(
            InfogoraStarter.class, new CommandArgs(argStrings));

        setupLoopbackPortB();
        long starterPortL=
            theLocalSocket.getServerSocket().getLocalPort();
        appLogger.info("run(): starterPortL="+starterPortL);
        String [] commandOrArgStrings= new String[] {
            ( // Path of java command in array.
              "jre1.8.0_191\\bin\\java.exe"
              /*   /// "." +
              File.separator +
              "bin" +
              File.separator + 
              "java.exe" */   ///
              ),
            "-cp", // java.exe -cp (classpath) option.
            "Infogora.jar", // Path of .jar file to run
            "allClasses.Infogora" // entry point.
            ///fix? add argStrings at end?
            ,"-starterPort" // For exit notifications which 
            ,""+starterPortL // use this port on loopback interface.
            };
        appLogger.debug("InfogoraStarter.main() starting Infogora process.");
        theProcess= ProcessStarter.startProcess(commandOrArgStrings);
        if (theProcess == null)
          appLogger.error("InfogoraStarter.main() Process start failed.");
          else 
          { // Wait for termination of process and all its descendants.
            appLogger.info(
              "InfogoraStarter.main() waiting for process termination.");
            try {theProcess.waitFor();} catch (InterruptedException e) {}
            appLogger.info(
                "InfogoraStarter.main() First child process has terminated.");
            try {theLoopbackMonitorThread.join();} 
              catch (InterruptedException e) {}
            appLogger.info(
                "InfogoraStarter.main() Last child process is terminating.  "
                + "Waiting 1 second before exiting.");
            EpiThread.uninterruptableSleepB( 1000 ); // Extra time for safety.
            }
        
        appLogger.info(true, "InfogoraStarter.main() calling exit(0). "
            + "======== STARTER IS ENDING ========");
        appLogger.setBufferedModeV( false ); // Disabling buffered logging.
        System.exit(0); // Will kill any remaining unknown threads running??
        } // main(..)

    private static LocalSocket theLocalSocket= new LocalSocket();
      // Used for instance existence testing and communication. 

    private static boolean setupLoopbackPortB()
      /* This method is called at app start-up.
        It tries to setup the feedback port on the loopback interface
        including a thread to process input to it.
        */
      { 
        if (theLocalSocket.bindB(0)) 
          {
            setupMonitoringThreadV();
            }
          else
          {
            appLogger.error("setupLoopbackPortB() no ports available.");
            }
        return false;   ///
        }

    private static void setupMonitoringThreadV()
      /* This method sets up a thread for monitoring
        the now bound LocalSocket for connections and input.
        It also sets up termination of that thread using
        theShutdownerListener.
       */
      {
        theLoopbackMonitorThread= new LoopbackMonitorThread();
        theLoopbackMonitorThread.setName("LoopbackMonitor");
        theLoopbackMonitorThread.start();
        }
    
    static class LoopbackMonitorThread extends Thread
      /* This class contains the run() method which waits for
        messages from other apps and processes them.  */
      {
        public void run()
          /* This method, for as long as the ServerSocket is open,
            waits for connections from other running apps 
            and processes single line messages from them.
            */
          {
            appLogger.info("run(): beginning.");
            while (! theLocalSocket.isClosedB()) {
              try {
                theLocalSocket.acceptV(); // Wait for connection or exception.
                { // Do a software update check using data from the socket.
                  theReentrantLock.lock(); // Wait until we have lock.
                  try {
                      theLocalSocket.inputFromConnectionV();
                      appLogger.debug("run(): got data.");
                      processConnectionDataV(
                          theLocalSocket.getCommandArgs());
                    } finally {
                      theReentrantLock.unlock(); // Release the lock.
                    }
                  }
                theLocalSocket.closeConnectionV();
                } 
              catch (IOException e) // Accepting of connection failed.
                { // We must be terminating.
                  theLocalSocket.closeAllV(); // Make certain all are closed.
                  }
              } // while
            appLogger.info("Socket closed, ending.");
            }

        private void processConnectionDataV(CommandArgs theCommandArgs)
          throws IOException
          /* This method processes the input gotten by processLineFromSocketV().
            It tries to interpret that input as 
            a path to a file which is a possible software update.
            One switch, the -delegateeExiting, is a signal to exit
            because it means all processes spawned directly or indirectly
            by this one have or are about to exit, so this process should also. 
            */
          {
            appLogger.info("processConnectionDataV(..)"+theCommandArgs);
            
            if ( theCommandArgs.switchPresent("-delegatorExiting"))
              appLogger.info(
                  "processConnectionDataV(..) -delegatorExiting received");
            if ( theCommandArgs.switchPresent("-delegateeExiting")) {
              theLocalSocket.closeAllV(); // Close all to exit loop and thread.
              }
            { // Log any unprocessed arguments.
              String[] targetStrings= theCommandArgs.targets(); 
              if (targetStrings.length>0) // If unprocessed args...
                appLogger.error( // log them as an error.
                  "processConnectionDataV(..), unused arguments:\n  "
                  + Arrays.toString(targetStrings));
              }
            }
          
        } // class InstanceManagerThread
    
  }
