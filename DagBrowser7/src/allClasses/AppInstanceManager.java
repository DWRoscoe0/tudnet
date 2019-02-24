package allClasses;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static allClasses.Globals.*;  // appLogger;

public class AppInstanceManager {

  /* This class detects and manages instances of app, and 
    some if the communication between them.  
    
    ///org : Though this works and works well, it is difficult to understand.
      Fix this by reorganizing it.

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

    The app tries to maintain the following conditions about its instances:
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

		The code which does all this is in methods:
	  	* managingInstancesWithExitB( ) which is called at startup.
	  	* thingsToDoPeriodicallyV() which is called periodically from by a timer.

    It also pauses to display dialogs informing the user what is happening.
    
    The running instance detection code in this class was originally based on 
    code at http://www.rbgrn.net/content/43-java-single-application-instance

		The app is interested in 2 folders:
		* The standard folder.
		* The possibly different folder where the app was originally run.
		
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
    
  // Internal dependency variables, set after construction.
    int inputCheckFileDelaySI= Config.localUpdateDelaySI; 
    	// Delay in seconds before checking for a local update file begins.
    int tcpCheckFileDelaySI= Config.tcpUpdateDelaySI;
      // Same for TCP update.
    
    // File names.  Some of these might be equal.
	  private final File standardAppFile=  // App File name in standard folder.
    		Config.userAppJarFile;
    private File runningAppFile; // App File name of running app.
    private File otherAppFile= null;  // File name of another instance.
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
    private String[] inputStrings= null;  // Storage for String inputs to app
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
	      { // Calculating File name of this app's file instance.
	        theLocalSocket= new LocalSocket();
		      URI thisAppURI = null;
		      try {
		          thisAppURI = Infogora.class.getProtectionDomain().
		            getCodeSource().getLocation().toURI();
		        } catch (URISyntaxException e1) {
		          // TODO Auto-generated catch block
		          appLogger.error("AppInstaneManager.initializeV" + e1);
		          e1.printStackTrace();
		        }
		      runningAppFile= new File( thisAppURI );
	      	}
	      tcpCopierAppFile= // Calculating File name of TCPCopier target file.
	      		Config.makeRelativeToAppFolderFile( 
	      				Config.tcpCopierOutputFolderString 
	      				+ File.separator 
	      				+ Config.appJarString 
	      				);
	      setInputsV( startCommandArgs );  // Setting app args as inputs.
	      logInputsV(); // Log start up inputs.
	      }
	

	// Public service-providing code.

	  public boolean managingInstancesWithExitB( )
	    /* This method manages running instances and file instances of 
	      this app.  This method is called at app start-up.
	
		    It detects whether there is another running instance
		    by trying to open a particular network socket.
		    * If it can't open the socket it is because 
		      another running instance did.
		    	In this case it communicates its presence to the other instance 
		    	by connecting to the socket and sending this instance's path to it.
		    * If it could open the socket then it sets up a Listener on it
		      for messages from instances of the app which might be run later.
	
	      It returns true if this app should exit because its job is done,
	        except possibly for calling another instance at shutdown
	        to continue the management protocol.
	      It returns false if this app instance should continue with 
	        normal start-up, run its GUI, etc.,
	        because no other running instance was detected.
	      */
	    {
	  		appLogger.info( "managingInstancesWithExitB() begins." );
	  		appLogger.info( "App path is:\n  " + runningAppFile.getAbsolutePath() );
		  	appLogger.info( 
		  			"App time-stamp is " + Misc.dateString( runningAppFile ) );
	    	
	      boolean appShouldExitB= true;  // Setting default result for app exit.
	
	      if ( tryARunningInstanceActionB( ) )
	        ; // Leave appShouldExitB == true to cause app exit.
	      else if ( tryAFileInstanceActionB( ) )
	        ; // Leave appShouldExitB == true to cause app exit.
	      else // App instance management actions failed.
		      { // Set appShouldExitB == Cause to cause app exit.
			  		appLogger.info( 
			  				"======== THIS INSTANCE STAYING, GUI WILL START ========" );
		        appShouldExitB= false; // Setting return result to prevent app exit.
		        }
	
		  	appLogger.info( "managingInstancesWithExitB() ending." );
	      return appShouldExitB;  // Return whether app should exit.
	      }

