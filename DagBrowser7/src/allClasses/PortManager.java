package allClasses;

import static allClasses.Globals.*;  // appLogger;

public class PortManager {

  /* This class manages network port numbers used by the app.
    It provides port numbers when requested.
    It avoids allowing opening the same port for multiple simultaneous uses,
    such as Multicast and Unicast uses

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

  public int getDiscoveryPortI()
    /* Get port to be used for peer discoveries.  
      This same port will be used as:
      * TCP port for discovering other local running app instances.
      * UDP multicast port for discovering remote/peer running app instances.
     */
    {
      return 44444;
      }
  
  public int getLocalPortI()
    /* This method returns a value to be used as this node's local port.
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
	  	  int localPortI= 0;
   	  toReturnValue: {
  	 	toGenerateNewValue: {
		    String localPortString= 
		    		thePersistent.getDefaultingToBlankString("LocalPort");
		    if ( localPortString.isEmpty() ) break toGenerateNewValue; 
	      try { 
	  	    localPortI= Integer.parseInt( localPortString );
	      	}
	      catch ( NumberFormatException theNumberFormatException ) {
	        appLogger.error(
	        		"getLocalPortI() corrupted port property="+localPortString);
	      	break toGenerateNewValue;
	      	}
        appLogger.info("getLocalPortI() reusing port="+localPortI);
      	break toReturnValue;
	  	} // toGenerateNewValue:
	  	  localPortI= (int)(System.currentTimeMillis()) & 32767 | 32768;
        appLogger.info("getLocalPortI() generated new random port="+localPortI);
    		thePersistent.putV("LocalPort", ""+localPortI); // Save it.
			} // toReturnValue:
	  	  return localPortI;
      }

  }
