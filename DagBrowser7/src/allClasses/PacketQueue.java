package allClasses;


public class PacketQueue extends InputQueue<SockPacket>
  /* This is a convenience class created only to save typing, 
    by making PacketQueue a synonym for InputQueue<SockPacket>.
    
    ?? Unfortunately it's not an exact synonym, and it causes errors
    when used in a formal parameter and 
    InputQueue<SockPacket> is used as an actual parameter.
    Maybe I should eliminate it?  I have been using it less.
    */
  {

    PacketQueue(  // Constructor.
    		LockAndSignal destinationThreadLockAndSignal
    		)
      {
        super( destinationThreadLockAndSignal );
        }

    }
