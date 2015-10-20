package allClasses;

import java.net.DatagramPacket;

public class SockPacket
  /* This class represents a datagram packet with other features attached.

    ?? This class might be deprecated, or at least be renamed.
    
    ?? This should probably be renamed to 
    EnPacket for Enhanced Packet or maybe EpiPacket.

    DatagramPacket can't be sub-classed because it is declared final.
    So compsition is used here instead.
    
    Originally and presently the other field was 
    the DatagramSocket through which the packet passed.
    Hence the class name.
    But having DatagramSocket isn't very useful for 2 reasons:
    1. Bind errors that happen when trying to create connected sockets 
      when an unconnected socket already exists.
    2. IOExceptions caused by a temporary network problems 
      can render a DatagramSocket permanently unusable and 
      needing to be replaced.
      
    As a result, DatagramSocket will probably be replaced,
    possibly by a class which implements an interface that 
    contains a send() method and a reference to a DatagramSocket,
    or it will be completely eliminated.
    */
	{
		private DatagramPacket theDatagramPacket; // The packet to send/receive.
      // This stores remote IP and port.

    public SockPacket(  // Constructor.
        DatagramPacket inDatagramPacket
        )
      /* Constructs a SockPacket associated with 
        DatagramSocket inDatagramSocket and DatagramPacket inDatagramPacket.

        This constructor would not be possible if
        this class had extended DatagramPacket.
        */
      {
        //theDatagramSocket= inDatagramSocket;  // Save socket.
        theDatagramPacket= inDatagramPacket;  // Save packet.
        }

    public DatagramPacket getDatagramPacket()
      // This returns the DatagramPacket associated with this SockPacket.  
      {
        return theDatagramPacket;
        }

		}
