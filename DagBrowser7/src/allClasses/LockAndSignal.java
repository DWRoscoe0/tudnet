package allClasses;

//import static allClasses.Globals.appLogger;

public class LockAndSignal  // Combination lock and signal class.

  /* An instance of this class may be used by a thread to manage and process
    several types of inputs without using busy-waiting.  The types are: 

    * Data from a data source (Input.NOTIFICATION).
    * The passage of real time (Input.TIME and Input.TIME_PASSED.
    * Thread.isInterrupted() (Input.INTERRUPTION).

    A thread can make use of any one, two, or all three types of inputs.
    
    Though this class is thread-safe, the thread-safety of 
    the objects used to pass NOTIFICATION data between threads, 
    queues for example, must be guaranteed by other means. 
    
		The doNotify() method is called by source threads
    to indicate to destination threads the readiness of new inputs.
    It can be called using a direct LockAndSignal instance reference,
    or called indirectly, for example in the add(..) method of 
    the InputQueue class which contains a LockAndSignal reference.

    The doWait...() methods are called by the destination thread
    to wait until new input has been provided and signaled by source threads.
    They will also return if a user-specified time-out passes
    or if the thread isInterrupted() returns true.
    Typically a destination thread will have a loop containing:
	    
	    * A call to one of the LockAndSignal.doWait...() methods.
	    * The processing of at least one of the inputs that became available.

    A destination thread needs only one LockAndSignal instance
    to manage its inputs regardless of the number of source threads
    that are providing those inputs.  
    In fact it makes no sense to have more than one.
    Therefore, it might make sense to include one LockAndSignal
    in every EpiThread for use in these operations??

    A destination thread's one LockAndSignal instance may also be used 
    as the monitor lock for synchronized code blocks.
    
	  This class works by serving the following two roles:
	  
	    * As a monitor-lock for Object.wait(..), Object.notify(),
	      and synchronized code blocks.
	    * As an associated boolean NOTIFICATION signal variable.

    This class was created because neither the Java boolean primitive nor 
    the wrapper Boolean class could be used for these two roles.
    The wrapper Boolean is immutable.  Assuming aBoolean == false, then
    "aBoolean= true;" is equivalent to "aBoolean= new Boolean(true);", 
    which changes the object referenced by aBoolean,
    which causes an IllegalMonitorStateException.
    This class solves this problem by using
    methods to access an internal mutable boolean value.

    When control is returned from any of the doWait...() methods,
    it returns a value of type Input.
    The range of this return value depends on the call and execution context.
    The Input type value returned is the type of 
    the first (highest priority) input found.
		These values, listed in increasing priority order, are:

      NOTIFICATION // doNotify() was called at least once.
      TIME // The time limit, either time-out or absolute time, was reached.
      //TIME_LATE // Same as TIME but the time limit has long passed!
      INTERRUPTION // The thread's isInterrupted() status is true.

    On return, if only one Input type is to be checked and processed, 
    then it should be the one specified by the returned value.
    The returned value may be ignored, but if it is ignored then 
    the caller should check for and process manually ALL possible Input types.
    
    Also, if any NOTIFICATION inputs are available, 
    then ALL NOTIFICATION inputs must be checked and processed.
    This is because they share a single NOTIFICATION flag signalB.
    This can be done in multiple ways:

    * As an example, if processing only NOTIFICATION inputs,
     an obvious way to do it is to do this:

	      while ( true ) {
		      doWaitE(); // Wait for the next batch of inputs.
		      while ( unprocessedNotificationInputsAvailableB() )
		        processOneNotificationInput();
		      }

      but this way adds nested loops, which can make coding 
      more complex systems difficult.  For example, 
      it makes encoding the state of an algorithm or protocol 
      with the position of a thread's instruction pointer virtually impossible.

    * Another way that might be less obvious, 
      but makes easier coding of complex systems,
      is to first create a specialized test-or-wait method, such as this:

        Input testWaitE() 
	        {
	          if ( unprocessedNotificationInputsAvailableB() )
	            return NOTIFICATION; // Returning immediately.
	            else
	            return doWaitE(); // Waiting for next batch.
	          }

      and use it this way:

	      while ( true ) {
		      testWaitE(); // Wait for any single input.
		      processOneNotificationInput();
		      }

		  No nested loops are required.  Using a method like testWaitE()
		  can greatly simplify the coding of complex protocols and algorithms.

    ?? Create a similar class which takes an argument to the constructor
    which is the client object to be used as 
    the monitor lock to use instead of the LockAndSignal object.
    This would make use with synchronized methods in other classes easier.
    Unfortunately this defeats the goal of using 
    constructor Dependency Injection.
    It creates a circular dependency between the LockAndSignal and its client
    that can not be eliminated by restructuring.
    It requires dividing the client, as follows:
    
      theClientPart1: // The lock object which also contains thread-safe state.
        methods synchronized on (ClientPart1 this).
        
      theLockAndSignal:
        lockObject: ref:theClientPart1
        
      theClientPart2: // For thread that receives inputs.
        theClientPart1: ref:theClientPart1:
        theLockAndSignal: ref:theLockAndSignal
          // Used by input wait loop.  

    ?? It might be worthwhile to use an object reference, or a null,
    instead of a boolean, to be the signal.
    Additional versions of the doWait() and doNotify() methods
    could pass these values.  This would allow passing of
    more complex information as if through a single element queue,
    but only from one specific object to another.
    Or maybe it would be better to have a different class for this.
    */
  {

    public enum Input {  // Return value indicating an input which has appeared.
    	// These values are listed from lowest to highest priority.
    	// NONE, // There has been no input.
      NOTIFICATION, // doNotify() was called to signal data input.
      TIME, // The time limit, either time-out or absolute time, was reached.
      // TIME_LATE, // Same as TIME but the time limit has long passed!
      INTERRUPTION, // The thread's isInterrupted() status is true.
      }  // These return values can simplify input processing in some cases.
      
    private boolean signalB= false;  // NOTIFICATION flag.


    // Input wait methods, of which there are several.

    public long correctionMsL( long targetTimeMsL, long periodMsL )
      /* This method returns how much targetTimeMsL should be shifted,
        in integer multiples of intervalMsL, to correct it for
        any jumps that might have happened to 
        the value returned by System.currentTimeMillis().
        Normally it will return 0, but sometimes it will return
        positive or negative multiples of intervalMsL,
        depending on whether time has been advanced or retarded.
        */
		{
    	long shiftMsL= 0;
    	long targetTimeOutMsL;
			final long currentTimeMsL= System.currentTimeMillis();
    	while (true) { // Advance target until time-out positive.
    		targetTimeMsL+= periodMsL; // Advancing target time.
  			targetTimeOutMsL= targetTimeMsL - currentTimeMsL;
    	  if // Exiting loop if time-out is positive.
    	    ( targetTimeOutMsL > 0 ) 
    	  	break;
    	  shiftMsL+= periodMsL;
				}
    	while (true) { // Retarding target while time-out excessive.
    		if // Exiting loop if time-out not above period. 
    			( targetTimeOutMsL <= periodMsL ) 
    			break; 
    		targetTimeMsL-= periodMsL; // Retarding target time.
  			targetTimeOutMsL= targetTimeMsL - currentTimeMsL;
  			shiftMsL-= periodMsL;
     	  }
    	return shiftMsL; // Returning how much shift is needed.
			}

    public long timeOutForMsL( long targetTimeMsL, long periodMsL )
      /* This method returns a value to be used as a time-out parameter
        to terminate a wait at time targetTimeMsL+periodMsL.
        The value returned will be greater than 0 
        but not greater than periodMsL,
        and the time-out will occur on 
        a multiples of periodMsL from the targetTimeMsL.
        The process is similar to the one used by correctionMsL(..).
        */
		{
    	long targetTimeOutMsL= // Calculating tentative time-out. 
    			(targetTimeMsL + periodMsL) - System.currentTimeMillis();
    	while ( targetTimeOutMsL <= 0 ) // Correcting while too low. 
    		targetTimeMsL+= periodMsL; 
    	while ( targetTimeOutMsL > periodMsL ) // Correcting while too high.
    		targetTimeMsL-= periodMsL; 
    	return targetTimeOutMsL;
			}

    public Input doWaitE()
      /* This method, called by a source thread,
        waits for any of the following:
          * An input signal notification.
          * The condition set by Thread.currentThread().interrupt().
            This condition is not cleared by this method.
        It returns an Input value indicating why the wait ended.
        */
      {
      	Input theInput;  // For type of Input that ended wait.
      	do { // Waiting but ignoring any time-outs.
      		theInput= doWaitWithTimeOutE( 
	          Long.MAX_VALUE  // A very long time-out.  Any big value would work.
	          );
      		} while ( theInput == Input.TIME ); // Loop if it did time out.
        return theInput;  // Returning why the wait loop ended.
        }
    
    public synchronized Input doWaitWithTimeOutE( long delayMsL )
      /* This is like doWaitWithIntervalE(..) with 
        startMsL set to the current time.
        Use this when reaching a time limit will not terminate a protocol loop.
        */
      {
		    return doWaitWithIntervalE( 
		      System.currentTimeMillis(),
		      delayMsL 
		      );
        }

    public synchronized Input doWaitWithIntervalE( 
    		long startMsL, long lengthMsL 
    		)
	    /* This method, called by a source thread,
	      waits for any of the following:
	        * The condition set by Thread.currentThread().interrupt().
	          This condition is not cleared by this method.
	        * Time is outside the interval beginning at startMsL
	          and of length lengthMs milliseconds. 
	        * An input signal notification.
	      It checks the conditions in the above order.
	      If a different order is desired, the call to this method can
	      be preceded by another test or tests that should be first.
	      
	      This method returns an Input value of the first Input type
	      that it found present.  It might not be the only one.
	
	      The wait(..) can end for several different reasons.
	      Here is how this method distinguishes between and handles each:
	
			  * Some other thread interrupts this thread with Thread.interrupt().
			    This is detected with Thread.currentThread().isInterrupted().
	
			  * The time is outside the specified time-out interval.
			    The reason it is done this way is because the value returned by 
			    System.currentTimeMillis() can change suddenly by large amounts
			    when the computer's clock is adjusted.
			    The amount of real time which has passed 
			    can not be determined with accuracy.
	        If this method finds the System.currentTimeMillis() either
	        before or after the time-out interval 
	        then it will treat it as a time-out.
	        It's normal to return after the time-out interval.
	        If it returns before the time-out interval then
	        it's probably because the clock was set forward a large amount.
	        Treating this as a time-out is a way of preventing
	        the code getting stuck in a wait state.
	
			  * Some other thread invokes the notify(..) or notifyAll(..) methods 
			    for this object.  By convention this must be done after
			    setting the signal variable true.
			    Typically this is done by calling doNotifyV(). 
			    
		    Anything else is considered a spurious wake-up.  
		    This is ignored and the method loops.
		    The method does not return in this case.

	      Use this when reaching a time limit will terminate a protocol loop.
	      
			  ?? Change to use await() instead of wait() ??
	      */
	    {
	      Input theInput;  // For type of Input that will exit wait loop.
	      while (true) { // Looping until one of several conditions is true.
	        if // Handling thread interrupt signal.
	          ( Thread.currentThread().isInterrupted() )
	          { theInput= Input.INTERRUPTION; break; }  // Exiting loop.
	        final long remainingMsL= intervalRemainingMsL( startMsL, lengthMsL ); 
	        if // Exiting if time before or after time interval.
	          ( remainingMsL == 0 )
	          { theInput= Input.TIME; break; } // Exiting loop.
	        if ( getB() ) // Handling explicit input notification.
	          {  // Resetting input signal and exiting loop
	        	  setV(false);  // Resetting NOTIFICATION flag for next doDnotify().
	            theInput= Input.NOTIFICATION; 
	        	  break; 
	        	  }
	        try { // Waiting for notification or time-out.
	          wait(  // Wait for call to notify() or...
	          	remainingMsL
	            );
	          } 
	        catch (InterruptedException e) { // Handling wait interrupt.
	          Thread.currentThread().interrupt(); // Re-establishing for test.
	          }
	        }
	      return theInput;  // Returning why the wait loop ended.
	      }

    public long intervalRemainingMsL( long startMsL, long lengthMsL )
      /* Returns 0 if System.currentTimeMillis() is outside the interval,
        or the ms to the end of the interval if inside it.
        The result can be used as an argument for 
        the wait(..) method or doWaitWithTimeOutE(..) if used immediately. 
        */
			{
    	  long remainingMsL= 0; // Assuming time now is outside interval.
    	  process: {
	    	  final long nowMsL= System.currentTimeMillis(); // Getting now time.
	    	  final long endMsL= startMsL + lengthMsL; // Calculating end time.
	    	  if ( nowMsL-endMsL >= 0) // Exiting with 0 if after interval end.
	    	  	break process;
	    	  if ( nowMsL-startMsL < 0) // Exiting with 0 if before interval start.
	    	  	break process;
	    	  remainingMsL= endMsL - nowMsL; // Setting result to remaining time.
    	  	} // process:
		    return remainingMsL;
		    }

    // Input notification methods, of which there is only one.
	
	    public synchronized void doNotifyV()
	      /* This method is called by threads which are data sources.
	        It delivers notification that new input has been provided 
	        to the destination thread by a source thread.
	        It is called only by a source thread.
	        */
	      {
	        setV(true);  // Set signal to end the loop in doWaitUntilE().
	        notify();  // Terminate any active wait() blocking.
	        }


    // Other methods.

    public boolean getB()  // Get mutable boolean value.
      { return signalB ; }

    private void setV( boolean aB )  // Set mutable boolean value.
      { signalB= aB; }

    }
