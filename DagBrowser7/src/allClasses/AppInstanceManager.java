package allClasses;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;

import static allClasses.Globals.*;  // appLogger;

public class AppInstanceManager {

  /* This class manages instances of the app that uses it,  
    It does this by doing these things:
    * It prevents more than one app instance 
      being active simultaniously.
    * It performs updates such that the standard folder
      always contains a copy of the latest version of the app.
    * It re-runs the app from the standard folder whenever possible.

    Normally managingInstancesThenNeedToExitB() is called at start-up.
    It detects whether there is another running instance
    by trying to open a particular network socket.
    If it couldn't open the socket it is because the other instance did.
    In this case it connects to the socket and 
    sends the current app path to it.
    If it could open the socket then it sets up a Listener on it
    for messages from instances of the app run later.
    It returns true if an older instance is running, false otherwise.

    This running instance code was originally based on code at 
    http://www.rbgrn.net/content/43-java-single-application-instance

    Presently if this manager wants this app to exit, 
    it calls exit(0) after calling doShutdown().
    This work but it might be better to actually return from 
    managingInstancesThenNeedToExitB(), which
    would then call doShutdown() and exit normally.

    ??? Eliminate redundant manipulation of argStrings left over from
    before it was passed into constructor.

    ??? Write a state-machine description summarizing the entire process.
    
    ??? Finish partial conversion to state-machine state naming conventions.
    
    ??? One running instance can start another by using either:
    * Java Runtime.exec(..)
    * Java ProcessBuild.start()

    ??? This code should  be more robust.
    It needs to handle when the socket link is not working.
    Maybe if indications are that there is no older app instance
    it launches another instance of itself, 
    with a flag command line argument to prevent infinite recursion,
    to test the whole communication system.
    To be extra safe against intentional errors as well as
    unintentional ones, it might make sense to have a special
    configuration file used only for instance [and update] management.
    */

  /* Old Singleton code.  This is made thread-safe and fast with the
    Initialization on Demand Holder (IODH) idiom.
    */

  private Shutdowner theShutdowner;

