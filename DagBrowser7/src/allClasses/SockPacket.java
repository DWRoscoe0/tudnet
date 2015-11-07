package allClasses;

import java.net.DatagramPacket;

public class SockPacket
  /* This class represents a DatagramPacket with other features attached.
    This class exists because DatagramPacket can't be sub-classed 
    because it is declared final.  This class uses composition instead.
    
    ?? This class should probably be renamed to EpiPacket.
    If it's not renamed then it should probably be deprecated.
    
    ?? Add time-stamp field for use in calculating low-level round-trip-time.
    
    ?? Add nested stream capability?
    
    A bit of history:
	    
	    Originally the other field in this class was 
	    the DatagramSocket through which the packet passed.
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
	    This would allow p2p connections to outlive the DatagramSockets
	    though which pass their packets.
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
