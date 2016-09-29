package allClasses;

import java.awt.AWTEvent;
import java.awt.EventQueue;

//import static allClasses.Globals.appLogger;

public class BackgroundEventQueue extends EventQueue {

	/* This simple class extends EventQueue so that
	  background Runnable tasks can be done 
	  only when the EventQueue is empty.
	  There can be a maximum of one background Runnable.
	  */

	private Runnable backgroundRunnable= null; // The one background Runnble.
	
  public BackgroundEventQueue() // Constructor. 
	 {
  	 super();
	   }

  synchronized public void setBackgroundRunnableV( Runnable backgroundRunnable )
    /* This will be run on the EDT, 
      so backgroundRunnable should finish quickly. 
      */
	  {
	  	this.backgroundRunnable= backgroundRunnable;
	  	}
	  
  @Override
  synchronized protected void dispatchEvent(AWTEvent event) 
	  {
	    super.dispatchEvent(event); // Processing present event.
	    
	    if ( // Running background runnable if
	    		( backgroundRunnable != null ) && // there is one and
	    		( peekEvent() == null ) // there are no more regular events. 
	    		)
		    {
		    	backgroundRunnable.run(); // Running background runnable.
		    	backgroundRunnable= null; // Resetting to prevent repeat.
		      }
	    }
  
  } // BackgroundEventQueue

