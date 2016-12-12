package allClasses;

//import static allClasses.Globals.appLogger;


public class NetcasterInputStream

  extends EpiInputStream<
		  IPAndPort,
		  NetcasterPacket,
			NetcasterQueue,
			NetcasterPacketManager
			>

  {

  	public NetcasterInputStream( // Constructor. 
  		NetcasterQueue receiverToNetcasterNetcasterQueue, 
  		NamedLong packetCounterNamedLong
  		)
  	{
    	super( 
      		receiverToNetcasterNetcasterQueue, 
      		packetCounterNamedLong
      		);
  		}

	}
