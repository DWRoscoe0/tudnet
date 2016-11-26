package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
//import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
		
		// Other variables.
		private ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor=
				new ScheduledThreadPoolExecutor(1); ////// Inject this dependency.
		//private Random theRandom= new Random(1);
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
	  	  beforeReturn: while (true) { // Processing packets until terminated.
      		if // Exiting loop if thread termination is requested.
      		  ( EpiThread.exitingB() ) 
      			break beforeReturn;

          if ( tryingToProcessOneQueuedSockPacketB() ) 
          	continue beforeReturn; // Looping if success.

          senderLockAndSignal.waitingForInterruptOrNotificationE();
	        } // while (true) beforeReturn:
        }


    private boolean tryingToProcessOneQueuedSockPacketB()
      /* This method tries to send one SockPacket stored in the send queue.
        This method returns true if one packet was sent, false otherwise.

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

        beforeReturn: { // Processing maximum of one queued packet.
          NetcasterPacket theNetcasterPacket= // Trying to get next packet.
          		netcasterToSenderNetcasterQueue.poll();
          if (theNetcasterPacket == null)  // Exiting if no queued packets. 
          	break beforeReturn;
          packetsProcessedB= true; // Recording a packet to be processed.
        	DatagramPacket theDatagramPacket= 
        			theNetcasterPacket.getDatagramPacket();
          IPAndPort theIPAndPort= theNetcasterPacket.getKeyK();
          theDatagramPacket.setAddress(theIPAndPort.getInetAddress());
          theDatagramPacket.setPort(theIPAndPort.getPortI());

         	processingDatagramPacketV( theDatagramPacket );
          } // beforeReturn: 
        return packetsProcessedB;
        }

    private void processingDatagramPacketV( 
    		final DatagramPacket theDatagramPacket 
    		)
      /* This method processes theDatagramPacket.
        Generally that means sending it, but depending on Debug state,
        it might mean dropping it, or delaying it.
       	*/
	    {
    	  beforeReturn: {
			    if  // Debug: drop a fraction of packets to test retries logics.
			    	( testingForDropOfPacketB( theDatagramPacket ) )
			    	break beforeReturn;
			    //// add Timer delay logic here.
          if ( Delay.packetSendDelayMsL == 0L )
          	sendingDatagramPacketV( theDatagramPacket );
          	else
          	{
          		theScheduledThreadPoolExecutor.schedule(
          				new Runnable() { 
          					public void run() {
	          					sendingDatagramPacketV( theDatagramPacket );
	          					} 
          					},
          				Delay.packetSendDelayMsL,
          				TimeUnit.MILLISECONDS
          				);
            	sendingDatagramPacketV( theDatagramPacket );
          		}
    			} // beforeReturn:
	    	}

    boolean testingForDropOfPacketB( DatagramPacket theDatagramPacket )
      /* This method, when its code is enabled,
        tests whether a packet should be randomly dropped.
        It displays a log message including theDatagramPacket
        if the caller should drop the packet.
        It returns true if the packet should be dropped, false otherwise.
       */
	    {
    	  boolean droppingB= 
    	  		//(theRandom.nextInt(20) == 0); // Debug.
    	  		false;
		    if ( droppingB )
			    {
			      appLogger.debug( // Logging the drop of the packet.
			      		"dropping packet "
			      		+PacketManager.gettingDirectedPacketString(
			      				theDatagramPacket,true
			      				)
			      		);
			    	}
		    return droppingB;
	    	}

    private void sendingDatagramPacketV( DatagramPacket theDatagramPacket )
      // This method sends theDatagramPacket and handles exceptions.
	    {
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
	    }
    
    }
