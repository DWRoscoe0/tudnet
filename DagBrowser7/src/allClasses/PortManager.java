package allClasses;

public class PortManager {

  /* This class manages network port numbers used by the app.
    It provides port numbers when requested.
    It avoids allowing opening the same port for multiple simultaneous uses,
      such as Multicast and Unicast uses
      
    Its job might become more complicated when and if
    it algorithmically changes Unicast or Multicast ports
    as a function of time to make blocking more difficult.
    */

  public static int getDiscoveryPortI()
    /* Get port to be used in discoveries.  It will be used to:
     *   * Discover other app instances.
     *   * Discover other peer nodes.
     */
    {
      return 44444;
      }

  public static int getDiscoveryLocalPortI()
    // Experimental method.
    {
      return 44444;
      //return 40404;
      }

  public static int getLocalPortI()
    // Experimental method.
    {
      return 55111;
      }

  }
