package allClasses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

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
    
    //// markIndivisibleBlock().
    Add this method which records the buffer position as the end of the block.
    If the buffer becomes full, only the bytes up to 
    the last block mark will be sent in a packet.
    The remaining bytes will be copied to the front of the buffer
    and become the beginning of the next packet. 

    ////?? delayedFlush(long delayMsL).
    Add this variation of flush() which takes a time limitMsL,
    which is the maximum number of milliseconds before
    an actual physical flush() happens.  
    This will allow fuller packets and better bandwidth utilization allowing:
    * a parent EpiOutputStream to combine data from
    	child multiplexed streams into larger packets
    * interactive apps to produce fewer packets.
    This is a good case for using a single Timer. 
    Alternatively, a setFlushIntervalV(..) method might be better.
    It would remain in effect until changed.

    //// Eventually this will be used with DataOutputStream for writing
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
		private final NamedLong packetCounterNamedLong;
		  // This is the count of sent packets. 
		  // The value is 0 during read of 1st packet data, assuming
		  // it is constructed with a value of 0.
			// It becomes 1 after the packet containing that data is queued.
		private Timer theTimer; // For delayedBlockFlushV( long delayMsL ).

  	private TimerTask theTimerTask= null;
  	private long sendTimeMsL;

		private int bufferSizeI= 0; // 0 forces initial flush() to allocate buffer.
		private byte[] bufferBytes= null;
		private int indexI= 0; // 0 prevents sending packet during initial flush().
		private int sendableI= 0; // End of data when packet is sent.

		EpiOutputStream(  // Constructor.
				Q notifyingQueueQ,
				M packetManagerM,
				NamedLong packetCounterNamedLong,
	  		Timer theTimer
				)
			{
				this.notifyingQueueQ= notifyingQueueQ;
				this.packetManagerM= packetManagerM;
				this.packetCounterNamedLong= packetCounterNamedLong;
        }

		public NamedLong getCounterNamedLong() 
		  { return packetCounterNamedLong; }

		public M getPacketManagerM()
		  { return packetManagerM; }
		
		public void write(int value) throws IOException
		  // This writes one byte to the stream.
		  //// Because this is UDP, it should never flush here.  Make Exception?
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
	  		queuingBufferDataV(); // Queuing packet with buffer data if any.
	  	  queuingForSendV( theKeyedPacketE ); // Queuing new data argument packet.
	    	}

	  public void flush() throws IOException 
	    /* This outputs any bytes written to the buffer so far, if any,
		    and prepares another buffer to receive more bytes.
		    It can be called internally when the buffer becomes full,
		    or externally when written bytes need to be sent and
		    thereby create a new packet boundary.
		    It is equivalent to a delayedBlockflushV(...), with no delay.
		    */
	    { 
	  		////delayedBlockflushV( 0 ); 
	  		doOrScheduleFlushB( 0 );
	  		}

    public synchronized boolean doOrScheduleFlushB( long delayMsL )
    /* This method either queues a packet for sending 
      containing sendable bytes(), or schedules the send for later.
      The packet will contain only the bytes written so far,
      even though more might have been written by the time the send occurs.
      It returns true if a flush was done, false if it was scheduled.

    	The proper time for flushing is the nearer of delayMsL or
    	the remaining time on the flush Timer.
      The flush Timer is cancelled, and reset if necessary, 
      depending on the circumstances.
     */
    {
    	boolean queuePacketB;
    	TimerTask newTimerTask= null;

    	// Defining break goto targets.
    	beforeExit: {
    	beforeCancelAndReplaceTimerTask: {
    	beforeCreateCancelAndReplaceTimerTask: {
  
	  		sendableI= indexI; // Marking new end of sendable bytes.
    		queuePacketB= ( delayMsL == 0 );
    	  if ( queuePacketB ) { // Queuing send now if no delay requested.
      		queuingBufferDataV();
	        break beforeCancelAndReplaceTimerTask;
    	    }
    	  if (theTimerTask == null) // Creating new TimerTask if none already. 
    	  	break beforeCreateCancelAndReplaceTimerTask;

    	  { // Exiting if the existing TimerTask will do the job.
    	  	long newSendTimeMsL= System.currentTimeMillis() + delayMsL;
    	  	if ( newSendTimeMsL >= sendTimeMsL )
    	  		break beforeExit; // Exiting because new send would be later.
    	  }

    	} // beforeCreateCancelAndReplaceTimerTask:
    		newTimerTask= new TimerTask() { // Creating TimerTask.
	        public void run()
	          {
	        		queuingBufferDataV();
	        	  }
	    		};
	    	theTimer.schedule(newTimerTask, delayMsL); // Scheduling it.
	    	sendTimeMsL= System.currentTimeMillis() + delayMsL; // Saving its time.
	    	  
    	} // beforeCancelAndReplaceTimerTask: 
	  		if (theTimerTask != null) { // Canceling old TimerTask if it exists.
	  				theTimerTask.cancel();
			    	theTimerTask= null; // Recording no longer scheduled.
	    			}
	  		if (newTimerTask != null) { // Recording new TimerTask, if any.
	  			  theTimerTask= newTimerTask;
	  			  newTimerTask= null;
	    	  	}

     	} // beforeExit: 
	      return queuePacketB;
    	}

	  public void queuingBufferDataV()
	    /* Queues a packet containing send-able buffer bytes, if there are any.
	      It also allocates a new buffer to replace the one queued,
	      and copies any unsendable bytes from the old buffer to the new one.
	      */
	  	{
	  	  ////boolean testB;
	  		// Allocating new buffer because old one will be queued.
  			byte[] newBufferBytes= 
  					packetManagerM.produceDefaultSizeBufferBytes();
	  	  processing: {
	  	  	//// Changed this to use sendableI.
		  	  //%testB= (indexI > 0); // Testing for bytes in buffer.
	  	  	////testB= (sendableI > 0); // Testing for send-able bytes in buffer.
			  	if ( sendableI > 0 ) // Outputting packet if any bytes in buffer.
				  	{
							///indexI = 0; // Resetting buffer index.
				  		E keyedPacketE= packetManagerM.produceKeyedPacketE(
			        		bufferBytes, indexI // Using buffer containing bytes.
						  		);
				  		for // Copying unsent bytes to beginning of new buffer.
						  	( int dstI=0, srcI= sendableI; srcI < indexI ; )
						  	newBufferBytes[dstI++]= bufferBytes[srcI++];
						  indexI-= sendableI; // Subtracting sent bytes from buffer index.
						  sendableI= 0;  // Indicating no bytes are sendable.
				  		queuingForSendV( keyedPacketE ); // Queuing old buffer.
						  break processing; // Exiting with new buffer needed.
				  		}
					////testB=  // Testing whether initial buffer allocation needed.
			  	////		(bufferSizeI==0);
	  	  	}
	  		bufferBytes= newBufferBytes; // Start using new buffer.
	    	bufferSizeI= newBufferBytes.length; // Caching its length. 
		    }
	    

	  private void queuingForSendV( E theKeyedPacketE )
		  // This method queues a packet and counts it.
			{
		    ///notifyingQueueQ.add( theKeyedPacketE ); // Adding packet to queue.
	  		notifyingQueueQ.put( theKeyedPacketE ); // Adding packet to queue.
				packetCounterNamedLong.addDeltaL( 1 ); // Counting the packet.
				}
				
	}
