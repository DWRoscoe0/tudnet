package allClasses;

import static allClasses.AppLog.theAppLog;

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

  /* This class is a network output stream which generates UDP packets 
    from a stream bytes and other dataand markers.

    Note, because DDP is an unreliable protocol,
    care should be taken with regard to data which spans packet boundaries.
    Data should not span packet boundaries.  No block should be longer than a packet.
    A packet may contain multiple blocks if they fit,
    but if a given block will not fit in the remaining space,
    the stream should be flushed, and that block should be carried by the next packet. 
    
    Packets produced by this class aren't necessarily sent directly to a network.
    If used by a Unicaster then they are.
    If used by a Subcaster then they are queued for multiplexing to other OutputSteams.

    ///fix Synchronize methods to make thread-safe because of use of TimerTask.
      
    ///? Note, this class contains code which was added to manage
      indivisible blocks of bytes for output and delayed stream flushing,
      but this code has not been tested and probably contains bugs!
      It you see some code that doesn't make sense, that might be why.
      A good example of this is any code involving a Timer.
	      
	    ///enh markIndivisibleBlock().
	    Add this method which records the buffer position as the end of the block.
	    If the buffer becomes full, only the bytes up to 
	    the last block mark will be sent in a packet.
	    The remaining bytes will be copied to the front of the buffer
	    and become the beginning of the next packet. 
	
	    ///enh delayedFlush(long latestMsL).
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
	
    ///? Eventually this will be used with DataOutputStream for writing
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
		private Timer theTimer;
		@SuppressWarnings("unused") ///opt
    private char delimiterChar;
		
  	private TimerTask theTimerTask= null;
  	private long nextPacketSendTimeMsL; // Time at which next packet send will happen.

  	// Stream state.
		private byte[] bufferBytes= new byte[0]; // Initial 0-length throw-away buffer.
    private int bufferSizeI= 0; // Cached buffer size. 
    // 0 forces initial flush() to allocate buffer.
		private int indexI= 0; // 0 prevents sending packet during initial flush().
		private int sendableI= 0; // End of data when packet is sent.
    private boolean writingBlockB= false; // true when some but not all bytes
      // of a data node have been written.

		EpiOutputStream(  // Constructor.
				Q notifyingQueueQ,
				M packetManagerM,
				NamedLong packetCounterNamedLong,
	  		Timer unusedTimer, ///opt remove?
	  		char delimiterChar
				)
			{
				this.notifyingQueueQ= notifyingQueueQ;
				this.packetManagerM= packetManagerM;
				this.packetCounterNamedLong= packetCounterNamedLong;
				this.delimiterChar= delimiterChar;
				}

		public NamedLong getCounterNamedLong() 
		  { return packetCounterNamedLong; }

		public M getPacketManagerM()
		  { return packetManagerM; }


    protected void writeAndSendInBlockV( String theString ) throws IOException
      /* This method writes theString to the stream and then 
        finishes its data block and sends to the stream in a packet 
        that block and any blocks that has been written previously .
        */
      {
    		writeInBlockV( theString );
    		endBlockAndSendPacketV();
        }

    protected void writeInBlockV( long theL ) throws IOException
      /* This method writes theL long int 
        followed by the delimiterC to the stream,  
        but it doesn't force a flush().
        */
      { 
    		writeInBlockV( theL + "" ); // Converting to String.
        }

    public void writeInBlockV( String theString ) 
        throws IOException
      /* This method writes to the buffer, theString, 
        preceded by the appropriate block delimiters.
        */
      { 
        beginOrContinueBlockV();
        writeV( theString );
        }
    
    public void beginOrContinueBlockV() throws IOException
      /* This method writes to the buffer the appropriate block delimiters
        in preparation for writing a string.
        */
      { 
        if (! writingBlockB) // If no partial block has been written yet,
          {
            writeV( "[" ); // write block sequence introducer
            writingBlockB= true; // and record that the block is now underway.
            }
          else
          writeV( "," ); // write block sequence element separator.
        }

    public void endBlockAndSendPacketV() throws IOException
      /* This method writes any bytes needed to finish the present data node, 
        if any, then it schedules the packet for immediate sending.
        */
      { 
        endBlockV();
        sendNowV();
        }

    public void endBlockV() throws IOException
      /* This method writes any bytes needed to finish the present block, if any,
        and marks all bytes written to that point as send-able.
        At that point the packet may be sent, or additional bytes may be written.
        */
      { 
        if (writingBlockB) // If block has been started
          {
            writeV( "]" ); // write block terminator
            writingBlockB= false; // and record that block is no longer active.
            }
        makeAllBufferBytesSendableV();
        }

    public void writeV( String theString ) throws IOException
      /* This method writes theString to the stream buffer.
        */
      {
    		byte[] buf = theString.getBytes(); // Getting byte buffer from String.
        write(buf); // Writing it to stream buffer.
        }

		public void write(int value) throws IOException
		  /* This writes one byte to the stream.
		    This means writing to the buffer if it is not full.
		    If the buffer is full then any send-able bytes 
		    will be written first to make room.
		   */
			{
				if (indexI >= bufferSizeI) // I there is no more room in buffer 
	        queueSendableBytesV(); // make room by sending all send-able block bytes.
				bufferBytes[indexI]= (byte) (value & 0x0ff); // Storing byte in buffer.
				indexI++; // Advancing buffer index.
				}

	  @SuppressWarnings("unused")
    private void writeV( E theKeyedPacketE )
		  throws IOException
	    /* This method writes a packet theKeyedPacketE to the stream.
	      First it queues a packet containing any bytes in the present buffer.
	      Then it queues theKeyedPacketE.
	      */
	    {
	  		queueSendableBytesV(); // Queuing packet with buffer data if any.  ///? needed?
	  	  queuingForSendV( theKeyedPacketE ); // Queuing new data argument packet.
	    	}

    public void flush() throws IOException
      /* This method attaches the buffer to a UDP packet and sends the packet.
        Because UDP is unreliable, this method should be called only when
        the buffer contains one or more complete self-contained blocks of data.
        It should not be called because an attempt is made to write to a full buffer
        unless blocks are actually bytes.
        */
      { 
        makeAllBufferBytesSendableV();
        sendNowV();
        } 

    private void makeAllBufferBytesSendableV()
      { 
        sendableI= indexI;
        } 

    public void sendNowV() throws IOException
      /* This method schedules a packet to be sent immediately
        with all send-able bytes, if any.
        If no bytes in the buffer are send-able, no bytes will be sent.
        Note, this is different from flush(), 
        which leaves no buffer bytes unwritten.
        */
      { 
        scheduleSendB(0); // Schedule packet to be send with zero delay.
        }

    public synchronized boolean scheduleSendB( long latestMsL ) throws IOException
    /* This method schedules the sending of all send-able bytes in the buffer
      at latestMsL ms in the future or earlier.  
      If latestMsL is 0 then sending happens immediately. 
      It returns true if the send happened immediately, 
      false if it will happen in the future.

    	The actual time used for sending will be the earlier of 
    	latestMsL or the remaining time on the send TimerTask if it exists.
      The send TimerTask is cancelled, recreated, and set to latestMsL if 
      latestMsL represents an earlier time.

      The purpose of this method is to allow multiple blocks of data that
      do not need to be sent immediately to be concatenated in the byte buffer,
      and sent together, thereby reducing the number of packets that need to be sent.
      The packet is not sent until the earliest latestMsL times 
      of all the blocks that the packet contains.
     	*/
    {
        boolean sendNowB= ( latestMsL == 0 );
      	TimerTask newTimerTask= null;
    	// Defining break target labels.
    	toReturn: { 
    	toCancelAndReplaceTimerTask: {
    	toCancelReplaceAndCreateNewTimerTask: {
    	  if ( sendNowB ) { // Queue send now if zero delay requested.
      		queueSendableBytesV();
	        break toCancelAndReplaceTimerTask;
    	    }
    	  if (theTimerTask == null) // Go create new TimerTask if none exists. 
    	  	break toCancelReplaceAndCreateNewTimerTask;
    	  { // Exiting if the existing TimerTask will do the job.
    	  	long newSendTimeMsL= System.currentTimeMillis() + latestMsL;
    	  	if ( newSendTimeMsL >= nextPacketSendTimeMsL )
    	  		break toReturn; // Exiting because existing TimerTask is good enough.
    	    }
    	} // toCancelReplaceAndCreateNewTimerTask:
    		newTimerTask= new TimerTask() { // Creating new TimerTask.
	        public void run() { queueSendableBytesV(); }
	    		};
	    	nextPacketSendTimeMsL= System.currentTimeMillis() + latestMsL; // Save send time.
        theTimer.schedule(newTimerTask, latestMsL); // Scheduling send.
    	} // toCancelAndReplaceTimerTask: 
	  		if (theTimerTask != null) { // Canceling old TimerTask if it exists.
	  				theTimerTask.cancel();
			    	theTimerTask= null; // Recording no longer scheduled.
	    			}
	  		if (newTimerTask != null) { // Recording new TimerTask, if any.
	  			  theTimerTask= newTimerTask;
	  			  newTimerTask= null;
	    	  	}
     	} // toReturn: 
	      return sendNowB;
    	}

	  private void queueSendableBytesV()
	    /* Queues for sending a packet containing all send-able buffer bytes, 
	      if there are any.  If there are none then no packet will be queued.
	      In either case this method allocates a new byte buffer,
	      and copies any bytes that are not send-able,, 
	      from the old buffer to the new buffer, 
	      to be sent the next time this method is called. 
	      */
	  	{
	      byte[] newBufferBytes= // Always allocate a new byte buffer.
          packetManagerM.produceDefaultSizeBufferBytes();
        int bytesToSendI= sendableI; 
        if (bytesToSendI <= 0) // There are no bytes to send?
          { // Report the error and clear the buffer.
            theAppLog.error("EpiOutputStream.queueSendableBytesV(): no sendable bytes.");
            ///enh Include bytes discarded in message.
            indexI= 0; // Discard all bytes stored so far.
            }
          else // There are send-able bytes.  Send them in a packet. 
          { // Send packet containing at least one send-able byte. 
    	  		E keyedPacketE= packetManagerM.produceKeyedPacketE( // Create packet
            		bufferBytes, indexI // using old buffer containing send-able bytes.
    			  		);
    	  		for // Copy bytes which will not be sent now to beginning of new buffer.
    			  	( int dstI=0, srcI= bytesToSendI; srcI < indexI ; )
    			  	newBufferBytes[dstI++]= bufferBytes[srcI++]; ///opt
    	  		queuingForSendV( keyedPacketE ); // Queue packet with old buffer.
            }
	  		bufferBytes= newBufferBytes; // Start using new buffer.
	    	bufferSizeI= bufferBytes.length; // Cache the buffer length. 
        indexI-= bytesToSendI; // Subtracting bytes sent from buffer index.
        sendableI= 0;  // Reset count of send-able bytes.
		    }
	    

	  private void queuingForSendV( E theKeyedPacketE )
		  // This method queues a packet and counts it.
			{
	  		notifyingQueueQ.put( theKeyedPacketE ); // Adding packet to queue.
				packetCounterNamedLong.addDeltaL( 1 ); // Counting the packet.
				}
				
	}
