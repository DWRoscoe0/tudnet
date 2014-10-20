package allClasses;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class SockPacket
  /* This class represents a datagram packet with other features attached.

    ??? This should probably renamed to EnPacket for Enhanced Packet.

    Originally and presently the other features was 
    the DatagramSocket through which it passed.
    Hence the class name.
    But this isn't very useful for 2 reasons:
    1. Bind errors that heppen when trying to create connected sockets 
      when an unconnected already exists.
    2. IOExceptions caused by a temporary network problem 
      can render a DatagramSocket permanently unusable and 
      needing to be replaced.
      
    As a result, DatagramSocket will probably be replaced,
    possibly by a class which implements an interface that 
    contains a send() method and a reference to a DatagramSocket.
    */
	{
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

    /* ???
    public String getSocketAddressesString()
      /* This returns a String representing the
        end-point addresses (IP and port) of this SockPacket.
        The String actually contains 3 parts:
          SL: Socket Local.
          SR: Socket Remote.
          PR: Packet Remote.
        */
    /* ???
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
    */

		}
