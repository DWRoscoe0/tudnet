package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import static allClasses.Globals.*;  // appLogger;

public class ConnectionManager 

  ///extends EpiThread
  
  implements Runnable

  /* This class makes and maintains simple UDP unicast connections 
    based on input from various other Threads.

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
    * one for multicast receiving and 
    * another for every thing else.
    Their ports must be different/unique.

    There are several types of thread classes defined in this file
    and in other java files.
    * ConnectionManager: manages everything.  
      ? Maybe rename to ConnectionsManager?
    * UnconnectedReceiver: receives packets for Connection(s)Manager.
    * Peer: manages one connection.  
      ? Maybe rename to ConnectionManager?
    * PeerDiscovery (now in its own file): 
      sends and receives multicast packets
      useded for discovering other peers on the LAN.

    An IOException can be caused by external events,
    such as a link going down, the computer going to sleep, etc.
    These are handled by closing the socket, opening another,
    and retrying the operation.
    */

  { // ConnectionManager/Connections.

  
    // Injected instance variables, all private.
    
    private ConnectionManager.Factory theConnectionManagerFactory; 

    // Other instance variables, all private.

    ///private HashMap<SocketAddress,Peer> peerSocketAddressHashMap;
    private HashMap<SocketAddress,PeerValue> peerSocketAddressHashMap;
      /// Being converted from Peer to PeerValue.
      /* This initially empty Collection provides access to the Peers.
        It can be used for displaying the Peers.
        It can also be used to lookup Peers by SocketAddress,
        in the case of packets received on 
        unconnected sockets if remote addresses were always stored in 
        Packets (or SockPackets).

        Maybe these should be in a separate class for this??
        */

    private DatagramSocket unconnectedDatagramSocket; // For UDP io.

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
      // For received uniconnected packets.


    public ConnectionManager(   // Constructor.
        ConnectionManager.Factory theConnectionManagerFactory 
        )
      {
        ///super( "Connections" );  // Name here because setName() not reliable.

        // [some] variable initializations happen where they are declared.

        this.theConnectionManagerFactory= theConnectionManagerFactory;

        peerSocketAddressHashMap=  // Setting no peers initially.
          ///new HashMap<SocketAddress,Peer>();
          new HashMap<SocketAddress,PeerValue>();
        theLockAndSignal= new LockAndSignal(false);  // Creating signaller.
        sendPacketQueue=
          new PacketQueue(theLockAndSignal);
        multicastSignallingQueueOfSockPackets=
          new PacketQueue(theLockAndSignal);
        uniconnectedSignallingQueueOfSockPackets=
          new PacketQueue(theLockAndSignal);

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
        appLogger.info("Connections.run(): thread beginning.");
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
        appLogger.info("Connections.run(): thread ending.");  // Connections.
        } // Connections.


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

        ///theUnconnectedReceiver.start();  // Starting thread.
        theUnconnectedReceiverEpiThread.start();  // Starting thread.
        }

    private void withSocketFinalizingV()
      {
        terminatingPeerThreadsV(); // most of theUnconnectedReceiver users.

        ///theUnconnectedReceiver.stopV();  // Requesting termination of
        theUnconnectedReceiverEpiThread.stopV();  // Requesting termination of
          // theUnconnectedReceiver thread.
        unconnectedDatagramSocket.close(); // Causing immediate unblock of
          // unconnectedDatagramSocket.receive() in that thread.
        ///theUnconnectedReceiver.joinV();  // Waiting for termination of
        theUnconnectedReceiverEpiThread.joinV();  // Waiting for termination of
          // theUnconnectedReceiver thread.
        }

    private void terminatingPeerThreadsV()
      /* This method terminates all of the Peer threads 
        that were created to communicate with discovered Peer nodes.
        It does this in 2 loops: one to request termination,
        and another to wait for termination to complete; because 
        concurrent terminations are possible when there are multiple Peers.
        */
      {
        appLogger.info("Connections.terminatingPeerThreadsV() beginning.");

        Iterator<PeerValue> anIteratorOfPeerValues;  // For peer iterator.

        anIteratorOfPeerValues=  // Getting new peer iterator.
          peerSocketAddressHashMap.values().iterator();
        while  // Requesting termination of all Peers.
          (anIteratorOfPeerValues.hasNext())
          {
            PeerValue thePeerValue= anIteratorOfPeerValues.next();
            ///Peer thePeer= thePeerValue.getPeer();
            ///thePeer.stopV();  // Requesting Termination of Peer.
            thePeerValue.getEpiThread().stopV();  // Requesting Termination of Peer.
            }

        anIteratorOfPeerValues=  // Getting new peer iterator.
          peerSocketAddressHashMap.values().iterator();
        while  // Waiting for termination of all Peers.
          (anIteratorOfPeerValues.hasNext())
          {
            PeerValue thePeerValue= anIteratorOfPeerValues.next();
            ///Peer thePeer= thePeerValue.getPeer();
            ///thePeer.joinV();  // Waiting for termination of Peer.
            thePeerValue.getEpiThread().joinV();  // Waiting for termination of Peer.
            anIteratorOfPeerValues.remove(); // Removing from HashMap...
            }

        appLogger.info("Connections.terminatingPeerThreadsV() ending.");
        }

    private void processingInputsAndExecutingEventsV()
      /* This method cointains a single loop which
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
              }

            theLockAndSignal.doWaitE();  // Waiting for new inputs.
            }

        appLogger.info("Connections: termination begun.");
        }

    private void startingPeerDiscoveryV()
      // Klugy start of PeerDiscover daemon, which needs a lot of work???
      {
        try { // PeerDiscoery
          new PeerDiscovery(  // Construct PeerDiscovery...
            sendPacketQueue // ...with send queue,...
            ,multicastSignallingQueueOfSockPackets // ...receive queue,...
            ,unconnectedDatagramSocket // ...and socket to use.
            );  // PeerDiscovery will start its own thread.
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

        Channelling all outgoing packets through this code
        enables the ConnectionManager to better manage flow.
        
        ??? Though unlikely, unconnectedDatagramSocket.send(..) 
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
          if (theSockPacket == null) break;  // Exitting if no more packets.
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
        the PeerDiscovery Thread and forwarded here,
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
        It adds an entry to the Peer data structures
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
        Peer thePeer=  // Get or create Peer.
          getOrCreateAndAddPeer( theSockPacket );
        thePeer.puttingReceivedPacketV( theSockPacket );  // Why needed ???
        }

    private Peer getOrCreateAndAddPeer( SockPacket theSockPacket )
      /* Gets or creates the Peer associated with theSockPacket.
        It adds the Peer to the appropriate data structures.
        It returns the found or created Peer.
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
        Peer thePeer=  // Get or create Peer.
          getOrCreateAndAddPeer(peerInetSocketAddress);
        return thePeer;
        }

    private Peer getOrCreateAndAddPeer(
        InetSocketAddress peerInetSocketAddress
        )
      /* Gets or creates the Peer whose SocketAddress
        is peerInetSocketAddress.
        If a new Peer is created then it also:
        * Adds the Peer to the appropriate data structures.
        * Starts the Peer thread.
        It returns the found or created Peer.
        */
      {
        ///Peer thePeer;
        PeerValue thePeerValue;
        
        thePeerValue=  // Testing whether the peer already stored.
          peerSocketAddressHashMap.get(peerInetSocketAddress);
        if (thePeerValue == null) // Adding entry if not stored already.
          {
            thePeerValue= 
              theConnectionManagerFactory.buildPeerValue(
                peerInetSocketAddress,
                sendPacketQueue
                );
            peerSocketAddressHashMap.put(  // Add to HashMap with...
              peerInetSocketAddress,  // ...the address as the key and...
              thePeerValue  // ...the PeerValue as the value.
              ///thePeer  // ...the Peer object as the value.
              );
            ///thePeer= thePeerValue.getPeer();
            ///thePeer.start();  // Start the peer's associated thread.
            thePeerValue.getEpiThread().start();  // Start the peer's associated thread.
            }
        return thePeerValue.getPeer();
        }


    // Nested classes.

    public static class Root  // ConnectionManager's DataNode entry point.
      extends NamedList 
      {  // ???

        /* This class is ConnectionManager's entry in
          the Infogora DataNode hierarchy.
          */

        public Root( )
          {
            super( 
              "Connections" 
              , new NamedLeaf( "dummy child 1" )
              , new NamedLeaf( "dummy child 2" )
              , new NamedLeaf( "dummy child 3" )
              );
            }

        }

    public static class Factory {

      /* This is the factory for class lifetimes
        of the ConnectionManager or shorter.
        */

      private ConnectionManager theConnectionManager;

      public Factory( )  // Constructor.
        {
          theConnectionManager= new ConnectionManager( this );
          }

      public ConnectionManager getConnectionManager()
        { return theConnectionManager; }

      public Peer buildPeer(
          InetSocketAddress peerInetSocketAddress,
          PacketQueue sendPacketQueue
          ///ConnectionManager theConnectionManagerNOT ///
          )
        {
          return new Peer(
            peerInetSocketAddress,
            sendPacketQueue,
            theConnectionManager
            );
          }

      public PeerValue buildPeerValue(
          InetSocketAddress peerInetSocketAddress,
          PacketQueue sendPacketQueue
          )
        {
          Peer thePeer= buildPeer(
            peerInetSocketAddress,
            sendPacketQueue
            );
          return new PeerValue(
            peerInetSocketAddress,
            thePeer
            );
          }

      }

    private static class PeerValue  // Value of HashMap entry.

     /* This class was created when it became necessary to
       have access to both the Peer class and its Thread.
       */

      {

        private Peer thePeer;
        private EpiThread theEpiThread;

        public PeerValue(  // Constructor. 
            InetSocketAddress peerInetSocketAddress,
            Peer thePeer
            )
          {
            this.thePeer= thePeer;
            this.theEpiThread= new EpiThread( 
              thePeer,
              "Peer-"+peerInetSocketAddress
              );
            }
            
        public Peer getPeer()
          { 
            return thePeer; 
            }
            
        public EpiThread getEpiThread()
          { 
            return theEpiThread; 
            }
        }

    private static class Peer  // Nested class managing peer connection data.

      ///extends EpiThread
      implements Runnable
      
      ///implements Comparable<Peer> // for use by PriorityQueue.

      /* Each instance of this nested class contains and manages data about 
        one of the peer nodes of which the ConnectionManager is aware.
        Its job is to manage a single Datagram connection
        with another peer node.
        
        This class is also a Thread.
        The thread informally implements a state machine
        which manages the connection.
        
        Originally it was planned for each Peer thread to 
        send and receive packets using a connected DatagramSocket.
        Different peers would use different DatagramSocket,
        each with different remote addresses, but the same local addresses.
        Bind errors can be avoided by using 
        DatagramSocket.setReuseAddress(true).
        Unfortunately, such connected and bound DatagramSockets
        will not receive any packets if there is 
        an unonnected DatagramSocket bound to the same local address
        used for receiving the initial connection requests.
        Closing the unconnected socket allows the connected ones to work,
        but reopening the unconnected socket disables the 
        connected ones again.
        As a result, connected sockets are not used.
        Instead all packets are received by 
        an unconnected DatagramSocket, and those packets 
        are demultiplexed and forwarded to the appropriate peer thread.
        
        The plan is to add functionality to this class in small stages.
        At first peers will simply exchange packets,
        waiting half a period, making no attempt to measure ping times.
        More functionality can be added later.

        Here is the beginnings of a table representing
        a state machine for unicast connections.
        It will probably change.
        It might be changed to an HSM (HierarchicalStateMachine)
          
          StateName     Input           Next-State  Output
          -----------   ------------    ----------  ----------
          Unconnected   Receive-packet  WillReply   Pass-packet
          Replying      (none)          Ignoring    Send-reply
          Ignoring      Receive-packet  Ignoring    Pass-packet
                        Half-period     RePingable   
          RePingable    Half-period     IPing
                        Receive-packet  Replying
          IPing         (none)          GetReply    SendReply
                        Retry-period  
          GetReply

        */

      { // Peer.

        // Fields (constant and variales).
        
          // Constants.
          
          private final long PeriodMillisL=  // Period between sends or receives.
            4000;   // 4 seconds.

          private final long HalfPeriodMillisL= // Half of period.
            PeriodMillisL / 2;  


          // Instance variables which are copies of constructor arguments.

          private InetSocketAddress peerInetSocketAddress;  // Address of peer.
          
          private final PacketQueue sendQueueOfSockPackets;
            // Queue to receive SockPackets to be sent to Peer.
                
          private final ConnectionManager theConnectionManager;


          // Other instance variables.

          LockAndSignal peerLockAndSignal;  // LockAndSignal for this thread.

          int packetIDI; // Sequence number for sent packets.

          // Independent times.
          ///private long receivedMillisL;  // Time a packet was last received from peer.
          ///private long sentMillisL;  // Time a packet was last sent to peer.

          // Dependent times.
          //long roundTripTimeL;
          ///private long nextSendMillisL;  // Time the next packet should be sent.

          Random theRandom;  // For random numbers for arbitratingYieldB().

          private final PacketQueue receiveQueueOfSockPackets;
            // Queue for SockPackets from ConnectionManager.


        public Peer(  // Constructor. 
            InetSocketAddress peerInetSocketAddress,
            PacketQueue sendQueueOfSockPackets,
            ConnectionManager theConnectionManager
            )
          /* This constructor constructs a Peer.
            It is constructed assuming that 
            a packet has just been received from it
            at peerInetSocketAddress,
            but no response has yet been made.
            Fields are defined in a way to cause an initial response.
            */
          {
            ///super(  // Naming thread here because setName() is not reliable.
            ///  peerInetSocketAddress+"-Manager"  // ???
            ///  );

            // Storing constructor arguments.
              this.peerInetSocketAddress= peerInetSocketAddress;
              this.sendQueueOfSockPackets= sendQueueOfSockPackets;
              this.theConnectionManager= theConnectionManager;

            peerLockAndSignal= new LockAndSignal(false);

            packetIDI= 0; // Setting starting packet sequence number.

            // Setting meanful defaults for packet send and receive times.
              ///this.receivedMillisL=  // Assume packet received recently.
              ///  System.currentTimeMillis();
                // It might be better to get this from a previously saved time.
              ///this.sentMillisL=  // But not sent for a long time.
              ///  receivedMillisL - PeriodMillisL;
              ///updateNextSendV( );  // Updating dependent time.
            
            theRandom= new Random(0);  // Initialize arbitratingYieldB().

            receiveQueueOfSockPackets=
              new PacketQueue( peerLockAndSignal );
                // For SockPackets from ConnectionManager.
            }


        public void run()  // Main Peer thread.
          /* This method contains the main thread logic in the form of
            a state machine composed of the highest level states.
            The only thing it does now is exchange ping and echo packets
            with the remote peer, first one way, then the other.
            
            ??? This is only temporary because 
            packets which are not ping or echo are ignored,
            so a connection can do nothing else.
            Later the protocol will be expanded by adding
            more types of packets, or 
            packets will be demultiplexed by protocol.
            In this case Packets would contain 
            a protocol value for this purpose.
            */
          {
            appLogger.info(
              Thread.currentThread().getName()+": run() beginning."
              );

            while (true) // Repeating until thread termination is requested.
              {
                if   // Exiting if requested.
                  ( Thread.currentThread().isInterrupted() ) 
                  break;
                //appLogger.info(getName()+":\n  CALLING tryingPingSendV() ===============.");
                tryingPingSendV();

                if   // Exiting if requested.
                  ( Thread.currentThread().isInterrupted() ) 
                  break;
                //appLogger.info(getName()+":\n  CALLING tryingPingReceiveV() ===============.");
                tryingPingReceiveV();
                }

            // Terminating.
            appLogger.info(
              Thread.currentThread().getName()+": run() ending."
              );
            }

        private void tryingPingSendV()
          /* This method tries to send a ping to the remote peer
            and receive an echo response.
            If it doesn't receive an echo packet in response
            within one-half period, it tries again.
            It tries several times before giving up and 
            terminating the current threat.
            */
          {
            int maxRetries= 4;
            LockAndSignal.Input theInput;  // Type of input that ends waits.
            retryLoop: for // Sending pings until something stops us.
              ( int retriesI= 0; ; retriesI++ ) // Retry counter.
              { // Trying ping send and echo receive one time, or exiting.
                if  // Checking and exiting if retries were exceeded.
                  ( retriesI >= maxRetries )  // Maximum attempts exceeded.
                  { // Terminating thread.
                    Thread.currentThread().interrupt(); break; 
                    }
                //long pingMillisL= System.currentTimeMillis();
                sendingPacketV("PING"); // Sending ping packet.
                long waitMillisL=  // Calculating half-period wait time.
                  System.currentTimeMillis()+HalfPeriodMillisL;
                waitLoop: while (true) { // Flushing for pause duration.
                  theInput= // Awaiting next input and storing its type.
                    peerLockAndSignal.doWaitUntilE( // Awaiting input or...
                      waitMillisL  // ...maximum wait time.
                      );
                  switch ( theInput ) {  // Handling the input type.
                    case INTERRUPTION: // Handling a thread's interruption.
                      break retryLoop; // Exiting to terminate thread.
                    case TIME: // Handling a time-out.
                      break waitLoop;  // Exiting wait loop to retry.
                    case NOTIFICATION:  // Handling packet inputs.
                      while (true) { // Handling possible multiple packets.
                        if ( ! testingPacketB( ) ) // Handling empty queue.
                          break;  // Exiting loop.
                        if // Handling echo packet, maybe.
                          ( testingPacketB( "ECHO" ) )
                          { // Handling echo and exiting.
                            //long echoMillisL= System.currentTimeMillis();
                            //roundTripTimeL= echoMillisL - pingMillisL;
                            //appLogger.debug("RTT= "+roundTripTimeL);
                            consumingOnePacketV(); // Consuming echo packet.
                            // This should handle unwanted received packets.
                            break retryLoop; // Finishing by exiting loop.
                            }
                        if // Handling ping-ping conflict, maybe.
                          ( testingPacketB( "PING" ) )
                          if // Handling ping-ping conflict.
                            ( arbitratingYieldB() ) // Let arbiter decide.
                            { // Yielding ping processing to other peer.
                              //appLogger.info(
                              //  getName()+":\n  PING ping abort: "+retriesI
                              //  );
                              break retryLoop; // Yielding by exiting loop.
                              // Ping packet remains in queue.
                              }
                        ignoringOnePacketV(); // Ignoring any other packet value.
                        //appLogger.info(getName()+":\n  echo NO-receive: "+retriesI);
                        }
                      // Dropping through to exit switch and continue waitLoop.
                      }
                  }
                }
            }

        private void tryingPingReceiveV()
          /* This method tries to receive a ping from the remote peer
            to which it replies by sending an echo response.
            It waits up to PeriodMillisL for a ping to arrive,
            after which it gives up and returns.
            If a ping is received it responds immediately by sending an echo,
            after which it waits PeriodMillisL while ignoring all
            received packets.
            */
          {
            LockAndSignal.Input theInput;  // Type of input that ends wait.
            long pingMillisL=  // Calculating latest ping receive time.
              System.currentTimeMillis()
              + PeriodMillisL
              + HalfPeriodMillisL;
            pingLoop: while (true) { // Inputing until one is acceptable.
              if // Handling a received ping if present.
                ( testingPacketB( "PING" ) )
                { // Handling received ping.
                  consumingOnePacketV(); // Consuming ping packet.
                  sendingPacketV("ECHO"); // Sending echo packet.
                  long pauseMillisL=  // Calculating end of post-echo pause.
                    System.currentTimeMillis()+PeriodMillisL;
                  while (true) { // Ignoring packets for pause duration.
                    theInput= // Awaiting next input and storing its type.
                      peerLockAndSignal.doWaitUntilE( pauseMillisL );
                      switch ( theInput ) {  // Handling input type.
                        case INTERRUPTION: // Handling thread interruption.
                          break pingLoop;  // Exiting outer loop.
                        case TIME: // Handling end of post-echo pause.
                          break pingLoop;  // Exiting outer loop.
                        case NOTIFICATION:  // Handling input packet(s).
                          while (true) { // Ignoring all input packets.
                            if ( ! testingPacketB( ) ) // Handling none left.
                              break;  // Exiting loop.
                            ignoringOnePacketV(); // Flushing one packet.
                            }
                        }
                    }
                  }
              ignoringOnePacketV(); // Ignoring non-ping packet.
              //appLogger.info(getName()+":\n  ignoring non-ping.");
              theInput= // Awaiting next input and storing its type.
                peerLockAndSignal.doWaitUntilE( // Awaiting input or...
                  pingMillisL  // ...ping time limit.
                  );
              switch ( theInput ) {  // Handling input based on type.
                case INTERRUPTION: // Handling a thread's interruption.
                  break pingLoop;  // Exiting to terminate thread.
                case TIME: // Handling the ping time-out.
                  break pingLoop;  // Exiting to abort wait.
                case NOTIFICATION:  // Handling a received packet.
                  // Dropping through to exit switch, loop and retry.
                }
              }
            }

        private boolean arbitratingYieldB()
          /* This method arbitrates when this local peer is trying
            to do the same thing as the remote peer.
            When executed on both peers, on one it should return true
            and on the other it should return false.
            It returns true when this peer should yield,
            and false when it should not.

            This method is not designed to be fair, but to 
            simply resolve conflicts.
            It is based on comparing each peer's port number.
            Most of the time, when one peer yields, 
            the other won't, and the conflict is resolved.
            Where the port numbers are equal 
            the yield result is chosen randomly,
            and the conflict is resolved 50% of the time.
            */
          {
            boolean yieldB;  // Storage for result.
            int portDifferenceI=  // Calculate port difference.
              peerInetSocketAddress.getPort() - PortManager.getLocalPortI();
            if ( portDifferenceI != 0 )  // Handling ports unequal.
              yieldB= ( portDifferenceI < 0 );  // Lower ported peer yields.
              else  // Handling rare case of ports equal.
              {
                theRandom.setSeed(System.currentTimeMillis());  // Reseting...
                  // ...the random number generator seed with current time.
                yieldB= theRandom.nextBoolean();  // Yield randomly.
                }
            //appLogger.info("arbitratingYieldB() = " + yieldB);
            return yieldB;
            }

        /* ???
        private long getNextSendL( )  // Returns time of next action.
          /* This method returns 
            the next time the user node is scheduled to do something,
            specifically to send a packet to the peer.
            */
          /* ???
          {
            return nextSendMillisL;
            }
          */

        /* ???
        private void updateNextSendV( )
          /* This method calculates and stores 
            the next time the user node is scheduled to do something,
            specifically to send a packet to the associated peer.
            This time depends on who transmitted last.
            It also updates the position in the priority queue.
            
            Previously the values used were calculated to cause 
            this node and the peer node 
            to alternate pinging and replying.
            */
          /* ???
          {
            nextSendMillisL=  // Set next send time to be...
              Wrap.laterL(  // ...the later of...
                ( sentMillisL  // ...the last send time...
                  + PeriodMillisL  // ...plus the period...
                  ),  // ...or...
                ( receivedMillisL  // ...the last receive time...
                  + HalfPeriodMillisL  // ...plus half the period.
                  )
                ) 
              ;
            }
          */

        /* ???
        public int compareTo( Peer anotherPeer )
          /* This method compares this Peer to anotherPeer.
            It returns the difference between their next event times.
            This method exists so that PriorityQueue can compare them.
            */
          /* ???
          {
            return 
              (int)
              ( getNextSendL( ) - anotherPeer.getNextSendL( ) )
              ;
            }
          */


        // Receive packet code.

        public void puttingReceivedPacketV( SockPacket theSockPacket )
          // This method adds theSockPacket to the peer's receive queue.
          {
            receiveQueueOfSockPackets.add(theSockPacket);
            }

        private void consumingOnePacketV()
          /* This method consumes one packet, if any,
            at the head of the queue.
            */
          {
            receiveQueueOfSockPackets.poll();  // Consuming the packet,..
              // ...if there was one at head of queue.
            }

        private void ignoringOnePacketV()
          /* This method logs that the present one packet, if any,
            at the head of the queue, is being ignored,
            and then consumes it.
            */
          {
            testingPacketB( "(ignoring)" );  // Log that its ignored, sor of.
            consumingOnePacketV();  // So it won't be seen again.
            }

        private boolean testingPacketB( )
          /* This method tests whether a packet, if any,
            at the head of the receiveQueueOfSockPackets,
            is available.
            It returns true if there is a packet available, false otherwise.
            */
          {
            return ( receiveQueueOfSockPackets.peek() != null );
            }

        private boolean testingPacketB( String aString )
          /* This method tests whether the packet, if any,
            at the head of the receiveQueueOfSockPackets,
            contains aString.
            It returns true if there is a packet and
            it is aString, false otherwise.
            The packet, if any, remains in the queue.
            */
          {
            boolean resultB= false;  // Assuming aString is not present.
            decodingPacket: {
              SockPacket receivedSockPacket= // Testing queue for a packet.
                receiveQueueOfSockPackets.peek();
              if (receivedSockPacket == null) // Handling no packet.
                {
                  //appLogger.info("testingPacketB() no packet is not "+aString);
                  break decodingPacket;  // Exiting with false.
                  }
              DatagramPacket theDatagramPacket= // Getting DatagramPacket.
                receivedSockPacket.getDatagramPacket();
              String dataString=  // Creating String from packet data.
                new String(
                  theDatagramPacket.getData()
                  ,theDatagramPacket.getOffset()
                  ,theDatagramPacket.getLength()
                  );
              if   // Handling unequal Strings.
                ( ! dataString.contains( aString ) )
                {
                  //appLogger.info(
                  //  "testingPacketB() packet "+dataString+" is NOT "+aString
                  //  );
                  break decodingPacket;  // Exiting with false.
                  }
              //appLogger.info(
              //  "testingPacketB() packet "+dataString+" IS "+aString
              //  );
              resultB= true;  // Changing result because Strings are equal.
              }
            return resultB;  // Returning the result.
            }

        private void sendingPacketV( String aString )
          /* This method sends a packet containing aString to the peer.
            */
          {
            String payloadString= ((packetIDI++) + ":" + aString);
            //appLogger.info( "sendingPacketV(): " + payloadString );
            byte[] buf = payloadString.getBytes();
            DatagramPacket packet = new DatagramPacket(
              buf, 
              buf.length, 
              peerInetSocketAddress.getAddress(),
              peerInetSocketAddress.getPort()
              );
            SockPacket aSockPacket=
              new SockPacket( 
                theConnectionManager.unconnectedDatagramSocket, 
                packet 
                );
            sendQueueOfSockPackets.add( // Queuing packet for sending.
              aSockPacket
              );
            }

        } // Peer.

    private static class PacketQueue extends SignallingQueue<SockPacket>
      {

        PacketQueue( LockAndSignal aLockAndSignal )  // Constructor.
          {
            super( aLockAndSignal );
            }

        }

    private class UnconnectedReceiver // Unconnected-unicast receiver.

      ///extends EpiThread // Doesn't need to be separate Thread and Runnable.
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
            ///super(  // Name thread here because setName() is not reliable.
            ///  "OLDUnconnectedReceiver"  
            ///  );
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
            appLogger.threadInfo("run() beginning.");
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
                    receiverDatagramSocket.receive(aDatagramPacket);
                    receiverSignallingQueueOfSockPackets.add(aSockPacket);
                    }
                  catch( SocketException soe ) {
                    // someone may have called disconnect()
                    appLogger.threadInfo("run() SocketException: " + soe );
                    Thread.currentThread().interrupt(); // Translating 
                      // exception into request to terminate this thread.
                    }
                  } // Receiving and queuing one packet.
              }
              catch( IOException e ) {
                appLogger.threadInfo("run() IOException: "+e );
                throw new RuntimeException(e);
              }
            appLogger.threadInfo("run() ending.");
            }

        }
      ; // UnconnectedReceiver

    /* ???
    private static class Wrap  // Cyclic/wrap-around operations on longs.
      /* This class is provides some utility methods which
        which perform some wrap-around operations on long integers.
        These operations were created for operating on 
        long times expressed in milliseconds,
        but they can be used in other applications also.
        */
      /* ???
      {
        private static boolean isBeforeB( long x1L, long x2L )
          /* Returns true if x1L is before x2L in the cycle.
            Returns false otherwise.
            It treats times as wrap-around long values
            by subtracting them and comparing the difference to 0.
            */
          /* ???
          {
            return ( ( x1L - x2L ) < 0 );
            }

        private static long laterL( long x1L, long x2L )
          /* Returns the later of times x1L and x2L.  
            It treats times as wrap-around long values.
            */
          /* ???
          {
            return ( isBeforeB( x1L, x2L ) ? x2L : x1L ) ;
            }

        }
        */

    }
