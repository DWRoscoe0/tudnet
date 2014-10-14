package allClasses;

import static allClasses.Globals.*;  // appLogger;

public class PortManager {

  /* This class manages network port numbers used by the app.
    It provides port numbers when requested.
    It avoids allowing opening the same port for multiple simultaneous uses,
    such as Multicast and Unicast uses

    It might eventually do pseudorandomly scramble port numbers
    as a function of time and peer ID to make blocking more difficult.
    
    ??? Should this be a Singleton?
    
    ??? This should probably be renamed to AddressManager
    and its role expanded to manage assigned IP addresses,
    such as the group Multicast addresses.
    */

  public static int getDiscoveryPortI()
    /* Get port to be used in discoveries.  It will be used to:
     *   * TCP port for discovering other app instances.
     *   * UDP multicast port for discovering other peer nodes.
     */
    {
      return 44444;
      }

  public static int getDiscoveryLocalPortI()
    // Experimental method.
    {
      //return 44444;
      return 40404;
      }

  private static int localPortI= 0;
  
  public static int getLocalPortI()
    // Experimental method.
    {
      while ( localPortI == 0 ) {
        localPortI= (int)(System.currentTimeMillis()) & 32767 | 32768;
        appLogger.info("getLocalPortI() port="+localPortI);
        }
      return localPortI;
      }

  }
