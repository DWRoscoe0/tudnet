package allClasses;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class NetOutputStream 

	extends OutputStream

  /* This class is a network output stream, at first generating UDP packets.
    
    ?? Eventually this will be used with DataOutputStream for writing
    particular types to the stream, as follows:
    * NetOutputStream extends OutputStream.
    * NetDataOutputStream(NetOutputStream) extends 
        DataOutputStream(OutputStream).
		NetFilterOutputStream is probably not needed, but could be added?
	  
	  */

	{
	  // Injected dependency variables.
		InputQueue<SockPacket> sendQueueOfSockPackets;
		InetAddress theInetAddress = null;
	  int thePortI = 0;
		NamedInteger counterNamedInteger;

		public static final int DEFAULT_BUFFER_SIZE = 1024;
	  
		byte[] bufferBytes= new byte[DEFAULT_BUFFER_SIZE];
  
		int bufferSizeI= 0; // 0 forces initial flush() to allocate bufferBytes.
		int indexI = 0; // 0 prevents sending any packet during initial flush().
		DatagramPacket theDatagramPacket = null;
    
		NetOutputStream(  // Constructor.
				InputQueue<SockPacket> sendQueueOfSockPackets, 
				InetAddress theInetAddress, 
				int thePortI,
				NamedInteger counterNamedInteger
				)
			{
				this.sendQueueOfSockPackets= sendQueueOfSockPackets;
				this.theInetAddress = theInetAddress;
			  this.thePortI = thePortI;		
				this.counterNamedInteger= counterNamedInteger;
        }
		
		public void write(int value) throws IOException
		  // This writes one byte to the stream.
			{
				if (indexI >= bufferSizeI) // Flushing if there is no room in buffer. 
				  flush();
				bufferBytes[indexI] = (byte) (value & 0x0ff); // Storing byte.
				indexI++; // Advancing buffer index.
				}
	  
    public void flush() throws IOException
      /* This writes any bytes written to the buffer so far, if any,
        and prepares another buffer to receive more bytes.
        */
      {
			  if (indexI > 0) // Sending packet if any bytes in buffer.
			  	{
					  theDatagramPacket = new DatagramPacket(
					  		bufferBytes, 0, indexI, theInetAddress, thePortI
					  		);
		        SockPacket aSockPacket= new SockPacket(theDatagramPacket);
		        sendQueueOfSockPackets.add( // Queuing packet for sending.
		            aSockPacket
		            );
		  			counterNamedInteger.addValueL( 1 ); // Counting received packet.
			  		}
    		bufferBytes = new byte[DEFAULT_BUFFER_SIZE]; // Allocating new buffer.
    		bufferSizeI= bufferBytes.length; 
			  indexI = 0; // Resetting buffer index.
  	    }

	}
