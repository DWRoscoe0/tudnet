package allClasses;

import java.util.concurrent.TimeUnit;

public class CountingPacketQueue extends PacketQueue

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
	      super( destinationThreadLockAndSignal );
	  		this.outputCountNamedInteger= outputCountNamedInteger;
	      }
	  
	  public SockPacket poll(long timeout, TimeUnit unit)
	        throws InterruptedException
	    {
	  	  SockPacket resultSockPacket= super.poll(timeout, unit);
	  	  if ( resultSockPacket != null ) // Counting packet if gotten.
	  	  	outputCountNamedInteger.addValueL( 1 );
	  	  return resultSockPacket;
	  	  }

	  public SockPacket take()
	       throws InterruptedException
	    {
	  	  SockPacket resultSockPacket= super.take();
	  	  outputCountNamedInteger.addValueL( 1 ); // Counting packet.
	  	  return resultSockPacket;
	    	}
	  	  
		}
