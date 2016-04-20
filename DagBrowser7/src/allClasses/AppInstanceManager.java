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

  /* This class detects, communicates, and manages 
    running instances and file instances of the app.

    It tries to maintain the following conditions:
    * A maximum of one instance running at any given moment.
    * The running instance should have been run from the standard folder.
    * The file in the standard folder and the running instance
      should be the newest one detected. 

    It maintains these conditions by doing various things, such as:
    * When first run if it detects another running instance then
      it communicates its presence to the one already running 
      and then terminates.
    * When first run if it does not detect another running instance,
      and it was not run from the standard folder, 
      and there is not a file instance in the standard folder
      which is at least as new as this one,
      then it copies its file to the standard folder,
      then runs the one in the standard folder and terminates.
    * If while running it detects a newer file instance of itself,
      then it runs that one and terminates itself.
    * If while running it receives a message indicating
      the presence of a new running instance but it is not a newer version,
      then it does a move-to-front.
    * If while running it receives a message indicating
      the presence of a new running instance and it is a newer version,
      then it runs that one and terminates itself.

    The method managingInstancesThenNeedToExitB() does all of this.
    It is supposed to be called at app start-up.
    It detects whether there is another running instance
    by trying to open a particular network socket.
    If it couldn't open the socket it is because the other instance did.
    In this case it communicates its presence to the other running instance by
    connecting to the socket and sending this app's path to it.
    If it could open the socket then it sets up a Listener on it
    for messages from instances of the app which might be run later.

    It returns true to indicate that this app is shutting down
    because another instance, running or to be run, will handle things.
    It returns false if it is the only instance and should continue running.

    This running instance code was originally based on code at 
    http://www.rbgrn.net/content/43-java-single-application-instance
    
    ?? Eliminate redundant manipulation of argStrings left over from
    before it was passed into constructor.

    ?? Write a state-machine description summarizing the entire process.

    ?? The network code should  be more robust.
    It needs to handle when the socket link is not working.
    Maybe if indications are that there is no older app instance
    it launches another instance of itself, 
    with a flag command line argument to prevent infinite recursion,
    to test the whole communication system.
    To be extra safe against intentional errors as well as
    unintentional ones, it might make sense to have a special
    configuration file used only for instance [and update] management.
    */

  private Shutdowner theShutdowner;

  /* Variables about the candidate/app, if any.
    This is another app that called this running app.  It came from either:
    * the first argument to main() when the other app started this app, or
    * through the instanceServerSocket from app run after this running app.
    */

    private String[] argStrings= null;  // Array of all arguments.

    private File argAppFile= null;  // First arguments converted to a File name.

  private File thisAppFile= null;  // File name of this running app.

  private File standardAppFile= null;  // File name of app in standard folder.
  
  private ServerSocket instanceServerSocket = null;

  public AppInstanceManager(  // Constructor.
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

  public boolean managingInstancesThenNeedToExitB( )
    /* This main method manages both Running and File instances of the app.
      This method is called at app start-up.
      inArgStrings is an array of strings which was used 
      to start this app instance and might also contain, for call-back purposes,
      the path of the instance that started this instance.

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
      
      boolean appShouldExitB= true;  // Set default return for app exit.

      if ( tryARunningInstanceActionB( ) )
        ; // Leave appShouldExitB == true to cause exit.
      else if ( tryAFileInstanceActionB( ) )
        ; // Leave appShouldExitB == true to cause exit.
      else // All app instance management actions failed.
        appShouldExitB= false; // Prevent exit, meaning, do a normal start-up.

      return appShouldExitB;  // Return whether app should exit.
      }

  private boolean updateTriggeredB= false; // For re-trigger detection.
  
  public void tryUpdateFromNewerFileInstanceV()
    /* This method is meant be called periodically by a timer
      to check for the appearance of a new version of the argAppFile
      and to do an update with it if a newer version appears.

      It works as follows:
	      If this app is the app in the standard folder.
	      and the new arg app is an approved updater app,
	      then setup to run the arg app, which will do the update,
	      and trigger a shutdown of this app.
      */
    {
  	  if (updateTriggeredB) // Preventing re-triggering of update. 
  	  	{ 
  	  	  appLogger.warning(
  	  	  		"tryUpdateFromNewerFileInstanceV(): re-triggered."
  	  	  		);
  	  		return;
      	  }
      //appLogger.debug("tryUpdateFromNewerFileInstanceV().");
      if ( argAppFile != null )  // argAppFile has been defined.
	      {
	        if   // Arg app approved to update app in standard folder.
	          ( updaterApprovedB() )
	          {
	        		updateTriggeredB= true;
	            // User approval or authenticity checks would go here.
	            appLogger.info("Detected an approved updater file.  Preparing it");
	            setForJavaCommandAndExitB(  // Chain to arg app to do the copy.
	              argAppFile.getAbsolutePath() 
	              );
	            }
	        }
      }

  private boolean updaterApprovedB()
    /* If this app is the app in the standard folder.
      and the arg app is newer that this app,
      then return true, otherwise return false.
      */
    {
      boolean resultB= false;  // Assume false.
      if // This app is the app in the standard folder.
        ( thisAppFile.equals( standardAppFile ) )
        {
          long argAppFileLastModifiedL= argAppFile.lastModified();
          long thisAppFileLastModifiedL= thisAppFile.lastModified();

          if // The arg app is newer than this app.
            ( argAppFileLastModifiedL > thisAppFileLastModifiedL )
            resultB= true;  // Override result.
          }
      return resultB;
      }
    
  private void identifyArgFileV( String[] inArgStrings )
    /* This method saves inArgStrings, the arguments used to run this app.
      By convention 
      * arg0 is the path to the file containing this app.
      * arg1 is the path to the file containing the app which ran this app.
      It also identifies the File associated with the first argument.
      */
    {
      argStrings= inArgStrings;
      if  // Extract calling app File if there is one.
        (argStrings.length>0) // There is at least one argument.
	      {
		      argAppFile= new File(argStrings[0]);  // Convert string to File.
	      	}
      }

  private void identifyAppFilesV( String[] inArgStrings )
    /* This method determines the jar file from which this app was loaded,
      and the jar file that should contain the app in the standard folder.
      It also logs the arguments.
      */
    {
      identifyArgFileV( inArgStrings );  // Save arg string[s].
      logInputsV();
      }

  // Code that manages running instances.

    private AppInstanceListener theAppInstanceListener= // Single listener ...
      null;  // ...to perform an action if desired.

    public static final int getInstancePortI() 
      { return PortManager.getDiscoveryPortI(); }
      // 56944;  // A high number I chose at random.
    
    public boolean tryARunningInstanceActionB( )
      /* Normally this method is called at app start-up.
        It detects whether there is a previously run running instance
        by trying to open a particular network socket.
        If it can't open the socket it is because an earlier run instance did.
        In this case it connects to the socket and sends through it
        the path of this app's jar file.
        If it can open the socket then it starts an InstanceManagerThread
        to monitor that socket for future messages from 
        later run running instances of the app.
        It returns true if this app should exit 
          to let the earlier run running instance decide what to do.
        It returns false if this app should continue with normal start-up,
          because the there is no earlier run running instance.
        */
      { 
        boolean appShouldExitB= false;  // Set default return for no app exit.

        try { // Try to start listening, indicating no other instances.
        	
          appLogger.info(
            "Local Host IP: " + 
            InetAddress.getLocalHost().getHostAddress() // Logging real IP. 
            );
        	
          //appLogger.info(
          //  "About to listen for a later app packets on port " + 
          //  getInstancePortI()
          //  );
          instanceServerSocket =  // Try opening listener socket.
            new ServerSocket(
              getInstancePortI(), 10, InetAddress.getLoopbackAddress() 
              );
          { // Setup InstanceManagerThread to monitor the socket.
            InstanceManagerThread theInstanceManagerThread=
              new InstanceManagerThread();
            theInstanceManagerThread.setName("InstcMgr");
            theInstanceManagerThread.start();
            } // Setup InstanceManagerThread to monitor the socket.

          theShutdowner.addShutdownerListener( // Adding this listener.
            new ShutdownerListener() {
              public void doMyShutdown() 
	              {
	                try {  // Terminating monitor thread by closing its socket.
	                		appLogger.info(
	                				"AppInstanceManager doMyShutdowner(..),"
	                				+" closing socket.");
		                  instanceServerSocket.close();
		                } catch (IOException e) {
		                  appLogger.error("Error closing instanceServerSocket" + e);
		                  e.printStackTrace();
		                }
	                }
              });
          ; // Leaving appShouldExitB false for no app exit.
        } catch (UnknownHostException e) { // Error.  What produces this??
          appLogger.error( e.getMessage()+ e );
          ; // Leaving appShouldExitB false for no app exit.
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
      /* This tryARunningInstanceActionB(..) sub-method 
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
          /* This method, for as long as the instanceServerSocket is open,
            waits for messages from other running app instances 
            announcing their presence, and processes those messages.
            Processing includes:
            * Parsing the received message and storing the result
              into argAppFile.
            * Depending on circumstances:
	            * firing any associated Listener, or  
	            * trigger an app shutdown to allow a software update to happen.
            */
          {
	          appLogger.info(Thread.currentThread().getName()+": beginning.");
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
      	          appLogger.info(Thread.currentThread().getName()+": beginning.");
                  appLogger.info(
                  	Thread.currentThread().getName()
                  	+"Received a newer app signal-packet:" + readString
                    );
                  identifyArgFileV( new String[] {readString} ) ;
                  logInputsV();
                  if // Exiting or firing event depending on other instance.
                    (tryAFileInstanceActionB( ))
                    theShutdowner.requestAppShutdownV(); // Triggering app exit.
                    else
                    fireNewInstance(); // Triggering AppInstanceListener action.
                  inBufferedReader.close();
                  clientSocket.close();
                } catch (IOException e) {
                  socketClosed = true;
                }
              }
              }
	          appLogger.info("Socket closed, ending.");
            }
        }

  // Code that does file instance management.

    private boolean tryAFileInstanceActionB( )
      /* Normally this method is called at app start-up,
        but only if there is not another running instance.
        It begins the process of checking for and installing or updating
        the app file in the Infogora standard folder to make it
        the latest version, and then runs it.
        It returns true if this app instance should shutdown
          because its job is done but is to be continued
          by another running instance which will be run to replace it.
        It returns false if this app instance should 
          continue with normal start-up, 
          because it has been determined that no further work is needed.
        The logic in the code below can result in as few as 
        1 app command to be run,
        in the case of a normal start-up from the standard folder,
        to as many as 4 app commands run, 
        for the case when a new app version is run
        outside the standard folder.
        depending on the conditions of the first app run.
        */
      { 
        boolean appShouldExitB= true;  // Set default return app exit.

        toReturn: { // The block after which all returns will go.
	        if ( argAppFile == null )  // The app command was without arguments.
	          { // Handle the arg-less command possibilities.
	            if (runningAppIsStandardAppB())  // Was from standard folder.
	              { // Prepare for normal startup.
	                appLogger.info("Starting app from standard folder.");
	                appShouldExitB= false;  // For normal startup.
	                } // Prepare for normal startup.
	              else  // Is arg-less but not from standard folder.
	              { // Try processing a plain app command without arguments.
	                if (tryInstallToStandardFolderB()) break toReturn;
	  	            if (tryUpdateToStandardFolderB()) break toReturn;
	                if (tryPokingAppInStandardFolderB()) break toReturn;
	                //appLogger.info("Exhausted without-arg possibilities.");
	                appShouldExitB= false;  // For normal startup.
	                } // Try processing a plain app command without arguments.
	            } // Handle the arg-less command possibilities.
	          else  // The app command has one (or more?) arguments.
	          { // Try processing an app command with argument[s].
	            if (tryRunningApprovedUpdaterB()) break toReturn;
	            if (tryUpdateToStandardFolderB()) break toReturn;
	            // Others to be added?
	            appLogger.info("Exhausted with-arg possibilities.");
	            appShouldExitB= false;  // For normal startup.
	            } // Handle app commands with argument[s].
          } // toReturn
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

    private boolean tryInstallToStandardFolderB()
      /* If the standard folder has no app in it then this method 
        copies the jar file of the running app to the standard folder,
        prepares to run it on exit, and returns true to cause exit.
        Otherwise it returns false.
        */
      {
    	  boolean appShouldExitB= false;
        if // A version of this app is not already in standard folder.
          ( ! standardAppFile.exists() )  // The file doesn't exist.
          {
            appLogger.info("Trying to install.");
            copyAppToStandardFolderAndPrepareToRunB();
            appShouldExitB= true;
            }
        return appShouldExitB;
        }

    private boolean tryUpdateToStandardFolderB()
      /* If the standard folder has an app jar file in it,
        but it is older than the running app,
        then this method updates it by replacing 
        the jar file in the standard folder
        with the jar file for this app,
        exits this app, and runs the updated app.
        Otherwise it returns.
        */
      {
    	  boolean appShouldExitB= false;
        if // This apps file is newer that the one in standard folder.
          ( thisAppFile.lastModified() > standardAppFile.lastModified() )
          {
            //appLogger.info("Updating by copying this app file to standard folder.");
        	  appShouldExitB= copyAppToStandardFolderAndPrepareToRunB();
            }
        return appShouldExitB;
        }

    private boolean tryPokingAppInStandardFolderB()
      /* If the running app is not in the standard folder,
        but it appears identical to the one in the standard folder,
        then this method exits this app, 
        and runs the app in the stanard folder.
        Otherwise it returns.
        */
      {
    		boolean appShouldExitB= false;
        if // This app is in the standard folder.
          ( ! thisAppFile.equals( standardAppFile ) )
          if // The date stamps are equal.
            ( thisAppFile.lastModified() == standardAppFile.lastModified() )
            {
              appLogger.info("Running identical app in standard folder.");
          	  appShouldExitB= 
          	    setForJavaCommandAndExitB( standardAppFile.getAbsolutePath() );
              }
        return appShouldExitB;
        }

    private boolean tryRunningApprovedUpdaterB()
      /* If this app is the app in the standard folder.
        and the arg app is newer that this app,
        then it prepares for shutdown, running the arg app,
        and it returns true.
        Otherwise it returns false.
        */
      {
    	  boolean appShouldExitB= false;
        if   // Arg app approved to update app in standard folder.
          ( updaterApprovedB() )
          {
            // User approval or authenticity checks would go here.
            appLogger.info("An approved updater has signalled.");
        	  appShouldExitB=   // Chain to arg app to do the copy.
	            setForJavaCommandAndExitB(
	              argAppFile.getAbsolutePath() 
	              );
            }
    	  return appShouldExitB;
        }

    private boolean copyAppToStandardFolderAndPrepareToRunB()
      /* This method tries to copy this app's jar file to the standard folder, 
        and prepare it to be run as a Process on exit. 
        It keeps trying until copying succeeds, or the thread is interrupted.
        If copying succeeds then it requests an app shutdown and returns true.
        If the thread is interrupted then it returns false.
        */
      {
	  	  boolean copySuccessB= false;
        if  // This app is not from a jar file.
          (! thisAppFile.getName().endsWith(".jar"))
          { // Probably a class file running in Eclipse.  Do normal startup.
            appLogger.info( "Not a jar file, so not copying or exiting.");
            }
          else
            while  // Keep trying until copy success and exit.
              (!EpiThread.exitingB() && !copySuccessB)
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
                  appLogger.info( "Copying successful." );
              	  copySuccessB= setForJavaCommandAndExitB( 
              	  		standardAppFile.getAbsolutePath() 
              	  		);
                  }
                catch (Exception e)  // Other instance probably still running.
                  { 
                    appLogger.info( 
                      "copyAppToStandardFolderAndPrepareToRunB().\n  "
                      +e.toString()
                      +"  Will retry after 1 second." 
                      ); 
                    (new LockAndSignal()).doWaitWithTimeOutE(1000);
                      // Wait 1 second.
                    }
        return copySuccessB;
        }
    
    private boolean setForJavaCommandAndExitB( String argString ) 
      /* This method is equivalent to a 
        setJavaCommandForExitV( argString )
        followed by triggering exiting of this app.
        It always true to simplify caller code and 
        to indicate that app should exit.
        */
      {
        setJavaCommandForExitV( argString );  // Setting up command.

        theShutdowner.requestAppShutdownV(); // Triggering controlled exit.
        
        return true;  // Returning to simplify caller code.
        }
    
    private void setJavaCommandForExitV( String argString )
      /* This method sets up the execution of a runnable jar file 
        whose name is argString as a Process.  
        It will execute as the last step in the app shutdown process.
        */
      {
        String [] commandOrArgStrings=  // Allocating space for for all args. 
          new String [
            2  // java -jar
            +1 // (.jar file to run)
            +1 // (.jar file of this app)
            ] ;

        commandOrArgStrings[0]= // Storing path of java command in array.
          System.getProperty("java.home") + 
          File.separator + 
          "bin" + 
          File.separator + 
          "java";
        commandOrArgStrings[1]= "-jar";  // Store java -jar option.
        commandOrArgStrings[2]=  // Store path of .jar file to run
          argString;
        commandOrArgStrings[3]=  // Store path of this app's .jar.
          thisAppFile.getAbsolutePath();  

        theShutdowner.setCommandV(  // Setting String as command to run later.
          commandOrArgStrings
          );
        }

  // Other miscellaneous code.

    private void logInputsV()
      // Logs all the file arguments.
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