  public AppInstanceManager(  // Constructor.  was private for Singleton.
      String[] inArgStrings,
      Shutdowner theShutdowner
      )
    {
      this.theShutdowner= theShutdowner;

      identifyAppFilesV(   // Identifying file containing this app.
        inArgStrings  
        );

      URI thisAppURI = null;
      try {
          thisAppURI = Infogora.class.getProtectionDomain().
            getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      thisAppFile= new File( thisAppURI );
      standardAppFile= AppFolders.resolveFile( "Infogora.jar" );
      }

  private File thisAppFile= null;  // This running app's file name.

  private File standardAppFile= null;  // Standard folder's...
    // ...app's file name.

  /* Variables about the candidate/app, if any.
    This is another app that called this running app.
    It came from either the first argument to main() or
    from an app that was run while this running app was active.
    */

    private String[] argStrings= null;  // Array of all arguments.

    private File argAppFile= null;  // First arg as a File name.
  
  private ServerSocket instanceServerSocket = null;

  public boolean managingInstancesThenNeedToExitB( )
    /* This main method manages both Running and File instances of the app.
      inArgStrings is an array of strings which was used 
      to start this app instance and might contain information 
      about the instance what started this instance, for call-back purposes.
      Normally this method is called at app start-up.
      It returns true if this app should exit because its job is done,
        except possibly for calling another instance at shutdown
        to continue management operations.
      It returns false if this app instance should continue with 
        normal start-up, because no other running instance was detected.
      */
    {
      appLogger.info(  // Logging app jar file information.
        "App jar file is dated " + thisAppDateString()
        );

  	  //identifyAppFilesV(   // Identifying file containing this app.
      //  inArgStrings  
      //  );
      
      boolean appShouldExitB= true;  // Set default return for app exit.

      if // Manage running instances and exit if needed.  
        ( managingRunningInstancesThenNeedToExitB( ) )
        ; // Leave appShouldExitB == true to cause exit.
      else if // Manage file instances and exit if needed.  
        ( managingFileInstancesThenNeedToExitB( ) )
        ; // Leave appShouldExitB == true to cause exit.
      else // All instance management completed.  No exit is needed.
        appShouldExitB= false; // Prevent exit, allowing normal start-up.

      return appShouldExitB;  // Return whether app should exit.
      }

  public void tryExitForChainToUpdateFromNewerArgAppV()
    /* If this app is the app in the standard folder.
      and the arg app is newer than this app,
      then exit and run the arg app.
      Otherwise return.
      
      This method is meant be called by a timer
      to check for the appearance of a new version of the argAppFile
      and to do an update if it appears.
      */
    {
      //appLogger.debug("tryExitForChainToUpdateFromNewerArgAppV().");
      if ( argAppFile != null )  // argAppFile has been defined.
      {
      	//appLogger.debug(
      	//  "tryExitForChainToUpdateFromNewerArgAppV(): argAppFile!=null."
      	//  );
        if   // Arg app approved to update app in standard folder.
          ( updaterApprovedB() )
          {
            // User approval or authenticity checks would go here.
            appLogger.info("Detected an approved updater.  Chaining to it");
            setJavaCommandAndExitV(  // Chain to arg app to do the copy.
              argAppFile.getAbsolutePath() 
              );
            }
        }
      }

  private boolean updaterApprovedB()
    /* If this app is the app in the standard folder.
      and the arg app is newer that this app,
      then return true,
      otherwise return false.
      */
    {
      boolean resultB= false;  // Assume false.
      //appLogger.debug(
      //  "updaterApprovedB() Files: "+thisAppFile+" "+standardAppFile
      //  );
      if // This app is the app in the standard folder.
        ( thisAppFile.equals( standardAppFile ) )
        {
          long argAppFileLastModifiedL= argAppFile.lastModified();
          long thisAppFileLastModifiedL= thisAppFile.lastModified();
          //appLogger.debug(
          //  "updaterApprovedB() times: "
          //  +argAppFileLastModifiedL
          //  +" "
          //  +thisAppFileLastModifiedL
          //  );

          if // The arg app is newer than this app.
            ( argAppFileLastModifiedL > thisAppFileLastModifiedL )
            resultB= true;  // Override result.
          }
      return resultB;
      }
    
  private void identifyArgsV( String[] inArgStrings )
    /* This method saves inArgStrings, the arguments used to run this app.
      By convention 
      * arg0 is the path to the file containing this app.
      * arg1 is the path to the file containing the app which ran this app.
      It calculates argAppFile from that.
      For now the other arguments are unused.
      */
    {
      argStrings= inArgStrings;
      if  // Extract calling app if there is one.
        (argStrings.length>0)
	      {
		      argAppFile= new File(argStrings[0]);  // Convert string to File.
	      	}
      }

  private void identifyAppFilesV( String[] inArgStrings )
    /* This method determines the jar file from which this app was loaded,
      and the jar file that should contain the app in the standard folder.
      ?? inArgStrings.readString
      */
    {
      identifyArgsV( inArgStrings );  // Save arg string[s].
      logInputsV();
      }

  // Running Instance management code.

    private AppInstanceListener theAppInstanceListener= // Listener ...
      null;  // ...to perform an action if desired.

    public static final int getInstancePortI() 
      { return PortManager.getDiscoveryPortI(); }
      // 56944;  // A high number I chose at random.
    
    public boolean managingRunningInstancesThenNeedToExitB( )
      /* Normally this method is called at app start-up.
        It detects whether there is an older running instance
        by trying to open a particular network socket.
        If it can't open the socket it is because an older instance did.
        In this case it connects to the socket and sends to it
        the path of this app's jar file.
        If it can open the socket then it starts an InstanceManagerThread
        to monitor the socket for future messages from 
        newer running instances of the app.
        It returns true if this app should exit 
          to let the older running instance decide what to do.
        It returns false if this app should continue with normal start-up,
          because the there is no other older running instance.
        */
      { 
        boolean appShouldExitB= false;  // Set default return for no app exit.

        try { // Try to start listening, indicating no other instances.
        	
          appLogger.info(
            "Local Host IP: " + 
            InetAddress.getLocalHost().getHostAddress() // Get real IP. 
            );
        	
          appLogger.info(
            "About to listen for a newer app packet on port " + 
            getInstancePortI()
            );
          instanceServerSocket =  // Try opening listener socket.
            new ServerSocket(
              ///getInstancePortI(), 10, InetAddress.getLocalHost()
              getInstancePortI(), 10, InetAddress.getLoopbackAddress() 
              );
          { // Setup InstanceManagerThread.
            InstanceManagerThread theInstanceManagerThread=
              new InstanceManagerThread();
            theInstanceManagerThread.setName("AppInstances");
            theInstanceManagerThread.start();  // Start thread...
            // ...on the socket just opened.
            } // Setup InstanceManagerThread.

          theShutdowner.addShutdownerListener(
            new ShutdownerListener() {
              public void doMyShutdown() 
              {
                try {
                  instanceServerSocket.close();
                } catch (IOException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                } // Close flag socket.
                }
              });
          ; // Leave appShouldExitB false for no app exit.
        } catch (UnknownHostException e) { // Error.  What produces this??
          appLogger.severe( "error:"+e.getMessage()+ e );
          ; // Leave appShouldExitB false for no app exit.
        } catch (IOException e) { // This error means port # already in use.
          appLogger.info(
            "Port "+getInstancePortI()+" is already taken."+
            "  Sending signal-packet to older app using it.");
          appShouldExitB=  // Set exit to the result of...
            sendPathInPacketB( );  // ...sending packet to port.
        }
      return appShouldExitB;  // Return whether or not app should exit.
      } 

    private boolean sendPathInPacketB( )
      /* This managingRunningInstancesThenNeedToExitB(..) sub-method 
        tries to send this app's path
        to an existing older running app instance via the socket
        to which that app is listening.
        It returns true if it succeeds and to indicate that
          this app should exit to let the other app handle things.
        It return false to indicate a sending error,
          and this app should not exit.
        */
      {
        try {
            Socket clientSocket =  // Create socket for send.
              new Socket(
                ///InetAddress.getLocalHost(), getInstancePortI()
                InetAddress.getLoopbackAddress(), getInstancePortI()
                );
            OutputStream out =   // Get its stream.
              clientSocket.getOutputStream();
            String outputString= thisAppFile.getAbsolutePath();
            appLogger.info("Sending: "+outputString);
            out.write(  // Send path to other app via stream.
              //thisAppFile.getAbsolutePath().getBytes() 
              outputString.getBytes()
              );
            out.close();  // Close stream.
            clientSocket.close();  // Close socket.
            appLogger.info("Successfully sent signal-packet.");
            return true;  // Packet sent, meaning success and exit.
        } catch (UnknownHostException e1) {
            System.out.println("error"+e1.getMessage()); //, e1);
            return false;  // Error in send, so do not exit.
        } catch (IOException e1) {
            System.out.println("error:Error connecting to local port for single instance notification");
            System.out.println("error"+e1.getMessage()); //, e1);
            return false;  // Error in send, so do not exit.
        }
      }

    public void setAppInstanceListener(AppInstanceListener listener) 
      {
        theAppInstanceListener = listener;
        }

    private void fireNewInstance() 
      {
        if (theAppInstanceListener != null) 
          theAppInstanceListener.appInstanceCreatedV();
        }

    class InstanceManagerThread extends Thread
      /* This class contains the run() method which waits for
        messages from other running app instances and processes them.  */
      {
        public void run() 
          /* This method, for as long as the communication socket is open,
            waits for messages from other running app instances and 
            processes them.  Processing includes:
            * Firing any associated Listener.  
            * Setting argAppFile to be the received message
              converted to a File object.
            */
          {
            boolean socketClosed = false;
            while (!socketClosed) {
              if (instanceServerSocket.isClosed()) {
                socketClosed = true;
              } else {
                try {
                  Socket clientSocket = instanceServerSocket.accept();
                  BufferedReader inBufferedReader = 
                    new BufferedReader(
                      new InputStreamReader(clientSocket.getInputStream()
                      )
                    );
                  String readString = inBufferedReader.readLine();
                  {
                    appLogger.info(
                      "Received a newer app signal-packet:" + readString
                      );
                    identifyArgsV( new String[] {readString} ) ;
                    logInputsV();
                    managingFileInstancesThenNeedToExitB( );  // Might not return.
                    fireNewInstance();  // Call AppInstanceListener it does.
                    }
                  inBufferedReader.close();
                  clientSocket.close();
                } catch (IOException e) {
                  socketClosed = true;
                }
              }
              }
            }
        }

  // File Instance management code.

    private boolean managingFileInstancesThenNeedToExitB( )
      /* Normally this method is called at app start-up,
        but only if there is not another running instance.
        It begins the process of checking for and installing or updating
        the app file in the Infogora standard folder to make it
        the latest version, and then runs it.
        It returns true if this app instance should exit 
          because its job is done but it to be continued
          by another running instance which has been started.
        It returns false if this app instance should 
          continue with normal start-up, 
          because instance management has determined that
          no further work is needed.
        The logic in the code below can result in as few as 
        1 app command to be run,
        in the case of a normal start-up from the standard folder,
        to as many as 4 app commands run, 
        for the case when a new app version is run
        outside the standard folder.
        depending on the conditions of the first app run.
        */
      { 
        boolean appShouldExitB= false;  // Set default return for no app exit.

        if ( argAppFile == null )  // The app command was arg-less.
          { // Handle the arg-less command possibilities.
            if (runningAppIsStandardAppB())  // Was from standard folder.
              { // Prepare for normal startup.
                appLogger.info("We are starting plain app from standard folder.");
                ;  // Keep appShouldExitB false for normal startup.
                } // Prepare for normal startup.
              else  // Is argless but not from standard folder.
              { // Try processing a plain (arg-less) app command.
                tryExitForInstallToStandardFolderV();
                tryExitForUpdateToStandardFolderV();
                tryExitForChainToIdenticalAppInStandardFolderV();
                appLogger.info("Exhausted without-arg possibilities.");
                } // Try processing plain (arg-less) app commands.
            } // Handle the arg-less command possibilities.
          else  // The app command has one (or more?) arguments.
          { // Handle app commands with argument[s].
            tryExitForChainToApprovedUpdaterV();
            tryExitForUpdateToStandardFolderV();
            // Others to be added.
            appLogger.info("Exhausted with-arg possibilities.");
            } // Handle app commands with argument[s].
          
        return appShouldExitB;  // Return whether or not app should exit.
        }

    private boolean runningAppIsStandardAppB()
      /* If the file of the running instance app
        is the same as the app file in the standard folder 
        then return true, otherwise return false.
        */
      {
        return thisAppFile.equals( standardAppFile );
        }

    private void tryExitForInstallToStandardFolderV()
      /* If the standard folder has no app in it then this method 
        copies the jar file of the running app to the standard folder,
        exits this app, and runs the installed app.
        Otherwise it returns.
        */
      {
        if // A version of this app is not already in standard folder.
          ( ! standardAppFile.exists() )  // The file doesn't exist.
          {
            appLogger.info("Trying to install.");
            copyAppToStandardFolderAndChainToIt();
            }
        }

    private void tryExitForUpdateToStandardFolderV()
      /* If the standard folder has an app jar file in it,
        but it is older than the running app,
        then this method updates it by replacing 
        the jar file in the standard folder
        with the jar file for this app,
        exits this app, and runs the updated app.
        Otherwise it returns.
        */
      {
        if // This apps file is newer that the one in standard folder.
          ( thisAppFile.lastModified() > standardAppFile.lastModified() )
          {
            appLogger.info("Updating.");
            copyAppToStandardFolderAndChainToIt();
            }
        }

    private void tryExitForChainToIdenticalAppInStandardFolderV()
      /* If the running app is not in the standard folder,
        but it appears identical to the one in the standard folder,
        then this method exits this app, 
        and runs the app in the stanard folder.
        Otherwise it returns.
        */
      {
        if // This app is in the standard folder.
          ( ! thisAppFile.equals( standardAppFile ) )
          if // The date stamps are equal.
            ( thisAppFile.lastModified() == standardAppFile.lastModified() )
            {
              appLogger.info("Running identical app in standard folder.");
              setJavaCommandAndExitV( standardAppFile.getAbsolutePath() );
              }
        }

    private void tryExitForChainToApprovedUpdaterV()
      /* If this app is the app in the standard folder.
        and the arg app is newer that this app,
        then run the arg app.
        Otherwise return.
        */
      {
        if   // Arg app approved to update app in standard folder.
          ( updaterApprovedB() )
          {
            // User approval or authenticity checks would go here.
            appLogger.info("An approved updater had signalled.");
            setJavaCommandAndExitV(  // Chain to arg app to do the copy.
              argAppFile.getAbsolutePath() 
              );
            }
        }

    private void copyAppToStandardFolderAndChainToIt()
      /* This method tries to copy this app's jar file 
        to the standard folder, start it as a Process, and exit.
        If copying fails it keeps retrying unless the thread
        is interrupted, in which case it returns.
        */
      {
        if  // This app is not from a jar file.
          (! thisAppFile.getName().endsWith(".jar"))
          { // Probably a class file running in Eclipse.  Do normal startup.
            appLogger.info( "Not a jar file, so not exiting.");
            }
          else
            while  // Keep trying until copy success and exit.
              (!Thread.currentThread().isInterrupted())
              try 
                {
                  appLogger.info( 
                    "Copying jar file to standard folder."
                    );
                  Files.copy(
                      thisAppFile.toPath()
                      ,standardAppFile.toPath()
                      ,StandardCopyOption.COPY_ATTRIBUTES
                      ,StandardCopyOption.REPLACE_EXISTING
                      );
                  setJavaCommandAndExitV( standardAppFile.getAbsolutePath() );
                  }
                catch (Exception e)  // Other instance probably still running.
                  { 
                    appLogger.info( 
                      "copyAppToStandardFolderAndChainToIt().\n  "
                      +e.toString()
                      +"  Will retry after 1 second." 
                      ); 
                    Misc.snoozeV(1000);  // Wait for other instance.
                    }
        }
    
    private void setJavaCommandAndExitV( String argString )
      /* This method is equivalent to a 
        setJavaCommandForExitV( argString )
        followed by exit(0).
        It never returns.
        */
      {
        setJavaCommandForExitV( argString );  // Setting up command.

        theShutdowner.doShutdown();  // Starting that command.

        System.exit(0);  // Exitting.
          // Before this would use ShutdownHook to trigger command.
        }

    private void setJavaCommandForExitV
      ( String argString )
      /* This method sets up the execution of a runnable jar file 
        whose name is JarFilePathString as a Process.  
        It will execute when this app terminates,
        probably by calling System.exit(0).
        */
      {
        String [] commandOrArgStrings=  // Allocation for all Process args. 
          new String [
            2  // java -jar
            +1 // (.jar file to run)
            +1 // (.jar file of this app)
            ] ;

        commandOrArgStrings[0]= // Store path of java command.
          System.getProperty("java.home") + 
          File.separator + 
          "bin" + 
          File.separator + 
          "java";
        commandOrArgStrings[1]= "-jar";  // Store java -jar option.
        commandOrArgStrings[2]=  // Store path of .jar file to run
          argString;
        commandOrArgStrings[3]=  // Store path of this app;s .jar.
          thisAppFile.getAbsolutePath();  

        theShutdowner.setCommandV(  // Setting String as command.
          commandOrArgStrings
          );
        }

  // Other miscellaneous code.

    private void logInputsV()
      {
        String logStriing= "";  //Declare and initialize string to be logged.
        { // Add parts to the string to be logged.
          logStriing+= "Inputs: ";
          logStriing+= "\n  Standard: " + standardAppFile;
          logStriing+= "\n  This:     " + thisAppFile;
          logStriing+= "\n  Arguments: ";
          for ( String argString : argStrings )
            logStriing+= "\n    " + argString;
          }
        
        appLogger.info( logStriing );  // Log the completed String.
        }

    public String thisAppDateString()
      /* Returns the lastModified time of this app's jar file as a String.  
        This is for use in appending to names of old versions of files
        so that they can all be stored in the same folder,
        and to display in the title bar of the app
        to distinguish one version from another.
        */
      {
        SimpleDateFormat aSimpleDateFormat= 
          new SimpleDateFormat("yyyyMMdd.HHmmss");
        String appModifiedString= 
          aSimpleDateFormat.format(thisAppFile.lastModified());
        return appModifiedString;
        }

}
