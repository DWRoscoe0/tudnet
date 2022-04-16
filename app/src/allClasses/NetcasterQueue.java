package allClasses;


public class NetcasterQueue 

  extends NotifyingQueue<NetcasterPacket>

  // This is a NotifyingQueue for elements of type NetcasterPacket only.

  {

    NetcasterQueue(  // Constructor.
        LockAndSignal destinationThreadLockAndSignal, 
        int capacityI, 
        String logIdString
        )
      {
        super( destinationThreadLockAndSignal, capacityI, logIdString);
        }

    }
