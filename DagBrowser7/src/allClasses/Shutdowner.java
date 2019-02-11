package allClasses;

import java.io.IOException;
//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Enumeration;
//import java.util.Vector;



import javax.swing.event.EventListenerList;

import static allClasses.Globals.*;  // appLogger;

public class Shutdowner

  /* This class is used for app shut-down.  

   Shutting down a Java app is tricky.  
   Java uses threads a lot, maybe too much.
   Knowing when app termination has been requested requires
   creating a thread and registering it as a shutdown hook.
   Shutdown hook threads are started shortly after JVM termination begins.

   In theory, a shutdown hook can perform shutdown operations.
   This is suggested indirectly by the documentation.

   In practice, it is probably better for a shutdown hook to simply
   signal one or more other app threads to do their shutdowns
   and then wait for them to finish those operations.
   This is the approach taken by this Shutdowner class.  

   This class does the following:

    1. Except in the case of severe system errors,
      it makes it provides the ability to guarantee
      that app shutdown completes before JVM shutdown does,
      regardless of which one starts to shutdown first.
      It does this by creating, initializing, and registering with the JVM,
      a shutdown hook thread with the ability to send to another thread 
      a signal that shutdown has been requested, 
      and the ability to receive a signal from that second thread 
      when its shutdown operations are done.

    2. It manages a list of app ShutdownerListeners 
      which are called as part of app shutdown.  
      This is similar to what Java's Runtime shutdown hook threads do,
      but with a well-defined order.  

    3. It provides the ability to create and start 
      a user-defined executable Process just before this app terminates.
      The new process is often a different instance or version
      of this same app.  It is intended to be used for 
      instance management and software updating.

    Shutdown can be initiated by either
    * a trigger internal to the app.  Examples of internal triggers are:
      *. The closing of the app's last window.
      *. The AppInstanceManager chaining to another process.
      *. The execution of an exit(int) command.
    * a trigger external to the app.  Examples of external triggers are:
      * The user logging out.
      * A manual process termination.
      * A computer restart.

    What follows are two example shutdown sequences: 
    * one for external triggers, and
    * one for internal triggers.
    Note, however, that many other sequences are possible if
    a second shutdown trigger of the opposite type
    happens before the first shutdown trigger is fully processed.

    The sequence for internally initiated shutdowns is as follows:
    * The app adds Shutdowner listeners to the listener list
      that will do shutdown actions that can't be done by normal means.
    * The main app starts auxiliary threads to perform the app's work.
    * The main app thread calls Shutdowner.waitForAppShutdownRequestedV().
    * Normal, non-shutdown app activity occurs in its auxiliary threads.
    Pre-shutdown preparations are done.  Shutdown begins now.
    * One of the app's auxiliary threads decides that shutdown is desired
      and calls Shutdowner.requestAppShutdownV() to begin that sequence.  
    * The app's main thread returns from 
      its call to waitForAppShutdownRequestedV(). 
    * The app's main thread does an orderly shutdown of 
      everything that it can shut down, such as 
      flushing buffers, closing files, closing connections, 
      signaling and waiting for the termination of 
      other non-daemon threads.
      The only shutdown activity it does not do is what will be done by 
      the previously mentioned Shutdowner listeners.
    * The app's main thread calls Shutdowner.finishAppShutdownV().
    * Shutdowner.finishAppShutdownV() performs the last of 
      the app's shutdown operations, such as calling
      each of the previously registered ShutdownerListeners, and
      possibly creating and starting a new Process to replace this one.
    * Shutdowner.finishAppShutdownV() signals that app shutdown 
      is complete by calling appShutdownDoneLockAndSignal.doNotifyV().
      This is a signal to the shutdown hook thread that it can stop waiting.
      In this example, the shutdown hook thread hasn't started yet,
      but when it is finally started, it will finish quickly.
    * The app's main thread returns from Shutdowner.finishAppShutdownV().
    * The app's main thread terminates by either 
      calling exit(..) or returning from the app's main(..) method.
      Assuming other non-daemon threads have terminated,
      this initiates the shutdown of the JVM.
    * The JVM starts all of its registered shutdown hook threads.
    * The Shutdowner's shutdown hook thread calls 
      Shutdowner.requestAppShutdownV().  This call has no affect because
      requestAppShutdownV() was called earlier by another app thread.  
    * The Shutdowner's shutdown hook thread waits for 
      app shutdown completion to be signaled.  
      This wait ends immediately because shutdown completion 
      was signaled earlier by finishAppShutdownV().
    * The Shutdowner's shutdown hook thread terminates. 
    * The JVM finishes waiting for 
      all of its shutdown hook threads to terminate.
    * The JVM calls finisher methods, if this feature is enabled, and halts.
    Shutdown is complete.

    The shutdown sequence for externally initiated shutdowns 
    At first it's the same as for internal triggers.
    * The app adds Shutdowner listeners to the listener list
      that will do shutdown actions that can't be done by normal means.
    * The main app starts auxiliary threads to perform the app's work.
    * The main app thread calls Shutdowner.waitForAppShutdownRequestedV().
    * Normal, non-shutdown app activity occurs in its auxiliary threads.
    Pre-shutdown preparations are done.  Shutdown begins now.
    Here is where things become different.
    * An external event, such as a user logout or a system restart,
      causes the Operating System OS to signal the JVM 
      that it must terminate.  This initiates the shutdown of the JVM.
    * The JVM starts all of its registered shutdown hook threads.
    * The Shutdowner's shutdown hook thread calls 
      Shutdowner.requestAppShutdownV().  
    * The app's main thread returns from 
      its call to waitForAppShutdownRequestedV(). 
    * The app's main thread does an orderly shutdown of 
      everything that it can shut down, such as 
      flushing buffers, closing files, closing connections, 
      signaling and waiting for the termination of 
      other non-daemon threads.
      The only shutdown activity it does not do is what will be done by 
      the previously mentioned Shutdowner listeners.
    * The app's main thread calls Shutdowner.finishAppShutdownV().
    * Shutdowner.finishAppShutdownV() performs the last of 
      the app's shutdown operations, such as calling
      each of the previously registered ShutdownerListeners, and
      possibly creating and starting a new Process to replace this one.
    * Shutdowner.finishAppShutdownV() signals that app shutdown 
      is complete by calling appShutdownDoneLockAndSignal.doNotifyV().
      This is a signal to the shutdown hook thread that it can stop waiting.
    * The Shutdowner's shutdown hook thread, which has been waiting for 
      app shutdown completion to be signaled, ends its waiting, 
      and terminates. 
    * The JVM, which has been waiting for all of its shutdown hook threads 
      to terminate, resumes.
    * The JVM calls finisher methods, if this feature is enabled, and halts.
    Shutdown is complete.
    Note, finishAppShutdownV() need not, and might not, return, because
    shutdown was triggered externally and JVM shutdown was already underway.
    
    */

  {

    private boolean shutdownRequestedB= false;
      // This is needed to detect re-entry because testing and waiting 
      // clear the appShutdownRequestedLockAndSignal notification flag.
    
	  private LockAndSignal appShutdownRequestedLockAndSignal=
	  		new LockAndSignal(); // This is signaled when shutdown is requested.
	      // It is signaled by Shutdowner.requestAppShutdownV()
	      // It is waited-for by Shutdowner.waitForAppShutdownRequestedV()
	      // A shutdown can be triggered by either the JVM shutdown hook or 
	      // an app request.
	  
	  private LockAndSignal appShutdownDoneLockAndSignal= 
	  		new LockAndSignal(); // This is signaled when shutdown is complete.
	      // It means all app shutdown actions, excluding JVM, are done.
	      // It is signaled by Shutdowner.finishAppShutdownV().
	      // It is waited-for by ShutdownHook.run().
	  
	  public void initializeV() // Prepares ShutdownHook as shutdown hook.
		  {
	      Runtime.getRuntime().addShutdownHook(
	      		new ShutdownHook("ShutDwn")
	      		); // Adding...
	        // ...it to Runtime to be run at shut-down time.
		    }

    class ShutdownHook  // To allow the JVM to terminate this app.
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
	        // This method runs when the JVM's shutdown hook thread activates. 
	        {
	      		appLogger.info( 
	      				"ShutdownHook.run() beginning, "
	      				+ "calling requestAppShutdownV() on behalf of JVM." 
	      				);
	      		requestAppShutdownV();

	      	  appShutdownDoneLockAndSignal.
	      	    waitingForInterruptOrNotificationE(); // Waiting
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
	    /* This method requests app shutdown.
	      It also records that it is underway.
	      It detects any re-entry of this method and logs it 
	      but otherwise ignores it.
	      It may be called multiple times from many different places.
	      Only the first call is significant. 
	      */
	    { 
	  	  if ( shutdownRequestedB )
	      	appLogger.info(  // Log re-entry.
		  				"Shutdowner.requestAppShutdownV(), called again, ignored." 
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
        * app shutdown has been requested, or
        * this thread's isInterrupted() is true. 
       */
      { 
        appShutdownRequestedLockAndSignal.
          waitingForInterruptOrNotificationE(); 
        appLogger.info( "Shutdowner.waitForAppShutdownRequestedV() done." );
        }

    private boolean finishVCalledB= false; // For detecting re-entry.

    public void finishAppShutdownV()
      /* This method performs the last of an app's shutdown operations,
        operations managed by this class,
        operations that can not easily be done earlier by normal means.
        These operations are does in the following order:

        1. It calls each of the ShutdownerListeners in the Listener list.
          It does this in the reverse of the order in which they were added 
          by calls to addShutdownerListener(ShutdownerListener listener) 
          earlier.

        2. If the shutdown of this process is not the final shutdown,
           meaning this process is being replaced by another process,
           then ProcessBuilder is used to create and start that next process.
           If this is the final Infogora process shutdown then 
           it signals this to the Infogora starter process by 
           deleting flag file InfogoraAppActiveFlag.txt.
          
        3. It signals this classes shutdown hook thread,
          which was previously registered with the JVM,
          that this app's shutdown is complete,
          at least everything this app's Java code can do.
          This is done by calling appShutdownDoneLockAndSignal.doNotifyV().  
          The ShutdownHook thread might, or might not, 
          be waiting for this notification at this time.    

				If this method is able to return, the app should exit ASAP.
				
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
	        appLogger.info( "Shutdowner.finishAppShutdownV() beginning, calling ShutdownerListeners." );
	        reverseFireShutdownerListeners(); // Calling all listeners in reverse.
	        if (argStrings != null) // Act based on whether this process exit is final exit.
	          startProcess(argStrings); // No, start other process before exiting.
  	        else { // Yes, signal Infogora starter process by deleting flag file.
  	          appLogger.info("Shutdowner.finishAppShutdownV() deleting InfogoraAppActiveFlag.txt.");
  	          Config.makeRelativeToAppFolderFile("InfogoraAppActiveFlag.txt").delete();
  	          }
	        appLogger.info( 
	        	"Shutdowner.finishAppShutdownV() notify shutdown hook that shutdown is done, ending.");
          appLogger.setBufferedModeV( false ); // This closes log file.
	    	  appShutdownDoneLockAndSignal.notifyingV(); // Signal shutdown hook.
	    	    // Flow might end here if JVM initiated shutdown.
  			} // toReturn:
        }
    
    // ShutdownerListener code.  Maintains and calls our ShutdownListeners.
    
      private EventListenerList theEventListenerList= new EventListenerList();

      public synchronized void addShutdownerListener
        ( ShutdownerListener listener ) 
        {
          appLogger.debug("addShutdownerListener("+listener+")"); 
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
        // Fire listeners in the reverse of the order they were added.
        {
          ShutdownerListener theShutdownerListeners[]=
            theEventListenerList.getListeners(ShutdownerListener.class);
          for (int i = theShutdownerListeners.length-1; i>=0; i-=1)
            { 
              ShutdownerListener aShutdownerListener= 
                theShutdownerListeners[i];
              appLogger.debug( 
              	"reverseFireShutdownerListeners( ): calling ShutdownerListener: "
              	+ aShutdownerListener ); 
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

      public Process startProcess(String... inArgStrings)
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
	                "Shutdowner.startAProcessV(..) w: " 
	                + Arrays.toString(inArgStrings)
	                );
	              ProcessBuilder myProcessBuilder= // Build the process. 
	                new ProcessBuilder(inArgStrings);
	              
	              resultProcess= // Start the process.
	                  myProcessBuilder.start();
	
	              appLogger.info( "Shutdowner.startAProcessV(..): succeeded." ); 
              } catch (IOException e1) {
                appLogger.error( "Shutdowner.startAProcessV(..): FAILED." ); 
              }
          return resultProcess;
          }

    }
