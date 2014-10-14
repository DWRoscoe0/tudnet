package allClasses;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SignallingQueue<E> // For inter-thread communication.

extends ConcurrentLinkedQueue<E>

/* This convenience class combines the following:
  * A LockAndSignal monitor-lock variable that multiple threads 
    can use to synchronize communication using that queue.
  * A thread-safe queue.
  * Methods that combine addition of elements to the queue
    with notification of the destination thread of the new data.
  Normally a single thread will use a set of these SignallingQueues,
  all constructed referencing the same LockAndSignal object,
  to receive data produced by other threads.
  */

{
  LockAndSignal threadLockAndSignal;  // The monitor-lock to use.
  
  SignallingQueue( LockAndSignal aLockAndSignal )  // Constructor.
    {
      threadLockAndSignal= aLockAndSignal;
      }

  public boolean add( E anE )
    {
      boolean resultB= super.add( anE );
      threadLockAndSignal.doNotify();
      return resultB;
      }

  public boolean addAll(Collection<? extends E> aCollection) 
    {
      boolean resultB= super.addAll( aCollection );
      threadLockAndSignal.doNotify();
      return resultB;
      }
  }
