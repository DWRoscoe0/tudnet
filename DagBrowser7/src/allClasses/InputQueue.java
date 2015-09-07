package allClasses;

import static allClasses.Globals.appLogger;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InputQueue<E> // For inter-thread communication.
	
	extends ConcurrentLinkedQueue<E>
	
	/* This convenience class combines the following:
	  * The thread-safe ConcurrentLinkedQueue.
	  * A LockAndSignal monitor-lock variable that a threads
	    can use to manage multiple input, such as this queue.
	  * Methods that combine addition of new data elements to the queue with 
	    notification of the destination thread of those additions.
	  Normally a single destination thread will use a set of these queues,
	  all constructed with the same LockAndSignal object,
	  to receive data produced by source threads.

	  */
	
	{
	  LockAndSignal destinationThreadLockAndSignal;  // The monitor-lock to use.
	  
	  InputQueue( LockAndSignal destinationThreadLockAndSignal )  // Constructor.
	    {
	      this.destinationThreadLockAndSignal= destinationThreadLockAndSignal;
	      }
		
	  public boolean add( E anE )
	    /* This method adds anE to the queue and returns immediately.
	      It does not wait for it to be completely processed 
	      by the destination thread.
	     	*/
	    {
	      boolean resultB= super.add( anE );
	      destinationThreadLockAndSignal.doNotifyV();
	      return resultB;
	      }

	  public boolean addAll(Collection<? extends E> aCollection) 
	    /* This method adds aCollection, all elements of it,
	      E to the queue and returns immediately.
	      It does not wait for those elements to be completely processed 
	      by the destination thread.
 	      */
	    {
	      boolean resultB= super.addAll( aCollection );
	      destinationThreadLockAndSignal.doNotifyV();
	      return resultB;
	      }

	  }

// ?? JobStatus and JobQueue are no longer used.  
// Leave them in case they are useful later.
interface JobStatus {
	boolean getJobDoneB();
	void setJobDoneV( Boolean jobDoneB );
  }

class JobQueue<E extends JobStatus> extends InputQueue<E>
  /* This class provides everything InputQueue oes, plus
    addAndWait(..), a synchronous version of add(..).
    
    If the destination is not doing a complex or lengthy operation
    then it doesn't need to be a thread, and the operation
    for which this method is being used could more easily done by
    a simple synchronized method in the destination.
   */
	{
	  
		JobQueue( LockAndSignal destinationThreadLockAndSignal )  // Constructor.
	    {
			  super( destinationThreadLockAndSignal );
	      }
	
	  public boolean addAndWait( E anE )
	    /* This method adds anE to the queue and waits until
	      that object has been completely processed by the destination thread.
	      The destination signals this by doing anE.notify().
	      This method is used when processing must be synchronous.
	     */
	    {
				appLogger.debug("addAndWait(..) begin.");
				boolean isInterruptedB= false;
				boolean resultB;
			  synchronized ( anE ) { // Waiting until anE is completely processed.
			  	anE.setJobDoneV(false);
		  		resultB= add( anE ); // Add job anE to queue.
			    while (true) { // Looping until notification or interrupt.
			      if (anE.getJobDoneB()) // Exiting loop if job is done.
			      	break; 
			      try { 
				  		appLogger.debug("addAndWait(..) calling wait().");
				      anE.wait( ); // Waiting for notification or interrupt.
				  		appLogger.debug("addAndWait(..) notify() ended wait().");
			        } 
			      catch (InterruptedException e) { // Handling interrupt.
				  		appLogger.debug("addAndWait(..) interrupted.");
				  		isInterruptedB= true; // Recording interrupt status for later.
			        }
			      }
			  	}
		    if ( isInterruptedB )  // Re-establishing interrupt if one occurred.
		    	Thread.currentThread().interrupt();
				return resultB;
	  		}

  };

