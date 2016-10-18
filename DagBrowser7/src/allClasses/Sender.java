package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;

public class Sender // Uunicast and multicast sender thread.

  implements Runnable

  /* This simple thread repeatedly receives DatagramPackets 
    via a queue from other threads and sends them
    though a DatagramSocket.
    
    //// Limit queue size.  Block queuers when full.
    
    //// Change this or callers to gracefully finish sending queued packets 
    before closing socket and terminating.
      
    ?? Add congestion control and fair queuing.
    It would limit not only total data rate,
    but also data rates to  the individual peers
    on the branches of the tree of paths determined by
    tracert-like measurements.
    To do this it would maintain one queue per peer,
    organized as a tree similar to a tree showing
    tracert paths to those peers, and decorated with
    bandwidths of the various edges.
    
    ?? Record the times that packets are passed to the DatagramSocket
    with the send(..) method, to be used for determining round trip time
    as accurately, instead of doing it in Unicaster as it is done now.
    NO, because RTT should include time in queue. 
    */

  {

    // Injected dependency instance variables.  Define.
	  private DatagramSocket theDatagramSocket;
	  	// Unconnected socket through which packets are sent.
		private NetcasterQueue netcasterToSenderNetcasterQueue;
		  // Queue from which this thread inputs packets to be sent.
		private LockAndSignal senderLockAndSignal;  
			// LockAndSignal for inputs to this thread.  It should be the same 
		  // LockAndSignal instance in netcasterToSenderNetcasterQueue construction. 
		
		private Random theRandom= new Random(1);
		  // Seed is not zero so first DISCOVER packet is sent.  

		Sender( // Constructor. 
        DatagramSocket theDatagramSocket,
        NetcasterQueue netcasterToSenderNetcasterQueue,
        LockAndSignal senderLockAndSignal
        )
      { 
        this.netcasterToSenderNetcasterQueue= netcasterToSenderNetcasterQueue;
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
	      		if // Exiting loop if thread termination is requested.
	      		  ( EpiThread.exitingB() ) 
	      			break toReturn;

	          processingSockPacketsToSendB(); // Processing inputs.
	          senderLockAndSignal.waitingForInterruptOrNotificationE();
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
        makes congestion control theoretically possible here, 
        though it's not done here yet.
        
        ?? DatagramSocket.send(..) could block the thread 
        if the network queue fills.
        To avoid this, maybe a DatagramChannel could be used,
        or sending could be done in a separate thread.
        However the eventual addition of congestion control 
        might make this unnecessary.
        */
      {
        boolean packetsProcessedB= false;  // Assuming no packet to send.

        while (true) {  // Processing all queued send packets.
          NetcasterPacket theNetcasterPacket= // Trying to get next packet.
          		netcasterToSenderNetcasterQueue.poll();
          if (theNetcasterPacket == null) break;  // Exiting if no more packets.
        	DatagramPacket theDatagramPacket= 
        			theNetcasterPacket.getDatagramPacket();
          IPAndPort theIPAndPort= theNetcasterPacket.getKeyK();
          theDatagramPacket.setAddress(theIPAndPort.getInetAddress());
          theDatagramPacket.setPort(theIPAndPort.getPortI());
          if  // Debug: Send all but 1/10 of packets to test retries.
            (theRandom.nextInt(20) == 0)
            appLogger.debug( // Drop the packet.
            		"dropping packet "
            		+PacketManager.gettingDirectedPacketString(
            				theDatagramPacket,true
            				)
            		);
	          else
	          try { // Send the packet.
	          		PacketManager.logSenderPacketV(theDatagramPacket);
	          		  // Log before sending so log will make sense.
		            theDatagramSocket.send(   // Send packet.
		            	theDatagramPacket
		              );
	            } catch (IOException e) { // Handle exception by dropping packet.
	              appLogger.error(
	                "processingSockPacketsToSendB(),"
	                +e
	                );
	            }

          packetsProcessedB= true; // Recording that a packet was processed.
          }
          
        return packetsProcessedB;
        }
    
    }
