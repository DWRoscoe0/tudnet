package allClasses;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;

import static allClasses.AppLog.theAppLog;


public class EpiInputStream<
		K, // Key.
		E extends KeyedPacket<K>, // Packet. 
		Q extends NotifyingQueue<E>, // Packet queue.
		M extends PacketManager<K,E> // Packet manager.
		>
	
	extends InputStream

	/* This class is a network input stream.
	  It provides methods from InputStream to do normal stream operations,
	  but it also provides additional methods for dealing with 
	  the UDP (Datagram) packets from which the stream data comes.
	  It gets the packets from a NetcasterQueue.
	
	  The read methods in this class block if data is not available.
	  Time-outs are not supported by these read methods,
	  but time-outs at the packet level can be done in
	  the standard way with the thread's LockAndSignal instance,
	  which should be the same LockAndSignal as the one
	  in this class's NetcasterQueue.
	  Use InputStream.available() as the input availability test.
	
	  This code uses IOException and InterruptedException, but
	  exactly how they interact has not been completely determined ??
	
	  ///enh Eventually this will be used with DataInputStream for reading
	  particular types from the stream, as follows:
	  * NetcasterInputStream extends InputStream.
	  * NetDataInputStream(NetcasterInputStream) extends 
	    DataInputStream(InputStream).
		NetFilterInputStream is probably not needed, but could be added?
	  
	  ?? Add close() which causes IOException which can signal termination.
	  */
	
	{
	
	  // Constructor-injected instance variables.
		private final Q receiverToStreamcasterNotifyingQueueQ;
		private final NamedLong packetCounterNamedLong;
		  // This is the count of received packets. 
		  // The value is 1 during read of 1st packet data, assuming
		  // it is constructed with a value of 0.
		private final char delimiterChar;
		
	  // Other instance variables.
		// Whole packet buffer.
		private E loadedKeyedPacketE= null;
		private DatagramPacket loadedDatagramPacket = null;
		private byte[] bufferBytes = null;
    // Stream scan position: Buffer is empty/consumed when packetIndexI <= packetSizeI.
		private int packetSizeI = 0;
		private int packetIndexI = 0;
		// Data node parsing state.
    private SequenceEpiNode packetSequenceEpiNode= null;
    private int packetListIndexI= 0;
    // Old position marking variables.
    private boolean markedB= false;  
    private int markIndexI= -1; 

		public EpiInputStream ( // Constructor. 
				Q receiverToStreamcasterNotifyingQueueQ, 
				NamedLong packetCounterNamedLong,
				final char delimiterChar
				)
			{
				this.receiverToStreamcasterNotifyingQueueQ= 
						receiverToStreamcasterNotifyingQueueQ;
				this.packetCounterNamedLong= packetCounterNamedLong;
				this.delimiterChar= delimiterChar;
				}
	
		public NamedLong getCounterNamedLong()
		  // Returns an integer which counts packets processed.
		  { return packetCounterNamedLong; }
	
		public Q getNotifyingQueueQ()
		  // Returns the queue through which data is passing.
			{ return receiverToStreamcasterNotifyingQueueQ; }
	
		public LockAndSignal getLockAndSignal()
		  // Returns the LockAndSignal associated with the receive queue 
		  // through which data is passing, mainly for debugging.
			{ return receiverToStreamcasterNotifyingQueueQ.getLockAndSignal(); }

		

    protected boolean tryingToGetStringB( String theString ) throws IOException
      /* This method tries to get a particular String theString.
        It consumes the String and returns true 
        if the desired string is there, 
        otherwise it does not consume the String and returns false.
        The string is considered to be not there if either:
        * There are no characters available in the input stream buffer.
        * The characters available in the input stream buffer are
          not the desired string.
        */
      {
  			boolean gotStringB= false;
    		mark(0); // Marking stream position.
    		String inString= tryingToGetString();
		    //appLogger.debug( "tryingToGetStringB(): inString= "+ inString );
    	  gotStringB=  // Testing for desired string.
    	  		theString.equals( inString );
		    if ( ! gotStringB ) // Resetting position if String is not correct.
    	  	reset(); // Putting String back into stream.
    	  return gotStringB;
      	}

    protected String tryingToGetString() throws IOException
    /* This method tries to get any String.
      It returns a String if there is one available, null otherwise.
      */
    {
			String inString= null;
			if // Overriding if desired string is able to be read. 
			  ( available() > 0 )
				{
	    	  inString= readAString();
	    	  }
  	  return inString;
    	}

		protected int readANumberI()
  		throws IOException, BadReceivedDataException
  		/* This method reads and returns one int number 
  		  converted from a String ending in the delimiterC.
  		  This means it could not be used for floating point numbers.  
  		  It blocks if a full number is not available.
  		  It converts NumberFormatExceptions to a BadReceivedDataExceptions. 
  		  */
			{
				String numberString= readAString();
	      int numberI;
	      try { 
	      	numberI= Integer.parseInt( numberString ); 
	      	}
	      catch ( NumberFormatException theNumberFormatException ) {
	      	throw new BadReceivedDataException(theNumberFormatException);
	      	}
			  return numberI;
				}

    protected String readAString() throws IOException
      /* This method is a kludge.
        It reads and returns one String from either the old style strings-terminated-by-!, 
        or the next scalar string from a flow-style YAML sequence.
        The String returned does not include any delimiters.
        This method does not block.
        If a complete string, including terminating delimiter, is not available,
        then it logs this as an error and returns an empty string.
       */
      {
          String accumulatorString= "";
          int byteI;
        toReturn: { toNoData: {
          accumulatorString= tryViaYAMLSequenceString(); // Try string from sequence.
          if (accumulatorString != null) break toReturn; // Exiting if gotten.

          theAppLog.debug( "readAString(): trying old !-delimited parsing.");
          accumulatorString= ""; // Set accumulator to empty string.
          while (true) { // Skipping possible YAML lead-in characters.
            byteI= tryBufferByteI();
            if ( ! isLeadDelimiterB(byteI) ) break; // Exiting if not lead-in byte.
            // Ignore lead-in character.
            }
          while (true) { // Reading and accumulating string bytes until terminator.
            if ( isDelimiterB(byteI) ) break toReturn; // Exiting if terminator seen.
            accumulatorString+= (char)byteI; // Append string byte.
            if ( bufferByteCountI() <= 0 ) break toNoData;
            byteI= read();
            }
        } // toNoData: Being here means end of packet reached.
          accumulatorString+="!NO-DATA-AVAILABLE!";
          theAppLog.error( "readAString(): returning " + accumulatorString );
        } // toReturn:
          return accumulatorString;
        }

    private String tryViaYAMLSequenceString() throws IOException
      /* This method tries to get a String by parsing and caching YAMLSequences,
        then extracting Strings from them.
        If it succeeds it returns the next String.
        If it fails it returns null.
        */
      { 
        String elementString= null; // Set default result to indicate failure.
        while (true) { // Keep trying until no more sequence elements to return.
          if (packetSequenceEpiNode == null) { // Handle missing sequence if needed.
            packetSequenceEpiNode=  // Try parsing sequence.
                SequenceEpiNode.trySequenceEpiNode(this); // Try parsing sequence.
            if (packetSequenceEpiNode == null) break; // No sequence, so exit with fail.
            packetListIndexI= 0; // Reset index for scanning sequence elements.
            }
          if (packetListIndexI < packetSequenceEpiNode.sizeI()) {
            ScalarEpiNode theScalarEpiNode=
                packetSequenceEpiNode.getScalarEpiNode(packetListIndexI); 
            elementString= theScalarEpiNode.getString();
            packetListIndexI++;
            break;  // Exit with success.
            }
          packetSequenceEpiNode= null; // Reset to try for another sequence.
          } // while
        return elementString;
        }
    
    // Parsers of YAML-like language.

    @SuppressWarnings("unused") ///
    private String remainingBufferString() throws IOException
      /* This method returns all available stream bytes in the packet stream buffer.
        */
      {
        int byteI;
        String accumulatorString= ""; // Clear character accumulator.
        while (true) {
            if (bufferByteCountI() <= 0) break;
            byteI= read();
            accumulatorString+= (char)byteI;
            }
        return " Remaining bytes:"+accumulatorString; 
        }

    public boolean tryByteB(int desiredByteI) throws IOException
      /* Tries to read desiredByteI from the stream.
        This is like getByteB(..) except that the stream position
        is not changed if desiredByteI is not read from the stream.
        */
      {
        int positionI= getPositionI(); // Save stream position.
        boolean successB= getByteB(desiredByteI); // Read and check byte.
        if ( ! successB )
          setPositionV(positionI); // Restore stream position if failure.
        return successB;
        }

    public boolean getByteB(int desiredByteI) throws IOException
      /* Reads a byte from the input stream and compares it to desiredByteI.
        If they are equal it returns true, otherwise false.
        Failure can happen when either the byte read is not the desired byte or
        if there is no byte available.
        */
      {
        int byteI= tryBufferByteI();
        boolean successB= // Check byte.   
            (byteI == desiredByteI); // Fails if -1 or incorrect byte.
        return successB;
        }

    private int tryBufferByteI() throws IOException
      /* Returns the next stream byte if available in the packet buffer, -1 otherwise. 
        It will not attempt to load the next packet.
        */
      {
        int byteI;
        if ( bufferByteCountI() > 0 ) // If byte available 
          byteI= read(); // read the byte
          else
          byteI= -1; // otherwise return -1.
        return byteI;
        }
    
		public boolean isLeadDelimiterB(int byteI)
      /* This method tests byteI for YAML lead-in delimiters.
        */
      {
        boolean delimiterB= true;
        process: {
          if ( delimiterChar==byteI ) break process;
          if ( '['==byteI ) break process;
          delimiterB= false;
          } // process:
        return delimiterB;
        }

		public boolean isDelimiterB(int byteI)
      /* This method tests byteI for either the original delimiterChar (!)
        or several YAML delimiters.
        */
      {
        boolean delimiterB= true;
        process: {
          if ( delimiterChar==byteI ) break process;
          if ( ','==byteI ) break process; // separator.
          if ( ']'==byteI ) break process; // terminator.
          if ( '}'==byteI ) break process; // terminator.
          delimiterB= false;
          } // process:
        return delimiterB;
        }
    
    public int available() throws IOException 
	    /* This method tests whether there are any bytes available for reading.
	      If there are bytes in the byte buffer it returns the number of bytes.
	      If not then it tries to load the byte buffer from 
	      the next packet in the queue and checks again.
	      If there are no bytes in the buffer and no more packets in the queue
	      then it returns 0.
	
	      This method may be used for asynchronous stream input, however
	      it should not be used to detect packet boundaries in the input,
	      For that, use bufferByteCountI().
	      it returns the number of bytes remaining in the packet buffer.  
       */
	    {
	  		int availableI;
	  	  while (true) {
	  	    availableI= bufferByteCountI();
	    	  if ( availableI > 0) break; // Exiting if any bytes in buffer.
	    	  if  // Exiting with 0 if no packet in queue to load.
	    	    ( receiverToStreamcasterNotifyingQueueQ.peek() == null ) 
	    	  	break;
	        loadNextPacketV();
	  	  	}
		    return availableI;
		    }
	
    protected int bufferByteCountI()
      /* Returns the number of bytes remaining in the packet buffer.  
       This should be used instead of the method available() when
       packet boundaries are significant, 
       which with unreliable UDP is most of the time.
       Unlike the method available(),
       this method will not load the buffer with the next packet
       if the end of buffer is reached.
       */
      { 
        return packetSizeI - packetIndexI;
        }
    
	  public int read() throws IOException
	    /* Reads one byte, reading to read one or more packets 
	      from the input queue if needed, blocking to wait for a packet if needed.
	      */
	    {
	    	while  // Receiving and loading packets until bytes are in buffer.
	    		(packetIndexI == packetSizeI)
	        loadNextPacketV();
			  int value = bufferBytes[packetIndexI] & 0xff;
			  packetIndexI++;
			  return value;
			  }
	  
	  @SuppressWarnings("unused") ///
    private boolean tryReadB(byte[] bufferBytes, int offsetI, int lengthI)
	  		throws IOException
	    /* This method tries to read lengthI bytes into buffer bufferBytes
	  	  starting at offset offsetI.
	  	  Returns true if the read is successful.
	  	  Return false if there were not enough bytes to fill the request,
	  	    and the stream is reset to before the read.
	  	  */
		  {
		  	mark(0); // Mark in case we need to undo a bad read.
		    int bytesReadI= read( bufferBytes, offsetI, lengthI );
		    boolean successB= // Sufficient bytes read means a success. 
		    		( bytesReadI >= lengthI );
		    if ( ! successB ) reset(); // Undo read if not successful.
		    return successB;
		    }

	  public void emptyingBufferV()
		  {
	  		packetIndexI= packetSizeI; // Making all bytes appear consumed.
	  		}
	  
	  public E getKeyedPacketE() throws IOException
	    /* Returns the current KeyedPacket associated with this stream.
	      Initially it returns null.
	      After data has been read from the stream
	      it returns a reference to the packet that
	      was most recently loaded.
	      The packet and its bytes can still be read.
	      
	      This is not the same as reading a packet as any other data type,
	      which is done by readKeyedPacketE().
	      In that case the packet and its data are no longer available
	      for reading, which makes available()==0.
				*/
	    {
	  	  return loadedKeyedPacketE;
	    	}
	
	  public E readKeyedPacketE() throws IOException
	    /* This reads a packet if one is available.  
	      If one isn't then it blocks until one is loaded.
	      It also prevents the packet or any of its bytes 
	      being gotten or read later.
	      
	      This is not presently used.
	      */
	    {
		  	if // Loading a packet if none loaded.
		  		(loadedKeyedPacketE == null)
		      loadNextPacketV();
	  		E returnKeyedPacketE= loadedKeyedPacketE; // Save reference to packet.
	  		packetIndexI= packetSizeI; // Preventing any byte reads from packet.
	  		loadedKeyedPacketE= null; // Preventing packet rereads.
	  		return returnKeyedPacketE;
	    	}
	
	  private void loadNextPacketV() throws IOException 
	    /* This method loads the packet buffers from the 
	      next packet in the input queue.
	      It blocks if no packet is immediately available.
	      This method changes virtually all objects at once.
	      This includes the packet counter.
	      */
	    {
	  		if (AppLog.testingForPingB)
		  		theAppLog.info("loadNextPacketV() executing.");
	  		if // Adjusting saved mark index for buffer replacement. 
	  		  (markedB) // if stream is marked. 
	  			markIndexI-= packetIndexI; // Subtracting present index or length ??
	
	      try {
	      	loadedKeyedPacketE= receiverToStreamcasterNotifyingQueueQ.take();
	        } catch (InterruptedException e) { // Converting interrupt to error. 
	        	throw new IOException(); 
		      } 
	
				packetCounterNamedLong.addDeltaL( 1 ); // Counting received packet.
	
	      // Setting variables for reading from the new packet.
	  	  loadedDatagramPacket= loadedKeyedPacketE.getDatagramPacket();
	      bufferBytes= loadedDatagramPacket.getData();
	      packetIndexI= loadedDatagramPacket.getOffset();
	      packetSizeI= loadedDatagramPacket.getLength();
		    }

    public void bufferLoggerV(String messageString, int positionI)
    /* This method log the state of the buffer as a debug message.
      It is meant for use during debugging.
      * messageString is a message to be included in the log entry.
      * positionI is the present position of input from the buffer.
        This should be <= the buffer scan index.
        Use 0 if unsure. 
      */
    {
      theAppLog.debug(
          messageString
          + "; buffer="
          + new String(bufferBytes,0,positionI)
          + "-^-"
          + new String(bufferBytes,positionI,packetIndexI-positionI)
          + "-^-"
          + new String(bufferBytes,packetIndexI,packetSizeI-packetIndexI)
          );
      }
      

	  public boolean markSupported()
	    /* This method reports that mark(..) and reset() are supported.
	      However it works only within individual packet buffers.
	     */
	    {
		    return true;
		    }
    
    public void mark(int readlimit) 
      /* Saves the state of the stream so that it might be restored
        with the reset() method.  This allows undoing reads after
        it has been concluded that the bytes read since mark(..)
        are not what we were looking for.
        This allows only one level of undoing, but this often suffices.
        */
      {
        //appLogger.info( "NetcasterInputStream.mark(..), "+markIndexI+" "+packetIndexI);
        markIndexI= getPositionI(); // Recording present buffer byte index.
        markedB= true; // Recording that stream is marked.
        }
  
    public void reset() throws IOException 
      // Restores the stream to the state recorded by mark(int).
      {
        //appLogger.info( "NetcasterInputStream.reset(..), "+markIndexI+" "+packetIndexI);
        if ( markedB ) // Un-marking if marked
          {
            setPositionV(markIndexI); // Restoring buffer byte index.
            markIndexI= -1; // Restoring undefined value.
            markedB= false; // Recording that stream is unmarked.
            }
        }
    
    public int getPositionI() 
      /* Gets the position in the stream buffer 
        so that it might be restored later with the setPositionV(int) method later.  
        This allows undoing nested reads of the stream.
        It is more general than the not nest-able mark(int) method.
        
        ///enh This method could be replaced by getStreamState(StreamState),
          and setPositionV(int0 could be similarly replaced,
          to make stream state saving more general, at least temporarily,
          while strings are being gotten from YAML-like sequences.
          StreamState would store additional information, such as
          list of sequence elements and an index to the next one.
        */
      {
        return packetIndexI; // Recording present buffer byte index.
        }
  
    public void setPositionV(int oldPositionI) throws IOException 
      /* Restores the stream to the state previously gotten by getPositionI().
        It is more general than the not nest-able reset() method.
        */ 
      {
        packetIndexI= oldPositionI; // Restoring buffer byte index.
        }
	
		}
