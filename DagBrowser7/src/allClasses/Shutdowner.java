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
	    This can happen in two ways:

	    1.  From inside the app, for example when the uses closes 
	      the last of the app's windows.

	    2.  From outside the app, while the JVM is shutting down,
	      when it runs the app's shutdown hook thread.
	      For example, this would happen when the OS shuts down.

	    Note that requestAppShutdownV() might be called from both places,
	    but if it does then the second call has no effect.
    
    2.  Control returns to an app thread which earlier had called 
      Shutdowner.waitForAppShutdownV(). 
      
    3.  The app does an orderly shutdown of all its threads.
    
    4.  The app calls Shutdowner.finishV().
    
    5.  Shutdowner.finishV() performs the last of 
    	the app's shutdown operations, such as calling
    	each of the previously registered ShutdownerListeners, and
    	possibly creating and starting a new Process to replace this one.
    
    6.  Shutdowner.finishV() signals that app shutdown is complete 
      by calling appShutdownDoneLockAndSignal.doNotifyV().
      This is the signal to the ShutdownHook thread that it can stop waiting.

		7. It returns from main(..) thereby terminating the last of its threads,
		  or it calls exit(..).  This will start JVM shutdown unless 
		  it is already underway.

		The JVM shutdown sequence is as follows:
		
		1.  Something initiates JVM shutdown, such as:
		
		  1.  The app's shutdown sequence completes and 
		    last of the it's threads terminate, or the app calls exit(..).
		  
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
    
        1. The execution of an exit command.
        2. The closing of the app's last window.
      
    */

  {

	  private LockAndSignal appShutdownRequestedLockAndSignal= 
	  		new LockAndSignal(false); // Requested by JVM shutdown hook or app.

	  private LockAndSignal appShutdownDoneLockAndSignal= 
	  		new LockAndSignal(false); // Shutdown, excluding libs and JVM, is done.

	  private boolean shutdownUnderwayB= false;
	  
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
	      It requests app shutdown, which might or might not have started already.
	      Next it waits for the app to signal completion of its shutdown. 
	      Then this thread ends.
	      */
	    { // ShutdownHook
	      
    	  public ShutdownHook( String nameString ) // Constructor.
    	  	{ super( nameString ); }
    	  
	      public void run()
	        {
	      		appLogger.info( 
	      				"ShutdownHook.run() beginning, calling requestAppShutdownV()." 
	      				);
	      		requestAppShutdownV(); // Requesting app code shutdown.
	      		  // App code might have done this already.

	      	  appShutdownDoneLockAndSignal.doWaitE(); // Awaiting shutdown done.
	      		appLogger.info( "ShutdownHook.run() app shutdown done, ending." );
	          }
	
	      } // ShutdownHook

    public boolean isShuttingDownB()
      /* This method returns a boolean indication of whether
        the app's shutdown process has begun.
        This method is used to control conditional code.
        * Sometimes extra code must be executed if shutdown is underway,
          such as code to break connections with peers.
        * Sometimes code must be prevented from executing if
          shutdown is underway, such as code which calls modules which
          might themselves be in the process of shutting down.
        */
    	{ return  shutdownUnderwayB; }
    
	  public void waitForAppShutdownUnderwayV()
	    /* This method Waits until either:
	      * app shutdown has started or has been requested, really the same thing.
	      * this thread isInterrupted() is true. 
	     */
	    { 
		  	//appLogger.info( "Shutdowner.waitForAppShutdownStartedV()." );
	  	  appShutdownRequestedLockAndSignal.doWaitE(); 
	  	  }

	  public void requestAppShutdownV()
	    /* This method requests app shutdown and records that it is underway.
	      Shutdown might have begun already, but if it hasn't, it now will.
	      */
	    { 
	  	  if ( shutdownUnderwayB )
		  		appLogger.info( 
		  				"Shutdowner.requestAppShutdownV(), already underway." 
		  				);
		  	  else
		  	  {
			  		shutdownUnderwayB= true; // Recording that shutdown is underway.
			  		appLogger.info( "Shutdowner.requestAppShutdownV() initiating." );
			  	  appShutdownRequestedLockAndSignal.doNotifyV();
		  	    }
	  	  }
	  
    private boolean finishVCalledB= false; // For detecting reentry.
    
    public void finishV()
      /* This method performs the last of an app's shutdown operations,
        which are the following, which it does in the following order:

        1. It calls each of the ShutdownerListeners in the Listener list.
          It does this in the reverse of the order they were added.

        2. It uses ProcessBuilder to create and start a new Process.
          if the argStrings has been defined for doing that.
          This is for chaining from this app instance to another one.
          
        3. It signals that app shutdown is complete by doing a 
	        appShutdownDoneLockAndSignal.doNotifyV().

				After this method is called, the app should exit.
				
				?? Log a list of all unterminated non-daemon threads.  
				There should be none.

        */
      {
    		if ( finishVCalledB ) // Prevent multiple executions of this method.
      		{
            appLogger.error( "Shutdowner.finishV() finishV() already called." );
            return;
        		}
        finishVCalledB= true;

        appLogger.info( "Shutdowner.finishV() beginning, calling listeners." );
        reverseFireShutdownerListeners(); // Calling all listeners in reverse.

        //appLogger.info( 
        //	"Shutdowner.finishV() listeners done, starting process if requested." 
        //	);
        startAProcessV(argStrings); // Executing an external command.

        appLogger.info( 
        		"Shutdowner.finishV() signaling app shutdown done, ending." 
        		);
    	  appShutdownDoneLockAndSignal.doNotifyV(); // Signaling shutdown done.

    	  //appLogger.info( "Shutdowner.finishV(), ending." );
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
        // Fire listeners in the reverse of the order they were added.
        {
          ShutdownerListener theShutdownerListeners[]=
            theEventListenerList.getListeners(ShutdownerListener.class);
          for (int i = theShutdownerListeners.length-1; i>=0; i-=1)
            { 
              ShutdownerListener aShutdownerListener= 
                theShutdownerListeners[i];
              aShutdownerListener.doMyShutdown( );
              }
        }

    // Code for defining and starting other processes and ending this one.

      private String[] argStrings = null; // Command to be executed at exit.

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
