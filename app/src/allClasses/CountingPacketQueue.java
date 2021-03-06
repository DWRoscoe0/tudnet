package allClasses;

import java.util.concurrent.TimeUnit;

public class CountingPacketQueue extends NetcasterQueue

  /* I decided to remove this class and move the counting elsewhere.
   
    This class adds packet counting to its superclass.
    It counts the packets removed from  the queue
    in a NamedLong injected at construction time.

    ?? Not all removal methods are overridden.
    To do so properly would need to account for methods, 
    such as drainTo(..), which might call other removal methods,
    so as to avoid double counting.
    */

  {
    private NamedLong outputCountNamedLong;
  
    CountingPacketQueue(  // Constructor.
        LockAndSignal destinationThreadLockAndSignal,
        NamedLong outputCountNamedLong,
        int queueSizeI,
        String logIdString
        )
      {
        super( destinationThreadLockAndSignal, queueSizeI, logIdString );
        this.outputCountNamedLong= outputCountNamedLong;
        }
    
    public NetcasterPacket poll(long timeout, TimeUnit unit)
          throws InterruptedException
      {
        NetcasterPacket resultNetcasterPacket= super.poll(timeout, unit);
        if ( resultNetcasterPacket != null ) // Counting packet if gotten.
          outputCountNamedLong.addDeltaL( 1 );
        return resultNetcasterPacket;
        }

    public NetcasterPacket take()
         throws InterruptedException
      {
        NetcasterPacket resultNetcasterPacket= super.take();
        outputCountNamedLong.addDeltaL( 1 ); // Counting packet.
        return resultNetcasterPacket;
        }
        
    }