	  public void thingsToDoPeriodicallyV()
	    /* This method is meant be called once per second by a timer thread
	      to check for the appearance of a new version of the otherAppFile
	      and to do an update with it if it is a newer version.

	      It works as follows:
		      If this app is the app in the standard folder.
		      and the new arg app is an approved later version updater app,
		      then setup to run the arg app, which will do the update,
		      and trigger a shutdown of this app.
	      */
	    {
	  	  boolean updatingB= true;  // Assume updating.
	  	  if ( updaterReentrantLock.tryLock() ) // Act now only if not locked.
					{
	  	  		try {
					      //appLogger.debug("thingsToDoPeriodicallyV().");
				  	  if ( (--inputCheckFileDelaySI < 0) && 
					  	  		tryUpdateFromNewerFileInstancesB( otherAppFile ) )
					  	  	;
					  	  else if ( (--tcpCheckFileDelaySI < 0) && 
					  	  		tryUpdateFromNewerFileInstancesB( tcpCopierAppFile ) )
					  	  	;
					  	  else
					  	  	updatingB= false; // Neither update is happening.
					    } finally {
					    	if (updatingB) { // Log effect of timer input if updating.
					    		logInputsV();
					    	  }
					    	updaterReentrantLock.unlock();
					    }
					}
	      }

	  private boolean tryUpdateFromNewerFileInstancesB( File otherAppFile )
	    /* This method, which is meant to be be called periodically,
	      tests whether otherAppFile is a valid update.
	      If it is, it displays a message and 
	      initiates the performance of the update.
	      It returns true if an update was processed, false otherwise.
	     */
		  {
			  boolean appShouldExitB= false;
		    if ( otherAppFile != null )  // otherAppFile has been defined.
		      {
		        if   // Other app is approved to update app in standard folder.
		          ( isUpdateValidB( otherAppFile ) )
		          {
		            // User approval or authenticity checks would go here.
		            appLogger.info(
		            		"Detected an approved updater file.  Preparing it"
		            		);
		            if ( displayUpdateApprovalDialogB(
		          			false, // Get approval.
		                "A file containing an update of this app was detected.\n"
		                + "It will now replace this one because it is newer.",
		                otherAppFile
		          			) )
			            {
					      		appShouldExitB= // Chain to other app to do copy and run.
					      			requestForJavaCommandAndExitTrueB( 
					      					otherAppFile.getAbsolutePath() );
				            }
		            }
		        }
		    return appShouldExitB;
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
        return Misc.dateString( runningAppFile );
        }

