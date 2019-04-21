package allClasses;

import static allClasses.Globals.appLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class InfogoraStarter

  /* ///enh: Remove this class?  
    It is no longer being used since discovering that
    if there is no mixing of exe and jar processes then
    each exe process has its own temporary directory.
    
    ///fix: The 7ZipSFXModule seem to leave empty temporary directories
      if more than one folder is active at a time.  
      It seems to be holding other temporary directories that it isn't using.
      Have app delete any empty temporary directories on exit
      to prevent their number growing beyond a few.
    
    This class is used as a process to start the Infogora app process.
    It is in the same jar file as the Infogora class.
    Both this class and the Infogora class have their own main(..) methods.
    
    This cless was created because of the characteristics of the
    7Zip SFX Modules used as a decompressor and launcher.
    It decompresses the app into a temporary directory,
    starts the specified app process, waits for that process to terminate,
    and then deletes the temporary directory and its contents.
    It doesn't understand that the process that it starts
    might start other processes just as dependent on that temporary directory.
    This class, by waiting for all descendant child processes to terminate
    before terminating itself, guarantees that the temporary directory
    is deleted only when everything has stopped using it.

    Legal switches input in the command line from the 7zip SFX launcher are;
    -userDir : followed by user directory from which 
      the 7zip SFX launcher command was run.
    -tempDir : followed by the directory into which 
      this app was placed before running it.
    If these switches are not present, 
    the starter was not run from the 7zip SFX launcher. 
    In this case, presently, execution is aborted.

    Switches output in command line to 
    a descendant Infogora app process that is being started:
    -starterPort : followed by port number descendant processes should use
      to send messages back to this process.
    -userDir : passed through, see above.
    -tempDir : passed through, see above.

    Legal switches input through the -starterPort TCP socket from
    descendant Infogora app processes are;
    -delegatorExiting : indicates that a descendant process which
      delegated its job to its own descendant, is exiting.
      Its descendant might still need the temporary directory to exist. 
    -delegateeExiting : indicates that a descendant process which
      did not delegate its job to its own descendant, is exiting.
      Therefore it should be safe for the temporary directory to be deleted. 

    ///fix When Infogora.exe delegates to another Infogora.exe,
      the temporary directory doesn't needs to be preserved.
      Figure out a different logic for this.
      
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
          appLogger= new AppLog(new File( // Constructing logger.
              new File(System.getProperty("user.home") ),Config.appString));
          appLogger.setIDProcessV("Starter");
          // AppLog should now be able to do logging.
        toExit: {
          System.out.println(
              "main(..) DISCOVER CAUSE OF DISPLAY OF CONSOLE WINDOW.");
          
          Process theProcess= null;
          DefaultExceptionHandler.setDefaultExceptionHandlerV(); 
          appLogger.info(true,
              "InfogoraStarter.main() beginning. ======== STARTER IS STARTING ========");
          CommandArgs theCommandArgs= new CommandArgs(argStrings); 
          AppSettings.initializeV(InfogoraStarter.class, theCommandArgs);
          if (theCommandArgs.switchValue("-userDir") == null) {
            appLogger.error("InfogoraStarter.main() Not an exe file launch!");
            break toExit;
            }
          setupLoopbackPortB();
          long starterPortL=
              theLocalSocket.getServerSocket().getLocalPort();
          appLogger.info("run(): starterPortL="+starterPortL);
          
          ArrayList<String> theArrayListOfStrings= new ArrayList<String>();
          theArrayListOfStrings.add(System.getProperty("java.home") + 
              File.separator + "bin" + File.separator + "java.exe");
          theArrayListOfStrings.add("-cp"); // java.exe -cp (classpath) option.
          theArrayListOfStrings.add("Infogora.jar"); // Path of .jar file to run
          theArrayListOfStrings.add("allClasses.Infogora"); // entry point.
          theArrayListOfStrings.add("-starterPort"); // For feedback...
            theArrayListOfStrings.add(""+starterPortL); // ...using this port.
          theArrayListOfStrings.add("-userDir"); // Pass userDir switch.
            theArrayListOfStrings.add(theCommandArgs.switchValue("-userDir"));
          theArrayListOfStrings.add("-tempDir"); // Pass tempDir switch.
            theArrayListOfStrings.add(theCommandArgs.switchValue("-tempDir"));
          theArrayListOfStrings.add("SENTINEL"); // ///dbg
  
          appLogger.debug("InfogoraStarter.main() starting Infogora process.");
          theProcess= ProcessStarter.startProcess(
              theArrayListOfStrings.toArray(new String[0]));
          if (theProcess == null) { // Handle start failure.
            appLogger.error("InfogoraStarter.main() Process start failed.");
            break toExit;
            }
          appLogger.info(
            "InfogoraStarter.main() waiting for process termination.");
          try { // Waiting for terminations.
            int exitCodeI= theProcess.waitFor();
            if (exitCodeI != 0 ) { // Handle termination error.
              appLogger.error(
                "InfogoraStarter.main() child process exit code="+exitCodeI);
              break toExit;
              }
            appLogger.info(
                "InfogoraStarter.main() First child process has terminated.");
            theLoopbackMonitorThread.join();
            appLogger.info(
                "InfogoraStarter.main() Last child process is terminating.  "
                + "Waiting 1 second before exiting.");
            Thread.sleep( 1000); // Wait extra time for safety.
          } catch (InterruptedException e) {
            appLogger.info("InfogoraStarter.main() "
                + "wait for process termination was interrupted.");
            break toExit;
          } // Waiting for terminations.
          } // toExit:
        appLogger.info(true, "InfogoraStarter.main() calling exit(0). "
            + "======== STARTER IS ENDING ========");
        appLogger.closeFileIfOpenB(); // Close log for exit.
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
              appLogger.info( "processConnectionDataV(..) "
                  + "-delegatorExiting received, ignoring.");
            if ( theCommandArgs.switchPresent("-delegateeExiting")) {
              appLogger.info("processConnectionDataV(..) "
                  + "-delegateeExiting received, will exit.");
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
