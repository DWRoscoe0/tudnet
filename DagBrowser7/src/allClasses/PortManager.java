package allClasses;

import static allClasses.AppLog.theAppLog;


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

  private final String instancePortNameString= "instancePort";
  private int instancePortI= -1; /* Less than 0 means undefined.
      This port will be used for TCP local app instance discovery
      and communication:
      * for checking for server socket port binding.
      * for server local port, for incoming path strings from updater app.
      * for client remote port, for sending paths of local updater app.
      */
  
  public int getInstancePortI()
    {
      if ( instancePortI < 0 ) // Undefined.
        { // Define it.
          instancePortI= getConfingletonPortI(instancePortNameString);
          }
      return instancePortI;
      }
  
  public int getConfingletonPortI(String fileNameString)
    /* Get port to be used for network communication.
      fileNameString contains the name of the Confingleton file
      where a numeric string is stored.
      */
    {
      String nameString= fileNameString;
      int portI= Confingleton.getValueI(nameString);
      if (portI < 0) 
        portI= 44444; // If no or bad value, change to default value.
        else 
        ; // otherwise, use as is.
      Confingleton.putValueV(nameString, portI);
      return portI;
      }

  private int normalPortI= 0;

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
   	  toReturnValue: {
        if (normalPortI != 0) break toReturnValue; // Exit if already defined.
  	 	toGenerateNewValue: {
		    String localPortString= 
		    		thePersistent.getEmptyOrString("NormalPort");
		    if ( localPortString.isEmpty() ) break toGenerateNewValue; 
	      try { 
	  	    normalPortI= Integer.parseInt( localPortString );
	      	}
	      catch ( NumberFormatException theNumberFormatException ) {
	        theAppLog.error(
	        		"getNormalPortI() corrupted port property="+localPortString);
	      	break toGenerateNewValue;
	      	}
        theAppLog.info("getNormalPortI() reusing port: "+normalPortI);
      	break toReturnValue;
	  	} // toGenerateNewValue:
	  	  normalPortI= (int)(System.currentTimeMillis()) & 32767 | 32768;
        theAppLog.info(
        		"getNormalPortI() generated new random port: "+normalPortI);
        /// thePersistent.putV("normalPort", ""+normalPortI); // Using old erroneous name.
        thePersistent.putV("NormalPort", ""+normalPortI); // Make it persist.
			} // toReturnValue:
	  	  return normalPortI;
      }

  }
