package allClasses;

import java.io.IOException;
import java.io.OutputStream;

public class EpiOutputStream<
		K, // Key.
		E extends KeyedPacket<K>, // Packet. 
		Q extends NotifyingQueue<E>, // Packet queue.
    M extends PacketManager<K,E> // Packet manager.
    >

	extends OutputStream

  /* This class is a network output stream which 
    generates UDP packets from a stream of data, mostly bytes.
    If used by a Unicaster then the packets are queued for sending.
    If used by a Subcaster then the packets are queued for multiplexing
    before sending. 

    ?? Eventually this will be used with DataOutputStream for writing
    particular types to the stream, as follows:
    * EpiOutputStream extends OutputStream.
    * NetDataOutputStream(EpiOutputStream) extends 
        DataOutputStream(OutputStream).
		NetFilterOutputStream is probably not needed, but could be added?
	  */

	{
	  // Injected dependency variables.
		private final Q notifyingQueueQ; // This is the output packet queue.
		private final M packetManagerM; // Used to produce buffers and packets.
		private final NamedInteger packetCounterNamedInteger;

		private int bufferSizeI= 0; // 0 forces initial flush() to allocate buffer.
		private byte[] bufferBytes= null;
		private int indexI= 0; // 0 prevents sending packet during initial flush().

		EpiOutputStream(  // Constructor.
				Q notifyingQueueQ,
				M packetManagerM,
				NamedInteger packetCounterNamedInteger
				)
			{
				this.notifyingQueueQ= notifyingQueueQ;
				this.packetManagerM= packetManagerM;
				this.packetCounterNamedInteger= packetCounterNamedInteger;
        }

		public NamedInteger getCounterNamedInteger() 
		  { return packetCounterNamedInteger; }

		public M getPacketManagerM()
		  { return packetManagerM; }
		
		public void write(int value) throws IOException
		  // This writes one byte to the stream.
			{
				if (indexI >= bufferSizeI) // Flushing buffer if no more room there. 
				  	flush();
				bufferBytes[indexI]=  // Storing byte in buffer.
						(byte) (value & 0x0ff);
				indexI++; // Advancing buffer index.
				}

	  public void writeV( E theKeyedPacketE )
		  throws IOException
	    /* This method writes a packet theKeyedPacketE to the stream.
	      First it queues a packet containing any bytes in the present buffer.
	      Then it queues theKeyedPacketE.
	      */
	    {
	  		queuingBufferDataB(); // Queuing packet with buffer data if any.
	  	  queuingV( theKeyedPacketE ); // Queuing new data argument packet.
	    	}

	  public void flush() throws IOException
      /* This outputs any bytes written to the buffer so far, if any,
        and prepares another buffer to receive more bytes.
        It can be called internally when the buffer becomes full,
        or externally when written bytes needs to be sent
        or a packet boundary is needed.

        ?? Add a variation of this which takes a time limit limitMsL,
        which is the maximum number of milliseconds before
        an actual physical flush() happens.  
        This will allow a parent EpiOutputStream to combine data from
        child multiplexed streams into larger packets
        for better bandwidth utilization.
        */
      {
	  		if  // Queuing packet with buffer bytes if any.
	  		  (queuingBufferDataB() )
		  		{ // Allocating new buffer because old one was queued.
		    		bufferBytes= // Allocating new buffer.
					  	  packetManagerM.produceDefaultSizeBufferBytes();
		    		bufferSizeI= bufferBytes.length; // Caching its length. 
					  ///indexI = 0; // Resetting buffer index.
			  		}
  	    }


	  public boolean queuingBufferDataB() throws IOException
	    /* Queues a packet containing bytes in the buffer, if there are any.
	      It returns true if a new buffer needs to be allocated, 
	      false otherwise.
	      A new buffer needs to be allocated if:
	      * the previous buffer was queued, or
	      * a buffer has never been allocated.
	      */
	  	{
	  	  boolean testB;
	  	  processing: {
		  	  testB= (indexI > 0); // Testing for bytes in buffer.
			  	if ( testB ) // Outputting packet if any bytes in buffer.
				  	{
				  		E keyedPacketE= packetManagerM.produceKeyedPacketE(
			        		bufferBytes, indexI // Using buffer containing bytes.
						  		);
				  		queuingV( keyedPacketE );
						  indexI = 0; // Resetting buffer index to indicate empty.
						  break processing; // Exitting with new buffer needed.
				  		}
					testB=  // Testing whether initial buffer needed.
							(bufferSizeI==0);
	  	  	}
		    return testB; // Returning whether anew buffer is needed.
		    }
	    

	  private void queuingV( E theKeyedPacketE )
		  // This method queues a packet and counts it.
			{
		    notifyingQueueQ.add( theKeyedPacketE ); // Adding packet to queue.
				packetCounterNamedInteger.addDeltaL( 1 ); // Counting the packet.
				}
				
	}
