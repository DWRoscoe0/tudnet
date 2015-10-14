package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
//import static allClasses.Globals.appLogger;


public class NetInputStream
	{

	  /* This class is a network input stream.
	    It provides methods from OutputStream to do normal stream operation,
	    but it also provides additional methods for dealing with 
	    the UDP (Datagram) packets from which the stream data comes.
	    It gets the packets from a receiveQueueOfSockPackets.

	    The read methods in this class block if data is not available.
	    Time-outs are not supported by these read methods,
	    but time-outs at the packet level can be done in
	    the standard way with the thread's LockAndSignal instance,
	    which should be the same LockAndSignal as the one
	    in this classes receiveQueueOfSockPackets.
	    Use InputStream.available() as the input availability test.

      This code uses IOException and InterruptedException, but
      exactly how has not been completely determined ??
      
	    ?? When working, rename fields to things more meaningful.

      ?? Eventually this will be used with DataInputStream for reading
        particular types from the stream.
        
	    . Maybe give it ability to read packets and detect packet boundaries.
	    
	    ?? Add close() which causes IOException which can signal termination.
	    */

	  // Constructor-injected instance variables.
		PacketQueue receiveQueueOfSockPackets= null;

	  // Other instance variables.
		SockPacket theSockPacket= null;
    DatagramPacket theDatagramPacket = null;
    byte[] ddata = null;
    int packSize = 0;
    int packIdx = 0;
    boolean markedB= false;  
    int markIndexI= -1; 
  
		public NetInputStream( 
			PacketQueue receiveQueueOfSockPackets, 
			InetAddress address, int portI  // Remove unneeded arguments??
			)
		{
			this.receiveQueueOfSockPackets= receiveQueueOfSockPackets;
	    }

    public int available() throws IOException 
      /* This method tests whether there are any bytes available for reading.
        If there are bytes in the byte buffer it returns true.
        If not then it tries to load the byte buffer from packets in the queue.
        If there are no bytes and no non-empty packets in the queue
        then it returns false.
        
        This method may be used for asynchronous stream input, however
        it should not be used to detect/parse packet boundaries in the input,
        because delays, such as single-stepping during debugging,
        could affect program flow.
       */
      {
    		int availableI;
    	  while (true) {
      	  availableI= packSize - packIdx; // Calculating bytes in buffer.
	    	  if ( availableI > 0) break; // Exiting if any bytes in buffer.
	    	  if  // Exiting if no packet in queue to load.
	    	    ( receiveQueueOfSockPackets.peek() == null ) 
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
      		(packIdx == packSize)
	        loadNextPacketV();
			  int value = ddata[packIdx] & 0xff;
			  packIdx++;
			  return value;
			  }

    public SockPacket getSockPacket() throws IOException
      /* Returns the current SockPacket associated with this stream.
        Initially it return null.
        After data has been read from the stream
        it returns a reference to the packet from which 
        the most recent data was gotten.
       */
      {
    	  return theSockPacket;
      	}

    private void loadNextPacketV() throws IOException 
      /* This method loads the packet buffers from the 
        next packet in the input queue.
        It blocks if no packet is immediately available.
        This method changes virtually all object at once.
        */
	    {
    		if // Adjusting saved mark index for buffer replacement. 
    		  (markedB) // if stream is marked. 
    			markIndexI-= packIdx; // Subtracting present index or length ??

        try {
        	theSockPacket= receiveQueueOfSockPackets.take();
	        } catch (InterruptedException e) {
	        	throw new IOException(); 
		      } 

	      // Setting variables from the new packet.
	  	  theDatagramPacket= theSockPacket.getDatagramPacket();
	      ddata= theDatagramPacket.getData();
	      packIdx= theDatagramPacket.getOffset();
	      packSize= theDatagramPacket.getLength();
		    }

    public boolean markSupported()
      /* This method reports that mark(..) and reset() are supported.
        However it works only within individual packet buffers.
       */
	    {
		    return true;
		    }
    
    public void mark(int readlimit) 
    	{
    		//appLogger.debug( "NetInputStream.mark(..), "+markIndexI+" "+packIdx);
    		markIndexI= packIdx; // Recording present buffer byte index.
    		markedB= true; // Record that stream is marked.
        }

    public void reset() throws IOException 
	    {
    		//appLogger.debug( "NetInputStream.reset(..), "+markIndexI+" "+packIdx);
	    	if ( markedB ) // Un-marking if marked
	    		{
			      packIdx= markIndexI; // Restoring buffer byte index.
			    	markIndexI= -1; // Restoring undefined value.
			    	markedB= false; // Ending marked state.
		    		}
	    	}

	}
