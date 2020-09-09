package allClasses;

import static allClasses.AppLog.theAppLog;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TimerInput
	/* This class performs functions similar to java.util.Timer,
	  such as the ability to call a Runnable's run() method,
	  but also provides methods with which 
	  the state of the timer can be tested.
	  It can time only one interval at a time, but it can be reused.
	  It's called TimerInput because it is meant to provide inputs to
	  a thread or a state machine.
	  There is a rescheduleB(.) method for doing exponential back-off.

	  This class uses a  java.util.Timer to do the timing.
	  The run() method that is triggered must return quickly or 
	  other events using the same Timer could be delayed.
		An earlier version of this class used LockAndSignal.notifyingV()
	  in the run() method of TimerTask instances that it creates for quickness.
	  This version does not have that guarantee.

  . ///enh Change class TimerInput to use 
      java.util.concurrent.ScheduledThreadPoolExecutor
      instead of java.util.Timer.
      ! Underway.  At first code with be conditional on whether or not
        theTimer is null.  Later, that will be removed.
    
    ///enh Add ability to track total schedule and reschedule time
      since previous cancel so it can trigger on both retry interval times
      and interval total times.

	  */
	{	
	  // Injected dependencies.
	  private Timer theTimer= null;
	  private Runnable inputRunnable;
	  private ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor= null;

		// Other (non-injected) variables.

	  // Common variables
    private boolean inputArrivedB= false; 
    private long lastDelayUsedMsL= 0;
    private boolean enabledB= true;
    
    // Timer variables
    private TimerTask ourTimerTask= null;
    
    // ScheduledThreadPoolExecutor variables.
    private Runnable outputRunnable;
    private ScheduledFuture<?> outputFuture;
    
    
    public TimerInput( // Constructor.  ///opt
        Timer theTimer,
        Runnable inputRunnable
        )
      {
        this(
          theTimer,
          null,
          inputRunnable
          );
        }

    public TimerInput( // Constructor.
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor,
        Runnable inputRunnable
        )
      {
        this(
          null,
          theScheduledThreadPoolExecutor,
          inputRunnable
          );
        }

    private TimerInput( // Constructor, common form.
        Timer theTimer,
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor,
        Runnable inputRunnable
        )
      {
        this.theTimer= theTimer;
        if (null != theTimer)
          theAppLog.debug( "TimerInput(.) constructing with Timer." );
        
        this.theScheduledThreadPoolExecutor= theScheduledThreadPoolExecutor;
        if (null != theScheduledThreadPoolExecutor)
            theAppLog.debug( 
                "TimerInput.(.) constructing with ScheduledThreadPoolExecutor." );

        this.inputRunnable= inputRunnable;
        }

    public synchronized void disableV()
      /* This is used for disabling the timer when doing debug traces.  */
      { 
        enabledB= false; 
        }

    public synchronized void purgeV()
      /* This is used to disable pending timer events
        when doing debug traces.  
        ///doc The above documentation might not be correct.
          In actuality it might remove cancelled, saving space only.
          */
      { 
        if (null != theTimer)
          theTimer.purge(); // Purge pending timer events.
          else
          theScheduledThreadPoolExecutor.purge(); // Purge pending timer events.
        }
    
    public synchronized boolean getInputArrivedB() 
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
    
    public synchronized boolean testInputArrivedB() 
      /* Tests whether or not the timer input has been activated
        and returns the result.

        It does not cancel the input, so if it returns true,
        and is immediately called again, it will return true again.
       */
      { 
        return inputArrivedB; 
        }
	
	  public synchronized void scheduleV( long delayMsL )
	    /* Schedules this timer for input activation 
	      after delayMsL milliseconds.
	      If this timer object is already scheduled or active 
	      then the old scheduled activation is cancelled first.
	     */
	    {
        if (delayMsL <= 0)
          theAppLog.warning( "TimerInput.schedule(..) non-positive delay!" );

        cancelingV(); // Canceling any previous input.
        if (null != theTimer) 
          {
            theAppLog.debug("TimerInput.schedule("
                + delayMsL + ") using Timer." );
    	    	ourTimerTask= new TimerTask() {
    	        public void run()
    	          // Our Runnable method to process triggering of the timer.
    		        {
    	        		if (enabledB) // Unless disabled for debug tracing,...
      	        		{ // Take appropriate triggered action.
    	        		    inputArrivedB= true;  // Record that end time has arrived.
      	        		  inputRunnable.run(); // Run user handler Runnable's run().
      	        		  }
    		          }
    	    		};
    	    	theTimer.schedule(ourTimerTask, delayMsL);
            }
          else
          {
            outputRunnable= new Runnable() { // Runnable for scheduler.
              public void run() 
                {
                  if (enabledB) // Unless disabled for debug tracing,...
                    { // Take appropriate triggered action.
                      inputArrivedB= true;  // Record that end time has arrived.
                      inputRunnable.run(); // Run user's Runnable.run().
                      }
                  }
              };
            outputFuture= theScheduledThreadPoolExecutor.schedule(
              outputRunnable,
              delayMsL,
              TimeUnit.MILLISECONDS
              );
            }
        lastDelayUsedMsL= delayMsL;
	    	}

	  public synchronized boolean rescheduleB( long maxDelayMsL )
	    /* Reschedules this timer for input activation using 
	      exponential back-off, doubling the previous delay used,
	      but not greater than maxDelayMsl.
	      Otherwise, it works like scheduleV(.).
	      Returns true if maximum delay was exceeded, false otherwise.
	      If the limit is exceeded then it does not schedule the timer. 
	     */
	    {
	  	  long theDelayMsL= 2 * lastDelayUsedMsL; // Double previous delay.
	  	  boolean limitedExceededB= // Calculate whether maximum exceeded.
	  	  		(theDelayMsL > maxDelayMsL);
	  	  if (!limitedExceededB) // Schedule timer if limit is not exceeded.
	  	    scheduleV(theDelayMsL); // Schedule using result.
	  	  return limitedExceededB;
	    	}

    public synchronized long getLastDelayMsL()
      /* Returns the last delay set in this timer. 
       */
      {
        return lastDelayUsedMsL;
        }
	
	  public synchronized void cancelingV()
	    /* Cancels future generation of a timer input.
	      This will cancel both inputs that have been scheduled
	      but not yet arrived, and inputs that have arrived.
	
	      An earlier version returned true if cancellation was successful, 
	      false otherwise, but it could not be trusted.
	      */
	    {
	      if (null != theTimer)
  	      {
    	  		if (ourTimerTask != null) // Handling timer created and scheduled.
      	    	{
    	  				inputArrivedB= false; // Erasing any arrived input.
    	    			ourTimerTask.cancel(); // Canceling timer.
    			    	ourTimerTask= null; // Recording as not created and scheduled.
    	    			}
    	      }
	        else
      	  {
            inputArrivedB= false; // Erasing any arrived input.
            if (null != outputFuture) {
              outputFuture.cancel(true);
              outputFuture= null;
              }
            }
	    	}	 
	
		} // class TimerInput
