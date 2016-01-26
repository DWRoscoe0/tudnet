package allClasses;

//import static allClasses.Globals.appLogger;

import java.util.Collection;
//import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NotifyingQueue<E> // For inter-thread communication.
	
	//extends ConcurrentLinkedQueue<E>
	extends LinkedBlockingQueue<E>
	
	/* This convenience class combines the following:

	  * The thread-safe LinkedBlockingQueue.  
	  * A LockAndSignal monitor-lock variable that producer threads
	    can use to notify a single consumer thread 
	    when new input is available.
	  * Methods that combine addition of new data elements to the queue with 
	    notification of the consumer thread of those additions.

	  Normally a single consumer thread 
	  will use one or more of these queues,
	  and perhaps other types of inputs,
	  all linked to the same LockAndSignal object,
	  to detect the availability and receive 
	  data produced by producer threads.

		Interface BlockingQueue<E> methods take() and poll(time, unit)
		should not be used.  When used they block the current thread until 
		their conditions are satisfied.  
		However they are incompatible with the use of class LockAndSignal.
		Instead of these methods, use methods poll() or peek() instead, in 
		a LockAndSignal wait loop.  See class LockAndSignal for more information.

    This file had contained another class, JobQueue,
    whose addAndWait( E anE ) method did not return until
    the element had been removed and processed,
    but was removed because its functionality was not being used.
    
    Previous names for this class were:
    * InputQueue.
    * SignalingQueue?

    ?? This class extended ConcurrentLinkedQueue until 
    blocking methods became needed.
	  */
	
	{
	  LockAndSignal consumerThreadLockAndSignal;  // The monitor-lock to use.
	  
	  NotifyingQueue( LockAndSignal consumerThreadLockAndSignal )  // Constructor.
	    {
	      this.consumerThreadLockAndSignal= consumerThreadLockAndSignal;
	      }
		
	  public LockAndSignal getLockAndSignal()
	    // Returns the ThreadLockAndSignal associated with this queue,
	    // mainly for debugging.
	    { return consumerThreadLockAndSignal; } 
	  
	  public boolean add( E anE )
	    /* This method adds anE to the queue and returns immediately.
	      It also notifies the consumer thread of the new element.
	      It does not wait for anE to be completely processed 
	      by the destination thread.
	     	*/
	    {
	      boolean resultB= super.add( anE );
	      consumerThreadLockAndSignal.doNotifyV();
	      return resultB;
	      }

	  public boolean addAll(Collection<? extends E> aCollection) 
	    /* This method adds aCollection, all elements of it,
	      to the queue and returns immediately.
	      It also notifies the consumer thread of the new elements.
	      It does not wait for those elements to be completely processed 
	      by the destination thread.
 	      */
	    {
	      boolean resultB= super.addAll( aCollection );
	      consumerThreadLockAndSignal.doNotifyV();
	      return resultB;
	      }

	  }
