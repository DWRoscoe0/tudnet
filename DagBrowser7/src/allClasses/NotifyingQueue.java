package allClasses;

import java.util.concurrent.LinkedBlockingQueue;
import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class NotifyingQueue<E> // Queue inter-thread communication.
	
	//extends ConcurrentLinkedQueue<E>
	extends LinkedBlockingQueue<E>
	
	/* This convenience class combines the following was created to make easier
	  the passing of streams of objects between threads.
	  It has changed over time as needs have changed and problems encountered and solved.
	  It has the following features:

	  * It is a subclass of either the thread-safe 
	    * LinkedBlockingQueue<E> or
	    * ConcurrentLinkedQueue<E>
	    classes depending on whether queue size limitations and blocking are needed.
	  * A LockAndSignal monitor-lock variable that producer threads
	    can use to notify a single consumer thread 
	    when new input is available, of which adding to the queue is an example.
	  * Methods that combine addition of new data elements to the queue with 
	    notification of the consumer thread of those additions using the LockAndSignal.

    This class was created to allow easy changing of 
    the properties of queues used for thread communication.  
    These properties can have a large affect on app behavior.
    * Blocking can be used to throttle producer threads by consumer threads.
    * Blocking can make app behavior be more unpredictable.

	  Normally a single consumer thread will use one or more of these queues,
	  and perhaps other types of inputs, all linked to the same LockAndSignal object,
	  to detect the availability and receive data produced by producer threads.

		Interface BlockingQueue<E> methods take() and poll(time, unit)
		should not be used externally.  
		When used they block the current thread until their conditions are satisfied.  
		However they are incompatible with the use of class LockAndSignal.
		Instead of these methods, use methods poll() or peek() instead, 
		in a LockAndSignal wait loop.  See class LockAndSignal for more information.

    This file had contained another class, JobQueue,
    whose addAndWait( E anE ) method did not return until
    the element had been removed and processed,
    but was removed because its functionality was not being used.
    
    Previous names for this class were:
    * InputQueue.
    * SignalingQueue?
	  */
	
	{
	  LockAndSignal consumerThreadLockAndSignal;  // The monitor-lock to use.
	  int sizeLimitI; // Size which triggers warning log message.
	  
	  NotifyingQueue(   // Constructor. 
	  		LockAndSignal consumerThreadLockAndSignal, int capacityI 
	  		)
	    {
	      super(Integer.MAX_VALUE); // Construct superclass with no size limit.
	  	  /// this.sizeLimitI= capacityI; // Set size limit to capacity parameter.
	      this.sizeLimitI= 4;  // 0; // Use 0 to test logic.
	      this.consumerThreadLockAndSignal= consumerThreadLockAndSignal;
	      }
		
	  public LockAndSignal getLockAndSignal()
	    // Returns the ThreadLockAndSignal associated with this queue,
	    // mainly for debugging.
	    { return consumerThreadLockAndSignal; }
	  
	  /*  //// Don't use this.  It doesn't have the capacity limit checking.
	  public boolean add( E anE )
	    /* 
	      If this method returns true then:
	      * It added anE to the queue.
	      * It notified the consumer thread of the new element.
	      * It did not wait for anE to be processed by the destination thread.

	      If this method throws an IllegalStateException then:
	      * It means that there was not enough space in the queue for 
	        the new element.

				It never returns false.  This is the spec. of Queue.add(E).
	     	*/
	   /*  ////
	    {
	      boolean resultB= super.add( anE );
	      consumerThreadLockAndSignal.notifyingV();
	      return resultB;
	      }
	    */  ////
	  
    public void put( E anE ) //// Non-blocking put(..).
      /* This method
        * Logs a warning if the size limit it being exceeded.
          If this happens, the size limit is increased by one. 
        * It adds anE to the queue.
        * It notifies the consumer thread of the new element.
        It ignores but maintains Thread interrupt status for testing later.
        */
      {
        if ( size() >= sizeLimitI ) { // Handle whether size limit exceeded.
          sizeLimitI++;
          theAppLog.info(
              "NotifyingQueue<E>.put(E) growing queue to size "+sizeLimitI
              + " for:" + NL + "  " + anE);
          }
        super.add( anE ); // Adding element to queue.
        consumerThreadLockAndSignal.notifyingV();
        }

	  /*  ////  Blocking put(..).
	  public void put( E anE )
	    /* If there is no space available in the queue 
	      then it blocks until there is.  When there is space:
	      * It adds anE to the queue.
	      * It notifies the consumer thread of the new element.
	      * It does not wait for anE to be processed by the destination thread.
	      It ignores but maintains Thread interrupt status for testing later.
	      
	      ///err Test for full queue and log message if so before putting?
	     	*/
    /*  ////  Blocking put(..).
	    {
	  	  boolean interruptedB= false; // Assume no interruption will happen.

	  	  if (! offer(anE)) { // Put on queue, logging block if needed.
	        theAppLog.info("NotifyingQueue<E>.put(E) queue full, will now block.");
  	  	  while (true) // Looping until element added to queue, meaning block ends.
  		  	  {
  			      try {
  				  		super.put( anE ); // Adding element to queue.  This might block.
  				  		break; // Exiting loop if put finished without interruption.
  			        } 
  			      catch( InterruptedException ex ) { // Block was interrupted.
  			      	interruptedB= true; // Recording interrupt for restoring later.
  			        }
  			  	  }
	  	    }
	      consumerThreadLockAndSignal.notifyingV();

      	if // Restoring interrupt status if interrupt happened.
	      	( interruptedB ) 
	      	Thread.currentThread().interrupt();
	      }
	      */  ////  Blocking put(..).

	  /*///
	  public boolean XaddAll(Collection<? extends E> aCollection) 
	    /* This method adds aCollection, all elements of it,
	      to the queue and returns immediately.
	      It also notifies the consumer thread of the new elements.
	      It does not wait for those elements to be completely processed 
	      by the destination thread.
 	      */
	  /*///
	    {
	      boolean resultB= super.addAll( aCollection );
	      consumerThreadLockAndSignal.doNotifyV();
	      return resultB;
	      }
	  *///

	  }
