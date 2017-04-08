package allClasses;

import java.util.Timer;
import java.util.TimerTask;

public class TimerInput
	/* This class performs functions similar to java.util.Timer,
	  such as the ability to call a Runnable's run() method,
	  but also provides methods with which the state of the timer can be tested.
	  It can time only one interval at a time, but it can be reused.
	  It's called TimerInput because it is meant to provide inputs to
	  a thread or a state machine.

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
	
	  TimerInput( // Constructor.
	  		Timer theTimer,
	  		Runnable theRunnable
	  		)
	  	{
		  	this.theTimer= theTimer;
		  	this.userRunnable= theRunnable;
		  	}
	
	  public boolean getInputArrivedB() 
	    // Returns whether or not the timer input has been activated.
	    { return inputArrivedB; }
	
	  public boolean getInputScheduledB() 
	    // Returns whether or not the timer input has been scheduled.
	    { return theTimerTask != null; }
	
	  public synchronized void scheduleV( long delayMsL )
	    /* Schedules this timer for input activation after delayMsL milliseconds.
	      If this timer object is already scheduled or active 
	      then the old scheduled activation is cancelled first.
	     */
	    {
	  		cancelingV(); // Canceling any previous input.
	    	theTimerTask= new TimerTask() {
	        public void run()
	          // Our Runnable method to process triggering of the timer.
		        {
	        		inputArrivedB= true;  // Record that end time has arrived.
	        	  userRunnable.run(); // Run user handler Runnable.
		          }
	    		};
	    	theTimer.schedule(theTimerTask, delayMsL);
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