    private final int getInstancePortI()
    	/* Returns port to be used for local app instance discovery.
        ///enh Write file only if its contents must change.
          Use the code from thePortManager.getInstancePortI().
        ///enh Generate random port if none defined.
        */
      { return thePortManager.getInstancePortI(); }
    
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
	      boolean resultB= false;  // Assume update is not valid.
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
          resultB= true;  // Override default false result.
  	      appLogger.debug("isUpdateValidB() ======== NEWER APP FILE DETECTED ========" +  
  	      		"\n    otherAppFile:    " + Misc.fileDataString(otherAppFile) + 
  	      		"\n    standardAppFile: " + Misc.fileDataString(standardAppFile)
  	      		);
	        } // validation
	      return resultB;
	      }

    private void setInputsV( CommandArgs theCommandsArgs )
      /* This method extracts and saves the inputs this module needs
        from theCommandsArgs.  By convention 
        * arg0 is the path to the file instance of another Java app instance,
          either the running instance that started this app, 
          or a running instance that will be started by this app,
          or both. 
        Other args are not presently used.
        */
      {
        appLogger.debug( "AppInstanceManager setInputsV(..), starting." );
        inputStrings= theCommandsArgs.args();
        if ( theCommandsArgs.switchPresent("-otherAppIs"))
          { 
            otherAppFile=  // Convert value string File name.
                new File(theCommandsArgs.switchValue("-otherAppIs"));
            }
        else if (inputStrings.length == 0) ; // Nothing is an acceptable input.
        else { // Anything else is an error.
          appLogger.error( "AppInstanceManager setInputsV(..), illegal input.  \n  "
              + inputStrings );
          }
        }

	  // Code that manages running instances.
	
	    private AppInstanceListener theAppInstanceListener= // Single listener ...
	      null;  // ...to perform an action if desired.
	    
	    private boolean tryARunningInstanceActionB( )
	      /* Normally this method is called at app start-up.
	        It detects whether there is a previously run running instance
	        by trying to bind a network ServerSocket on a particular port.

          If this app can bind the socket then it means that
          no other running app instance has done so.
          In this case it starts an InstanceManagerThread
          to monitor the socket for future connections and messages from 
          later running instances of this app.
          Then it returns false to indicate that this app instance
          should continue with normal GUI start-up, etc.

	        If this app can't bind the socket then it is probably because 
	        an earlier app instance is running and already did that.
	        In this case it connects to the socket and tries to send through it
	        to the other earlier app instance the path of this app's jar file.
	        If this succeeds then it returns true to indicate that 
	        this app should exit.  The other running instance 
	        will take its own action if appropriate, based on file time-stamps.

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

	          theLocalSocket.bindV(getInstancePortI());
	          { // Setup InstanceManagerThread to monitor the socket.
	            InstanceManagerThread theInstanceManagerThread=
	              new InstanceManagerThread();
	            theInstanceManagerThread.setName("InstcMgr");
	            theInstanceManagerThread.start();
	            } // Setup InstanceManagerThread to monitor the socket.
	
	          theShutdowner.addShutdownerListener( // Adding this listener.
	            new ShutdownerListener() {
	              public void doMyShutdown()
	                // This will cause IOException and terminate InstcMgr thread.
		              {
                    appLogger.info(
                        "AppInstanceManager ShutdownerListener.doMyShutdowner(..),"
                        +" closing socket.");
                    theLocalSocket.closeAllV();
                		appLogger.info( 
                				"AppInstanceManager ShutdownerListener.doMyShutdowner(..), done" 
                				);
		                }
	              });
	          ; // Leaving appShouldExitB false for no app exit.
	        } catch (UnknownHostException e) { // Error.  What produces this??
	          appLogger.error( e.getMessage()+ e );
	          ; // Leaving appShouldExitB false for no app exit.
	        } catch (IOException e) { // This error means port # already in use.
	          appLogger.info( "TCP Port "+getInstancePortI()+" is listening." );
			      appShouldExitB=  // Set exit to the result of...
	            connectAndSendToAppHoldingSocketB( );  // ...sending to it.
	        }
	      return appShouldExitB;  // Return whether or not app should exit.
	      }
	
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
	        boolean successB= false; // Assume failure.
	        String outputString= // Make input string to be used in packet. 
	            "-otherAppIs " + // Infogora -otherAppIs option.
	            runningAppFile.getAbsolutePath(); // Path of this app's file. 
	        try {
	            Socket clientSocket= // Create socket for send.
	              new Socket(InetAddress.getLoopbackAddress(), getInstancePortI());
	            OutputStream out= // Get its stream.
	                clientSocket.getOutputStream();
	            out.write(  // Send output string to other app via stream.
	              outputString.getBytes());
	            out.close();  // Close stream.
	            clientSocket.close();  // Close socket.
	            appLogger.info(
	              "======== SUCCESS SENDING TCP INSTANCE-PACKET ======== :\n  "
	              +outputString );
	            successB= true;  // Packet sent, meaning success and should exit.
	          } catch (IOException e1) {
	            appLogger.error(
                "======== FAILED SENDING INSTANCE-PACKET ======== :\n  " 
                + outputString+ "\n  ", e1);
	          }
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
		          appLogger.info(Thread.currentThread().getName()+": beginning.");
	            while (! theLocalSocket.isClosedB()) {
                try {
                  theLocalSocket.acceptV(); // Wait for connection or exception.
                  { // Do a software update check using data from the socket.
                    updaterReentrantLock.lock(); // Wait until we have lock.
                    try {
                        theLocalSocket.inputFromConnectionV();
                        appLogger.debug("run(): got data.");
			                  processConnectionDataV(
			                      theLocalSocket.getCommandArgs());
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
		          appLogger.info("Socket closed, ending.");
	            }
	        
	        } // class InstanceManagerThread
  
        private void processConnectionDataV(CommandArgs theCommandArgs)
          throws IOException
          /* This method processes the input gotten by processLineFromSocketV().
            It tries to interpret that input as 
            a path to a file which is a possible software update. 
            */
          {
  		      setInputsV(theCommandArgs);
              // Assumes only a single string argument.
  		      logInputsV(); // Report inputs received through socket connection.
  		      if // Exiting or firing event depending on other instance.
  		      	( isUpdateValidB( otherAppFile ) )
  		        { // Report pending update.
  		         	if ( displayUpdateApprovalDialogB( 
  	          			false, // Get approval.
  		        			"A newer running instance of this app "
  		        			+ "has been detected.\n"
  		              + "It will be used in a software update because "
  		              + "it is newer than this app instance.",
  		              otherAppFile
  		        			) ) 
  		         	  {
  		         			// Chain to other app to do copy and run.
  		         				requestForJavaCommandAndExitTrueB( 
  			      					otherAppFile.getAbsolutePath() );
  			      			theShutdowner.requestAppShutdownV(); // App exit.
  		         			}
  		        	}
  		        else
  		        {
  		        	displayUpdateApprovalDialogB( 
  	          			true, // Just inform user.  Don't request approval.
  		        			"Another running instance of this app "
  		        			+ "was detected briefly.\n"
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
	        appLogger.info("displayUpdateApprovalDialogB(..) begins.");
		  		final AtomicBoolean resultAtomicBoolean= new AtomicBoolean(true);
		  		/*  ///tmp ///dbg
	    		final String outString= 
	    				messageString
	    				+ "\nThe file that contains the other app is: "
	    				+ appFile.toString()
	    				+ "\nIt's creation time is: "
	    				+ Misc.dateString(appFile)
	    				+ ( informDontApproveB ? "" : "\nDo you approve?");
		  		EDTUtilities.runOrInvokeAndWaitV( // Run following on EDT thread. 
			    		new Runnable() {
			    			@Override  
			          public void run() {
			    				if (!informDontApproveB) { // Approving.
			    				 	int answerI= JOptionPane.showConfirmDialog(
			    				 		null, // No parent component. 
			                outString,
			                "Infogora Info",
			                JOptionPane.OK_CANCEL_OPTION
			                );
	    		    	  	resultAtomicBoolean.set(
	    		    	  			(answerI == JOptionPane.OK_OPTION) );
			    					}
			    				else // Informing only.
				    				JOptionPane.showMessageDialog(
			    						null, // No parent component. 
			                outString,
			                "Infogora Info",
			                JOptionPane.INFORMATION_MESSAGE
			                );
			    				}
			          } 
			        );
		  		*/  ///tmp ///dbg
    			appLogger.info(
    					"displayUpdateApprovalDialogB(..) ends, value= " 
    					+ resultAtomicBoolean.get() );
		  		return resultAtomicBoolean.get();
		  		}

	  // Code that does file instance management.
	
	    private boolean tryAFileInstanceActionB( )
	      /* Normally this method is called at app start-up,
	        before the GUI has been activated.
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
	        outside the standard folder,
	        which requires control transfers and 
	        a copy of an app file to the standard folder.
	        */
	      { 
	        boolean appShouldExitB= true;  // Set default return app exit.
	
	        toReturn: { // The block after which all returns will go.
		        if // The app command was without arguments.
		        	( otherAppFile == null ) 
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
	        return runningAppFile.equals( standardAppFile );
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
	          ( runningAppFile.lastModified() > standardAppFile.lastModified() )
	          {
	            //appLogger.info("Updating by copying this app file to standard folder.");
	        	  appShouldExitB= copyAppToStandardFolderAndPrepareToRunB();
	            }
	        return appShouldExitB;
	        }
	
	    private boolean trySwitchingToAppInStandardFolderB()
	      /* If the running app is not in the standard folder,
	        but it appears identical to the one in the standard folder,
	        then this method exits this app, 
	        and runs the app in the standard folder.
	        Otherwise it returns.
	        */
	      {
	    		boolean appShouldExitB= false;
	        if // This app is in the standard folder.
	          ( ! runningAppFile.equals( standardAppFile ) )
	          if // The date stamps are equal.
	            ( runningAppFile.lastModified() == standardAppFile.lastModified() )
	            {
	              appLogger.info("Running identical app in standard folder.");
	          	  appShouldExitB= 
	          	    requestForJavaCommandAndExitTrueB( 
	          	    		standardAppFile.getAbsolutePath() );
	              }
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
	                "Another file instance of this app was detected.\n"
	                + "It will now replace this one because it is newer.",
	                otherAppFile
	          			) )
		          	{
			            appLogger.info("An approved updater has been detected.");
			        	  appShouldExitB=   // Chain to arg app to do the copy.
				            requestForJavaCommandAndExitTrueB(
				              otherAppFile.getAbsolutePath() 
				              );
		          		}
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
	          (! runningAppFile.getName().endsWith(".jar"))
	          { // Probably a class file running in Eclipse.  Do normal startup.
	            appLogger.info( "App not jar file, so not copying or exiting.");
	            ///fix Use standard method-name log format.
	            }
	          else
	            while  // Keep trying until copy success and exit.
	              (!EpiThread.exitingB() && !copySuccessB)
	              try 
	                {
	                  appLogger.info( 
	                    "======== COPYING JAR FILE TO STANDARD FOLDER ======== "
	                    );
	                  Files.copy(
	                      runningAppFile.toPath()
	                      ,standardAppFile.toPath()
	                      ,StandardCopyOption.COPY_ATTRIBUTES
	                      ,StandardCopyOption.REPLACE_EXISTING
	                      );
	                  appLogger.info( "Copying successful." );
	              	  copySuccessB= requestForJavaCommandAndExitTrueB( 
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
	                    (new LockAndSignal()).
	                      waitingForInterruptOrDelayOrNotificationE(
	                      		Config.fileCopyRetryPause1000MsL
	                      		); // Wait 1 second.
	                    }
	        return copySuccessB;
	        }
	    
	    private boolean requestForJavaCommandAndExitTrueB( String argString ) 
	      /* This method is equivalent to a 
	        setJavaCommandForExitV( argString )
	        followed by requesting shutdown of this app.
	        It always returns true to simplify caller code and 
	        to indicate that the app should exit.
	        */
	      {
	        setJavaCommandForExitV( argString );  // Setting up command to execute at exit.

	        theShutdowner.requestAppShutdownV(); // Triggering app shutdown.

	        return true;  // Returning true to simplify caller code.
	        }
	    
	    private void setJavaCommandForExitV( String argString )
	      /* This method sets up the execution of a runnable jar file 
	        whose name is argString as a Process.  
	        It will execute as the last step in the app shutdown process.
	        */
	      {
	        String [] commandOrArgStrings= new String[] {
            ( // Path of java command in array.
              System.getProperty("java.home") + 
              File.separator + 
              "bin" + 
              File.separator + 
              "java.exe"
              ),
            "-jar", // java.exe -jar option.
            argString, // Path of .jar file to run
            "-otherAppIs", // Infogora -otherAppIs option.
            runningAppFile.getAbsolutePath() // Path of this app's file. 
	          };
	        ProcessStarter.setCommandV(  // Setting String as command to run later.
	          commandOrArgStrings
	          );
	        }
	
	  // Other miscellaneous code.
	
	    private void logInputsV()
	      /* Logs all inputs, raw, and parsed into
	        file arguments, logged with their time-stamps.
	        */
	      {
	        String logString= "AppInstanceManager.logInputsV()";
	        { // Add parts to the string to be logged.
            logString+= "\n  raw inputs:"; 
            logString+= argsOnLinesString(inputStrings);

            logString+= "\n  parsed inputs: (variable-name: time-stamp, path-name)";
	          logString+= "\n    standardAppFile:    " 
	          		+ Misc.fileDataString( standardAppFile );
	          logString+= "\n    runningAppFile:     " 
	          		+ Misc.fileDataString( runningAppFile );
	          logString+= "\n    otherAppFile:       " 
	          		+ Misc.fileDataString( otherAppFile );
	          logString+= "\n    tcpCopierAppFile:   " 
	          		+ Misc.fileDataString( tcpCopierAppFile );
	          }
	        
	        appLogger.info( logString );  // Log the completed String.
	        }
		    
	    private String argsOnLinesString( String inputStrings[])
	      /* This method returns a String which is the concatenation of lines,
	        each line consisting of 4 spaces, the element index, and the string.
	        The first line is a heading line. 
	        */
	      { 
  	      String logStriing= "\n    " + "n   String";
  	      for (int i=0; i < inputStrings.length; i++) {
            logStriing+= "\n    " + i + "   " + inputStrings[i];
  	        }
	        return logStriing; 
	        }
	}
