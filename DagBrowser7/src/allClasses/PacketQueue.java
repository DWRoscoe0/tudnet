package allClasses;


public class PacketQueue extends InputQueue<SockPacket>
  /* This is a convenience class created only to save typing, 
    by making PacketQueue a synonym for InputQueue<SockPacket>.
    
    ?? Unfortunately it's not an exact synonym, 
    and it causes compile errors when used in a formal parameter and 
    InputQueue<SockPacket> is used as an actual parameter.
    Code with a mixture of these two is difficult to maintain.
    Maybe I should eliminate it?  I have been using it less often.
    */
  {

    PacketQueue(  // Constructor.
    		LockAndSignal destinationThreadLockAndSignal
    		)
      {
        super( destinationThreadLockAndSignal );
        }

    }
