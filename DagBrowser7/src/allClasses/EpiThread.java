package allClasses;

import static allClasses.Globals.*;  // appLogger;

public class EpiThread

  extends Thread

  /* This class adds some useful features to Thread.

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
      	appLogger.info("EpiThread(" + getName() + ").startV(): starting.");

        start();
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
        //appLogger.info("EpiThread(" + getName() + ").stopV(): stopping.");

        interrupt(); // Requesting termination of EpiThread thread.
        }

    public void joinV()  // Waits for this thread to terminate.
      /* Thread.currentThread() calls this method to wait for the
        termination of this's EpiThread.
        this.stopV() should already have been called.
        This method will not return until the this's EpiThread terminates.
        It uses this.join() to wait until that termination completes.
        
        When this method returns, Thread.currentThread()'s interrupt status 
        will set if it was set when the method began or became set
        while the method was executing.  
        Otherwise Thread.currentThread()'s interrupt status will be false.
        */
      {
    	  boolean currentThreadInteruptStatusB= false;
        for  // Looping until this's thread has terminated.
          ( boolean thisThreadTerminatedB= false ; 
        		!thisThreadTerminatedB ; 
        		)
          try { // Calling blocking join() and handling how it ends.
              join();  // Trying to wait for this's thread to terminate.
              // Being here means this's thread terminated.
              thisThreadTerminatedB= true;  // Setting flag to terminate loop.
              }
            catch (InterruptedException e) {  // Handling interrupt of join().
              // Being here means current thread's interrupt status was set.
            	currentThreadInteruptStatusB= true; // Recording it for later.
              }
        if  // Restoring current thread's interrupt status if recorded earlier.
          ( currentThreadInteruptStatusB )
          Thread.currentThread().interrupt(); // Setting interrupt status.
        appLogger.info("EpiThread(" + getName() + ").joinV(): stopped.");
        }

    public static boolean interruptableSleepB( long msL )
      /* This method works like Thread.sleep( msI ),
        causing the current thread to sleep for msI milliseconds,
        except that it does not throw an InterruptedException 
        if it was interrupted while sleeping.
        Instead, if that happens then the method simply returns with
        the thread's interrupt status set.
        It can be processed by the caller.
        It returns true if the sleep was interrupted, false otherwise.  
        */
      {
    	  boolean interruptedB= false;
    	  
        try {
          Thread.sleep( msL ); // Try to sleep for desired time.
          } 
        catch( InterruptedException ex ) { // Handling interruption.
          Thread.currentThread().interrupt(); // Reestablish interrupted.
        	interruptedB= true; // Changing return value to indicate it.
          }
        
        return interruptedB;
        }

		public static boolean exitingB()
		  /* This is a method to save a little typing.
		    Like the Thread method that it calls,
		    it clears this thread's interrupted status.
		    */
			{ return Thread.currentThread().isInterrupted(); }
		
		}