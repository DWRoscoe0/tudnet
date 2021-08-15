package allClasses;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JOptionPane;

import allClasses.epinode.MapEpiNode;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

public class AppInstanceManager {

  /* This class detects and manages instances of this app, and 
    some if the communication between those instances.
    It is also responsible for communicating with the app launcher, 
    if there is one.  

    There are 2 types of app instances:
    
    * file instances, which are copies of 
      some version of the app stored in a disk file.
    * running instances, which are copies of 
      some version of the app running on a CPU

    This code can be executed 3 ways:
    * When called by the main thread at app start up,
      which means it is a new running app instance.
    * When called by a periodic timer thread, 
      to check for the appearance of new app file instances.
    * When the InstanceManagerThread unblocks when 
      instanceServerSocket.accept() returns.
      This is caused by a message from 
      a new running app instance starting up.

    The app tries to maintain or restore 
    the following conditions about its instances:
    * There should be a maximum of one running instance at any given moment.
      Extra running instances are terminated.
    * The file instance in the standard folder should be the newest one known.  
      If it is not then any newer one is copied to the standard folder.
      This is how a software update is done.
    * The running instance should have been run from the standard folder.
      If it was not then the one in the standard folder
      will be run and the present one will be terminated.

    The app maintains these conditions by following a protocol:
    The protocol happens when all running instances follow
    the following procedure: 
    * When first run:
      * If the app detects another running instance then
	      it communicates its presence and its identity (folder) to
	      the other running instance, and then terminates itself.
	    * If the app does not detect another running instance,
	      but it was not run from the standard folder, 
	      and there is not a file instance in the standard folder
	      that is at least as new as its own file instance,
	      then it copies its file instance to the standard folder,
	      runs that file instance from the standard folder,
	      and then terminates itself.  This effectively
	      does an install or update.
    * While the app is running (in the standard folder):
      * If the app receives a message indicating
	      the presence and identity of a new running instance 
	      that is not a newer version and therefore will terminate,
	      it informs the user about this, otherwise it does nothing.
l    * If the app receives a message indicating
	      the presence and identity of a new running instance 
	      that is a newer version, then it informs the user, 
	      reruns the newer version, which by now
      	has terminated itself, and then terminates itself.
	    * If the app detects the existence of a newer file instance,
	      then it runs that one and terminates itself.

		The code which does all this is in or called from 2 methods.  They are:
	  	* managingInstancesWithExitB( ) which is called at startup.
	  	* thingsToDoPeriodicallyV() which is called periodically from by a timer.

    It also pauses to display dialogs informing the user what is happening.
    
    The running instance detection code in this class was originally based on 
    code at http://www.rbgrn.net/content/43-java-single-application-instance

		The app is interested in 2 folders:
		* The standard folder.
		* The possibly different folder where the app was originally run.

    ///org : Though this class works and works well, 
      it is difficult to understand.  Fix this by reorganizing it.
		
    ///doc?? Write a state-machine description summarizing the entire process.

    ///enh?? Maybe simplify the process of replacing an app file.
      * This class was written based on the assumption that
	      the app file which is running can not be deleted or replaced
	      until the app has terminated.  Replacing an app file is done
	      by running an the executable .jar file of the newer version,
	      terminating the old app, terminating the old version,
	      running the app file at the copied destination,
	      and terminating the app file at the copied source. 
	    * It might be possible to simplify this process by 
	      replacing the running app file while it it running.
	      It can be done with Windows .exe files by 
	      moving the old file instead of deleting it, as described at:
	        https://www.codeproject.com/questions/621666/download-and-replace-running-exe
				This will work on Linux also, though a simple delete will also work.

    ///enh?? The network code should be more robust.
    It needs to handle when the socket link is not working.
    Maybe if indications are that there is no older app instance
    it launches another instance of itself, 
    with a flag command line argument to prevent infinite recursion,
    to test the whole communication system.
    To be extra safe against intentional errors as well as
    unintentional ones, it might make sense to have a special
    configuration file used only for instance [and update] management.

    */

  // Private injected dependency variables, initialized during construction.

    // Constructure injections.
		private Shutdowner theShutdowner;
    private CommandArgs startCommandArgs; // args from command line.
    private PortManager thePortManager;

  // Locks and guards.
    private static Lock updaterReentrantLock= new ReentrantLock();
      /* This is Lock variable is used 
       to prevent more than one thread starting an update.
       It is locked when an update check is being attempted.
       A periodic update check can be abandoned with a failing tryLock(), 
       because it will be attempted again shortly.
       Update checks triggered by messages containing a specific path 
       to a discovered file must wait for lock() to succeed. 
       */
    private boolean updatingB= false;  // Set when update underway.
        
  // Internal dependency variables, set after construction.
    int inputCheckFileDelaySI= Config.localUpdateDelaySI; 
    	// Delay in seconds before checking for a local update file begins.
    int tcpCheckFileDelaySI= Config.tcpFileUpdateDelaySI;
      // Same for TCP update.
    
    // File names.  Some of these might be equal.
	  private File standardAppFile=  null; // App File name in standard folder.
    private File runningAppFile= null; // App File name of running app.
    public File otherAppFile= null;  // File name of another instance.
      // It can come from either:
      // * a command line argument from 
      //   an earlier app instance that started this app instance, or
	    // * a tcp packet from an app instance run after this app instance. 
      //   This can happen multiple times during execution. 
    private File tcpCopierAppFile= null;  // App File in 
      // TCPCopier folder.
      // This is how files gotten by TCPCopier files update the app.
      // TCPCopier creates new versions of app here.

