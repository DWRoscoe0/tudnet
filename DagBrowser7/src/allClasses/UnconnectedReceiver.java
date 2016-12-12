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
    private NetcasterQueue unconnectedReceiverToConnectionManagerNetcasterQueue;
      // Queue which is destination of received packets.
    private UnicasterManager theUnicasterManager;
    private final NetcasterPacketManager theNetcasterPacketManager;

    UnconnectedReceiver( // Constructor. 
        DatagramSocket receiverDatagramSocket,
        NetcasterQueue unconnectedReceiverToConnectionManagerNetcasterQueue,
        UnicasterManager theUnicasterManager,
        NetcasterPacketManager theNetcasterPacketManager
        )
      { 
        this.unconnectedReceiverToConnectionManagerNetcasterQueue= 
        		unconnectedReceiverToConnectionManagerNetcasterQueue;
        this.receiverDatagramSocket= receiverDatagramSocket;
        this.theUnicasterManager= theUnicasterManager;
        this.theNetcasterPacketManager= theNetcasterPacketManager;
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
            ( ! EpiThread.exitingB() ) // requested.
            { // Receiving and queuing one packet.
              try {
                NetcasterPacket theNetcasterPacket= 
                		theNetcasterPacketManager.produceKeyedPacket();
                DatagramPacket theDatagramPacket= 
                		theNetcasterPacket.getDatagramPacket();
                appLogger.debug("run(): calling receive(..)");
                receiverDatagramSocket.receive(theDatagramPacket);
                PacketManager.logUnconnectedReceiverPacketV(
                		theDatagramPacket
                		);
                Unicaster theUnicaster= // Testing for existing Unicaster.
                		theUnicasterManager.tryingToGetUnicaster( 
                				theNetcasterPacket 
                				);
                if ( theUnicaster != null )  // Queuing packet to...
          	      theUnicaster.puttingKeyedPacketV( // Queuing to Unicaster.  
          	      		theNetcasterPacket
          	      		);
                	else
                	//%unconnectedReceiverToConnectionManagerNetcasterQueue.add(
                	unconnectedReceiverToConnectionManagerNetcasterQueue.put(
                			theNetcasterPacket
                			); // Queuing to ConnectionManager to let it decide.
                }
              catch( SocketException soe ) {
                appLogger.info("run(): " + soe );
                Thread.currentThread().interrupt(); // Translating 
                  // exception into request to terminate this thread.
                }
              } // Receiving and queuing one packet.
          }
          catch( IOException e ) {
		  			Globals.logAndRethrowAsRuntimeExceptionV( 
		  					"run() IOException: ", e
		  					);
          }
        }

    } // UnconnectedReceiver
