package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;

import static allClasses.AppLog.theAppLog;


public class EpiInputStream<
		K, // Key.
		E extends KeyedPacket<K>, // Packet. 
		Q extends NotifyingQueue<E>, // Packet queue.
		M extends PacketManager<K,E> // Packet manager.
		>
	
	extends RandomAccessInputStream

	/* This class is a network input stream.
	  It provides methods from InputStream to do normal stream operations,
	  but it also provides additional methods for dealing with 
	  the UDP (Datagram) packets from which the stream data comes.
	  It gets the packets from a NetcasterQueue.
	
	  The fact that data comes from UDP Datagram packets has the following consequences.
  	* The InputStream.read(..) methods in this class do not block.
  	  If no more bytes are available, the end-of-stream indicator -1 is returned.
  	* Each packet is treated as a self-contained sub-stream when loaded into a buffer.
  	  Each one produces its own end-of-stream.
  	* The InputStream.available() method will clear the end-of-stream condition
  	  if called when no more buffered bytes are available,
  	  but at least one received packet is in the packet queue.    
  	* Time-outs are not supported by these read methods,
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
	  
	  ///enh ?? Add close() which causes IOException which can signal termination.
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
    // Stream scan position: Buffer is empty/consumed when packetIndexI >= packetSizeI.
		private int packetSizeI = 0;
		private int bufferIndexI = 0;
		// Position of parsed EpiNode data gotten from packet.
    private MapEpiNode packetMapEpiNode= null; // Cached MapEpiNode parsed from packet.
    private int packetElementIndexI= 0; // Index of element within EpiNode.
    // Legacy position marking variables.
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
        
        ///opt  Remove old !-delimited parsing.
       */
      {
          String accumulatorString= "";
          //// int byteI;
        toReturn: { //// toNoData: {
          accumulatorString= tryFromEpiNodeString(); // Try string from sequence.
          if (accumulatorString != null) break toReturn; // Exiting if gotten.

          theAppLog.error( "readAString(): unable to get string from EpiNode.");
          /*  ////
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
          */  ////
        //// } // toNoData: Being here means end of packet reached.
          accumulatorString+="!NO-DATA-AVAILABLE!";
          // theAppLog.error( // Log this way to debug.
          theAppLog.warning( // Log this way normally.  
              "readAString(): returning " + accumulatorString );
          //// theAppLog.reallyDoStackTraceV(null);
        } // toReturn:
          return accumulatorString;
        }
    
    // Parsers of YAML-like language. //// temporary

    private String tryFromEpiNodeString() throws IOException
      /* This method tries to get a String by parsing and caching EpiNodes,
        then extracting Strings from them.
        If it succeeds it returns the next String.
        If it fails it returns null.
        */
      { 
        String elementString= null; // Set default result to indicate failure.
        while (true) { // Keep trying until no more EpiNode elements to return.
          if (packetMapEpiNode == null) { // Handle missing EpiNode if needed.
            //// packetEpiNode= EpiNode.tryEpiNode(this); // Try parsing node.
            packetMapEpiNode= tryMapEpiNode(); // Try parsing node and caching it.
            if (packetMapEpiNode == null) break; // No node, so exit with fail.
            packetElementIndexI= 0; // Reset index for scanning node elements.
            }
          elementString= packetMapEpiNode.extractFromEpiNodeString(packetElementIndexI);
          if (elementString != null) { // If got string, return it. 
            packetElementIndexI++; // Increment index for next string.
            break; // Exit with success.
            }
          packetMapEpiNode= null; // Reset to try for another node.
          } // while
        return elementString;
        }

    public MapEpiNode tryMapEpiNode() throws IOException
      /* This method tries to get an EpiNode from this InputStream.
        If it succeeds it returns the next EpiNode.
        If it fails it returns null.
        */
      { 
        MapEpiNode resultMapEpiNode= null; // Set default result to indicate failure.
        if (packetMapEpiNode == null) // If node not ready
          packetMapEpiNode= MapEpiNode.tryMapEpiNode(this); // try parsing one.
        if (packetMapEpiNode != null) { // If we have a node now
          resultMapEpiNode= packetMapEpiNode; // set it for return as result.
          packetMapEpiNode= null; // Reset since we're taking node away.
          }
        return resultMapEpiNode;
        }

    
    @SuppressWarnings("unused") ///
    private String remainingBufferString() throws IOException
      /* This method returns a String containing 
        all stream bytes remaining in the packet stream buffer.
        The stream position is moved past all those bytes.
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

    public int tryBufferByteI() throws IOException
      /* Returns the next stream byte if available in the packet buffer, -1 otherwise. 
        It will not attempt to load the next packet.
        */
      {
        int byteI;
        if ( bufferByteCountI() > 0 ) // If byte available 
          byteI= read(); // read the byte
          else
          byteI= -1; // otherwise set return value of -1.
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
	      If there are bytes in the byte buffer then it returns the number of bytes.
	      If not then it tries to load the byte buffer from 
	      the next packet in the queue and checks again.
	      If there are no bytes in the buffer and no more packets in the queue
	      then it returns 0.
	
	      This method may be used for asynchronous stream input, however
	      it should not be used to detect packet boundaries in the input,
	      For that, use bufferByteCountI(), which returns 
	      the number of bytes remaining in the packet buffer.  
       */
	    {
	  		int availableI;
	  	  while (true) { //While no bytes in buffer but packets in queue, keep loading them.
	  	    availableI= bufferByteCountI();
	    	  if ( availableI > 0) break; // Exiting if any bytes in buffer.
	    	  if  // Exiting with 0 if no packet in queue to load.
	    	    ( receiverToStreamcasterNotifyingQueueQ.peek() == null ) 
	    	  	break;
	        loadNextPacketV();
	  	  	}
		    return availableI;
		    }
    
	  public int read() throws IOException
	    /* This method returns one byte from the byte buffer, 
	      or the end-of-stream value -1 if the buffer is exhausted.  
	      So this method never blocks.

	      Each UDP packet is considered to be one complete stream.
	      This is because, since UDP is an unreliable protocol,
	      each UDP packet should contain one or more complete pieces of data.
	      No piece of data may span multiple packets.
        The end-of-stream condition can be cleared either
        * by loading a new packet containing at least one byte,
          which can be triggered by calling the method available(), or
        * by calling setPosition(int) to move the buffer pointer 
          back from the end of the buffer.

	      */
	    {
	      int resultByteI;
        if (bufferByteCountI() <= 0) // No bytes remaining in buffer.
            resultByteI= -1; // Return end-of-file indication.
          else // At least one byte remains. 
          { // Return first byte and update pointer.
            resultByteI= bufferBytes[bufferIndexI] & 0xff;
            bufferIndexI++;
            }
			  return resultByteI;
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
        return packetSizeI - bufferIndexI;
        }
	  
	  @SuppressWarnings("unused") ///opt
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
	  
	  public E getKeyedPacketE() throws IOException
	    /* Returns the current KeyedPacket associated with this stream.
	      Initially it returns null.
	      After data has been read from the stream
	      it returns a reference to the packet that
	      was most recently loaded.
	      The packet and its bytes can still be read.
	      
	      This is not the same as reading a packet as any other data type,
	      which can be done by readKeyedPacketE().
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
	      
	      ///opt This is not presently used.
	      */
	    {
		  	if // Loading a packet if none loaded.
		  		(loadedKeyedPacketE == null)
		      loadNextPacketV();
	  		E returnKeyedPacketE= loadedKeyedPacketE; // Save reference to packet.
        emptyingBufferV(); // Preventing any byte reads from packet.
	  		loadedKeyedPacketE= null; // Preventing packet rereads.
	  		return returnKeyedPacketE;
	    	}

    public void emptyingBufferV()
      {
        bufferIndexI= packetSizeI; // Making all bytes appear consumed.
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
	  			markIndexI-= bufferIndexI; // Subtracting present index or length ??
	
	      try {
	      	loadedKeyedPacketE= receiverToStreamcasterNotifyingQueueQ.take();
	        } catch (InterruptedException e) { // Converting interrupt to error. 
	        	throw new IOException(); 
		      } 
	
				packetCounterNamedLong.addDeltaL( 1 ); // Counting received packet.
	
	      // Setting variables for reading from the new packet.
	  	  loadedDatagramPacket= loadedKeyedPacketE.getDatagramPacket();
	      bufferBytes= loadedDatagramPacket.getData();
	      bufferIndexI= loadedDatagramPacket.getOffset();
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
          + new String(bufferBytes,positionI,bufferIndexI-positionI)
          + "-^-"
          + new String(bufferBytes,bufferIndexI,packetSizeI-bufferIndexI)
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
        return bufferIndexI; // Recording present buffer byte index.
        }
  
    public void setPositionV(int thePositionI) throws IOException 
      /* Restores the stream to the state previously gotten by getPositionI().
        It is more general than the not nest-able reset() method.
        */ 
      {
        bufferIndexI= thePositionI; // Restoring buffer byte index.
        
        packetMapEpiNode= null; //// Kludge: reset EpiNode parser.
        packetElementIndexI= 0; //// Kludge: reset EpiNode parser.
        }
	
		}