    // Other variables.
    private String[] argStrings= null;  // Storage for String inputs to app
      // from multiple sources.
      // inputStrings[0] is normally the path to the other app file instance 
      // that ran the running app.
    private long newestAppLastModifiedL= 0; // For ignoring older updates.
      ///enh There might be cases when this is inadequate,
      // and there be a per-file newest value, 
      // or maybe something completely different.
    private LocalSocket theLocalSocket= null; // Used for 
      // instance existence testing and communication. 

  // Public initialization code.
	  
	  public AppInstanceManager(  // Constructor.
	      CommandArgs startCommandArgs,
	      Shutdowner theShutdowner,
	      PortManager thePortManager
	      )
	    {
		    this.theShutdowner= theShutdowner;
        this.startCommandArgs= startCommandArgs;
		    this.thePortManager= thePortManager;
		    }

	  public void initializeV()
	    // Does all initialization except constructor injection.
	    {
	      theLocalSocket= new LocalSocket();

	      runningAppFile= AppSettings.initiatorFile;

	      standardAppFile=
	          FileOps.makeRelativeToAppFolderFile( 
              AppSettings.initiatorNameString
              );

	      tcpCopierAppFile= // Calculating File name of TCPCopier target file.
	      		FileOps.makeRelativeToAppFolderFile( 
	      				Config.tcpCopierOutputFolderString 
	      				+ File.separator 
	      				+ AppSettings.initiatorNameString
	      				);
	      } // initializeV()

	  public void finalizeV()
	    /* This method is called to do things needed at app exit.
	     
	      Presently it only deletes empty temporary directories that
	      are apparently not deleted by the 7zip SFX launcher.
	      This seems to happen when an app instance starts 
	      a second instance of itself as a subprocess, 
	      and exits before the second instance exits.
	      
	      The following code doesn't delete all the temporary directories, 
	      but seems to delete all but one unless something interrupts it,
	      and prevents the accumulation of folders unless other errors occur.
	      It also logs the directories it tries to delete, 
	      and whether it was successful.
	      */
  	  {
        theAppLog.info("AppInstanceManager.finalizeV() deleter begins. ");
  	    String tempDirString= // Get temporary directory if it is in use. 
  	      startCommandArgs.switchValue("-tempDir");
          // Example: "C:\\Users\\PCUser\\AppData\\Local\\Temp\\7ZipSfx.003";
  	    if (tempDirString != null) { // Act if temporary directory is in use.
  	      int positionI= // Position of file name extension is after the dot.
  	          tempDirString.lastIndexOf(".")+1;
  	      String headString= tempDirString.substring(0, positionI);
  	      String extensionString= tempDirString.substring(positionI);
  	      int extensionI= // Parse hexadecimal file-name extension. 
  	          Integer.parseInt(extensionString,16);
  	      while (--extensionI >= 0) { // For all lower extensions down to 0...
  	        String dirString= headString + String.format ("%03x", extensionI);
    	      File toDeleteFile= new File(dirString); 
    	      boolean successB= toDeleteFile.delete();  // delete directory.
            theAppLog.info("AppInstanceManager.finalizeV(): deleting "
    	        + dirString + ", success=" + successB); 
    	      }
  	    }
  	  }
    
	// Public service-providing code.

	  public boolean tryDelegatingToAnotherAppInstanceB() // Called at start.
	    /* This method tries to delegate its work to another app instance.
        This method is called at app start-up.
	
		    First it tries to delegate to a non-running instance
        of the app file in the standard folder.
		    If this happens then execution of that file will be started
		    before this process terminates.
		    
		    If that fails then it tries to delegate to a running instance
		    of the app file in the standard folder.
	
	      It returns true if this app instance should exit because 
	        delegation was a success.
	      It returns false if this app instance should continue with 
	        normal start-up, run its GUI, etc..
	        This should happen only if this app instance 
	        was run from the app's standard folder,
	        and there is no other app instance running already.

	      */
	    {
	  		theAppLog.info( "tryDelegatingToAnotherAppInstanceB() begins."
	  		    + NL + "  App path is: " + runningAppFile.getAbsolutePath()
	  		    + NL + "  App time-stamp is " + FileOps.dateString( runningAppFile ) );
        processCommandArgsV( startCommandArgs ); // Setting app args as inputs.
        logInputsV("tryDelegatingToAnotherAppInstanceB()");

	      boolean successB= true;  // Assume delegation will succeed.
	      tryDelegation: {
  	      if ( tryDelegationToRunningInstanceB( ) ) break tryDelegation;
  	      if ( tryDelegationToFileInstanceB( ) ) break tryDelegation;
	    		theAppLog.info( "tryDelegatingToAnotherAppInstanceB() "
		  				+"======== DELEGATION FAILED SO GUI WILL START ========" );
	        successB= false; // Setting return result to prevent app exit.
  		    } // tryDelegation:
	
		  	theAppLog.info( 
		  	    "tryDelegatingToAnotherAppInstanceB() ending: "+successB );
	      return successB;  // Return whether app should exit.
	      }

