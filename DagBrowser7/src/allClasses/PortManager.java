package allClasses;

import static allClasses.Globals.*;  // appLogger;

public class PortManager {

  /* This class manages network port numbers used by the app.
    It provides port numbers when requested.
    It tries to avoid allowing the opening of the same port for 
    multiple simultaneous uses, such as Multicast and Unicast uses.
    This might not be practical in the end, and
    conflicts between ports will need to be detected by
    the modules that need the ports.

    ///enh? Figure out how to makes this deal with multiple NetworkInterface-s.
      Should it have a different port on each interface?

    ///org? This should probably be renamed to AddressManager
    and its role expanded to manage assigned IP addresses,
    such as the group Multicast addresses.

    ///enh? It might eventually do run-time pseudo-random
      scrambling of port numbers as a function of
      time and peer ID to make blocking more difficult.

    */

  private final Persistent thePersistent;

  public PortManager( final Persistent thePersistent )
	  {
	  	this.thePersistent= thePersistent;
		  }

  public int getMulticastPortI()
    /* Get port to be used for peer discovery.  
      This port will be used for
      UDP multicast for remote peer app discovery using
      DISCOVERY query packets and ALIVE response packets.
      The server port is this discovery port.
      The client port is the local port.
s     */
    {
      return 44444;
      }

  private int instancePortI= -1; // Less than 0 means undefined.
  
  public int getInstancePortI()
    /* Get port to be used for local instance management.  
      This port will be used for TCP local app instance discovery
      and communication:
      * for checking for server socket port binding.
      * for server local port, for incoming path strings from updater app.
      * for client remote port, for sending paths of local updater app.
s     */
    {
      if ( instancePortI < 0 ) // Undefined.
        { // Define it.
          String instancePortNameString= "instancePort";
          String portString= 
              Confingleton.getValueString(instancePortNameString);
          instancePortI= 44444; // Default port if Confingleton unreadable.
          if ( portString != null )  // If port file exists...
            try { instancePortI= Integer.parseInt(portString); } // .. parse it.
              catch ( NumberFormatException e ) { /* Ignore, using default. */ }
          portString= instancePortI + "";  // Convert int to string.
          Confingleton.putValueV(instancePortNameString, portString);

          appLogger.debug("PortManager.getInstancePortI() port="+instancePortI);
          }
      return instancePortI;
      }
  
  public int getNormalPortI()
    /* This method returns a value to be used as this node's normal local port.
      First it tries reading a previously stored value.
      If that fails then it generates a new value and stores it for later.
      The value is generated randomly in the interval 32768 to 65535.
    
			The app's local port should rarely change.
			This makes it easier to reestablish a connection after it is broken.
			There is also no reason for it to be different if it moves to another IP.
			Therefore only one value needs to be stored per node.  

      In some cases there might be a conflict with 
      other services on the network if it is behind a firewall.
      In these cases a new port might need to be generated. 
      */
    {  
	  	  int NormalPortI= 0;
   	  toReturnValue: {
  	 	toGenerateNewValue: {
		    String localPortString= 
		    		thePersistent.getDefaultingToBlankString("NormalPort");
		    if ( localPortString.isEmpty() ) break toGenerateNewValue; 
	      try { 
	  	    NormalPortI= Integer.parseInt( localPortString );
	      	}
	      catch ( NumberFormatException theNumberFormatException ) {
	        appLogger.error(
	        		"getNormalPortI() corrupted port property="+localPortString);
	      	break toGenerateNewValue;
	      	}
        appLogger.info("getNormalPortI() reusing port: "+NormalPortI);
      	break toReturnValue;
	  	} // toGenerateNewValue:
	  	  NormalPortI= (int)(System.currentTimeMillis()) & 32767 | 32768;
        appLogger.info(
        		"getNormalPortI() generated new random port: "+NormalPortI);
    		thePersistent.putB("NormalPort", ""+NormalPortI); // Make it persist.
			} // toReturnValue:
	  	  return NormalPortI;
      }

  }
