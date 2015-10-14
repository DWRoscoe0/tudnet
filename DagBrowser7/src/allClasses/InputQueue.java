package allClasses;

//import static allClasses.Globals.appLogger;

import java.util.Collection;
//import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class InputQueue<E> // For inter-thread communication.
	
	//extends ConcurrentLinkedQueue<E>
	extends LinkedBlockingQueue<E>
	
	/* This convenience class combines the following:
	  * The thread-safe LinkedBlockingQueue.  
	  * A LockAndSignal monitor-lock variable that a thread
	    can use to manage multiple inputs, such as these queues.
	  * Methods that combine addition of new data elements to the queue with 
	    notification of the destination thread of those additions.
	  Normally a single destination thread will use a set of these queues,
	  all constructed with the same LockAndSignal object,
	  to receive data produced by source threads.

		Interface BlockingQueue<E> methods take() and poll(time, unit)
		will block the thread until their conditions are satisfied,
		but they can not be used with LockAndSignal to wait for multiple inputs.
		To do this, use only methods poll() or peek() in a loop with
		other input checks and calls to LockAndSignal wait methods.

    This file had also contained another class, JobQueue,
    whose addAndWait( E anE ) method did not return until
    the element had been removed and processed,
    but was removed because its functionality was not being used.
    
    This class used ConcurrentLinkedQueue until blocking methods became needed.
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
	      to the queue and returns immediately.
	      It does not wait for those elements to be completely processed 
	      by the destination thread.
 	      */
	    {
	      boolean resultB= super.addAll( aCollection );
	      destinationThreadLockAndSignal.doNotifyV();
	      return resultB;
	      }

	  }