	  public boolean tryToStartUpdateB()
	    /* This method is meant be called once per second
	      to check for the appearance of a new version of the app file 
	      and to do an update with it if it is a newer version.
	      It checks both the otherAppFile and the tcpCopierAppFile.

	      It works as follows:
	      If this app is the app in the standard folder.
	      and one of the apps discovered is an approved later version updater app,
	      then setup to run that second app, which will do the update,
	      and then trigger a shutdown of this app.
	      If an update is triggered in this way,
	      a lock will remain to prevent another update until this app exits,
	      and it returns true, otherwise it returns false.
	      */
	    {
	      tryUpdate: {
          if (updatingB) break tryUpdate; // Prevent re-entry if updating.
          updaterReentrantLock.lock(); 
          try { 
      		    updatingB= true;  // Assume update will begin.
  	  		    if ( (--inputCheckFileDelaySI < 0) && 
  			  	  		tryUpdateFromNewerFileInstancesB( otherAppFile ) )
  			  	  	break tryUpdate;
          	  if ( (--tcpCheckFileDelaySI < 0) && 
  			  	  		tryUpdateFromNewerFileInstancesB( tcpCopierAppFile ) )
                break tryUpdate;
          	  updatingB= false; // Neither update is happening.
  	  		  } finally {
              updaterReentrantLock.unlock();
  			    }
  				} // tryUpdate:
      if (updatingB) 
        theAppLog.info("thingsToDoPeriodicallyV() updating is [still] underway.");
      return updatingB;
      }

