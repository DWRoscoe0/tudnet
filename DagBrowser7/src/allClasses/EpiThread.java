package allClasses;

import static allClasses.AppLog.theAppLog;


public class EpiThread

  extends Thread

  /* This class adds some useful features to Thread.
   
    When dealing with threads, it is important to distinguish between
    "this thread" and the "current thread".
    * "This thread", within an instance method or a constructor,
      refers to the current object, an object in the conventional sense,
      which happens to be a Thread object.
      "This thread" is meaningful only within a Thread 
      instance method or constructor. 
    * "Current thread" refers to the thread which is currently executing.
      There is always a current thread. 
      It might be the same as "this thread", if there is one,
      but they might be different, for example when executing the
      start() and join() methods. 

	  ///enh A destination thread needs only one LockAndSignal instance
	  to manage its inputs regardless of the number of source threads
	  that are providing those inputs.  
	  In fact it makes no sense to have more than one.
	  Therefore, it might make sense to include one LockAndSignal instance
	  and an access method, in every EpiThread for use in these operations??
	  
	  */

  {

	  /* The following constructors vary in the presence or absence of
	    * nameString: a String to be used as the name of the thread,
	      otherwise one will be assigned, but these are problematic.
	    * aRunnable: an instance of a subclass of Runnable to be
	      associated with the EpiThread.  If a Runnable is not specified,
	      the EpiThread is probably being subclassed.
	    These constructors correspond with similar constructors for Thread.
	    */
	
    public EpiThread( String nameString ) // Constructor.
      {
        super( nameString ); // Name here because setName() not reliable.
        }

    public EpiThread( Runnable aRunnable ) // Constructor.
      {
        super( aRunnable );
        }

    public EpiThread( Runnable aRunnable, String nameString ) // Constructor.
      {
        super( aRunnable, nameString );
        }

    public void startV()
      /* This method writes to the log and calls start().  */
      {
      	theAppLog.info("EpiThread(" + getName() + ").startV(): starting.");

      	try { 
        	    start();
        	    } 
      	catch 
    	    (IllegalThreadStateException theIllegalThreadStateException) 
    	    {
            theAppLog.exception(
                "EpiThread.startV() already started",
                theIllegalThreadStateException
                );
    	      }
        }

    public static void stopAndJoinIfNotNullV( EpiThread theEpiThread )
      /* This is like theEpiThread.stopAndJoinV() but 
        does nothing if theEpiThread == null.
        */
	    { if ( theEpiThread != null ) theEpiThread.stopAndJoinV(); }

    public void stopAndJoinV()  // Another thread uses to stop "this" thread.
      /* This method uses stopV() to request termination of "this" thread,
        and then joinV() to wait for that termination to complete.
        */
      {
        stopV(); // Requesting termination of EpiThread thread.
        joinV();  // Waiting until that termination completes.
        }

    public static void stopIfNotNullV( EpiThread theEpiThread )
      /* This is like theEpiThread.stopV() but 
        does nothing if theEpiThread == null.
        */
	    { if ( theEpiThread != null ) theEpiThread.stopV(); }

    public void stopV()  // Requests stopping of "this" thread.
      /* Thread.currentThread() calls this method to request 
        termination of the "this" (EpiThread) Thread.
        It does this by calling this.interrupt().
        Later JoinV() should be called to wait for termination to complete.
        It's okay to call this method more than once because
        the only thing it does is call interrupt().
        
        ?? Threads which use code which blocks without 
        supporting InterruptedException could override this method 
        to take action to end the block, for example by
        closing the socket or stream which might be blocking the thread. 
        */
      {
        /// appLogger.info("EpiThread(" + getName() + ").stopV(): stopping.");

        interrupt(); // Requesting termination of EpiThread thread.
        }

    public void joinV()  // Waits for this thread to terminate.
      /* This method is called to wait for the termination of this's Thread.  
        this.stopV() should already have been called to begin the termination process.
        It uses this.join() to wait until the termination process completes 
        or a safety time-out interval passes.
        It also logs the termination or time-out, whichever happens.

        The time-out was added when it was discovered that some threads,
        ones whose termination was being assisted by closing resources they are using,
        were taking inexplicably long times to terminate.

        When this method returns, Thread.currentThread()'s interrupt status 
        will be set if it was set when the method began or became set
        while this method was executing.  
        Otherwise Thread.currentThread()'s interrupt status will be false.
        */
      {
        // appLogger.debug("joinV(" + getName() + ") begins.");
    	  boolean currentThreadWasInterruptedB= interrupted(); // Save and clear 
    	    // current thread's interrupt status.
        for  // Looping until this's thread has terminated.
          ( boolean waitLoopShouldTerminateB= false ; !waitLoopShouldTerminateB ; )
          try { // Calling blocking join() and handling how it ends.
              join( // Trying to wait for this's thread to terminate
                  Config.threadJoinTimeOutMsL); // or a wait time limit.
              if (isAlive()) // Did thread terminate?
                theAppLog.warning("EpiThread(" + getName() // No, report time-out. 
                  + ").joinV(): termination incomplete after time-out of " 
                  + Config.threadJoinTimeOutMsL + " ms.");
                else
                theAppLog.info("EpiThread(" + getName()  // Yes, report termination.
                    + ").joinV(): termination complete.");
              ; // Yes, being here means this's thread has terminated.
              waitLoopShouldTerminateB= true; // In either case, terminate wait loop.
              }
            catch (InterruptedException e) {  // Handling any new thread interrupt.
              // Being here means current thread's interrupt status was set.
              theAppLog.debug("EpiThread(" + getName() 
                + ").joinV() recording new interrupt.");
            	currentThreadWasInterruptedB= true; // Combine new interrupt with old.
              }
        if  // Setting current thread's interrupt status if it was set earlier.
          ( currentThreadWasInterruptedB )
          Thread.currentThread().interrupt(); // Set interrupt status.
        /// theAppLog.info("EpiThread(" + getName() + ").joinV() ends.");
        }

    public static boolean interruptibleSleepB( long msL )
      /* This method works like Thread.sleep( msI ),
        causing the current thread to sleep for msI milliseconds,
        except that it does not throw an InterruptedException 
        if it was interrupted while sleeping.
        Also if msL is less than 0 it returns immediately
        instead of producing an illegal argument exception.
        Instead, if that happens then the method returns immediately with
        the thread's interrupt status set and the sleep delay incomplete.
        The interrupt status can be sensed and processed by the caller.
        Also it returns true if the sleep was interrupted, false otherwise,
        but because the interrupt status can be sensed,
        this is probably not very useful.
        */
      {
    	  boolean interruptedB= false;
    	  
        try {
        	if ( msL >= 0 ) // Skip if less than 0.
        		Thread.sleep( msL ); // Try to sleep for desired time.
          } 
        catch( InterruptedException ex ) { // Handling interruption.
          Thread.currentThread().interrupt(); // Reestablish interrupted.
        	interruptedB= true; // Changing return value to indicate it.
          }
        
        return interruptedB;
        }

    public static boolean uninterruptibleSleepB( long msL )
      /* This method works exactly like interruptibleSleepB(..)
        except instead of returning immediately 
        if sleep is interrupted, it sleeps again, 
        repeatedly if needed, until the thread has slept the full delay of msL.
        Because this can delay a thread for long periods of time,
        it should only be used for debugging and testing
        or when it is certain that the delay will not cause a problem.
        */
      {
		  	long beginTimeMsL= // Recording beginning of sleep interval. 
			  		System.currentTimeMillis();
		  	long endTimeMsL=  // Calculating end of sleep interval.
		  			beginTimeMsL + msL;
    	  boolean interruptedB= false;

    	  while (true) { // repeatedly sleep until full delay passes.
    	  	if // Record and clear thread interrupt status.
    	  	  (Thread.currentThread().isInterrupted())
    	  		interruptedB= true;
    	  	long remainingMsL= // Calculate time in ms remaining to sleep. 
    	  			endTimeMsL - System.currentTimeMillis();
    	  	if ( remainingMsL <= 0L ) break; // Exit if there's no remaining time. 
    	  	interruptibleSleepB( remainingMsL ); // Try sleeping for that time.
    	  	}

    	  if ( interruptedB ) // IF an interruption happened at any time
    	  	Thread.currentThread().interrupt(); // reestablish interrupt status.
      	return interruptedB;
        }

    public static boolean testInterruptB()
      /* This tests the current thread's interrupt status 
        but it does not clear it or change it in any other way.
        This means it it can be used to exit 
        multiple levels of methods and loops.
        */
      { 
        return Thread.currentThread().isInterrupted(); // Just return status.
        }
		
		}