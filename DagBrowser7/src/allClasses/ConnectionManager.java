package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static allClasses.Globals.*;  // appLogger;

public class ConnectionManager 

  extends MutableList

  implements Runnable

  /* This class makes and maintains simple UDP unicast connections
    with other Infogora peer nodes.

    It extends MutableList.  This is a list of connected peers.
    Because this list is accessed by a TreeModel on the EDT,
    this List is asynchronously updated when changes are needed
    using java.awt.EventQueue.invokeLater(..) method which runs on 
    the Event Dispatch Thread (EDT).

    For maintaining the set of peers that can be updated immediately
    by this ConnectionManager Thread, a Map is used.
    It is also used to quickly ?

    This class makes use of other threads to manage the individual connections.
    It receives inputs from those connection via thread-safe queues.

    Originally the plan was to use a separate 
    connected DatagramSocket for each peer of a node.
    Unfortunately, unlike TCP, which can demultiplex packets
    based on both local and remote (IP,port) pairs,
    USP uses only the local (IP,port) pair when it demultiplexes.
    So to prevent BindExceptions on sockets bound to
    the same local ports, each peer of a given node would need 
    to connect to the remote peer using a different remote number.
    This is an unacceptable complication.
    
    So, only 2 Datagram sockets are used: 
    * one socket for multicast receiving and 
    * another socket for every thing else.
    Their ports must be different/unique.

    There are several types of thread classes defined in this file
    and in other java files.
    * ConnectionManager: the main thread for this class.
      It manages everything.  
    * UnconnectedReceiver: receives packets for ConnectionManager.
    * Unicaster: manages one connection.  
      ? Maybe rename to ConnectionManager?
    * Multicaster (now in its own file): 
      sends and receives multicast packets
      used for discovering other peers on the LAN.

    An IOException can be caused by external events,
    such as a link going down, the computer going to sleep, etc.
    These are handled by closing the socket, opening another,
    and retrying the operation.
    */

  { // class ConnectionManager.

  
    // Injected instance variables, all private.
	    
	    private ConnectionsFactory theConnectionsFactory; 

    // Other instance variables, all private.
	
	    private Map<SocketAddress,NetCasterValue> peerSocketAddressConcurrentHashMap;
	      /* This initially empty Collection provides lookup of Peers
	        by associated SocketAddress.
	        It is used mainly to determine which, if any, Unicaster
	        is the source of a newly received packet.

					Presently only connected Peers are put in this Map,
					and it is kept synchronized with the superclass MutableList,
					but maybe it should contain presently and past connected Peers??
	        Or maybe these should be in a separate class for this??
	        */
	
	    public DatagramSocket unconnectedDatagramSocket; // For UDP io.
	
	    private UnconnectedReceiver theUnconnectedReceiver; // Receiver and
	    private EpiThread theUnconnectedReceiverEpiThread ; // its thread.
	
	    private LockAndSignal theLockAndSignal;  // LockAndSignal for this thread.
	      /* This single object is used to synchronize communication between 
	        the ConnectionManager and all threads providing data to it.
	        The old way used an Objects for a lock and a separate 
	        boolean signal.
	        */
	
	    private PacketQueue sendPacketQueue; // Queue of packets to be sent.
	
	    private PacketQueue multicastSignallingQueueOfSockPackets; 
	      // Discovery packets queue.
	
	    private PacketQueue uniconnectedSignallingQueueOfSockPackets;
	      // For received unconnected packets.
	
	    private SignallingQueue<Unicaster> unicasterQueue;
	      // For Peers signaling their idleness.


    public ConnectionManager(   // Constructor.
        ConnectionsFactory theConnectionsFactory,
        DataTreeModel theDataTreeModel
        )
      {
        super(  // Constructing base class.
          theDataTreeModel, // Injected, to be notified when Unicaster List changes.
          "Connections", // DataNode (and thread) name.
          new DataNode[]{} // Initially empty List of Peers.
          );

        // Storing other injected dependencies stored in this class.
        this.theConnectionsFactory= theConnectionsFactory;
        }


    public void run()  // Main ConnectionManager thread logic.
      /* This method creates unconnectedDatagramSocket 
        for its use and uses it and does other things
        by calling settingThreadsAndDoingWorkV().
        This is inside of a try-catch block in a loop
        to recover and retry after SocketException-s.
        The name of the thread should be Connections??
        */
      { // Connections.
    		///appLogger.info("Connections.run(): thread beginning.");
    		initilizeV();  // Do non-injection initialization.

        while   // Repeating until termination is requested.
          ( !Thread.currentThread().isInterrupted() )
          {
            try { // Creating and using unconnected DatagramSocket.
              unconnectedDatagramSocket= // Construct socket for UDP io.
                new DatagramSocket(null);
              unconnectedDatagramSocket.setReuseAddress(true);
              unconnectedDatagramSocket.bind(  // Bind socket to...
                new InetSocketAddress(  // ...new INetSocketAddress...
                  PortManager.getLocalPortI()  // ...bound to app's local port.
                  )  // Note, the IP is not defined.
                );
              settingThreadsAndDoingWorkV(); 
              }
            catch ( SocketException e ) { // Handling SocketException.
              // Doing nothing now because we might retry.
              appLogger.error("Connections.run(): SocketException:"+e);
              }
            finally { // Closing socket if it exists.
              if ( unconnectedDatagramSocket != null )
                unconnectedDatagramSocket.close();
              }
            }
        ///appLogger.info("Connections.run(): thread ending.");  // Connections.
        } // Connections.

    private void initilizeV()
      {
		    peerSocketAddressConcurrentHashMap= // Setting socket-to-peer map empty.
		      new ConcurrentHashMap<SocketAddress,NetCasterValue>();
		        // This probably doesn't need to be a concurrent map,
		        // but it probably doesn't hurt.
		
		    // Setting all input queues empty.
		    theLockAndSignal= new LockAndSignal(false);  // Creating signaler.
		    sendPacketQueue=
		      new PacketQueue(theLockAndSignal);
		    multicastSignallingQueueOfSockPackets=
		      new PacketQueue(theLockAndSignal);
		    uniconnectedSignallingQueueOfSockPackets=
		      new PacketQueue(theLockAndSignal);
		    unicasterQueue=
		      new SignallingQueue<Unicaster>(theLockAndSignal);
    		}

    public String getValueString( )
      {
    	  return Integer.toString(getChildCount( ));
        }


    private void settingThreadsAndDoingWorkV()
      throws SocketException
      /* In this method, the unconnectedDatagramSocket 
        is assumed to be initialized.
        After doing more initializing,
        mainly creating and starting some threads,
        it processes various inputs and pending events until termination.
        Then it finalizes by terminating those same threads and returns.
        A return might also result from the throwing of a SocketException.
        */
      {
        withSocketInitializingV();

        processingInputsAndExecutingEventsV(); // Until thread termination...
          // ...is requested.

        withSocketFinalizingV();
        }

    private void withSocketInitializingV()
      {
        startingPeerDiscoveryV();  // (This daemon will terminate itself.)
        theUnconnectedReceiver=  // Constructing thread.
          new UnconnectedReceiver( 
            unconnectedDatagramSocket,
            uniconnectedSignallingQueueOfSockPackets
            );
        theUnconnectedReceiverEpiThread= new EpiThread( 
          theUnconnectedReceiver,
          "UnconnectedReceiver"
          );

        theUnconnectedReceiverEpiThread.startV();  // Starting thread.
        }

    private void withSocketFinalizingV()
      {
        terminatingPeerThreadsV(); // [most of?] theUnconnectedReceiver users.

        theUnconnectedReceiverEpiThread.stopV();  // Requesting termination of
          // theUnconnectedReceiver thread.
        unconnectedDatagramSocket.close(); // Causing immediate unblock of
          // unconnectedDatagramSocket.receive() in that thread.
        theUnconnectedReceiverEpiThread.joinV();  // Waiting for termination of
          // theUnconnectedReceiver thread.
        }

    private void terminatingPeerThreadsV()
      /* This method terminates all of the Unicaster threads 
        that were created to communicate with discovered Unicaster nodes.
        It does this in 2 loops: the first to request terminations,
        and a second to wait for terminations to complete; 
        because faster concurrent terminations are possible 
        when there are multiple Peers.

        ?? Though it might be good to update the displayed Unicaster list,
        by removing the Peers from the inherited MutableList
        during the second loop of the termination process 
        to see any slow terminators, this is not done presently. 
        */
      {
        ///appLogger.info("Connections.terminatingPeerThreadsV() beginning.");

        Iterator<NetCasterValue> anIteratorOfNetCasterValues;  // For peer iterator.

        anIteratorOfNetCasterValues=  // Getting new peer iterator.
          peerSocketAddressConcurrentHashMap.values().iterator();
        while  // Requesting termination of all Peers.
          (anIteratorOfNetCasterValues.hasNext())
          {
            NetCasterValue theNetCasterValue= anIteratorOfNetCasterValues.next();
            theNetCasterValue.getEpiThread().stopV();  // Requesting Termination 
              // of Unicaster.
            }

        anIteratorOfNetCasterValues=  // Getting new peer iterator.
          peerSocketAddressConcurrentHashMap.values().iterator();
        while  // Waiting for completion of termination of all Peers.
          (anIteratorOfNetCasterValues.hasNext())
          {
            NetCasterValue theNetCasterValue= anIteratorOfNetCasterValues.next();
            theNetCasterValue.getEpiThread().joinV(); // Waiting for termination 
            ///?theNetCasterValue.getEpiThread().stopAndJoinV();  // Waiting for termination
              // of Unicaster.
            anIteratorOfNetCasterValues.remove(); // Removing from HashMap...
            }

        ///appLogger.info("Connections.terminatingPeerThreadsV() ending.");
        }

    private void processingInputsAndExecutingEventsV()
      /* This method contains a single loop which
        does various types of work.  The work consists of:
          * Processing inputs it gets from several queues.
          - It was also processing scheduled jobs whose times have come,
            but these have been moved to other threads.
        When there is no more work, the loop blocks until 
        the next bit of work arrives, when it repeats the process.
        The loop continues until thread termination is requested
        by setting the Interrupt Status Flag with Thread.interrupt().
        This flag is preserved by this method,
        so it may be tested by its callers.
        */
      {
        while  // Repeating until thread termination is requested.
          ( !Thread.currentThread().isInterrupted() )
          {
            { // Trying to do work of processing all inputs.
              processingUnconnectedSockPacketsB();
              if ( processingSockPacketsToSendB() )
                continue;  // Loop if any SockPackets were sent.
              processingDiscoverySockPacketsB();
                // Last because these are rare.
              processingIdlePeersB();
              }

            theLockAndSignal.doWaitE();  // Waiting for new inputs.
            }

        ///appLogger.info("Connections: termination begun.");
        }

    private void startingPeerDiscoveryV()
      // Klugy start of PeerDiscover daemon, which needs a lot of work??
      {
  		Multicaster theMulticaster;
  		EpiThread PeerDiscoveryEpiThread;
        try { // PeerDiscoery
          //new Multicaster(  // Construct Multicaster...
        	theMulticaster=
	        	theConnectionsFactory.makeMulticaster(
	            sendPacketQueue // ...with send queue,...
	            ,multicastSignallingQueueOfSockPackets // ...receive queue,...
	            ,unconnectedDatagramSocket // ...and socket to use.
	            );  // Multicaster will start its own thread.
          add( theMulticaster );  // Add to DataNode List.
          theDataTreeModel. // Informing TreeModel.
            safelyReportingChangeV( this );
          PeerDiscoveryEpiThread= new EpiThread( 
              theMulticaster,
              "Multicaster-?" ///+peerInetSocketAddress
              );
          PeerDiscoveryEpiThread.startV();
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

    private boolean processingSockPacketsToSendB()
      /* This method processes packets stored in the send queue.
        Packets to be sent arrive via the sendPacketQueue
        to enable the ConnectionManager to track all network i/o.
        This method returns true if at least one packet was sent,
        false otherwise.
        It presently assumes that the packet is ready to be sent,
        and nothing else needs to be added first.

        Channeling all outgoing packets through this code
        enables the ConnectionManager to better manage flow.
        
        ?? Though unlikely, unconnectedDatagramSocket.send(..) 
        could block the thread if the network queue fills.
        To avoid this, maybe a DatagramChannel could be used,
        or sending could be done in a separate thread.
        However congestion control might make this unnecessary.
        */
      {
        boolean packetsProcessedB= false;  // Assuming no packet to send.
        SockPacket theSockPacket;

        while (true) {  // Processing all queued send packets.
          theSockPacket= // Trying to get next packet from queue.
            sendPacketQueue.poll();
          if (theSockPacket == null) break;  // Exiting if no more packets.
          try { // Send the gotten packet.
            theSockPacket.getDatagramSocket().send(   // Send packet.
              theSockPacket.getDatagramPacket()
              );
            } catch (IOException e) { // Handle by dropping packet.
              appLogger.info(
                "ConnectionManager.processingSockPacketsToSendB(),"
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

    private boolean processingDiscoverySockPacketsB() 
      /* This method processes unconnected packets received by 
        the Multicaster Thread and forwarded here,
        by adding the nodes that sent them to the known connections.
        It returns true if any Discovery packets were processed,
        false otherwise.
        */
      {
        boolean packetsProcessedB= false;
        SockPacket theSockPacket;

        while (true) {  // Process all received discovery packets.
          {
            theSockPacket= // Try getting next packet from queue.
              multicastSignallingQueueOfSockPackets.poll();
            }
          if (theSockPacket == null) break;  // Exit if no more packets.
          processReceivedPacketV(  // Process packet.
            theSockPacket
            );
          packetsProcessedB= true;
          }
          
        return packetsProcessedB;
        }

    private boolean processingIdlePeersB() 
      /* This method processes Peers that 
        have signaled that they need service
        by queuing themselves in the unicasterQueue.
        Presently this can mean only one thing,
        that the peer thread has lost contact
        with its associated peer node and is terminating its thread.
        The connection manager's job is to remove the Unicaster from
        the inherited MutableList and the Map.
        This method returns true if any Peers 
        were in the unicasterQueue and processed, false otherwise.
        */
      {
        boolean elementProcessedB= false;

        while (true) {  // Process all queued Peers.
          final Unicaster theUnicaster= // Getting next Unicaster from queue.
        		unicasterQueue.poll();
          if (theUnicaster == null) break;  // Exiting if no more Peers.

          remove( theUnicaster );  // Remove from DataNode List.
          InetSocketAddress theInetSocketAddress=
            theUnicaster.getInetSocketAddress();
          NetCasterValue theNetCasterValue=
          	peerSocketAddressConcurrentHashMap.get(theInetSocketAddress);
          theNetCasterValue.getEpiThread().stopAndJoinV(); // Terminate thread.
          peerSocketAddressConcurrentHashMap.remove( // Remove from Map.
            theInetSocketAddress
            );

          elementProcessedB= true;
          }

        theDataTreeModel.safelyReportingChangeV( this ); // Informing TreeModel.

        return elementProcessedB;
        } 

    private boolean processingUnconnectedSockPacketsB()
      /* This method processes unconnected unicastpackets 
        that are received by the UnconnectedReceiver Thread 
        and forwarded here.  It does this
        by adding the nodes that sent them to the known connections.
        It returns true if any packets were processed, false otherwise.
        */
      {
        boolean packetsProcessedB= false;
        SockPacket theSockPacket;

        while (true) {  // Process all received uniconnected packets.
          {
            theSockPacket= // Try getting next packet from queue.
              uniconnectedSignallingQueueOfSockPackets.poll();
            }

          if (theSockPacket == null) break;  // Exit if no more packets.
          
          //appLogger.info(
          //  "ConnectionManager.processingUnconnectedSockPacketsB()\n  "
          //  + theSockPacket.getSocketAddressesString()
          //  );
          processReceivedPacketV(  // Process packet.
            theSockPacket
            );

          packetsProcessedB= true;
          }
          
        return packetsProcessedB;
        }

    private void processReceivedPacketV(SockPacket theSockPacket)
      /* This method processes one packet received from a peer.
        The peer is assumed to be at the packet's remote address and port.
        It adds an entry to the Unicaster data structures
        if the entry doesn't already exist.
        If the entry does exist then it updates it.
        This should work with both unicast and multicast packets
        provided that their remote addresses are those of the remote peer.
        */
      {
        //appLogger.info(
        //  "ConnectionManager.processReceivedPacketV(..)\n  "
        //  + theSockPacket.getSocketAddressesString()
        //  );
	      Unicaster theUnicaster=  // Get or create Unicaster.
	          tryGettingExistingUnicaster( theSockPacket );
	      if ( theUnicaster == null )
		      {
		        appLogger.info(
		          "ConnectionManager.processReceivedPacketV(..)\n  "
		          + theSockPacket.getSocketAddressesString()
		          );
			      theUnicaster=  // Get or create Unicaster.
			          getOrBuildUnicaster( theSockPacket );
			      }
	      theUnicaster.puttingReceivedPacketV( // Giving to Unicaster  
	      		theSockPacket  // its genesis packet.
	      		);
        }

    private Unicaster tryGettingExistingUnicaster( SockPacket theSockPacket )
      /* This method returns the Uniaster associated with the
         source address of theSockPacket, if such as Unicaster exists.
         It returns null otherwise.
         */
	    { 
    		Unicaster resultUnicaster= null;
        DatagramPacket theDatagramPacket= theSockPacket.getDatagramPacket();
        InetSocketAddress peerInetSocketAddress= // Building packet's address.
          new InetSocketAddress(
            theDatagramPacket.getAddress(),
            theDatagramPacket.getPort()
            );
        NetCasterValue theNetCasterValue= // Getting associated NetCasterValue.
          peerSocketAddressConcurrentHashMap.get(peerInetSocketAddress);
        if (theNetCasterValue != null)
        		resultUnicaster= theNetCasterValue.getUnicaster();
        return resultUnicaster;
	      }

    private Unicaster getOrBuildUnicaster( SockPacket theSockPacket )
      /* Gets or creates the Unicaster associated with theSockPacket.
        It adds the Unicaster to the appropriate data structures.
        It returns the found or created Unicaster.
        */
      {
        DatagramPacket theDatagramPacket=  // Get DatagramPacket.
          theSockPacket.getDatagramPacket();
        InetSocketAddress peerInetSocketAddress=  // Build packet's address.
          //theDatagramPacket.getSocketAddress();
          new InetSocketAddress(
            theDatagramPacket.getAddress(),
            theDatagramPacket.getPort()
            );
        Unicaster theUnicaster=  // Get or create Unicaster.
          getOrBuildUnicaster(peerInetSocketAddress);
        return theUnicaster;
        }

    private Unicaster getOrBuildUnicaster(
        InetSocketAddress peerInetSocketAddress
        )
      /* Gets the Unicaster whose SocketAddress is peerInetSocketAddress.
        If such a Unicaster does not exist then it builds one. 
        If it builds one then it also:
        * Adds the Unicaster to the appropriate data structures.
        * Starts the associated Unicaster thread.
        It returns the gotten or built Unicaster.
        */
      {
        NetCasterValue resultNetCasterValue= // Testing whether peer is already stored.
          peerSocketAddressConcurrentHashMap.get(peerInetSocketAddress);
        if (resultNetCasterValue == null) // Adding peer if not stored already.
          {
            final NetCasterValue newNetCasterValue=  // Building new peer. 
              theConnectionsFactory.makeUnicasterValue(
                peerInetSocketAddress,
                sendPacketQueue,
                unicasterQueue,
                unconnectedDatagramSocket
                );
            peerSocketAddressConcurrentHashMap.put( // Adding to HashMap with...
              peerInetSocketAddress,  // ...the SocketAddress as the key and...
              newNetCasterValue  // ...the NetCasterValue as the value.
              );
            add( newNetCasterValue.getUnicaster() );  // Add to DataNode List.
            theDataTreeModel. // Informing TreeModel.
              safelyReportingChangeV( this );
            newNetCasterValue.getEpiThread().startV(); // Start peer's thread.
            resultNetCasterValue= newNetCasterValue;  // Using new peer as result.
            }
        return resultNetCasterValue.getUnicaster();
        }


    // Nested classes.

    public static class NetCasterValue  // Value of HashMap entry.

     /* This class was created when it became necessary to
       have access to both the Unicaster class and its Thread.
       */

      {

        private Unicaster theUnicaster;
        private EpiThread theEpiThread;

        public NetCasterValue(  // Constructor. 
            InetSocketAddress peerInetSocketAddress,
            Unicaster theUnicaster
            )
          {
            this.theUnicaster= theUnicaster;
            this.theEpiThread= new EpiThread( 
              theUnicaster,
              "Unicaster-"+peerInetSocketAddress
              );
            }
            
        public Unicaster getUnicaster()
          { 
            return theUnicaster; 
            }
            
        public EpiThread getEpiThread()
          { 
            return theEpiThread; 
            }
        }

    public static class PacketQueue extends SignallingQueue<SockPacket>
      {

        PacketQueue( LockAndSignal aLockAndSignal )  // Constructor.
          {
            super( aLockAndSignal );
            }

        }

    private class UnconnectedReceiver // Unconnected-unicast receiver.

      implements Runnable

      /* This simple thread receives and queues unicast DatagramPackets 
        from an unconnected DatagramSocket.
        Though the DatagramSocket is not connected, the Peers 
        from which the packets are received are considered connected.
        The packets are queued to a PacketQueue for consumption by 
        another thread, probably to the ConnectionManager.
        */
      {

        // Injected dependency instance variables.
        
        private DatagramSocket receiverDatagramSocket;
          // Unconnected socket which is source of packets.

        private PacketQueue receiverSignallingQueueOfSockPackets;
          // Queue which is destination of received packets.


        UnconnectedReceiver( // Constructor. 
            DatagramSocket receiverDatagramSocket,
            PacketQueue receiverSignallingQueueOfSockPackets
            )
          /* Constructs an instance of this class from:
              * receiverDatagramSocket: the socket receiving packets.
              * receiverSignallingQueueOfSockPackets: the output queue.
            */
          { 
            this.receiverSignallingQueueOfSockPackets=
              receiverSignallingQueueOfSockPackets;
            this.receiverDatagramSocket=
              receiverDatagramSocket;
            }
        

        @Override
        public void run() 
          /* This method repeatedly waits for and reads 
            DatagramPackets and queues them 
            for consumption by another thread.
            */
          {
            ///appLogger.threadInfo("run() beginning.");
            try { // Operations that might produce an IOException.
              while  // Receiving and queuing packets unless termination is
                ( ! Thread.currentThread().isInterrupted() ) // requested.
                { // Receiving and queuing one packet.
                  try {
                    byte[] buf = new byte[256];  // Construct packet buffer.
                    DatagramPacket aDatagramPacket=
                      new DatagramPacket(buf, buf.length);
                    SockPacket aSockPacket= new SockPacket(
                      receiverDatagramSocket, aDatagramPacket
                      );
                    appLogger.threadInfo("run(): waiting for packet.");
                    receiverDatagramSocket.receive(aDatagramPacket);
                    appLogger.threadInfo("run(): received packet.");
                    receiverSignallingQueueOfSockPackets.add(aSockPacket);
                    }
                  catch( SocketException soe ) {
                    appLogger.threadInfo("run(): " + soe );
                    Thread.currentThread().interrupt(); // Translating 
                      // exception into request to terminate this thread.
                    }
                  } // Receiving and queuing one packet.
              }
              catch( IOException e ) {
                appLogger.threadInfo("run() IOException: "+e );
                throw new RuntimeException(e);
              }
            ///appLogger.threadInfo("run() ending.");
            }

        }
      ; // UnconnectedReceiver


    } // class ConnectionManager.