	  public String thisAppDateString()
      /* This method returns the time-stamp String associated with
        the file instance of this app.
        This may be used to append to names of old versions of files
        so that they can all be stored in the same folder,
        and to be displayed as a version number in the title bar of the app
        to distinguish one version from another.
        It is based on the File.lastModified() of this app's jar file. 
		    If it was not run from a jar file, from the Eclipse IDE for example,
		    then it is the time-stamp of the main class file.
 		    */
      {
        return FileOps.dateString( runningAppFile );
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

	// Private support code.

    private boolean tryUpdateFromNewerFileInstancesB( File otherAppFile )
      /* This method, which is meant to be be called periodically,
        tests whether otherAppFile is a valid update.
        If it is, it displays a message and 
        initiates the performance of the update.
        It returns true if an update was processed, false otherwise.
       */
      {
        boolean appShouldExitB= false; // Assume something will fail.
        toReturn: {
          if ( otherAppFile == null )  // otherAppFile has not been defined.
            break toReturn;
          if // Other app is not approved to update app in standard folder.
            ( ! isUpdateValidB( otherAppFile ) )
            break toReturn;
          // Additional approval or authenticity checks would go here.
          theAppLog.info("Detected an approved updater file.  Preparing it");
          if ( ! displayUpdateApprovalDialogB( // Not approved by user?
              false, // Get approval.
              "A file containing an update of this app was detected." + NL
              + "It will now replace this one because it is newer.",
              otherAppFile
              ) )
            break toReturn;
          appShouldExitB= // Chain to other app to do copy and run.
            requestProcessStartAndShutdownTrueB( 
                otherAppFile.getAbsolutePath() );
          } // toReturn:
        return appShouldExitB;
        }
	  
	  private boolean isUpdateValidB(File otherAppFile)
	    /* If this app is the app File in the standard folder.
	      and otherAppFile's time-stamp has changed 
	      since this method was last called,
	      and otherAppFile's time-stamp is newer than this app File,
	      then return true, otherwise return false.
	      It assigns
          newestAppLastModifiedL= otherAppLastModifiedL;
	      */
	    {
	      boolean successB= false;  // Assume update is not valid.
	      validation: {
		      if ( ! runningAppFile.equals( standardAppFile ) )
		      	break validation;  // This app is not in standard folder, so exit.
          long otherAppLastModifiedL= otherAppFile.lastModified();
          if ( otherAppLastModifiedL <= newestAppLastModifiedL )
		      	break validation; // Same time-stamp, so already rejected, so exit.
          newestAppLastModifiedL= otherAppLastModifiedL;
          long standardAppLastModifiedL= standardAppFile.lastModified();
          if ( otherAppLastModifiedL <= standardAppLastModifiedL )
           	break validation; // File not newer, so exit
          successB= true;  // Override default false result.
  	      theAppLog.debug("isUpdateValidB() ======== NEWER APP FILE DETECTED ========" +  
  	      		NL + "    otherAppFile:    " + FileOps.fileDataString(otherAppFile) + 
  	      		NL + "    standardAppFile: " + FileOps.fileDataString(standardAppFile)
  	      		);
	        } // validation
	      return successB;
	      }

    private void processCommandArgsV( CommandArgs theCommandArgs )
      /* This method extracts and saves various input arguments 
        from theCommandArgs, which is assumed to have already 
        been loaded with data.
        It does some actual command execution also.
        
        ///fix Command argument processing should probably depend on context.
          One set of commands are valid at startup.
          A different set is valid when received on the loopback port.
          So divide this method?
        */
      {
        theAppLog.info( 
            "AppInstanceManager processCommandArgsV(..), starting." );
        argStrings= theCommandArgs.args(); // Save a copy of arg array.
        
        processFeedbackPortV(theCommandArgs);
        tryUpdateCommandsV(theCommandArgs); // Try the update subset.
        startCommandArgs.switchValue("-tempDir"); /// Use temporary directory switch. 

        { // Log any unprocessed/ungotten arguments.
          String[] targetStrings= // Get remaining arguments.
              theCommandArgs.targets(); 
          if (targetStrings.length>0) // If there are any then
            theAppLog.warning( // log them as errors.
              "AppInstanceManager processCommandArgsV(..), unused arguments:" + NL + "  "
              + Arrays.toString(targetStrings));
          }
        } // processCommandArgsV(..)

	  // Code that manages running instances.
	
	    private AppInstanceListener theAppInstanceListener= // Single listener ...
	      null;  // ...to perform an action if desired.
      
      private boolean tryDelegationToRunningInstanceB( )
        /* Normally this method is called at app start-up.
          It tries to delegate to a running instance of this app.

          It tests for delegation by trying to bind
          the app's instance port on the loopback interface.
          
          If this app can bind the socket then it means that
          no other running app instance has done so.
          In this case it starts an InstanceManagerThread
          to monitor the socket for future connections and messages from 
          later running instances of this app.
          Then it returns false to indicate delegation failure, meaning that 
          this app instance should continue with normal GUI start-up, etc.

          If this app can't bind the socket then it is [probably] because 
          an earlier app instance is running and already did that.
          In this case it connects to the socket and tries to send through it
          to the other earlier app instance the path of this app's jar file.
          If this succeeds then it returns true to indicate that 
          delegation succeeded and that this app should exit.  
          The other running instance will then take appropriate action.

          */
        { 
          logLocalHostV();

          boolean successB= false;  // Set default for delegation failure.
          if (theLocalSocket.bindB(thePortManager.getInstancePortI())) 
            {
              setupMonitoringThreadV();
              ; // Leaving succeededB false.
              }
            else
            {
              theAppLog.info( "TCP Port "+thePortManager.getInstancePortI()+" is listening." );
              successB=  // Set success to the result of...
                connectAndSendToAppHoldingSocketB( );  // ...sending to it.
              }
          return successB;  // Return whether or not app should exit.
          }

      private void setupMonitoringThreadV()
        /* This method sets up a thread for monitoring
          the now bound LocalSocket for connections and input.
          It also sets up termination of that thread using
          theShutdownerListener.
         */
        {
          { // Setup InstanceManagerThread to monitor the socket.
            InstanceManagerThread theInstanceManagerThread=
              new InstanceManagerThread();
            theInstanceManagerThread.setName("InstcMgr");
            theInstanceManagerThread.start();
            } // Setup InstanceManagerThread to monitor the socket.
          theShutdowner.addShutdownerListener( // Adding this listener.
              theShutdownerListener
              );
          }

	    private void logLocalHostV()
        {
          try { // Log localhost IP.
              theAppLog.info(
                "logLocalHostV() IP= " + 
                InetAddress.getLocalHost().getHostAddress() // IP. 
                );
            } catch (UnknownHostException e) {
              theAppLog.exception("logLocalHostV()",e);
            }
          }

      private ShutdownerListener theShutdownerListener=
        new ShutdownerListener() {
          public void doMyShutdown()
            // This will cause IOException and terminate InstcMgr thread.
            {
              theAppLog.info(
                  "AppInstanceManager ShutdownerListener.doMyShutdowner(..),"
                  +" closing socket.");
              theLocalSocket.closeAllV();
              theAppLog.info( 
                  "AppInstanceManager ShutdownerListener.doMyShutdowner(..), done" 
                  );
              }
          };

      private boolean connectAndSendToAppHoldingSocketB( )
	      /* This tryARunningInstanceActionB(..) sub-method 
	        tries to inform the app listening on the busy socket
	        that this app exists and is available 
	        as a source for possible updates. 
	        It returns true if it succeeds and to indicate that
	          this app should exit to let the other app handle things.
	        It returns false to indicate a sending error,
	          and this app should not exit.
	        */
	      {
	        String outputString= // Make input string to be used in packet. 
	            "-otherAppIs " + // App's -otherAppIs option.
	            runningAppFile.getAbsolutePath(); // Path of this app's file.
          boolean successB= LocalSocket.localSendToPortB(
	            outputString, thePortManager.getInstancePortI());
          return successB;
	        }
	
	    class InstanceManagerThread extends Thread
	      /* This class contains the run() method which waits for
	        messages from other running app instances and processes them.  */
	      {
	        public void run()
	          /* This method, for as long as the instanceServerSocket is open,
	            waits for connections from other running app instances 
	            announcing their presence, and processes a single line message
	            and processes it.  Processing includes:
	            * Parsing the received message and storing the result
	              into otherAppFile.
	            * Depending on circumstances:
		            * firing any associated Listener, or  
		            * trigger an app shutdown to allow a software update to happen.
	            */
	          {
		          theAppLog.info(Thread.currentThread().getName()+": beginning.");
	            while (! theLocalSocket.isClosedB()) {
                try {
                  theLocalSocket.acceptV(); // Wait for connection or exception.
                  { // Do a software update check using data from the socket.
                    updaterReentrantLock.lock(); // Wait until we have lock.
                    try {
                        theLocalSocket.inputFromConnectionV();
                        theAppLog.debug("run(): got data.");
                        processCommandArgsV(theLocalSocket.getCommandArgs());
			                } finally {
	                      updaterReentrantLock.unlock(); // Release the lock.
	                    }
                  	}
                  theLocalSocket.closeConnectionV();
                	} 
                catch (IOException e) // Accepting of connection failed.
                  { // We must be terminating.
                    theLocalSocket.closeAllV(); // Make certain all are closed.
	                  }
                } // while
		          theAppLog.info("Socket closed, ending.");
	            }
	        
	        } // class InstanceManagerThread

	    private void processFeedbackPortV(CommandArgs theCommandArgs)
        /* This method tries to read the feedbackPort value from
          theCommandArgs and injects it into the Shutdowner
          for notifying the starter process at exit time.
          */
  	    {
          theShutdowner.injectStarterPortV(
              startCommandArgs.switchLongValue("-starterPort",0L));
          }

      private void tryUpdateCommandsV(CommandArgs theCommandArgs)
        /* This method tries to do a software update based on theCommandArgs.
          It is called after receiving command arguments, either
          from the TCP loopback port, or from the command line.
          */
        {
          if ( !theCommandArgs.switchPresent("-otherAppIs")) return;
          otherAppFile= // Read other app path from switch value. 
              new File(theCommandArgs.switchValue("-otherAppIs"));
		      logInputsV("tryUpdateCommandsV"); // Report relevant [path] inputs.
		      if // Exiting or firing event depending on other instance.
		      	( isUpdateValidB( otherAppFile ) )
		        { // Report pending update.
              theAppLog.info("processConnectionDataV(..), "
                  + "Offered app file is newer and is valid.");
		         	if ( displayUpdateApprovalDialogB( 
	          			false, // Get approval.
		        			"A newer running instance of this app "
		        			+ "has been detected." + NL
		              + "It will be used in a software update because "
		              + "it is newer than this app instance.",
		              otherAppFile
		        			) ) 
		         	  {
		         			// Chain to other app to do copy and run.
		         				requestProcessStartAndShutdownTrueB( 
			      					otherAppFile.getAbsolutePath() );
			      			theShutdowner.requestAppShutdownV(); // App exit.
		         			}
		        	}
		        else
		        {
		          theAppLog.info("processConnectionDataV(..), "
		              + "Offered app file is not newer or is invalid.");
		        	displayUpdateApprovalDialogB( 
	          			true, // Just inform user.  Don't request approval.
		        			"Another running instance of this app "
		        			+ "was detected briefly." + NL
		              + "It was not used in a software update because "
		              + "it was not newer than this app instance.",
		              otherAppFile
		        			);
		          fireNewInstance(); // AppInstanceListener action.
		          }
		      }
	      
    	private boolean displayUpdateApprovalDialogB( 
    			final boolean informDontApproveB, String messageString, File appFile )
    	  /* This method displays a dialog box containing messageString
    	    which should be a string about a software update,
    	    and appFile, which is the file that contained the potential update.
    	    This method takes care of switching to the EDT thread, etc.
    	    If informDontApproveB is true, it only informs the user.
    	    If informDontApproveB is false, it asks for the user's approval
    	    and returns the approval as the function value.
    	    
    	    ///enh Change to allow user to reject update, return response,
    	    and have caller use that value to skip update.
    	   */
	    	{
    			java.awt.Toolkit.getDefaultToolkit().beep(); // Beep.
	        theAppLog.info("displayUpdateApprovalDialogB(..) begins.");
		  		final AtomicBoolean resultAtomicBoolean= // Set default of approval. 
		  		    new AtomicBoolean(true);
		  		boolean dialogB= false; // true;
		  		if (dialogB) { // Display dialog if desired.
  	    		final String outString= 
  	    				messageString
  	    				+ NL + "The file that contains the other app is: "
  	    				+ appFile.toString()
  	    				+ NL + "It's creation time is: "
  	    				+ FileOps.dateString(appFile)
  	    				+ ( informDontApproveB ? "" : NL + "Do you approve?");
  		  		EDTUtilities.runOrInvokeAndWaitV( // Run following on EDT thread. 
  			    		new Runnable() {
  			    			@Override  
  			          public void run() {
  			    				if (!informDontApproveB) { // Approving.
  			    				 	int answerI= JOptionPane.showConfirmDialog(
  			    				 		null, // No parent component. 
  			                outString,
  			                //// "Infogora Info",
  			                Config.appString + " Info",
  			                JOptionPane.OK_CANCEL_OPTION
  			                );
  	    		    	  	resultAtomicBoolean.set(
  	    		    	  			(answerI == JOptionPane.OK_OPTION) );
  			    					}
  			    				else // Informing only.
  				    				JOptionPane.showMessageDialog(
  			    						null, // No parent component. 
  			                outString,
  			                //// "Infogora Info",
                        Config.appString + " Info",
  			                JOptionPane.INFORMATION_MESSAGE
  			                );
  			    				}
  			          } 
  			        );
		  		  }
    			theAppLog.info(
    					"displayUpdateApprovalDialogB(..) ends, value= " 
    					+ resultAtomicBoolean.get() );
		  		return resultAtomicBoolean.get();
		  		}

	  // Code that does file instance management.
	
	    private boolean tryDelegationToFileInstanceB()
	      /* Normally this method is called at app start-up,
	        before the GUI has been activated.
	        It begins the process of checking for and installing or updating
	        the app file in the Infogora standard folder to make it
	        the latest version, and then runs it.
	        It returns true if this app instance should shutdown
	          because further work has been delegated to
	          another running instance which will be run to replace it.
	        It returns false if this app instance should 
	          continue with normal GUI start-up because 
	          no [further] delegation is to be done.
	        The logic in the code below can result in 
	        action by as few as 1 running app instance,
	        in the case of a normal start-up from the standard folder,
	        to action by as many as 4 running app instances,
	        for the case when a new app version is run 
	        outside the standard folder when 
	        an instance is already running from the standard folder.
          
          ///org : simplify by adding labels: toReturnTrue and toReturnFalse.
	        */
	      { 
  	        theAppLog.info("tryDelegationToFileInstanceB() enter");
  	        boolean appShouldExitB= true;  // Set default return app exit.
	        toReturn: { // The block after which all returns will go.
            if (! endsWithJarOrExeB(runningAppFile)) {
              appShouldExitB= false;  // For normal startup, in Eclipse.
              break toReturn;
              }
		        if // The app command was without arguments.
		        	( otherAppFile == null ) 
		          { // Handle the arg-less command possibilities.
		            if (runningAppIsStandardAppB())  // Was from standard folder.
		              { // Prepare for normal startup.
                    theAppLog.info("Starting app from standard folder.");
		                appShouldExitB= false;  // For normal startup.
		                } // Prepare for normal startup.
		              else  // Is arg-less but not from standard folder.
		              { // Try processing a plain app command without arguments.
		                if (tryInstallToStandardFolderB()) break toReturn;
		  	            if (tryUpdateToStandardFolderB()) break toReturn;
		                if (trySwitchingToAppInStandardFolderB()) break toReturn;
		                //appLogger.info("Exhausted without-arg possibilities.");
		                appShouldExitB= false;  // For normal startup.
		                } // Try processing a plain app command without arguments.
		            } // Handle the arg-less command possibilities.
		          else  // The app command has one (or more?) arguments.
		          { // Try processing an app command with argument[s].
		            if (tryRunningApprovedUpdaterB()) break toReturn;
		            if (tryUpdateToStandardFolderB()) break toReturn;
		            // Others to be added?
		            theAppLog.info("Exhausted with-arg possibilities.");
		            appShouldExitB= false;  // For normal startup.
		            } // Handle app commands with argument[s].
	        } // toReturn
            theAppLog.info("tryDelegationToFileInstanceB() exit");
  	        return appShouldExitB;  // Return whether or not app should exit.
	        }
	
	    private boolean runningAppIsStandardAppB()
	      /* If the file of the running instance app
	        is the same as the app file in the standard folder 
	        then return true, otherwise return false.
	        */
	      {
	        return runningAppFile.equals( standardAppFile );
	        }
	
	    private boolean tryInstallToStandardFolderB()
	      /* If the standard folder has no app in it then this method 
	        copies the jar file of the running app to the standard folder,
	        and returns true to indicate that this app should exit
          and run the copied app.
          Otherwise it returns false.
	        */
	      {
	    	  boolean appShouldExitB= false;
	        if // A version of this app is not already in standard folder.
	          ( ! standardAppFile.exists() )  // The file doesn't exist.
	          {
	            theAppLog.info("Trying to install.");
	            appShouldExitB= updateAndPrepareToRunB();
	            }
	        return appShouldExitB;
	        }
	
	    private boolean tryUpdateToStandardFolderB()
	      /* If the standard folder has an app file in it,
	        but it is older than the running app,
	        then this method updates it by replacing 
	        the jar file in the standard folder with the file for this app,
	        and returns true to indicate that this app should exit
	        and run the copied app.
	        Otherwise it returns false.
	        */
	      {
	          boolean appShouldExitB= false; // Default of no app exit.
	    	  toReturn: {
  	        if // This apps file is not newer than the one in standard folder.
  	          ( runningAppFile.lastModified() <= standardAppFile.lastModified() )
  	          break toReturn;
            appShouldExitB= true; // App exits in the following cases.
            if (updateAndPrepareToRunB()) break toReturn; // Successful update.
            theAppLog.error("tryUpdateToStandardFolderB(): "
                + "Update failed.  Waiting, then exiting.");
            EpiThread.interruptibleSleepB(5000); // Wait 5s.
          } // toReturn:
	          return appShouldExitB;
	        }
	
	    private boolean trySwitchingToAppInStandardFolderB()
	      /* If the running app file is not in the standard folder,
	        but it appears identical to or older than the one in the standard folder,
	        then this method requests that the standard folder file be run
	        and returns true to indicate that this app should exit. 
	        Otherwise it returns false to indicate this app should continue.
	        */
	      {
	          boolean appShouldExitB= false;
	    		toExit: {
            if (! endsWithJarOrExeB(runningAppFile)) break toExit;
  	        if // This app is already in the standard folder.
  	          ( runningAppFile.equals( standardAppFile ) )
  	          break toExit;
	          if // Running app is not The date stamps are not equal.
              (runningAppFile.lastModified() > standardAppFile.lastModified())
	            break toExit;
            theAppLog.info("Will run identical or newer app in standard folder.");
        	  appShouldExitB= 
        	    requestProcessStartAndShutdownTrueB( 
        	    		standardAppFile.getAbsolutePath() );
	    		} // toExit:
	    		  return appShouldExitB;
	        }
	
	    private boolean tryRunningApprovedUpdaterB()
	      /* If this app is the app in the standard folder.
	        and the arg app is newer that this app,
	        then it prepares running the arg app followed by shutdown, 
	        and it returns true.
	        Otherwise it returns false.
	        */
	      {
	    	  boolean appShouldExitB= false;
	        if   // Arg app approved to update app in standard folder.
	          ( isUpdateValidB( otherAppFile ) )
	          {
	            // User approval and authenticity checks would go here.
	          	if ( displayUpdateApprovalDialogB( // Inform user but don't get approval. 
	          			false, // Get approval.
	                "Another file instance of this app was detected." + NL
	                + "It will now replace this one because it is newer.",
	                otherAppFile
	          			) )
		          	{
			            theAppLog.info("An approved updater has been detected.");
			        	  appShouldExitB=   // Chain to arg app to do the copy.
				            requestProcessStartAndShutdownTrueB(
				              otherAppFile.getAbsolutePath() 
				              );
		          		}
	            }
	    	  return appShouldExitB;
	        }
      
      private boolean updateAndPrepareToRunB()
        /* This method tries to update this running app's executable file to 
          the standard folder, and prepare it to be run as a Process on exit.
          It keeps trying until copying succeeds, 
          or the thread is interrupted.
          If copying succeeds then it requests an app shutdown 
            and returns true.
          If the copy fails for any reason,
            including the thread being interrupted,
            then it returns false.
          */
        {
            theAppLog.debug("updateAndPrepareToRunB() begin");
            boolean successB= false;
          toExit: {
            if (! endsWithJarOrExeB(runningAppFile)) {
              theAppLog.info("updateAndPrepareToRunB() Not copying.  "
                  + "Not executable .jar or .exe file:" + NL + "  " + runningAppFile);
              break toExit;
              }
            if (! copyExecutableFileWithRetryB(runningAppFile, standardAppFile))
              break toExit; // Exit if copying of executable failed.
            successB= requestProcessStartAndShutdownTrueB( 
                standardAppFile.getAbsolutePath() 
                );
          } // toExit:
            theAppLog.debug("updateAndPrepareToRunB() ends= "+successB);
            return successB;
          }

      private boolean endsWithJarOrExeB( File theFile ) 
        {
          return endsWithJarOrExeB( theFile.getName() ); 
          }

      private boolean endsWithJarOrExeB( String theString ) 
        {
            boolean resultB= true;
          toExit: {
            if (theString.endsWith(".jar")) break toExit;
            if (theString.endsWith(".exe")) break toExit;
            theAppLog.debug(
                "endsWithJarOrExeB() Not exe or jar file:" + theString);
            resultB= false;
          } // toExit:
            return resultB;
          }

      @SuppressWarnings("unused") ////// save until useful code recycled.
      private boolean mergePersistentDataB(
          File sourceFile, File destinationFile)
        /* This method tries to merge the PersistentEpiNode.txt file
          from the same directory as the sourceFile to 
          the PersistentEpiNode.txt file in 
          the same directory as the destinationFile.
          If there is an IO error then the method returns false.
          If there is no IO error, or there is no file to merge,
          then it returns true.
          */
        {
            theAppLog.debug("mergePersistentDataB() entry.");
            boolean successB= false; // for now, assume that we will fail;
          toExit: { toSuccess: {
            File sourceFileFile= new File(
                sourceFile.getParentFile(),Config.persistentFileString);
            if (! sourceFileFile.exists()) // Treat file not existing 
              break toSuccess; // as a success.
            theAppLog.debug("mergePersistentDataB() source file exists.");
            MapEpiNode sourceMapEpiNode= 
                Persistent.loadMapEpiNode(sourceFileFile);
            if (null == sourceMapEpiNode) break toExit; // Load failed.
            theAppLog.debug("mergePersistentDataB() source file loaded.");
            File destinationFileFile= new File(
                destinationFile.getParentFile(),Config.persistentFileString); 
            MapEpiNode destinationMapEpiNode= 
                Persistent.loadMapEpiNode(destinationFileFile);
            if (null == destinationMapEpiNode) break toExit; // Load failed.
            theAppLog.debug("mergePersistentDataB() destination file loaded.");
            destinationMapEpiNode= // Merge the 2 sets of data into 1 set.
                mergeMapEpiNode(destinationMapEpiNode,sourceMapEpiNode);
            String errorString= Persistent.storeEpiNodeDataReturnString( 
              destinationMapEpiNode, destinationFileFile);
            if (null != errorString) break toExit; // Store failed.
            theAppLog.debug("mergePersistentDataB() store/write succeeded.");
          } // toSuccess:
            theAppLog.debug("mergePersistentDataB() overall success.");
            successB= true; // Whole succeeded because all parts succeeded.
          } // toExit:
            theAppLog.debug("mergePersistentDataB() exit, successB="+successB);
            return successB;
          }


      private MapEpiNode mergeMapEpiNode(
          MapEpiNode destinationMapEpiNode,MapEpiNode sourceMapEpiNode)
        /* This is a simple merge method.
         * It stores sourceMapEpiNode as the value of an entry
         * in destinationMapEpiNode and returns destinationMapEpiNode.   
         */
        {
          theAppLog.debug("AppInstanceManager.mergeMapEpiNode(.) before merge "
            + "sourceMapEpiNode=\n" + sourceMapEpiNode.toString(4));
          theAppLog.debug("AppInstanceManager.mergeMapEpiNode(.) before merge "
            + "destinationMapEpiNode=\n" + destinationMapEpiNode.toString(4));
          destinationMapEpiNode.putV(
              "ImportedPersistentData", sourceMapEpiNode);
          theAppLog.debug("AppInstanceManager.mergeMapEpiNode(.) after merge "
            + "destinationMapEpiNode=\n" + destinationMapEpiNode.toString(4));
          return destinationMapEpiNode; 
          }

      private boolean copyExecutableFileWithRetryB(
          File sourceFile, File destinationFile)
        /* This method tries to copy the executable 
          sourceFile to the destinationFile.
          First it does some tests on the file name extensions.
          They must both be either .jar or .exe.
          If they are not then the method returns false.
          If they are then it copies the file and returns true.
          */
        {
            boolean successB= false;
            String sourceNameString; 
          toExit: { toCopy: { toEqualityTest: {
            sourceNameString= sourceFile.getName(); 
            if (endsWithJarOrExeB(sourceNameString)) break toEqualityTest;
            theAppLog.error("copyExecutableFileB() Not exe or jar file."
              +FileOps.twoFilesString(sourceFile, destinationFile));
            break toExit;
          } // toEqualityTest:
            if (sourceNameString.equals(destinationFile.getName())) 
              break toCopy;
            theAppLog.error("copyExecutableFileB() File extension mismatch."
                +FileOps.twoFilesString(sourceFile, destinationFile));
            break toExit;
          } // toCopy:
            FileOps.copyFileWithRetryReturnString(sourceFile, destinationFile);
            successB= true;
          } // toExit:
            theAppLog.debug("copyExecutableFileB()= "+successB);
            return successB;
          }
          
	    private boolean requestProcessStartAndShutdownTrueB( 
	        String commandPathString ) 
	      /* This method is equivalent to a 
	        setCommandForExitV( commandString )
	        followed by requesting shutdown of this app.
	        It always returns true to simplify caller code and 
	        to indicate that the app should exit.
	        */
	      {
	        setCommandForExitV(  // Set command to execute at exit.
	            commandPathString );

	        theShutdowner.requestAppShutdownV(); // Triggering app shutdown.

	        return true;  // Returning true to simplify caller code.
	        }
      
      private void setCommandForExitV( String commandPathString )
        /* This method sets up the execution of a runnable file 
          whose name is commandString as a Process.  
          It will execute as the last step in the app shutdown process.
          It adds command switches appropriate to the present context.
           
          Presently it handles:
          * Java jar file commands.
          It is being modified to also handle non-jar file commands.
          */
        {
          ArrayList<String> theArrayListOfStrings= new ArrayList<String>();
          if (commandPathString.endsWith(".jar")) { // Add Java if jar file.
            theArrayListOfStrings.add(System.getProperty("java.home") + 
                File.separator + "bin" + File.separator + "java.exe");
            theArrayListOfStrings.add("-jar"); // java.exe -jar option.
            }
          theArrayListOfStrings.add(commandPathString); // Path of command.
          theArrayListOfStrings.add("-otherAppIs"); // -otherAppIs option.
          theArrayListOfStrings.add(runningAppFile.getAbsolutePath()); // app.
          theArrayListOfStrings.add("-starterPort"); // For feedback...
          theArrayListOfStrings.add( // ...using this port.
              ""+startCommandArgs.switchLongValue("-starterPort",0L));
          // theArrayListOfStrings.add("SENTINEL"); // ///dbg

          theShutdowner.setExitStringsV( // Set command String from array.
              theArrayListOfStrings.toArray(new String[0]));
          }
  
	  // Other miscellaneous code.
	
	    private void logInputsV(String callerString)
	      /* Logs all inputs, raw, and parsed into
	        file arguments, logged with their time-stamps.
	        */
	      {
	        String logString= "AppInstanceManager.logInputsV(";
	        
	        { // Add parts to the string to be logged.
            logString+= callerString;
            logString+= ")" + NL + "  raw argStrings:"; 
            logString+= Arrays.toString(argStrings);

            logString+= NL + "  app Files: (variable-name: time-stamp, path-name)";
	          logString+= NL + "    standardAppFile:    " 
	          		+ FileOps.fileDataString( standardAppFile );
	          logString+= NL + "    runningAppFile:     " 
	          		+ FileOps.fileDataString( runningAppFile );
	          logString+= NL + "    otherAppFile:       " 
	          		+ FileOps.fileDataString( otherAppFile );
	          logString+= NL + "    tcpCopierAppFile:   " 
	          		+ FileOps.fileDataString( tcpCopierAppFile );
	          }
	        
	        theAppLog.info( logString );  // Log the completed String.
	        }
		    
	    @SuppressWarnings("unused") ///
      private String argsOnLinesString( String inputStrings[])
	      /* This method returns a String which is the concatenation of lines,
	        each line consisting of 4 spaces, the element index, and the string.
	        The first line is a heading line. 
	        */
	      { 
  	      String logStriing= NL + "    " + "n   String";
  	      for (int i=0; i < inputStrings.length; i++) {
            logStriing+= NL + "    " + i + "   " + inputStrings[i];
  	        }
	        return logStriing; 
	        }
	}
