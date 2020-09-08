package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class Sender // Uunicast and multicast sender thread.

  implements Runnable

  /* This simple thread repeatedly receives DatagramPackets 
    via a queue from other threads and sends them
    though a DatagramSocket.
    
    This thread can be terminated in 1 way:
    * This thread's interrupt status can be set.
      This status is checked only after all queued packets have been sent.

    Operation of this thread is tricky because of
    its linkage to the UnconnectedReceiver thread 
    through their common DatagramSocket.
    If the socket is closed to terminate the Sender thread, 
    attempting to send packets will produce IOExceptions.
    There is no attempt at error recovery.
    If an IOException happens when attempting to send a packet,
    the packet is dropped, but the thread continues to run.
      
    Triggering and waiting for termination (joining) of these 2 threads
    may need to be done in a coordinated manner to prevent loss of
    important packets.

    Packets are logged after being removed from the send queue,
    but before processing it, usually calling the DatagramSocket.send(.) method.
    Logging is done with PacketManager.logSenderPacketV(theDatagramPacket),
    
    ///enh Simplify how the delay and dropping of packets for testing
    is controlled.
    
    ///enh Simplify the log format for delayed and dropped packets
    so that annotations precede the packet data.
      
    ///enh Change this or callers to gracefully finish sending queued packets 
    before closing socket and terminating, but limit the time doing so
    to a few seconds, in case of IO errors.
      
    ///enh Add congestion control and fair queuing.
    It would limit not only total data rate,
    but also data rates to  the individual peers
    on the branches of the tree of paths determined by
    tracert-like measurements.
    To do this it would maintain one queue per peer,
    organized as a tree similar to a tree showing
    tracert paths to those peers, and decorated with
    bandwidths of the various edges.
    
    ///enh Record the times that packets are passed to the DatagramSocket
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
				new ScheduledThreadPoolExecutor(1); ///? Inject this dependency.
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
        this.senderLockAndSignal= senderLockAndSignal;
        }

    @Override
    public void run() 
      /* This method repeatedly waits for and inputs of:
        * DatagramPackets from the queue, 
          which it sends them through the DatagramSocket.
        * Exit requests, which causes this method to exit.
        Processing queued packets has priority over termination.
        All queued packets will be transmitted before
        the thread will terminate.
        */
      {
	  	  beforeReturn: while (true) { // Processing packets until terminated.
          if ( tryingToProcessOneQueuedSockPacketB() ) // Try sending one. 
          	continue beforeReturn; // Looping if success.

      		if // Exiting loop if thread termination is requested.
      		  ( EpiThread.testInterruptB() ) 
      			break beforeReturn;

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

         	sendDropOrDelayPacketV( theDatagramPacket );
          } // beforeReturn: 
        return packetsProcessedB;
        }

    private void sendDropOrDelayPacketV(final DatagramPacket theDatagramPacket)
      /* This method processes theDatagramPacket which is ready to send.
        Generally that means only sending it, 
        but depending on conditions set for debugging or testing
        it might mean also:
        * dropping the packet
        * delaying the packet.
       	*/
	    {
    	  goReturn: {
          PacketManager.logSenderPacketV(theDatagramPacket);
            // Logging before sending, droping, or delaying,
            // so log order is always correct.
			    if  // If dropping a fraction of packets to test retry logic.
			    	( testingForDropOfPacketB( theDatagramPacket ) )
				    { ; // Drop this packet by doing nothing.
				    	break goReturn;
				    	}
			    long zeroL= 0L; // Prevent "Comparing identical" warning in next if...
	        if ( Config.packetSendDelayMsL != zeroL ) // Send after delay
          	{ // Send the packet after a delay.  This need not delay other packets.
          	  theScheduledThreadPoolExecutor.schedule( // Send after delay.
            		new Runnable() { 
          					public void run() {
            					sendingDatagramPacketV( theDatagramPacket );
            					} 
          					},
          				Config.packetSendDelayMsL,
          				TimeUnit.MILLISECONDS
          				);
              break goReturn;
          		}
          sendingDatagramPacketV( theDatagramPacket ); // Send packet immediately.
          break goReturn;
  				} // goReturn:
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
    	  		(theRandom.nextInt(2) == 0); // Debug.
    	  		// false;
    	  droppingB= false;  // Comment this out cause drop packets.
		    if ( droppingB )
			    {
			      /*  ////
			       theAppLog.debug( // Logging the drop of the packet.
			       	"dropping packet "
			      		+PacketManager.gettingDirectedPacketString(
			      				theDatagramPacket,true
			      				)
			      		);
			     	*/  ////
            theAppLog.appendToFileV("[DROPPING PACKET]");
            }
		    return droppingB;
	    	}

    private void sendingDatagramPacketV( DatagramPacket theDatagramPacket )
      /* This method tries to send theDatagramPacket.
        If the send fails because of an IOException then
        it will retry several times before giving up and discarding the packet.
        If there were failures it will log a summary of the failures.

        Although this now recovers when
          Skipped time has caused a java.net.SocketException: 
            No buffer space available (maximum connections reached?): 
              Datagram send failed 
        the packet passed to the send(..) method doesn't reach the remote end.
        The same thing happens with Later packets for several seconds.
        Then things begin to work normally again.  
        This is true under Windows at least.
        */
	    {
      		//appLogger.debug("sendingDatagramPacketV(..) calling send(..)." );
          int failuresI= 0;
          IOException savedIOException= null; 
        retryLoop: while (true) {
  	    	try { // Try sending the packet.
    	        theDatagramSocket.send(theDatagramPacket); // Send packet.
    	        // theAppLog.appendToFileV("[send]");
    	        break retryLoop;
    	      } catch (IOException theIOException) { // Handle exception.
              failuresI++;
    	        savedIOException= theIOException; // Save exception for logging later.
              if (failuresI>=5) { // Handle failure limit reached.
                theAppLog.debug("Sender.sendingDatagramPacketV() discarding packet.");
                break retryLoop;
                }
              theAppLog.debug("Sender.sendingDatagramPacketV() will retry sending.");
                     EpiThread.interruptibleSleepB(1000); // Sleep for 1 second.
              if (EpiThread.testInterruptB()) break retryLoop; // Termination requested.
    	        }
          } // retryLoop:
	    	if (failuresI != 0) // Report exceptions and retries if there were any. 
          theAppLog.debug("Sender.sendingDatagramPacketV(), "
              + "send failed "+failuresI+" times." + NL
              + PacketManager.gettingDirectedPacketString(theDatagramPacket,true)  
              + NL + "  last Exception was: "+savedIOException );
	      }
    
    }
