package allClasses;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class NetOutputStream 

	extends OutputStream

  /* This class is a network output stream, at first generating UDP packets.
    If used by a Unicaster then packets generated will be queued to the Sender.
    If used by a Subcaster then packets generated will be queued to 
    the parent Unicaster and the unicasterInetAddress and unicasterPortI
    will be ignored.
    
    ?? Eventually this will be used with DataOutputStream for writing
    particular types to the stream, as follows:
    * NetOutputStream extends OutputStream.
    * NetDataOutputStream(NetOutputStream) extends 
        DataOutputStream(OutputStream).
		NetFilterOutputStream is probably not needed, but could be added?
	  
	  */

	{
	  // Injected dependency variables.
		PacketQueue outputPacketQueue; // This is the queue of either
		  // the main Sender thread, or the parent NetOutputStream. 
		InetAddress unicasterInetAddress = null;
	  int unicasterPortI = 0;
		NamedInteger packetCounterNamedInteger;

		public static final int DEFAULT_BUFFER_SIZE = 1024;
	  
		int bufferSizeI= 0; // 0 forces initial flush() to allocate bufferBytes.
		byte[] bufferBytes= null;
		int indexI= 0; // 0 prevents sending any packet during initial flush().
		DatagramPacket theDatagramPacket = null;
    
		NetOutputStream(  // Constructor.
				PacketQueue outputPacketQueue, 
				InetAddress unicasterInetAddress, // Ignored if a Subcaster stream.
				int unicasterPortI, // Ignored if a Subcaster stream.
				NamedInteger packetCounterNamedInteger
				)
			{
				this.outputPacketQueue= outputPacketQueue;
				this.unicasterInetAddress= unicasterInetAddress;
			  this.unicasterPortI= unicasterPortI;		
				this.packetCounterNamedInteger= packetCounterNamedInteger;
        }

		public PacketQueue getPacketQueue () 
		  { return outputPacketQueue; }

		public NamedInteger getCounterNamedInteger() 
		  { return packetCounterNamedInteger; }

		public void write(int value) throws IOException
		  // This writes one byte to the stream.
			{
				if (indexI >= bufferSizeI) // Flushing buffer if there is no more room. 
				  flush();
				bufferBytes[indexI]=  // Storing byte in buffer.
						(byte) (value & 0x0ff);
				indexI++; // Advancing buffer index.
				}
	  
    public void flush() throws IOException
      /* This writes any bytes written to the buffer so far, if any,
        and prepares another buffer to receive more bytes.

        ?? Add a variation of this which takes a time limit limitMsL,
        which is the maximum number of milliseconds before
        an actual physical flush() happens.  
        This will allow a parent NetOutputStream to combine data from
        child multiplexed streams into larger packets
        for better bandwidth utilization.
        */
      {
			  if (indexI > 0) // Sending packet if any bytes in buffer.
			  	{
					  theDatagramPacket = new DatagramPacket(
					  		bufferBytes, 0, indexI, unicasterInetAddress, unicasterPortI
					  		);
		        SockPacket theSockPacket= new SockPacket(theDatagramPacket);
		        outputPacketQueue.add( // Queuing packet for sending.
		            theSockPacket
		            );
		  			packetCounterNamedInteger.addValueL( 1 ); // Counting sent packet.
			  		}
    		bufferBytes = new byte[DEFAULT_BUFFER_SIZE]; // Allocating new buffer.
    		bufferSizeI= bufferBytes.length; 
			  indexI = 0; // Resetting buffer index.
  	    }

	}
