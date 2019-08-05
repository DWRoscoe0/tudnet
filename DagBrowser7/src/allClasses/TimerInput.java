package allClasses;

import java.util.Timer;
import java.util.TimerTask;

public class TimerInput
	/* This class performs functions similar to java.util.Timer,
	  such as the ability to call a Runnable's run() method,
	  but also provides methods with which 
	  the state of the timer can be tested.
	  It can time only one interval at a time, but it can be reused.
	  It's called TimerInput because it is meant to provide inputs to
	  a thread or a state machine.
	  There is a rescheduleB(.) method for doing exponential back-off.
	  
	  ///ehn Add ability to track total schedule and reschedule time
	    since previous cancel so it can trigger on both retry interval times
	    and interval total times.

	  This class uses a  java.util.Timer to do the timing.
	  The run() method that is triggered must return quickly or 
	  other events using the same Timer could be delayed.
		An earlier version of this class used LockAndSignal.notifyingV()
	  in the run() method of TimerTask instances that it creates for quickness.
	  This version does not have that guarantee.
	  */
	{	
	  // Injected dependencies.
	  private Timer theTimer;
	  private Runnable userRunnable;
	  
		// Other variables.
		private TimerTask theTimerTask= null;
		private boolean inputArrivedB= false; 
    private long lastDelayUsedMsL= 0;
    private boolean enabledB= true;
    
	  
    TimerInput( // Constructor.
	  		Timer theTimer,
	  		Runnable theRunnable
	  		)
	  	{
		  	this.theTimer= theTimer;
		  	this.userRunnable= theRunnable;
		  	}

    public void disableV()
      /* This is used for disabling the timer when doing debug traces.  */
      { 
        enabledB= false; 
        }

    public void purgeV()
      /* This is used to disable pending timer events
        when doing debug traces.  */
      { 
        theTimer.purge(); // Purge pending timer events. 
        }
    
    public boolean getInputArrivedB() 
      /* Tests whether or not the timer input has been activated,
        and returns the result.
        If it returns true, then it cancels the timer input before returning,
        so if it is immediately called again, it will return false.
       */
      { 
        boolean returnB= inputArrivedB;
        if (returnB) // If input has arrived
          cancelingV(); // cancel it so it won't be processed again.
        return returnB; 
        }
    
    public boolean testInputArrivedB() 
      /* Tests whether or not the timer input has been activated
        and returns the result.

        It does not cancel the input, so if it returns true,
        and is immediately called again, it will return true again.
       */
      { 
        return inputArrivedB; 
        }
	
	  public boolean getInputScheduledB() 
	    // Returns whether or not the timer input has been scheduled.
	    { return theTimerTask != null; }
		
	  public synchronized void scheduleV( long delayMsL )
	    /* Schedules this timer for input activation 
	      after delayMsL milliseconds.
	      If this timer object is already scheduled or active 
	      then the old scheduled activation is cancelled first.
	     */
	    {
	  		cancelingV(); // Canceling any previous input.
	    	theTimerTask= new TimerTask() {
	        public void run()
	          // Our Runnable method to process triggering of the timer.
		        {
	        		if (enabledB) // Unless disabled for debug tracing,...
  	        		{ // Take appropriate triggered action.
	        		    inputArrivedB= true;  // Record that end time has arrived.
  	        		  userRunnable.run(); // Run user handler Runnable's run().
  	        		  }
		          }
	    		};
	      lastDelayUsedMsL= delayMsL;
	    	theTimer.schedule(theTimerTask, delayMsL);
	    	}

	  public synchronized boolean rescheduleB( long maxDelayMsL )
	    /* Reschedules this timer for input activation using 
	      exponential back-off, doubling the previous delay used,
	      but not greater than maxDelayMsl.
	      Otherwise, it works like scheduleV(.).
	      Returns true if maximum delay was exceeded, false otherwise. 
	     */
	    {
	  	  long theDelayMsL= 2 * lastDelayUsedMsL; // Double previous delay.
	  	  boolean resultB= // Calculate whether maximum exceeded.
	  	  		(theDelayMsL > maxDelayMsL);
	  	  if (resultB) // Limit time-out delay to maximum allowed. 
	  	  	theDelayMsL= maxDelayMsL;
	  	  scheduleV(theDelayMsL); // Schedule using result.
	  	  return resultB;
	    	}
	
	  public synchronized void cancelingV()
	    /* Cancels future generation of a timer input.
	      This will cancel both inputs that have been scheduled
	      but not yet arrived, and inputs that have arrived.
	
	      An earlier version returned true if cancellation was successful, 
	      false otherwise, but it could not be trusted.
	      */
	    {
	  		if (theTimerTask != null) // Handling timer created and scheduled.
	    		{
	  				inputArrivedB= false; // Erasing any arrived input.
	    			theTimerTask.cancel(); // Canceling timer.
			    	theTimerTask= null; // Recording as not created and scheduled.
	    			}
	    	}	 
		
		} // class TimerInput
