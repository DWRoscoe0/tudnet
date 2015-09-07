package allClasses;


public class PacketQueue extends InputQueue<SockPacket>
  /* This is a convenience class created to save typing, 
    by making PacketQueue a synonym for InputQueue<SockPacket>.
    */
  {

    PacketQueue(  // Constructor.
    		LockAndSignal destinationThreadLockAndSignal
    		)
      {
        super( destinationThreadLockAndSignal );
        }

    }
