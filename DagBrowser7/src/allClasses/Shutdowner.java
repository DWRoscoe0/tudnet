package allClasses;

import java.io.IOException;
//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Enumeration;
//import java.util.Vector;



import javax.swing.event.EventListenerList;

import static allClasses.Globals.*;  // appLogger;

public class Shutdowner

  /* This class is used for app shut-down.  It does the following:

    1. It manages a list of app ShutdownerListeners 
      which are called as part of app shutdown.  
      This is similar to what Java's Runtime ShutdownHook threads do,
      but in a more controlled order.  

    2. It provides the ability to create and start 
      a user-defined executable Process just before this app terminates.
      The process is usually a different instance or version
      of this same app.  It is intended to be used for 
      instance management and software updating.
      
    3. It provides the ability to make certain that app shutdown completes 
      before java virtual machine (JVM) shutdown does,
      regardless of which one starts to shutdown first.
      It does this using one Java Runtime ShutdownHook thread
      to initiate app shutdown if it hasn't already been initiated,
      and then wait for app shutdown to complete.

    The app shutdown sequence is as follows:

	  1.  requestAppShutdownV() is called.  This initiates app shutdown.
	    It can be called from various places, and can happen in two ways:

	    1.  From inside the app, for example when the user closes 
	      the last of the app's windows.

	    2.  From outside the app, when the JVM is shutting down,
	      and it runs the app's shutdown hook thread.
	      For example, this could happen when the OS shuts down.
	      ///doc Test this by restarting windows and checking log.

	    Note that requestAppShutdownV() might be multiple times,
	    but if it is then all calls but the first call are ignored,
	    except for logging each call.

    2.  In the app's main thread, control returns from
      Shutdowner.waitForAppShutdownRequestedV(). 

    3.  The app main thread does an orderly shutdown of 
      some of the things it started, including all its other threads.

    4.  The app's main thread calls Shutdowner.finishAppShutdownV()
      to do any remaining shut operations.

    5.  Shutdowner.finishAppShutdownV() performs the last of 
    	the app's shutdown operations, such as calling
    	each of the previously registered ShutdownerListeners, and
    	possibly creating and starting a new Process to replace this one.
    
    6.  Shutdowner.finishAppShutdownV() signals that app shutdown is complete 
      by calling appShutdownDoneLockAndSignal.doNotifyV().
      This is the signal to the ShutdownHook thread that it can stop waiting,
      and terminate, and that the JVM shutdown may continue if it is underway.

		7.  Shutdowner.finishAppShutdownV() returns, 
		  and the app's main(..) thread and terminates either 
		  by calling exit(..) or returning from the app's main(..) method.
		  This starts the JVM shutdown unless it is already underway.

		The JVM shutdown sequence is as follows:

		1.  Something initiates JVM shutdown, such as:

		  1.  The app's shutdown sequence completes and 
		    the last of the it's threads terminates, or the app calls exit(..).

		  2.  An external trigger, such as an OS restart.
		
		2.  The JVM starts all registered shutdown hooks in some unspecified order. 
		  One of these is the app's shutdown hook, which does only two things:
		  
		  1.  It calls requestAppShutdownV(), which starts 
		    the app's shutdown sequence unless it has been started already.
		    
		  2,  It waits until the app's shutdown sequence is complete.

    3.  The JVM waits until all shutdown hook threads, 
      including the app's shutdown hook described above,  have finished.

    4.  The JVM runs all un-invoked finalizers if 
      finalization-on-exit has been enabled.
      
    5.  The JVM halts.

	  The app's shutdown hook guarantees that under normal circumstances,
	  regardless of whether shutdown is initiated internally or externally,
	  app shutdown will be started and finished before JVM shutdown completes.

    Examples of how shutdowns might be triggered external to the app are:
    
        1. A computer shutdown.
        2. A computer restart.
        3. A manual process termination.

    Examples of how shutdowns might be triggered internal to the app are:
    
        1. The closing of the app's last window.
        2. The AppInstanceManager chaining to another Infogora process.
        3. The execution of an exit command because of a fatal error.
       
    */

  {

    private boolean shutdownRequestedB= false;
      // This is needed to detect re-entry because testing and waiting 
      // clear the appShutdownRequestedLockAndSignal notification flag.
    
	  private LockAndSignal appShutdownRequestedLockAndSignal=
	  		new LockAndSignal(); // This is signaled when a shutdown is requested.
	      // It is signaled by Shutdowner.requestAppShutdownV()
	      // It is waited-for by Shutdowner.waitForAppShutdownUnderwayV()
	      // A shutdown can be triggered by either the JVM shutdown hook or 
	      // an app request.
	  
	  private LockAndSignal appShutdownDoneLockAndSignal= 
	  		new LockAndSignal(); // This is signaled when shutdown actions are complete.
	      // It means all app shutdown actions, excluding libs and JVM, are done.
	      // It is signaled by Shutdowner.finishAppShutdownV().
	      // It is waited-for by ShutdownHook.run().
	  
	  public void initializeV() // Prepares ShutdownHook as shutdown hook.
		  {
	      Runtime.getRuntime().addShutdownHook(
	      		new ShutdownHook("ShutDwn")
	      		); // Adding...
	        // ...it to Runtime to be run at shut-down time.
		    }

    class ShutdownHook  // For terminating the app.
	    extends Thread
	    /* This nested shutdown hook Thread's run() method
	      is started when JVM shutdown is underway.
	      It requests or re-requests app shutdown.
	      Next it waits for the app to signal completion of its shutdown. 
	      Then this thread ends.
	      */
	    { // ShutdownHook
	      
    	  public ShutdownHook( String nameString ) // Constructor.
    	  	{ super( nameString ); }
    	  
	      public void run()
	        /* This method run when the ShutdownHook thread activates.  */ 
	        {
	      		appLogger.info( 
	      				"ShutdownHook.run() beginning, calling requestAppShutdownV()." 
	      				);
	      		requestAppShutdownV(); // Requesting app-shutdown.

	      	  appShutdownDoneLockAndSignal.waitingForInterruptOrNotificationE(); // Waiting
	      	    // for app-shutdown to complete.
	      		appLogger.info( "ShutdownHook.run() ending. ======== APP SHUTDOWN DONE ========");
	          }
	
	      } // ShutdownHook

    @SuppressWarnings("unused") ////
    private boolean isShuttingDownB() // Not referenced anywhere.
      /* This method returns a boolean indication of whether
        the app's shutdown process has begun.
        This method is used to control conditional code.
        * Sometimes extra code must be executed if shutdown is underway,
          such as code to break connections with peers.
        * Sometimes code must be prevented from executing if
          shutdown is underway, such as code which calls modules which
          might themselves be in the process of shutting down.
        */
    	{ return  shutdownRequestedB; }

	  public synchronized void requestAppShutdownV()
	    /* This method requests app shutdown and records that it is underway.
	      It detects any re-entry of this method and logs it but otherwise ignores it.
	      It may be called from many different places. 
	      */
	    { 
	  	  if ( shutdownRequestedB )
	      	appLogger.info(  // Log re-entry.
		  				"Shutdowner.requestAppShutdownV(), called again." 
		  				);
	  	  else
  	  	  {
            shutdownRequestedB= true; // Recording that shutdown is underway.
  		  		appLogger.info( "Shutdowner.requestAppShutdownV() called." ); // Log it.
  		  	  appShutdownRequestedLockAndSignal.notifyingV();
  	  	    }
	  	  }

    public void waitForAppShutdownRequestedV()
      /* This method Waits until either:
        * app shutdown has started or has been requested, really the same thing, or
        * this thread's isInterrupted() is true. 
       */
      { 
        appShutdownRequestedLockAndSignal.waitingForInterruptOrNotificationE(); 
        appLogger.info( "Shutdowner.waitForAppShutdownRequestedV() done." );
        }

    private boolean finishVCalledB= false; // For detecting re-entry.

    public void finishAppShutdownV()
      /* This method performs the last of an app's shutdown operations,
        operations that can not easily be done earlier.
        These operations are does in the following order:

        1. It calls each of the ShutdownerListeners in the Listener list.
          It does this in the reverse of the order in which they were added by
          calling addShutdownerListener(ShutdownerListener listener) earlier.

        2. If the shutdown of this process is not the final shutdown then
           ProcessBuilder is used to create and start the next process.
           If this is the final Infogora process shutdown then it signals this to
           the Infogora starter process by deleting flag file InfogoraAppActiveFlag.txt.
          
        3. It signals the ShutdownHook thread that app shutdown is complete by doing a 
	        appShutdownDoneLockAndSignal.doNotifyV().  The ShutdownHook thread might,
	        or might not, be waiting for this notification.    

				After this method is called, the app should exit ASAP.
				
				///enh ? Log a list of all unterminated non-daemon threads.  
				There should be none.

        */
      {
    	  toReturn: {
          synchronized (this) {
  	    		if ( finishVCalledB ) { // Exiting if executed before.  This is  re-entry.
              appLogger.error( "Shutdowner.finishAppShutdownV() finishAppShutdownV() already called." );
              break toReturn;
          		}
  	        finishVCalledB= true; // Do this in case this method is re-entered.
            }
	        appLogger.info( "Shutdowner.finishAppShutdownV() beginning, calling listeners." );
	        reverseFireShutdownerListeners(); // Calling all listeners in reverse.
	        if (argStrings != null) // Act based on whether this process exit is final exit.
	          startAProcessV(argStrings); // No, start other process before exiting.
  	        else { // Yes, signal Infogora starter process by deleting flag file.
  	          appLogger.info("Shutdowner.finishAppShutdownV() deleting InfogoraAppActiveFlag.txt.");
  	          Config.makeRelativeToAppFolderFile("InfogoraAppActiveFlag.txt").delete();
  	          }
	        appLogger.info( 
	        	"Shutdowner.finishAppShutdownV() notify shutdown hook that shutdown is done, ending.");
          appLogger.setBufferedModeV( false ); // Disabling buffered logging.
	    	  appShutdownDoneLockAndSignal.notifyingV(); // Signaling ShutdownHook thread.
  			} // toReturn:
        }
    
    // ShutdownerListener code.  Maintains and calls ShutdownListeners.
    
      private EventListenerList theEventListenerList= new EventListenerList();

      public synchronized void addShutdownerListener
        ( ShutdownerListener listener ) 
        {
          theEventListenerList.add(ShutdownerListener.class, listener);
          }

      public synchronized void removeShutdownerListener
        ( ShutdownerListener listener ) 
        {
          theEventListenerList.remove(ShutdownerListener.class, listener);
          }

      /* forwardFire...  not used.
      private synchronized void fireShutdownerListeners( )
        // Fire listeners in the same order they were added.
        {
          for 
            ( ShutdownerListener aShutdownerListener: 
              theEventListenerList.getListeners(ShutdownerListener.class)
              )
            aShutdownerListener.doMyShutdown( );
        }
      */

      private synchronized void reverseFireShutdownerListeners( )
        ///fix? Should this be synchronized?
        // Fire listeners in the reverse of the order they were added.
        {
          ShutdownerListener theShutdownerListeners[]=
            theEventListenerList.getListeners(ShutdownerListener.class);
          for (int i = theShutdownerListeners.length-1; i>=0; i-=1)
            { 
              ShutdownerListener aShutdownerListener= 
                theShutdownerListeners[i];
              //appLogger.debug( 
              //		"reverseFireShutdownerListeners( ): calling listener." 
              //	); 
              aShutdownerListener.doMyShutdown( );
              }
        }

    // Code for defining and starting other processes and ending this one.

      private String[] argStrings = null; // Command to be executed at exit.
        // If null then no command is to be executed.

	    public void setCommandV( String... inArgStrings )
	      /* This method sets to inArgStrings the array of Strings which
	        defines the command Process to be created and executed 
	        at shut-down time by ProcessBuilder.
	        If at shutdown time inArgStrings is null 
          then no command will be executed.
	        */
	      {
          appLogger.info( 
          	"Shutdowner.setCommandV(..): " 
            + Arrays.toString(inArgStrings)
            );
          
	    	  argStrings = inArgStrings; 
	    	  }

      private void startAProcessV(String... inArgStrings)
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
	        if  // Executing an external command if...
		        ( inArgStrings != null ) // ...command arguments were defined.
            try {
	              appLogger.info( 
	                "Shutdowner.startAProcessV(..) w: " 
	                + Arrays.toString(inArgStrings)
	                );
	              ProcessBuilder MyProcessBuilder= // Build the process. 
	                new ProcessBuilder(inArgStrings);
	              
	              MyProcessBuilder.start();  // Start the process.
	
	              appLogger.info( "Shutdowner.startAProcessV(..): succeeded." ); 
              } catch (IOException e1) {
                appLogger.error( "Shutdowner.startAProcessV(..): FAILED." ); 
              }
          }

    }
