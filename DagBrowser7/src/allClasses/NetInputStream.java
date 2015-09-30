package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import static allClasses.Globals.appLogger;

public class NetInputStream
	{

	  /* This class is a network output stream,
	    at first reading UDP packets from a receiveQueueOfSockPackets.
	    
	    Time-outs are not supported by the read methods in this class.
	    But time-outs at the packet level can be done in
	    the standard way with the thread's LockAndSignal instance,
	    which should be the same LockAndSignal in the receiveQueueOfSockPackets,
	    using Use InputStream.available() as the input availability test.
  
	    ??? When working, rename fields to things more meaningful.
	    
	    ?? Add close() which causes IOException which can signal termination.
	    */

	  // Constructor-injected instance variables.
		PacketQueue receiveQueueOfSockPackets= null;
		InetAddress iAdd = null;
	  int port = 0;

	  // Other instance variables.
    DatagramPacket dpack = null;
    byte[] ddata = null;
    int packSize = 0;
    int packIdx = 0;
    int markIndexI= -1; 
  
		public NetInputStream( 
			PacketQueue receiveQueueOfSockPackets, InetAddress address, int portI
			)
		{
			this.receiveQueueOfSockPackets= receiveQueueOfSockPackets;
			iAdd = address;
		  port = portI;		
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
	    	  try {
		          loadNextPacketV();
	          } catch (InterruptedException e) {
		          // TODO Auto-generated catch block
		          e.printStackTrace();
	          }
    	  	}
		    return availableI;
		    }

    public int read() throws IOException
      {
      	while  // Receiving and loading packets until bytes are in buffer.
      		(packIdx == packSize)
	        try {
		          loadNextPacketV();
	          } catch (InterruptedException e) {
		          // TODO Auto-generated catch block
		          e.printStackTrace();
	          }
			  int value = ddata[packIdx] & 0xff;
			  packIdx++;
			  return value;
			  }

    private void loadNextPacketV() throws IOException, InterruptedException 
      /* This method loads the buffer from the next packet.
        It blocks if no packet is immediately available.
        */
	    {
    		if // Adjusting mark for replacing buffer
    		  (markIndexI >= 0 ) // if stream is marked. 
    			markIndexI-= packIdx; // Subtracting present index or length.

	      SockPacket receivedSockPacket= // Getting next packet from queue.
	    	    receiveQueueOfSockPackets.take(); 

	      // Preparing to read new packet's data.
	  	  dpack= receivedSockPacket.getDatagramPacket();
	      ddata= dpack.getData();
	      packIdx= dpack.getOffset();
	      packSize= dpack.getLength();

	      appLogger.debug( 
    				"loadNextPacketV() \""
    				+PacketStuff.gettingPacketString( dpack )
    				+ "\"" );
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
    		appLogger.debug( "NetInputStream.mark(..), "+markIndexI+" "+packIdx);
    		markIndexI= packIdx;
        }

    public void reset() throws IOException 
	    {
    		appLogger.debug( "NetInputStream.reset(..), "+markIndexI+" "+packIdx);
	    	if ( markIndexI < 0 ) // Preventing reset if illegal.

	      packIdx= markIndexI; // Restore buffer index.
	    	markIndexI= -1; // Ending marked state.
	    	}

	}
