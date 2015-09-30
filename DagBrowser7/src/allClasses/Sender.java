package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Sender // Uunicast and multicast sender thread.

  implements Runnable

  /* This simple thread repeatedly receives DatagramPackets 
    from via a queue from other threads and sends them
    though a DatagramSocket.
    
    ?? This thread might later do some rate control,
    including rate control affected by attributes of
    segments of the tracrt paths to the peers.
    
    ?? This thread might later perform OutputStream multiplexing services.
    
    ?? This stream might later record and report send times
    for the purpose of determining round trip time.
    */

  {

    // Injected dependency instance variables.  Define.
	  private DatagramSocket theDatagramSocket;
	  	// Unconnected socket through which packets are sent.
		private PacketQueue senderInputQueueOfSockPackets;
		  // Queue from which this thread inputs packets to be sent.
		private LockAndSignal senderLockAndSignal;  
			// LockAndSignal for inputs to this thread.  It should be the same 
		  // LockAndSignal instance in senderInputQueueOfSockPackets construction. 
		
    Sender( // Constructor. 
        DatagramSocket theDatagramSocket,
        PacketQueue senderInputQueueOfSockPackets,
        LockAndSignal senderLockAndSignal
        )
      { 
        this.senderInputQueueOfSockPackets= senderInputQueueOfSockPackets;
        this.theDatagramSocket= theDatagramSocket;
        this.senderLockAndSignal=  senderLockAndSignal;
        }

    @Override
    public void run() 
      /* This method repeatedly waits for and inputs 
        DatagramPackets from the queue and sends them
        through the DatagramSocket.
        */
      {
	  	  toReturn: {
	    		while (true) { // Repeating until thread termination requested.
	      		if // Exiting loop if  thread termination is requested.
	      		  ( Thread.currentThread().isInterrupted() ) break toReturn;
		      		
	          processingSockPacketsToSendB(); // Processing inputs.
	          senderLockAndSignal.doWaitE();  // Waiting for next input signal.
		        } // while (true)
	    		} // toReturn.
        }


    private boolean processingSockPacketsToSendB()
      /* This method sends packets stored in the send queue.
        This method returns true if at least one packet was sent,
        false otherwise.
        It presently assumes that the packet is ready to be sent,
        and nothing else needs to be added first.

        Channeling all outgoing packets through this code
        makes congestion control possible, though it's not done yet.
        
        ?? DatagramSocket.send(..) could block the thread 
        if the network queue fills.
        To avoid this, maybe a DatagramChannel could be used,
        or sending could be done in a separate thread.
        However the eventual addition of congestion control 
        might make this unnecessary.
        */
      {
        boolean packetsProcessedB= false;  // Assuming no packet to send.
        SockPacket theSockPacket;

        while (true) {  // Processing all queued send packets.
          theSockPacket= // Trying to get next packet from queue.
          		senderInputQueueOfSockPackets.poll();
          if (theSockPacket == null) break;  // Exiting if no more packets.
          try { // Send the gotten packet.
          	DatagramPacket theDatagramPacket= theSockPacket.getDatagramPacket();
            theDatagramSocket.send(   // Send packet.
            	theDatagramPacket
              );
            } catch (IOException e) { // Handle by dropping packet.
              appLogger.info(
                "processingSockPacketsToSendB(),"
                +"IOException."
                );
            }
          //appLogger.info(
          //  "sent unconnected packet:\n  "
          //  + theSockPacket.getSocketAddressesString()
          //  );

          packetsProcessedB= true; // Recording that a packet was processed.
          }
          
        return packetsProcessedB;
        }
    
    } // UnconnectedReceiver
