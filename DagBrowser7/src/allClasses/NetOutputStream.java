package allClasses;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class NetOutputStream 

	extends OutputStream

  /* This class is a network output stream, at first generating UDP packets.
    
    ??? When working, rename fields to things more meaningful.

    ?? Eventually this will be used with DataOutputStream for writing
      particular types to the stream.
    	?? Maybe give it ability to write packets and control packet boundaries.
    		This is addition to flush() which forces an unconditional end of packet.
	  
	  */

	{

		public static final int DEFAULT_BUFFER_SIZE = 1024;
	  
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		PacketQueue sendQueueOfSockPackets;
		InetAddress iAdd = null;
    int port = 0;
  
		int idx = 0; // buffer index; points to next empty buffer byte
		DatagramPacket dpack = null;
    
		NetOutputStream(
				PacketQueue sendQueueOfSockPackets, InetAddress address, int portI
				)
			{
				this.sendQueueOfSockPackets= sendQueueOfSockPackets;
				iAdd = address;
			  port = portI;		
        }
		
		public void write(int value) throws IOException {
			buffer[idx] = (byte) (value & 0x0ff);
			idx++;
			
			if (idx >= buffer.length) {
			    flush(); ///???
			}
	  }
	  
    public void flush() throws IOException 
      {
			  if (idx == 0) {  // no data in buffer
			      return;
			  }
			  
			  // send data
			  dpack = new DatagramPacket(buffer, 0, idx, iAdd, port);
        SockPacket aSockPacket= new SockPacket(dpack);
        sendQueueOfSockPackets.add( // Queuing packet for sending.
            aSockPacket
            );

			  // reset buffer index
			  idx = 0;
			    }

	}
