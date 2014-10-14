package allClasses;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
///import java.net.SocketAddress;

public class SockPacket
  /* This class represents a socketed datagram packet,
    a combination of a DatagramPacket and a DatagramSocket
    through which it passes.
    This combination can provide port and IP for both end-points.
    
    ??? This should probably be changed to replace DatagramSocket
    with something else because an IOException caused by
    a temporary network problem can render a DatagramSocket
    permanently unusable and need to be replaced.
    
    See ??? deletion.
    */
	{

    SignallingQueue<SockPacket> 
      theSignallingQueue;  // Test...  ??? delete this?  Not needed.
      // ... for general queuer which can be reused. ???

		private DatagramPacket theDatagramPacket; // The packet to send/receive.
      // This stores remote IP and port.
    private DatagramSocket theDatagramSocket; // Associated Socket.
      // This might store both local and remote IP and port.

    public SockPacket(  // Constructor.
        DatagramSocket inDatagramSocket, DatagramPacket inDatagramPacket
        )
      /* Constructs a SockPacket associated with 
        DatagramSocket inDatagramSocket and DatagramPacket inDatagramPacket.

        This constructor would not be possible if
        this class had extended DatagramPacket.
        */
      {
        theDatagramSocket= inDatagramSocket;  // Save socket.
        theDatagramPacket= inDatagramPacket;  // Save packet.
        }

    public DatagramPacket getDatagramPacket()
      // This returns the DatagramPacket associated with this SockPacket.  
      {
        return theDatagramPacket;
        }

    public DatagramSocket getDatagramSocket()
      // This returns the DatagramSocket associated with this SockPacket.  
      {
        return theDatagramSocket;
        }

    public String getSocketAddressesString()
      /* This returns a String representing the
        end-point addresses (IP and port) of this SockPacket.
        The String actually contains 3 parts:
          SL: Socket Local.
          SR: Socket Remote.
          PR: Packet Remote.
        */
      {
        String packetAddressString;

        try { // Calculating packet address separately in case of Exception.
            packetAddressString= 
              theDatagramPacket.getSocketAddress().toString();
            }
          catch (Exception e) { // Handling undefined value.
            packetAddressString= "undefined";
            }

        String valueString= "" // Assembling entire string.
              + " SL:" + theDatagramSocket.getLocalSocketAddress()
              + " SR:" + theDatagramSocket.getRemoteSocketAddress()
              + " PR:" + packetAddressString
              ;

        return valueString;
        }

		}
