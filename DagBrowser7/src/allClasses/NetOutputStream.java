package allClasses;

import java.io.IOException;
import java.io.OutputStream;

public class NetOutputStream<
		K, // Key.
		E extends KeyedPacket<K>, // Packet. 
		Q extends NotifyingQueue<E>, // Packet queue.
    M extends PacketManager<K,E> // Packet manager.
    >

	extends OutputStream

  /* This class is a network output stream which generates packets.
    If used by a Unicaster then the packets are queued for sending.
    If used by a Subcaster then the packets are queued multiplexing
    before sending. 
    
    ?? Eventually this will be used with DataOutputStream for writing
    particular types to the stream, as follows:
    * NetOutputStream extends OutputStream.
    * NetDataOutputStream(NetOutputStream) extends 
        DataOutputStream(OutputStream).
		NetFilterOutputStream is probably not needed, but could be added?
	  */

	{
	  // Injected dependency variables.
		private final Q notifyingQueueQ; // This is the output packet queue.
		private final M packetManagerP; // Used to produce buffers and packets.
		private final NamedInteger packetCounterNamedInteger;

		private int bufferSizeI= 0; // 0 forces initial flush() to allocate buffer.
		private byte[] bufferBytes= null;
		private int indexI= 0; // 0 prevents sending packet during initial flush().

		NetOutputStream(  // Constructor.
				Q notifyingQueueQ,
				M packetManagerP,
				NamedInteger packetCounterNamedInteger
				)
			{
				this.notifyingQueueQ= notifyingQueueQ;
				this.packetManagerP= packetManagerP;
				this.packetCounterNamedInteger= packetCounterNamedInteger;
        }

		public NamedInteger getCounterNamedInteger() 
		  { return packetCounterNamedInteger; }

		public void write(int value) throws IOException
		  // This writes one byte to the stream.
			{
				if (indexI >= bufferSizeI) // Flushing buffer if no more room there. 
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
			  		E keyedPacketE=
		        	packetManagerP.produceKeyedPacketE(
		        		bufferBytes, indexI // Using buffer containing bytes.
					  		);
		        notifyingQueueQ.add( // Queuing packet for output.
		            keyedPacketE
		            );
		  			packetCounterNamedInteger.addValueL( 1 ); // Counting packet.
			  		}
    		bufferBytes= // Allocating new buffer.
			  	  packetManagerP.produceDefaultSizeBufferBytes();
    		bufferSizeI= bufferBytes.length; // Caching its length. 
			  indexI = 0; // Resetting buffer index.
  	    }

	}
