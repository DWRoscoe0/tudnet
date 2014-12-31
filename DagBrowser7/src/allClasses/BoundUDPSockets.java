package allClasses;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class BoundUDPSockets 
  /* ??? This is not presently used.

    This class manages bound UDP sockets.
    It creates a DatagramSocket for sending and receiving
    all packets except received multicast packets.
    DatagramSocket is thread-safe,
    so it could be used by any number of threads,
    simultaniously for sending and receiving.

    At first it will manage only one DatagramSocket
    bound to PortManager.getLocalPortI().

    ?? Eventually it will need the ability to supply 
    Sockets bound to alternate port numbers in the cases where
    the first choice of a port number is already 
    bound or reserved by another application
    
    ??? Or this might disappear because of 
    the need for ConnectedDatagramSockets.
    */
  {
    private static DatagramSocket aDatagramSocket= null; // The one socket.

    synchronized static public DatagramSocket getDatagramSocket()
      throws SocketException
      // Returns a DatagramSocket bound to the standard local port.
      {
        if ( aDatagramSocket == null )  // Define socket if needed.
          aDatagramSocket=  // Socket becomes...
            new DatagramSocket(  // ...new DatagramSocket...
              new InetSocketAddress(  // ...from new INetSocketAddress...
                PortManager.getLocalPortI()  // ...bound to app's local port.
                )
              );
        return aDatagramSocket;
        }
    }
