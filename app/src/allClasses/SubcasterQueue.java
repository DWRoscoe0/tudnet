package allClasses;


public class SubcasterQueue 

  extends NotifyingQueue<SubcasterPacket>

  // This is a NotifyingQueue for elements of type SubcasterPacket only.

  {

    SubcasterQueue(  // Constructor.
        LockAndSignal destinationThreadLockAndSignal, 
        int capacityI, 
        String logIdString
        )
      {
        super( destinationThreadLockAndSignal, capacityI, logIdString );
        }

    }
