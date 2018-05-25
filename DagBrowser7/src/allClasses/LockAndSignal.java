package allClasses;

//import static allClasses.Globals.appLogger;

public class LockAndSignal  // Combination lock and signal class.

  /* 

	  As its name suggests, this class can serve the following two roles:

	    * As a monitor-lock for Object.wait(..), Object.notify(),
	      and synchronized code blocks.
	    * As an associated boolean signal flag variable.

    An instance of this class may be used by 
    a single consumer thread and one or more producer threads 
    to manage and process several types of signals,
    using potentially many different ports, 
    without using busy-waiting.

    Each consumer thread needs only one instance of this class
    to manage all its inputs regardless of the number of input paths, ports,
    or number of producer threads that are providing those inputs.  
    In fact it makes no sense to have more than one LockAndSignal instance.

    The Input type, listed in preferred decreasing priority order, are:

      INTERRUPTION: The thread's isInterrupted() status was set true.
        This is commonly used to request thread termination.
      TIME: A time event has occurred.  
        This is commonly used to generate delays and for time-outs.
      NOTIFICATION: notifyingV() was called at least once.
        This is commonly used to indicate the placement by producer threads
        of input data at pre-arranged input ports of the consumer thread.  

    	NONE: This means that there have been none of the above recognized inputs.

      SUBNOTIFICATION // This is a special value for use by other classes.
        This class defines it, but does not reference it anywhere.
        It is meant to be used to indicate that an input datum, 
        a group of which might have been signaled by a NOTIFICATION input, 
        is available.

    A thread can make use of any one, two, or all three types of inputs.

    If inputs occur to the consumer thread one at a time,  
    and it can process each input before the next input appears,
    then it doesn't matter the order in which the thread tests the inputs,
    because the thread will process them in the order they appeared.

    But if, as is more common, the thread can't keep up with its inputs, 
    then more than one input type can appear simultaneously.
    In this case the order of input testing may be important.
    Therefore each wait method and each test method
    has a well documented priority ordering of Input types, 
    which is also the order of Input testing that the method uses. 

    Several priority orderings of input types are possible.
    This class's wait and test methods do not provide all orderings,
    but it does support several useful ones.
    It also provides building block methods from which
    new methods which use any desired priority order can be built.

		As an example of why different input priorities might be desired,
		consider the INTERRUPT Input, which is commonly used 
		to request thread termination.
			* If the termination request is of the ABORT type 
			  then it should probably have top priority, 
			  otherwise other inputs might prevent it being noticed. 
			* If the termination request is NOT of the ABORT type, 
			  meaning it is a request to terminate eventually at a convenient time,
			  then it should probably have a lower priority, 
			  which would allow the thread to finish processing
			  other pending inputs properly before terminating. 

    Though this class is thread-safe, 
    the objects used to pass NOTIFICATION data between threads, 
    queues for example, must be guaranteed thread-safe by other means. 
    For an example see the NotifyingQueue class.

		This class's notifyingV() method is called by the producer threads
    to indicate the readiness of new NOTIFICATION inputs.
    In fact, it is the only method called by producer threads.
    It sets a signal variable and calls Object.notify().
    It can be called directly by the thread generating input data,
    or indirectly by objects being used as Input.NOTIFICATION ports.
    For an example see the add(..) method of the NotifyingQueue class.

    This class's Wait methods are called by the consumer thread
    to wait until at least one of the 3 Input types has appeared.
    If none of them has appeared before the time of call
    then it calls Object.wait() to block the thread until one does.
    Typically the consumer thread will have a loop containing:
    * A call to one of these Wait methods.
    * The processing of all of the inputs represented by the return value
      from the Wait method.

    This class's Test methods are similar to the Wait methods except that
    the Test methods always return immediately and
    might return the value Input.NONE,
    meaning that none of the desired input types has occurred, yet. 

    To prevent failure to process inputs in a timely manner,
    all inputs associated with an input type returned by
    either a test method or a wait method must be processed
    before a call is made to a test method or wait method
    which can return the same input type.
    For example, all entries on all input queues associated with
    Input.NOTIFICATION must be processed before calling a method
    which can return Input.NOTIFICATION.
    For details on the various Input types, and how to process them,
    see the documentation of:
    * Input testingForInterruptE() 
    * Input testingRemainingDelayE( long delayMsL )
    * Input testingForNotificationE()

    ///? It might make sense to add a LockAndSignal instance
    to the EpiThread class??

    This class was originally created because 
    neither the Java boolean primitive nor the wrapper Boolean class 
    could be used for the two previously mentioned roles.
    The wrapper Boolean is immutable.  Assuming aBoolean == false, then
    "aBoolean= true;" is equivalent to "aBoolean= new Boolean(true);", 
    which changes the object referenced by aBoolean,
    which causes an IllegalMonitorStateException.
    The LockAndSignal class solves this problem by using
    methods to access an internal mutable boolean value.

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

    ///? It might be worthwhile to use an object reference, or a null,
    instead of a boolean, to be the signal.
    Additional versions of the doWait() and notifyingV() methods
    could pass these values.  This would allow passing of
    more complex information as if through a single element queue,
    but only from one specific object to another.
    Or maybe it would be better to have a different class for this.
    */

  {

    public enum Input { // Types of inputs.
    	// The order shown is a common priority order, lowest to highest.
    	NONE, // There has been no recognized input.

      INTERRUPTION, // The thread's isInterrupted() status is true.
      TIME, // Clock time has moved outside a designated interval.
      NOTIFICATION, // notifyingV() was called to signal data input.
      SUBNOTIFICATION // Special value for use by caller.
      }
      
    private boolean signalB= false;  // NOTIFICATION flag.
      // Set to true when any producer thread notifies of a new input.
      // Set to false when the consumer thread receives notification.


	  /* Consumer thread methods that wait for various types of Input:

	    There are several wait methods.
	    Each one waits for one out of several possible 
	    combinations of input types to become available to the thread. 
	    But all the wait methods have some things in common.

	    First, a wait method might not actually block.
	    This happens if one of the inputs of interest
      is already available when the method is called.

	    Each wait method returns a value indicating the type of the Input
	    that it discovered available.  
      If inputs occur one at a time, and the consumer thread 
      can process each input before the next input arrives,
      then it doesn't matter the order in which 
      the wait method checks the inputs.
      But if the thread can't keep up, then the order may be important.
      Because more than one Input might be available, 
      each wait method has a priority ordering of Input types, 
      which is the order of Input checking.
	    The following is a list of of input types, 
	    in common priority ordering, but others are possible.

	      * Input.INTERRUPTION, which means the thread has been interrupted.
	        Because this is Java, it probably also means that 
	        thread termination has been requested.
	        Termination is rare but is usually considered important.  
	      * Input.TIME, which means that time has satisfied 
	        a condition of interest and something needs to be done now.
	      * Input.NOTIFICATION, which means that one or more normal data inputs
	        has become available and should eventually be processed. 
	        These generally have the lowest priority. 
			
      If a different priority ordering is desired than the method provide,
      there might be another one that uses a different order.
      If not then one of those methods can be preceded by 
      the test or tests that should be first.
      For an example see Streamcaster.testWaitInIntervalE(..).
      There are also building-block methods from which new new wait methods
      can be constructed.

	    When control is returned from any of the wait or test methods,
	    it returns a value of type Input.
	    The caller should process at least that input type because
	    the method might have cleared the condition
	    that is associated with that input type.
	    The input might be lost otherwise.
	    The caller may optionally check for and process other input types.

	    Another way to guarantee that the returned input type is processed,
	    is by ignoring the input type returned, 
	    but checking for ALL possible Input types,
	    and processing all that are available.
	    
	    If the NOTIFICATION input type is returned,
	    then the thread must check for and process 
	    ALL NOTIFICATION inputs that are available.
	    This is because they share a single NOTIFICATION flag,
	    variable signalB.  This can be done in multiple ways:
		
		    Here is an example of a loop that processes INTERRUPTION 
		    and NOTIFICATION inputs.
		
		      LockAndSignal myLockAndSignal= new LockAndSignal();
		      ...
		      while ( true ) {
			      LockAndSignal.Input theInput= 
			        myLockAndSignal.waitingForNotificationOrInterruptE()();
			      if (theInput == Input.INTERRUPTION) break;
			      if (theInput == Input.NOTIFICATION) // this test optional.
				      while ( notificationInputsAvailableB() )
				        processOneNotificationInput();
			      }
		
	      But this way adds nested loops, which can make coding 
	      more complex systems difficult.  For example, 
	      it makes encoding the state of an algorithm or protocol 
	      with the position of a thread's instruction pointer,
	      and different inputs have different meanings,
	      virtually impossible.
		
	      Another way that might be less obvious, 
	      but makes easier the coding of complex systems,
	      is to create and use a specialized wait method, as follows:

	        LockAndSignal.Input myWaitE() 
		        {
		          if ( unprocessedNotificationInputsAvailableB() )
		            return Input.NOTIFICATION; // Returning immediately.
		            else
		            return 
		              myLockAndSignal.waitingForNotificationOrInterruptE()();
		          }

				  ...
		      while ( true ) {
			      LockAndSignal.Input theInput= myWait();
			      if (theInput == Input.INTERRUPTION) break;
			      if (theInput == Input.NOTIFICATION) // this test optional.
				        processOneNotificationInput();
				    ...
			      }
	
			  No nested loops are required.  Using a method like myWaitE()
			  can greatly simplify the coding of complex protocols and algorithms,
			  especially ones in which there are multiple input ports.
			  Note, testing for queued input this way puts NOTIFICATION
			  at a higher priority than INTERRUPTION.

		  ///? Change wait methods to use await() instead of wait() ??

			*/
	
	    public Input waitingForInterruptOrNotificationE()
		    /* This method, called by a consumer thread,
			    waits for an input from the following list.
			     If more than one input type is available when the method is called 
			    then it returns the one that appears first.
			      Input.INTERRUPTION: 
			        The interrupt status set by Thread.currentThread().interrupt()
			        was true, and was then cleared by this method.
			      Input.NOTIFICATION: 
			        An input signal notification occurred.
			        This condition is cleared when the notification is returned.
		     	*/
			  {
			    Input theInput;  // For type of Input that will be returned.
			    while (true) { // Looping until there is an input.
		      	theInput= testingForInterruptE();
		      	if ( theInput != Input.NONE ) break;
		      	theInput= testingForNotificationE();
		      	if ( theInput != Input.NONE ) break;
			      waitingForInterruptOrDelayOrNotificationV( // Waiting for input.
		      		0 // This means waiting with no time limit.
		      		);
			      } // while(true)
			    return theInput;  // Returning why the wait loop ended.
			    }
	
	    public Input waitingForNotificationOrInterruptE()
		    /* This method, called by a consumer thread,
			    waits for an input from the following list.
			     If more than one input type is available when the method is called 
			    then it returns the one that appears first.
			      Input.NOTIFICATION: 
			        An input signal notification occurred.
			        This condition is cleared when the notification is returned.
			      Input.INTERRUPTION: 
			        The interrupt status set by Thread.currentThread().interrupt()
			        was true, and was then cleared by this method.
		     	*/
			  {
			    Input theInput;  // For type of Input that will be returned.
			    while (true) { // Looping until there is an input.
		      	theInput= testingForNotificationE();
		      	if ( theInput != Input.NONE ) break;
		      	theInput= testingForInterruptE();
		      	if ( theInput != Input.NONE ) break;
			      waitingForInterruptOrDelayOrNotificationV( // Waiting for input.
		      		0 // This means waiting with no time limit.
		      		);
			      } // while(true)
			    return theInput;  // Returning why the wait loop ended.
			    }
			    
			public synchronized Input waitingForInterruptOrDelayOrNotificationE( 
	    		long delayMsL 
	    		)
		    /* This method, called by a consumer thread,
			    waits for an input from the following list.
			    If more than one input type is available when the method is called 
			    then it returns the one that appears first in the list.
	          Input.INTERRUPTION: 
		          The interrupt status set by Thread.currentThread().interrupt()
		          was true, and was then cleared by this method.
			      Input.TIME.
			        Time is outside the interval that begins at the time of call
			        and ends delayMs milliseconds later. 
			      Input.NOTIFICATION: 
			        An input signal notification occurred.
			        This condition is cleared when the notification is returned.
			    */
	      {
			    return // Convert to call with time delay converted to time interval. 
		    		waitingForInterruptOrIntervalOrNotificationE( 
				      System.currentTimeMillis(),
				      delayMsL 
				      );
	        }
	
	    public synchronized Input waitingForInterruptOrIntervalOrNotificationE(
	    		long startMsL, long lengthMsL 
	    		)
		    /* This method, called by a consumer thread,
			    waits for an input from the following list.
			    If more than one input type is available when the method is called 
			    then it returns the one that appears first.
	          Input.INTERRUPTION: 
		          The interrupt status set by Thread.currentThread().interrupt()
		          was true, and was then cleared by this method.
			      Input.TIME.
			        Time is outside the interval that begins at startMsL
			        and ends delayMs milliseconds later. 
			      Input.NOTIFICATION: 
			        An input signal notification occurred.
			        This condition is cleared when the notification is returned.
	
		      Use this when reaching a time limit will terminate a protocol loop.
		      */
		    {
		      Input theInput;  // For type of Input that will be returned.
		      while (true) { // Looping until there is an input.
		        final long remainingInIntervalMsL= // [Re]calculating remaining time.
	        		timeCheckedDelayMsL( startMsL, lengthMsL ); 
		      	theInput= // Testing whether any input is available. 
	      			testingForInterruptOrDelayOrNotificationE( 
	      					remainingInIntervalMsL 
	      					);
		        if  // Exiting with input type unless...
		          ( theInput != Input.NONE ) // ...there is no input available. 
		        	break; // Exiting.
		        waitingForInterruptOrDelayOrNotificationV( // Waiting for any input.
			      		remainingInIntervalMsL
			      		);
		        } // while(true)
		      return theInput;  // Returning why the wait loop ended.
		      }
    
    
	  /* Consumer thread methods that test for various types of Input:
	
			These methods are similar to the methods that wait for inputs,
			but instead of waiting they return immediately.
			They return all the same values, with all the same meanings,
			plus one more, as follows:
	    
	      * Input.INTERRUPTION, which means the thread has been interrupted.
	      * Input.TIME, which means that time has satisfied 
	        a condition of interest and something needs to be done now.
	      * Input.NOTIFICATION, which means that one or more normal data inputs
	        has become available and should eventually be processed. 

			  * InputNONE, which means that none of the desired inputs is available
			    at the time of call.

	   	*/
		
	    public synchronized Input testingForInterruptTimeOrNotificationE( 
	    		long startMsL, long lengthMsL 
	    		)
		    /* This method, called by a consumer thread,
			    tests for an input from the following list.
			    If more than one input type is available when the method is called 
			    then it returns the one that appears first.
	
	          Input.INTERRUPTION: 
		          The interrupt status set by Thread.currentThread().interrupt()
		          was true, and was then cleared by this method.
			      Input.TIME.
			        Time is outside the interval that begins at startMsL
			        and ends lengthMs milliseconds later. 
			      Input.NOTIFICATION: 
			        An input signal notification occurred.
			        This condition is cleared when the notification is returned.
	
		     	  Input.NONE: None of the above inputs was available.
	
	        */
		    {
				  final long delayMsL= // Converting time interval to time delay.
				  		timeCheckedDelayMsL( startMsL, lengthMsL );
	
				  return testingForInterruptOrDelayOrNotificationE( delayMsL );
			    }

      public synchronized Input testingForInterruptOrDelayOrNotificationE( 
      		long delayMsL 
      		)
		    /* This method, called by a consumer thread,
			    tests for an input from the following list.
			    If more than one input type is available when the method is called 
			    then it returns the one that appears first.
	
	          Input.INTERRUPTION: 
		          The interrupt status set by Thread.currentThread().interrupt()
		          was true, and was then cleared by this method.
			      Input.TIME.
			        Time is outside the interval that begins now
			        and ends delayMs milliseconds later. 
			      Input.NOTIFICATION: 
			        An input signal notification occurred.
			        This condition is cleared when the notification is returned.
	
		     	  Input.NONE: None of the above inputs was available.
	
	        */
  	    {
		      Input theInput;  // For return value.
		      process: {
		      	theInput= testingForInterruptE();
		      	if ( theInput != Input.NONE ) break process;
					  theInput= testingRemainingDelayE( delayMsL );
		      	if ( theInput != Input.NONE ) break process;
		      	theInput= testingForNotificationE();
		      	// No need to test final result.  Just drop through.
		      	} // process:
		    return theInput;
		    }


    /* Consumer thread building block test and wait methods:

      From these methods, other test and wait methods can be built
      to test or wait for any combination of inputs.
      
      There are 3 test methods.
      Each test method tests for one, and only one, type of input.
      In some cases it also clears the condition for which it tests.
      In most cases, this is what the caller desired.
      If this is not true then the caller can reestablish the condition.  
      
      There are 2 wait methods.  
      The methods differ only in how they treat
      pre-existing Thread interrupted status.
      
      For examples of users of these methods,
      see their callers above this point in this file.
      */

      public synchronized Input testingForInterruptE() 
		    /* This method, called by a consumer thread,
			    tests for and resets the thread interrupt status input only.  
			    It returns results as follows:
			    
	          Input.INTERRUPTION: 
		          The current thread's interrupt status was found to be true,
		          and was then set to false.
	
		     	  Input.NONE: None of the above inputs was available.
		     	    This means current thread's interrupt status 
		     	    was found to be false, and was not changed.
	
					This method always tries to return with interrupt status false.
					If it was false then it stays false.
					If it was true then it is set false by the call to
					Thread.currentThread().isInterrupted() used to test it.
					If the caller wants the interrupt status to be true again
					then it must call Thread.currentThread().interrupt().
					It is also possible that another thread could set it true again
					by calling Thread.interrupt().
					This protocol is necessary to prevent loss of the signal
					that the interrupt status represents.
	        */
	      {
	 	      if // Testing and clearing thread interruption status.
	 	        ( Thread.currentThread().isInterrupted() )
	 	      	return Input.INTERRUPTION;
	 	      	else
	 	      	return Input.NONE;
	      	}

      public synchronized Input testingRemainingDelayE( long delayMsL )
		    /* This method, called by a consumer thread,
			    tests for a time input only.
          delayMs should be a value previously returned by timeCheckedDelayMsL().
			    This method returns results as follows:
	
			      Input.TIME.
			        delayMs equals 0, meaning no delay remains,
		     	    so the associated delayed action should happen now,
		     	    or the action should be queued and done soon.
	
		     	  Input.NONE: None of the above inputs was available.
		     	    This means that some of the delay remains and
		     	    the associated delayed action should not happen yet.
	
	        */
	      {
				  if ( delayMsL == 0 )
	 	      	return Input.TIME;
	 	      	else
	 	      	return Input.NONE;
	      	}

      public synchronized Input testingForNotificationE()
		    /* This method, called by a consumer thread,
			    tests for notification inputs only.
			    It returns results as follows:
	
			      Input.NOTIFICATION: 
			        An input signal notification occurred,
			        and was then cleared by this method.
			        This means that inputs have arrived on at least one of
			        the input ports that accompanied by calls to notifyingV().
			        All of these input ports should be tested and processed
			        before calling this method again.
	
		     	  Input.NONE: None of the above inputs was available.
		     	    This means that no notification inputs 
		     	    arrived since the last time this method was called.
	
					This protocol is necessary to prevent failure to process
					inputs in a timely manner.
	        */
        {
				  if ( getB() ) // Testing flag indicating that notifyingV() called.
				    {
				  	  setV(false); // Reset condition in preparation for next inputs.
		 	      	return Input.NOTIFICATION; 
				  	  }
		 	      else
		 	      return Input.NONE;
	      	}

      public synchronized void waitingForInterruptOrDelayOrNotificationV(
      		long waitTimeMsL
      		)
  	    /* This method, called by a consumer thread,
  		    waits for any of the following:
  		    * a thread interrupt, either a pre-existing interrupt status 
  		      one an interrupt that happens during the wait().
  		    * A call to notify() during the wait().
  		    * The passage of waitTimeMsL time.

  		    It does all of this with a call to Object.wait(waitTimeMsL).
  		    waitTimeMsL equaling 0 means that
  		    the passage of no amount of time will terminate the wait().

  		    This method does not loop and returns no results.
  		    It is the responsibility of the caller 
  		    to loop and test whether a desired input actually happened,
  		    and if not, to wait again, possibly with a reduced wait time.
  	      */
  	    {
	        try { // Waiting for new notify(), wait time, or interrupt().
	          wait( waitTimeMsL );
	          } 
	        catch (InterruptedException e) { // Handling thread wait interrupt.
	          Thread.currentThread().interrupt(); 
	          } // Re-establishing thread interrupt status for later tests.
  	      }

      public synchronized void waitingForDelayOrNotificationV(
      		long waitTimeMsL
      		)
  	    /* This method, called by a consumer thread,
  		    waits for any of the following:
  		    * A call to notify() during the wait().
  		    * The passage of waitTimeMsL time.
  		    It ignores, but preserves, thread interrupts.

  		    It does this with a call to Object.wait(waitTimeMsL).
  		    waitTimeMsL equaling 0 means that
  		    the passage of no amount of time will terminate the wait().

  		    It partially ignores, but preserves thread interrupts.
  		    If the thread's interrupted status is true when
  		    this method is called, it is ignored, but kept true.
  		    If the thread's interrupted status becomes true while
  		    while this method is blocked executing Object.wait(..),
  		    then the block ends, and the method returns
  		    with the thread's interrupted status true.

  		    This method does not loop or return any results.  
  		    It is the responsibility of the caller 
  		    to loop and test whether a desired input happened, 
  		    and if not, to wait again, possibly with a reduced wait time.
  		    
  		    ///fix It ignores an interrupt active on entry,
  		      but responds to an interrupt during wait(..).
  		      This method is not presently used.
  	      */
      {
    	  boolean interruptedB= // Saving and clearing interruption status. 
    	  		Thread.currentThread().isInterrupted();

        try {
          wait( waitTimeMsL ); // Waiting for new notify() or wait time.
	        if (interruptedB) // Restoring interrupt status to what it was.
	        	Thread.currentThread().interrupt(); 
          } 
        catch (InterruptedException e) { // Handling thread wait() interrupt.
          Thread.currentThread().interrupt(); 
          } // Re-establishing interrupt status which terminated wait.
        }

	      
    /* Consumer thread time calculation methods.

      These methods help threads safely deal 
      with real-time events with the possibility that time, 
      which is returned by System.currentTimeMillis(),
      can suddenly move forward or backward large amounts.
      By using these methods, a thread can ensure that
      such time changes not block a thread for long time intervals.
      
      Time can seem to suddenly move when:
      * he device's clock is set, or
      * the device sleeps for some period of time.
      
      ??? rename delay producers:
      shiftedDelay
      checkedDelay
      
      ??? new method to shift a time using shiftCorrectionMsL()?
     */

	    public long periodCorrectedShiftMsL( 
	    		long targetTimeMsL, long limitAndPeriodMsL 
	    		)
	      /* This method returns how much targetTimeMsL should be shifted
	        in order to put the present time in the interval
	        that begins at targetTimeMsL and is limitAndPeriodMsL long.
	        Most calls to this method return 0, but sometimes it will return
	        positive or negative multiples of lengthMsL,
	        depending on whether system time System.currentTimeMillis() 
	        has been advanced or retarded by amounts greater than lengthMsL.
	        */
			{
	    	long shiftMsL= 0;
	    	long delayMsL;
				final long currentTimeMsL= System.currentTimeMillis();
	    	while (true) { // Advancing target and shift until time-out positive.
	    		targetTimeMsL+= limitAndPeriodMsL; // Advancing target time.
	  			delayMsL= targetTimeMsL - currentTimeMsL;
	    	  if // Exiting loop if needed delay is positive.
	    	    ( delayMsL > 0 ) 
	    	  	break;
	    	  shiftMsL+= limitAndPeriodMsL;
					}
	    	while (true) { // Retarding target and shift while time-out excessive.
	    		if // Exiting loop if needed delay not above period. 
	    			( delayMsL <= limitAndPeriodMsL ) 
	    			break; 
	    		targetTimeMsL-= limitAndPeriodMsL; // Retarding target time.
	  			delayMsL= targetTimeMsL - currentTimeMsL;
	  			shiftMsL-= limitAndPeriodMsL;
	     	  }
	    	return shiftMsL; // Returning how much shift was needed.
				}
	
	    public long periodCorrectedDelayMsL( long targetTimeMsL, long lengthMsL )
	      /* This method returns a value to be used as 
	        a time-out interval parameter to terminate a wait 
	        at time targetTimeMsL plus lengthMsL.
	        The value returned will be greater than 0 
	        but not greater than lengthMsL,
	        and the time-out will occur on 
	        a multiple of lengthMsL from the targetTimeMsL.
	        Specifying a time-out time this way makes it possible to avoid
	        problems caused by large sudden changes in System.currentTimeMillis().
	        The process is similar to what is done in correctionMsL(..).
	        */
			{
	    	long delayMsL= // Calculating tentative time-out delay. 
	    			(targetTimeMsL + lengthMsL) - System.currentTimeMillis();
	    	while ( delayMsL <= 0 ) // Shifting up while too low. 
	    		delayMsL+= lengthMsL;
	    	while ( delayMsL > lengthMsL ) // Shifting down while too high.
	    		delayMsL-= lengthMsL;
	    	return delayMsL;
				}
	
	    public long timeCheckedDelayMsL( long startMsL, long lengthMsL )
	      /* This method produces a real-time-checked delay from
	        the time interval that begins at startMsl and is lengthMsL long.
		      * It returns 0 if System.currentTimeMillis() 
		        is either before or after the interval.
		      * It returns the number of milliseconds to the end of the interval
		        if the present time is within the interval.
	        The returned value, if used immediately, can be used as 
	        a delayMsL argument or any of this class's methods which takes it.
	        However it may NOT be used an a time-out delay argument to 
	        the Object.wait(..) method because 0 means an infinite time-out delay. 
	        This method helps threads avoid being blocked for long intervals
	        if the real time clock is set and changes by a large amount.  
	        */
				{
	    	  long delayMsL= 0; // Assuming time now is outside interval.
	    	  process: {
		    	  final long nowMsL= System.currentTimeMillis(); // Getting now time.
		    	  final long endMsL= startMsL + lengthMsL; // Calculating end time.
		    	  if ( nowMsL-endMsL >= 0) // Exiting with 0 if after interval end.
		    	  	break process;
		    	  if ( nowMsL-startMsL < 0) // Exiting with 0 if before interval start.
		    	  	break process;
		    	  // Time is within interval.  
		    	  delayMsL= // Set delay to be the time to the end of the interval.
		    	  		endMsL - nowMsL;
	    	  	} // process:
			    return delayMsL;
			    }


    // Producer thread input notification method.

	    public synchronized void notifyingV()
	      /* This method is called by producer threads,
	        the threads which are data producers.
	        It delivers notification that new input has been provided 
	        to the consumer thread by the producer thread on 
	        one or more pre-arranged ports.
	        */
	      {
	        setV(true);  // Set signal which will become Input.NOTIFICATION.
	        notify();  // Terminate any active wait() that is blocked.
	        }


    // Getter and setter methods.

	    private boolean getB()  // Get mutable boolean value.
	      { return signalB ; }

	    private void setV( boolean theB )  // Set mutable boolean value.
	      { signalB= theB; }

    }
