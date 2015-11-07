package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class UnconnectedReceiver // Unconnected-unicast receiver.

  implements Runnable

  /* This simple thread repeatedly receives unicast DatagramPackets 
    from the unconnected receiverDatagramSocket and 
    queues them to one of several destination threads.
    Most packets will be queued to an associated Unicaster thread.
    The packets that don't have associated Unicaster threads
    will be queued to the ConnectionManager.
    This thread is kept simple because the only known way to guarantee
    fast termination of the receive(..) operation
    is for another thread to close its DatagramSocket.
    Doing this will also cause this thread to terminate.
    */

  {

    // Injected dependency instance variables.
    
    private DatagramSocket receiverDatagramSocket;
      // Unconnected socket which is source of packets.
    private PacketQueue cmUnicastInputQueueOfSockPackets;
      // Queue which is destination of received packets.
    private UnicasterManager theUnicasterManager;

    UnconnectedReceiver( // Constructor. 
        DatagramSocket receiverDatagramSocket,
        PacketQueue cmUnicastInputQueueOfSockPackets,
        UnicasterManager theUnicasterManager
        )
      /* Constructs an instance of this class from:
          * receiverDatagramSocket: the socket receiving packets.
          * cmUnicastInputQueueOfSockPackets: the output queue.
        */
      { 
        this.cmUnicastInputQueueOfSockPackets= cmUnicastInputQueueOfSockPackets;
        this.receiverDatagramSocket=receiverDatagramSocket;
        this.theUnicasterManager= theUnicasterManager;
        }
    

    @Override
    public void run()
      /* This method repeatedly waits for and receives 
        DatagramPackets and queues each of them 
        for consumption by another appropriate thread.
        To terminates any pending receive and this thread,
        another thread should close the receiverDatagramSocket. 
        */
      {
        try { // Doing operations that might produce an IOException.
          while  // Receiving and queuing packets unless termination is
            ( ! Thread.currentThread().isInterrupted() ) // requested.
            { // Receiving and queuing one packet.
              try {
                byte[] buf = new byte[256];  // Construct packet buffer.
                DatagramPacket theDatagramPacket=
                  new DatagramPacket(buf, buf.length);
                SockPacket theSockPacket= new SockPacket(
                	theDatagramPacket
                  );
                receiverDatagramSocket.receive(theDatagramPacket);
                appLogger.debug(
                		" run() received: "
                		+ PacketStuff.gettingPacketString(theDatagramPacket)
                		);
                Unicaster theUnicaster= // Testing for associated Unicaster.
                		theUnicasterManager.tryGettingExistingUnicaster( 
                				theSockPacket 
                				);
                if ( theUnicaster != null )  // Queuing packet appropriately.
          	      theUnicaster.puttingReceivedPacketV( // To found Unicaster.  
          	      		theSockPacket
          	      		);
                	else
                	cmUnicastInputQueueOfSockPackets.add(theSockPacket); // To CM.
                }
              catch( SocketException soe ) {
                appLogger.info("run(): " + soe );
                Thread.currentThread().interrupt(); // Translating 
                  // exception into request to terminate this thread.
                }
              } // Receiving and queuing one packet.
          }
          catch( IOException e ) {
            appLogger.info("run() IOException: "+e );
            throw new RuntimeException(e);
          }
        }

    } // UnconnectedReceiver
