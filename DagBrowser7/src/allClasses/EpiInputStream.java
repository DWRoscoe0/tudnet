package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;

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
	
	  ?? Eventually this will be used with DataInputStream for reading
	  particular types from the stream, as follows:
	  * NetcasterInputStream extends InputStream.
	  * NetDataInputStream(NetcasterInputStream) extends DataInputStream(InputStream).
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
	
	  // Other instance variables.
		private E loadedKeyedPacketE= null;
		private DatagramPacket loadedDatagramPacket = null;
		private byte[] bufferBytes = null;
		private int packetSizeI = 0;
		private int packetIndexI = 0;
	  // Buffer is empty/consumed when packetIndexI <= packetSizeI.
		private boolean markedB= false;  
		private int markIndexI= -1; 
	
		public EpiInputStream ( // Constructor. 
			Q receiverToStreamcasterNotifyingQueueQ, 
			NamedLong packetCounterNamedLong
			)
			{
				this.receiverToStreamcasterNotifyingQueueQ= 
						receiverToStreamcasterNotifyingQueueQ;
				this.packetCounterNamedLong= packetCounterNamedLong;
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
	  
		public int available() throws IOException 
	    /* This method tests whether there are any bytes available for reading.
	      If there are bytes in the byte buffer it returns the number of bytes.
	      If not then it tries to load the byte buffer from 
	      the next packet in the queue and checks again.
	      If there are no bytes in the buffer and no more packets in the queue
	      then it returns 0.
	
	      This method may be used for asynchronous stream input, however
	      it should not be used to detect packet boundaries in the input,
	      because delays, such as single-stepping during debugging,
	      could make this unreliable.
	     */
	    {
	  		int availableI;
	  	  while (true) {
	    	  availableI= packetSizeI - packetIndexI; // Calculating bytes in buffer.
	    	  /* 
	    		if (AppLog.testingForPingB)
	  	  		appLogger.debug(
	  	  				"available() "+
	  	  				availableI+" "+packetSizeI+" "+packetIndexI+" "+
	  	  			  ( bufferBytes == null
	  	  			    ? ""
	  	  			    : new String(
	  	  			    		bufferBytes, packetIndexI, packetSizeI-packetIndexI
	  	  			    		)
	  	  			    )
	  	  				);
	  	  	*/
	    	  if ( availableI > 0) break; // Exiting if any bytes in buffer.
	    	  if  // Exiting with 0 if no packet in queue to load.
	    	    ( receiverToStreamcasterNotifyingQueueQ.peek() == null ) 
	    	  	break;
	        loadNextPacketV();
	  	  	}
		    return availableI;
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
		  		appLogger.debug("loadNextPacketV() executing.");
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
	  		//appLogger.debug( "NetcasterInputStream.mark(..), "+markIndexI+" "+packetIndexI);
	  		markIndexI= packetIndexI; // Recording present buffer byte index.
	  		markedB= true; // Recording that stream is marked.
	      }
	
	  public void reset() throws IOException 
	    // Restores the stream to the state recorded by mark(int).
	    {
	  		//appLogger.debug( "NetcasterInputStream.reset(..), "+markIndexI+" "+packetIndexI);
	    	if ( markedB ) // Un-marking if marked
	    		{
			      packetIndexI= markIndexI; // Restoring buffer byte index.
			    	markIndexI= -1; // Restoring undefined value.
			    	markedB= false; // Recording that stream is unmarked.
		    		}
	    	}
	
		}