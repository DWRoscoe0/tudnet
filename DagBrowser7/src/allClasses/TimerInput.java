package allClasses;

import java.util.Timer;
import java.util.TimerTask;

public class TimerInput // First developed for PS-PS RTT timing.
	/* This class functions as an input to processes modeled as threads.
	  It is not meant to be used with processes modeled as state-machines.
	  This class uses the LockAndSignal.notifyingV() method
	  in the run() method of the TimerTask instances that it creates,
	  so it is guaranteed to be quick.
	  The presence of an active input can be tested 
	  with the getInputArrivedB() method.
	
	  //// This class's documentation needs better terminology.
	 */
	{	
	  // Injected dependencies.
		//private LockAndSignal theLockAndSignal; //// no longer needed.
	  private Timer theTimer;
	  private Runnable theRunnable;
	  
		// Other variables.
		private TimerTask theTimerTask= null;
		private boolean inputArrivedB= false; 
	
	  TimerInput( // Constructor.
	  		//% LockAndSignal theLockAndSignal,
	  		Timer theTimer,
	  		Runnable theRunnable
	  		)
	  	{
		  	//% this.theLockAndSignal= theLockAndSignal;
		  	this.theTimer= theTimer;
		  	this.theRunnable= theRunnable;
		  	}
	
	  public boolean getInputArrivedB() 
	    // Returns whether or not the timer input has been activated.
	    { return inputArrivedB; }
	
	  public boolean getInputScheduledB() 
	    // Returns whether or not the timer input has been scheduled.
	    { return theTimerTask != null; }
	
	  public synchronized void scheduleV( long delayMsL )
	    /* Schedules this timer for input activation after delayML milliseconds.
	      If this timer object is already scheduled or active 
	      then the old scheduled activation is cancelled first.
	     */
	    {
	  		cancelingV(); // Canceling any older input.
	    	theTimerTask= new TimerTask() {
	        public void run()
	          // Activates this as an input and notifies interested thread.
		        {
	        		inputArrivedB= true;
	        	  ////theLockAndSignal.notifyingV();
	        	  theRunnable.run(); // cycleMachineB();
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
