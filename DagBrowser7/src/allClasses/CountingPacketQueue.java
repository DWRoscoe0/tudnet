package allClasses;

import java.util.concurrent.TimeUnit;

public class CountingPacketQueue extends NetcasterQueue

  /* I decided to remove this class and move the counting elsewhere.
   
    This class adds packet counting to its superclass.
    It counts the packets removed from  the queue
    in a NamedInteger injected at construction time.

    ?? Not all removal methods are overridden.
    To do so properly would need to account for methods, 
    such as drainTo(..), which might call other removal methods,
    so as to avoid double counting.
    */

	{
		private NamedInteger outputCountNamedInteger;
	
	  CountingPacketQueue(  // Constructor.
	  		LockAndSignal destinationThreadLockAndSignal,
	  		NamedInteger outputCountNamedInteger
	  		)
	    {
	      super( destinationThreadLockAndSignal, Integer.MAX_VALUE );
	  		this.outputCountNamedInteger= outputCountNamedInteger;
	      }
	  
	  public NetcasterPacket poll(long timeout, TimeUnit unit)
	        throws InterruptedException
	    {
	  	  NetcasterPacket resultNetcasterPacket= super.poll(timeout, unit);
	  	  if ( resultNetcasterPacket != null ) // Counting packet if gotten.
	  	  	outputCountNamedInteger.addDeltaL( 1 );
	  	  return resultNetcasterPacket;
	  	  }

	  public NetcasterPacket take()
	       throws InterruptedException
	    {
	  	  NetcasterPacket resultNetcasterPacket= super.take();
	  	  outputCountNamedInteger.addDeltaL( 1 ); // Counting packet.
	  	  return resultNetcasterPacket;
	    	}
	  	  
		}
