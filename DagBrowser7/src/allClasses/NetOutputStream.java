package allClasses;

import java.io.IOException;
import java.io.OutputStream;

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
		private final PacketQueue outputPacketQueue; // This is the queue of either
		  // the main Sender thread, or the parent NetOutputStream. 
		private final NetcasterPacketManager theNetcasterPacketManager;
		private final NamedInteger packetCounterNamedInteger;

		private int bufferSizeI= 0; // 0 forces initial flush() to allocate bufferBytes.
		private byte[] bufferBytes= null;
		private int indexI= 0; // 0 prevents sending packet during initial flush().
    
		NetOutputStream(  // Constructor.
				PacketQueue outputPacketQueue,
				NetcasterPacketManager theNetcasterPacketManager,
				NamedInteger packetCounterNamedInteger
				)
			{
				this.outputPacketQueue= outputPacketQueue;
				this.theNetcasterPacketManager= theNetcasterPacketManager;
				this.packetCounterNamedInteger= packetCounterNamedInteger;
        }

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
      /* This outputs any bytes written to the buffer so far, if any,
        and prepares another buffer to receive more bytes.

        ?? Add a variation of this which takes a time limit limitMsL,
        which is the maximum number of milliseconds before
        an actual physical flush() happens.  
        This will allow a parent NetOutputStream to combine data from
        child multiplexed streams into larger packets
        for better bandwidth utilization.
        */
      {
			  if (indexI > 0) // Outputting packet if any bytes in its buffer.
			  	{
		        NetcasterPacket theNetcasterPacket= 
		        	theNetcasterPacketManager.produceNetcasterPacket(
		        		bufferBytes, indexI
					  		);
		        outputPacketQueue.add( // Queuing packet for sending.
		            theNetcasterPacket
		            );
		  			packetCounterNamedInteger.addValueL( 1 ); // Counting sent packet.
			  		}
    		bufferBytes= // Allocating new buffer.
			  	  theNetcasterPacketManager.produceBufferBytes();
    		bufferSizeI= bufferBytes.length; 
			  indexI = 0; // Resetting buffer index.
  	    }

	}
