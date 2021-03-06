package allClasses;

import static allClasses.AppLog.theAppLog;

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
    
    Terminating this thread is tricky because of
    its linkage to the Sender thread through their common DatagramSocket.
    This thread can be terminated in 2 ways:
    * This thread's interrupt status can be set.
      This status is checked between received packets.
    * A close operation is performed on the DatagramSocket.
      This causes an IOException on the DatagramSocket.receive(..) operation.
      Receiving a packet and the occurrence of an IOException
      are the only 2 ways to terminate the DatagramSocket.receive(..) operation.
      Unfortunately closing the DatagramSocket can cause 
      the Sender thread to terminate at the same time.
    Triggering and waiting for termination (joining) of these 2 threads
    may need to be done in a coordinated manner to prevent loss of
    important packets.
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
        for consumption by another appropriate thread,
        either the a Unicaster thread or the ConnectionManager thread.
        To terminates any pending receive operation started by this thread
        and this thread, another thread should close the receiverDatagramSocket. 
        */
      {
        theAppLog.info("run(): begins");
        while  // Receiving and queuing packets unless termination is
          ( ! EpiThread.testInterruptB() ) // requested.
          { // Receiving and queuing one packet appropriately.
            try {
              NetcasterPacket theNetcasterPacket= // Get empty keyed packet.
                  theNetcasterPacketManager.produceKeyedPacket();
              DatagramPacket theDatagramPacket= // Get DatagramPacket from it.
                  theNetcasterPacket.getDatagramPacket();
              // theAppLog.debug("run(): before receive(..)");
              receiverDatagramSocket.receive(theDatagramPacket); // Receive.
              PacketManager.logUnconnectedReceiverPacketV(theDatagramPacket);
              Unicaster theUnicaster= // Lookup matching Unicaster.
                theUnicasterManager.tryingToGetUnicaster(theNetcasterPacket);
              if ( theUnicaster != null ) { // If Unicaster found, queue to it.
                theUnicaster.puttingKeyedPacketV( theNetcasterPacket );
                } else {// Unicaster n.Not found, so queue to ConnectionManager.
                  unconnectedReceiverToConnectionManagerNetcasterQueue.put(
                    theNetcasterPacket);
                }
              }
            catch( SocketException soe ) {
              theAppLog.info("run(): interrupted by " + soe );
              Thread.currentThread().interrupt(); // Translating 
                // exception into request to terminate this thread.
              }
            catch( IOException e ) {
              Misc.logAndRethrowAsRuntimeExceptionV("run() IOException: ", e);
              }
            } // Receiving and queuing one packet to appropriate destination.
        theAppLog.info("run(): ends");
        }

    } // UnconnectedReceiver
