package allClasses;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

import static allClasses.Globals.*;  // appLogger;

public class ConnectionManager extends Thread 

  /* This class makes and maintains simple UDP unicast connections based on 
    input from various other Threads.

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
      used for discovering other peers on the LAN.

    An IOException can be caused by external events,
    such as a link going down, the computer going to sleep, etc.
    These are handled by closing the socket, opening another,
    and retrying the operation.
    */

  { // ConnectionManager/Connections.
  
    // Variables.

      private boolean TerminatingB= false;

      /* Peer access variables, initialized to empty Collections.
        These Collections provide different types of access to the Peers.
        These structures must be maintained in parallel.

        Maybe these should be in a separate class for them??
        */

        private HashMap<SocketAddress,Peer> peerSocketAddressHashMap=
          new HashMap<SocketAddress,Peer>(); // For access by SocketAddress.
          /* This doesn't change much.
            It is used to lookup Peers by SocketAddress.
            It would be needed for only packets received on unconnected sockets
            if remote addresses were always stored in Packets (or SockPackets).
            */

        private PriorityQueue<Peer> peersPriorityQueueOf=
          new PriorityQueue<Peer>(); // For access by next event time.
          /* This changes a lot because:
            * An entry is removed every time real-time reaches
              the next keep-alive or transmit-time-out time.
              Removals are usually followed immediately by
              insertions at a high priority.
              These could be replaced by in-heat position rotation.
              need not be done at all.
            priority changes a lot.
            This data structure might need to be replaced by 
            something customized for priority changes.
            */

      protected static DatagramSocket unconnectedDatagramSocket= // UDP io.
        null;

      private static UnconnectedReceiver theUnconnectedReceiver;  // Receiver thread.

    public ConnectionManager( )  // Constructor.
      throws IOException
      {
        super( "Connections" );  // Name here because setName() not reliable.

        // Variable initializations happen where they are declared.

        }

    public void run()  // Main ConnectionManager thread logic.
      /* This method creates an unconnected DatagramSocket 
        for it use and uses it.
        This is inside of a try-catch block in a loop
        to recover and retry after SocketException-s.
        The name of the thread should be Connections.
        */
      { // Connections.
        appLogger.info("Connections.run(): thread beginning.");

        while  // Repeating until termination is requested.
          ( !TerminatingB )  // Termination is not requested.
          {
            try { // Creating and using unconnected DatagramSocket.
              unconnectedDatagramSocket= // Construct socket for UDP io.
                //BoundUDPSockets.getDatagramSocket();
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
              // Doing nothing because we will retry.
              appLogger.info("Connections.run(): SocketException:"+e);
              }
            finally { // Closing socket if it exists.
              if ( unconnectedDatagramSocket != null ) {
                appLogger.info("Connections.run(): closing socket.");
                unconnectedDatagramSocket.close();
                }
              }
            }

        appLogger.info("Connections.run(): thread ending.");  // Connections.
        } // Connections.

    private void settingThreadsAndDoingWorkV()
      throws SocketException
      /* In this method, the DatagramSocket is assumed to be initialized.
        After creating and starting some threads, 
        it processes various inputs and pending events until termination.
        Then it terminates those same threads and returns.
        A return might also result from the throwing of a SocketException.
        */
      {
        // Initializing.
          startingPeerDiscoveryV();  // (This daemon will terminate itself.)
          theUnconnectedReceiver=  // Constructing thread.
            new UnconnectedReceiver( 
              unconnectedDatagramSocket,
              uniconnectedSignallingQueueOfSockPackets
              );
          theUnconnectedReceiver.start();  // Starting thread.

        processingInputsAndExecutingEventsV(); // Until thread termination...
          // ...is requested.

        appLogger.info("Connections: termination begun.");

        // Finalizing.
          terminatingPeerThreadsV();
          appLogger.info(
            "Connections: doing UnconnectedReceiver.interrupt()."
            );
          theUnconnectedReceiver.interrupt();  // Requesting termination...
            // ..of theUnconnectedReceiver.
          unconnectedDatagramSocket.close(); // Causing receive() to end.

          for  // Waiting for termination of theUnconnectedReceiver to finish.
            ( boolean threadTerminatedB= false ; !threadTerminatedB ; )
            try { // Waiting for theUnconnectedReceiver thread to terminate.
                appLogger.info(
                  "Connections: blocking until UnconnectedReceiver terminateds."
                  );
                theUnconnectedReceiver.join();  // Blocking until terminated.
                threadTerminatedB= true;  // Recording termination complete.
                } 
              catch (InterruptedException e) {  // Handling interrupt().
                appLogger.info(
                  "Connections: settingThreadsAndDoingWorkV() InterruptedException."
                  );
                TerminatingB= true;  // Converting interrupt() to flag.
                }
        }

    private void terminatingPeerThreadsV()
      /* This method terminates all of the threads 
        that were created to communicate with discovered Peer nodes.
        */
      {
        appLogger.info("Connections.terminatingPeerThreadsV() beginning.");

        Iterator<Peer> anIteratorOfPeers=  // Creating peer iterator.
          peerSocketAddressHashMap.values().iterator();
        while  // Terminating all Peers in peerSocketAddressHashMap.
          (anIteratorOfPeers.hasNext())
          { // Terminating one Peer.
            Peer thePeer = anIteratorOfPeers.next();  // Getting Peer.
            thePeer.interrupt();  // Requesting termination of Peer thread.
            for   // Waiting for termination of Peer thread to complete.
              ( boolean peerTerminatedB= false ; !peerTerminatedB ; )
              try { // Waiting for Peer thread to terminate.
                  thePeer.join();  // Blocking until Peer thread terminates.
                  peerTerminatedB= true;  // Recording termination complete.
                  } 
                catch (InterruptedException e) {  // Handling interrupt().
                  TerminatingB= true;  // Converting to termination flag.
                  }
            anIteratorOfPeers.remove(); // Removing from HashMap...
            peersPriorityQueueOf.remove(  // Removing from event queue.
              thePeer
              );
            }

        appLogger.info("Connections.terminatingPeerThreadsV() ending.");
        }

    private void processingInputsAndExecutingEventsV()
      /* This method cointains a single loop which
        does various types of work.  The work consists of:
          * Processing inputs it get from several queues.
          * Processing scheduled jobs whose times have come.
        When there is no more work, the loop blocks until 
        the next bit of work arrives, when it repeats the process.
        The loop continues until the thread termination is requested
        by the condition set by Thread.currentThread().interrupt()
        and read by isInterrupted().
        This condition is not cleared by this method.
        */
      {
        while  // Repeating until thread termination is requested.
          ( !isInterrupted() )  // Thread termination is not requested.
          {
            // Trying to do work.
              processingUnconnectedSockPacketsB();
              if ( processingSockPacketsToSendB() )
                continue;  // Loop if any SockPackets were sent.
              processingDiscoverySockPacketsB();  // Last because these are rare.
              
            // Waiting until next scheduled event or new input.
              long earliestJobMillisL=  // Getting time of next scheduled job.
                getNextScheduledJobMillisL();
              theLockAndSignal.doWaitUntilV(  // Waiting for next input or...
                earliestJobMillisL // ...the next scheduled job.
                );
            }
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

    /* Common thread synchronization and internal timer code.
      This code is used to coordinate 
      the input and processing of data from other Threads.
      */

      LockAndSignal theLockAndSignal=  // LockAndSignal for this thread.
        new LockAndSignal(false);  /* This is used to synchronize
          communication between the ConnectionManager and 
          all threads providing data to it.
          */
        // The old way used a separate Object lock and a boolean signal.

      // void doWait(long nextJobMillisL) was moved to class LockAndSignal.

      // void doNotify() was moved to class LockAndSignal.
      
    /* Code for processing packets to be sent.

      Channelling all outgoing packets through this code
      enables the ConnectionManager to better manage flow.
      
      ??? Though unlikely, unconnectedDatagramSocket.send(..) 
      could block the thread.
      To avoid this, maybe a DatagramChannel could be used,
      or sending could be done in a separate thread.
      */

      private PacketQueue // Queue of packets to be sent.
        sendPacketQueue=
          new PacketQueue(theLockAndSignal);

      private boolean processingSockPacketsToSendB()
        /* This method processes packets stored in the send queue.
          Packets to be sent arrive via the sendPacketQueue
          to enable the ConnectionManager to track all network i/o.
          This method returns true if at least one packet was sent,
          false otherwise.
          */
        {
          boolean packetsProcessedB= false;  // Assuming no packet to do.
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

    // Code for processing unconnected multicast packets from PeerDiscovery.

      private PacketQueue // Discovery packets queue.
        multicastSignallingQueueOfSockPackets=
          new PacketQueue(theLockAndSignal);

      private boolean processingDiscoverySockPacketsB() 
        /* This method processes packets received by 
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

    // Code for processing unconnected unicast packets from UnconnectedReceiver.

      private PacketQueue // For received uniconnected packets.
        uniconnectedSignallingQueueOfSockPackets=
          //new ConcurrentLinkedQueue<SockPacket>();
          new PacketQueue(theLockAndSignal);

      private boolean processingUnconnectedSockPacketsB()
        /* This method processes packets that are received by 
          the uniconnected packet receiver Thread and forwarded here,
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
        thePeer.putReceivedPacketV( theSockPacket );  // Why needed ???
        thePeer.setReceivedV(  // Set last time received to be...
          System.currentTimeMillis()  // ...the present time.
          );  // This will setup the next send.
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
        It adds the Peer to the appropriate data structures.
        It returns the found or created Peer.
        */
      {
        Peer thePeer=  // Testing whether the peer already stored.
          peerSocketAddressHashMap.get(peerInetSocketAddress);
        if (thePeer == null) // Adding entry if not stored already.
          {
            thePeer= new Peer( // Creating new Peer object with...
              peerInetSocketAddress,  // ...this address.
              sendPacketQueue,
              peersPriorityQueueOf
              );
            //appLogger.info(
            //  "ConnectionManager.getOrCreateAndAddPeer(..)\n"+
            //    "  adding peer at "+peerInetSocketAddress
            //  );
            peerSocketAddressHashMap.put(  // Add to HashMap with...
              peerInetSocketAddress,  // ...the address as the key and...
              thePeer  // ...the Peer object as the value.
              );
            peersPriorityQueueOf.add(  // Add to event queue.
              thePeer
              );
            thePeer.start();  // Start the peer's associated thread.
            }
        return thePeer;
        }

    private long getNextScheduledJobMillisL()
      /* This method returns the time of the next scheduled send job,
        or a time in the most distant future if there is no such job.
        */
      {
        long earliestJobMillisL; // Storage for return value.
        Peer nextPeer=
          peersPriorityQueueOf.peek();
        if ( nextPeer == null )  // No jobs queued.
          {
            //appLogger.info(
            //  "ConnectionManager.getNextScheduledJobMillisL()), no Peer."
            //  );
            earliestJobMillisL=  // Set result to be unreachable value,...
              System.currentTimeMillis()
              + Integer.MAX_VALUE  // ...a time in the most distant future.
              ;
            }
          else // There is at least one job.
          {
            earliestJobMillisL=  // Set result to be...
              nextPeer.nextSendMillisL;  // ...job's next send time.
            }
        return earliestJobMillisL;  // Return result.
        }

    public static class Peer  // Nested class managing peer connection data.
      extends Thread
      implements Comparable<Peer> // for use PriorityQueue.

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

        public Peer(  // Constructor. 
            //DatagramSocket peerDatagramSocket,
            InetSocketAddress peerInetSocketAddress,
            PacketQueue sendQueueOfSockPackets,
            PriorityQueue<Peer> peersPriorityQueueOf
            )
          /* This constructor constructs a Peer assuming that
            a packet has just been received from the peer at peerInetSocketAddress,
            but no response has yet been made.
            Fields are defined in a way to cause an initial response.
            */
          {
            super(  // Naming thread here because setName() is not reliable.
              peerInetSocketAddress+"-Manager" 
              );
            // Storing constructor arguments.
              this.peerInetSocketAddress= peerInetSocketAddress;
              this.sendQueueOfSockPackets= sendQueueOfSockPackets;
              this.peersPriorityQueueOf= peersPriorityQueueOf;

            // Setting meanful defaults for packet send and receive times.
              this.receivedMillisL=  // Assume packet received recently.
                System.currentTimeMillis();
                // It might be better to get this from a previously saved time.
              this.sentMillisL=  // But not sent for a long time.
                receivedMillisL - PeriodMillisL;
            updateNextSendV( );  // Updating dependent time.
            }

        public void run()  // Main Peer thread.
          /* This method contains the main thread logic in the form of
            a state machine composed of the highest level states.
            It alternates between pinging and echoing with the peer.
            */
          {
            appLogger.info(getName()+": Peer thread beginning.");

            while  // Repeating until thread termination is requested.
              ( !isInterrupted() )  // Thread termination is not requested.
              {
                if // Processing ping receive or no-receive.
                  ( testAndConsumePacketB( "PING" ) )
                  //  ( receiveQueueOfSockPackets.poll() != null )
                  { // Processing ping receive.
                    appLogger.info(getName()+": ping receive.");
                    sendPacketV("ECHO"); // Sending echo packet.
                    peerLockAndSignal.doWaitUntilV( // Awaiting ping or...
                      System.currentTimeMillis()  // ... time-out.
                        +PeriodMillisL
                      );
                    appLogger.info(getName()+": after echo send wait.");
                    }
                  else { // Processing ping no-receive...
                    appLogger.info(getName()+": ping NO-receive.");
                    }  // ...by doing nothing.  We will ping send next.

                for // Trying ping send and echo receive up to 3 times.
                  ( int retriesI= 0; ; retriesI++ ) 
                  { // Trying ping send and echo receive one time.
                    if  // Checking and handling retries exceeded.
                      ( retriesI >= 4 )
                      { interrupt(); break; } // Terminating, exceeded.
                    sendPacketV("PING"); // Sending ping packet.
                    LockAndSignal.Cause theCause= // Waiting next input.
                      peerLockAndSignal.doWaitUntilV( // Awaiting echo or...
                        System.currentTimeMillis()  // ... time-out.
                          +PeriodMillisL
                        );
                    switch ( theCause ) {
                      case NOTIFICATION:
                      default:
                      }
                    if // Testing ping receive or no-receive.
                      ( testPacketB( "PING" ) )
                      { appLogger.info(getName()+": PING ping abort: "+retriesI);
                        break; // Processing ping receive by exiting loop.
                        }
                    if // Testing and handling echo receive or no-receive.
                      ( testAndConsumePacketB( "ECHO" ) )
                      { appLogger.info(getName()+": echo receive: "+retriesI);
                        peerLockAndSignal.doWaitUntilV( // Post-echo pauseing.
                          System.currentTimeMillis()
                            +PeriodMillisL
                            );
                        break; // Finishing echo receive by exiting loop.
                        }
                    appLogger.info(getName()+": echo NO-receive: "+retriesI);
                    }
                }

            // Terminating.
            appLogger.info(getName()+": Peer thread ending.");
            }

        private long getNextSendL( )  // Returns time of next action.
          /* This method returns 
            the next time the user node is scheduled to do something,
            specifically to send a packet to the peer.
            */
          {
            return nextSendMillisL;
            }

        private void setReceivedV( long timeMillisL )
          /* This method sets the receivedMillisL field to be timeMillisL,
            and then updates the dependent field,
            the time of the next send job.
            */
          {
            receivedMillisL= timeMillisL;  // Store time provided.
            updatePingV( );  // Update round-trip time.
            updateNextSendV( );  // Update dependend field.
            }

        private void updatePingV( )
          /* This method calculates and stores 
            the round-trip time needed for a packet
            sent to the Peer to get there and a response packet
            to return.
            */
          {
            long differenceMillisL=
              receivedMillisL - sentMillisL;
            if ( differenceMillisL > 0 )  // If it's posotive...
              pingMillisL= differenceMillisL;  // Save it.
            }

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
            updatePriorityQueueWithV(this); // Adjust heap position.
            }

        public int compareTo( Peer anotherPeer )
          /* This method compares this Peer 
            to anotherPeer.
            It returns the difference between their next event times.
            This method exists so that PriorityQueue can compare them.
            */
          {
            return 
              (int)
              ( getNextSendL( ) - anotherPeer.getNextSendL( ) )
              ;
            }

        private void updatePriorityQueueWithV(Peer thePeer)
          /* This helper method adjusts a Peer entry position
            in the PriorityQueue based on its next send packet time.
            
            ??? This uses the PriorityQueue.remove() method, but this is not 
            an efficient way or data structure for changing 
            the times/priorities of scheduled events.
            Execution time is linear (remove(Object); add(Object)).
            A faster implementation will be needed when
            the number of peers grows larger.
            There are several options:
            ? Use PriorityHeap with access to heap pointer/index.
              Might need to create a special PriorityHeap which:
              = Can return the final index of an element so that
                it can be stored in the element.
              = Add an operation to change the priority(time) 
                of an entry with a particular index and
                SiftUp or SiftDown until the position
                is okay for the new priority.
            ? Use some other data structure.
            */
          { 
            peersPriorityQueueOf.remove(  // Delete from queue if present.
              thePeer
              );  // This is inefficient for large queues.  Fix later.  ???
            peersPriorityQueueOf.add(  // Add again in correct position.
              thePeer
              );
            }

        // Variables.

          // Constructor arguments.
            
            private InetSocketAddress peerInetSocketAddress= null;  // Address of peer.
            
            private final PacketQueue // Send output.
              sendQueueOfSockPackets;  // SockPackets for ConnectionManager to send.
              
            PriorityQueue<Peer> peersPriorityQueueOf;

          LockAndSignal peerLockAndSignal=  // LockAndSignal for this thread.
            new LockAndSignal(false);

          DatagramSocket peerDatagramSocket= null;

          private final long PeriodMillisL=  // Period between sends or receives.
            4000; 
          private final long HalfPeriodMillisL= // Half of that.
            PeriodMillisL / 2;  

          // Independent times.
          private long receivedMillisL;  // Time a packet was last received from peer.
          private long sentMillisL;  // Time a packet was last sent to peer.

          // Dependent times.
          private long nextSendMillisL;  // Time the next packet should be sent.
          @SuppressWarnings("unused")
          private long pingMillisL;  // Time for a round-trip to peer.

        // Receive packet code.

          private final PacketQueue // Receive output.
            receiveQueueOfSockPackets= // SockPackets from ConnectionManager.
              new PacketQueue(
                peerLockAndSignal
                );

          public void putReceivedPacketV( SockPacket theSockPacket )
            // This method adds theSockPacket to the peer's receive queue.
            {
              receiveQueueOfSockPackets.add(theSockPacket);
              }

        private boolean testAndConsumePacketB( String aString )
          /* This method tests whether the next packet 
            in the receiveQueueOfSockPackets contains aString.
            It returns true if there is a next packet and
            it is aString, false otherwise.
            The packet, if any, is consumed.
            */
          {
            boolean resultB=  // Testing packet for desired String.
              testPacketB( aString );
            receiveQueueOfSockPackets.poll();  // Consuming the packet,..
              // ...if there was one.
            return resultB;  // Returning the result.
            }

        private boolean testPacketB( String aString )
          /* This method tests whether the next packet 
            in the receiveQueueOfSockPackets contains aString.
            It returns true if there is a next packet and
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
                  appLogger.info("testPacketB() no packet is not "+aString);
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
                ( ! aString.equals( dataString ) )
                {
                  appLogger.info(
                    "testPacketB() packet "+dataString+" is not "+aString
                    );
                  break decodingPacket;  // Exiting with false.
                  }
              resultB= true;  // Changing result to true because Strings are equal.
              }
            return resultB;  // Returning the result.
            }

        private void sendPacketV( String aString )
          /* This method sends a packet containing aString to the peer.
            */
          {
            appLogger.info( "sendPacketV(): " + aString );
            byte[] buf = aString.getBytes();
            DatagramPacket packet = new DatagramPacket(
              buf, 
              buf.length, 
              peerInetSocketAddress.getAddress(),
              peerInetSocketAddress.getPort()
              );
            SockPacket aSockPacket=
              new SockPacket( unconnectedDatagramSocket, packet );
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
      extends Thread 
      /* This local thread receives unicast DatagramPackets
        from a unconnected DatagramSocket.
        The packets are from peers to which connections 
        have not [yet] been made.
        They are passed out through a SignallingQueue.
        */

      {
        UnconnectedReceiver( // Constructor. 
            DatagramSocket receiverDatagramSocket,
            PacketQueue 
              receiverSignallingQueueOfSockPackets
            )
          /* Constructs an instance of this class from:
              * receiverDatagramSocket: the socket receiving packets.
              * receiverSignallingQueueOfSockPackets: the output queue.
            */
          { 
            super(  // Name thread here because setName() is not reliable.
              "UnconnectedReceiver"  
              );
            this.receiverSignallingQueueOfSockPackets=
              receiverSignallingQueueOfSockPackets;
            this.receiverDatagramSocket=
              receiverDatagramSocket;
            }
        
        @Override
        public void run() 
          /* This method continuously reads DatagramPackets and
            queues them for output.
            */
          {
            appLogger.info("UnconnectedReceiver: thread beginning.");

            try { // Operations that might produce an IOException.

              while ( ! interrupted() )
                { // Receive uniconnected packets.
                  try {
                    byte[] buf = new byte[256];  // Construct packet buffer.

                    // receive.
                    DatagramPacket aDatagramPacket=   // Construct packet.
                      new DatagramPacket(buf, buf.length);
                    SockPacket aSockPacket= new SockPacket(
                      receiverDatagramSocket, aDatagramPacket
                      );
                    //appLogger.info(
                    //  "UnconnectedReceiver.run() "
                    //  +"waiting to receive unconnected packet:\n  "
                    //  + aSockPacket.getSocketAddressesString()
                    //  );
                    receiverDatagramSocket.receive(aDatagramPacket);
                    //appLogger.info(
                    //  "UnconnectedReceiver.run() "
                    //  +"received unconnected packet:\n  "
                    //  + aSockPacket.getSocketAddressesString()
                    //  );
                    receiverSignallingQueueOfSockPackets.add(aSockPacket);
                    }
                  catch( SocketException soe ) {
                    // someone may have called disconnect()
                    appLogger.info( "UnconnectedReceiver.run() SocketException: " + soe );
                    interrupt();  // Terminate this thread.
                    }
                  } // Receive uniconnected packets.

              }
              catch( IOException e ) {
                appLogger.info( "UnconnectedReceiver.run() IOException: "+e );
                throw new RuntimeException(e);
              }
            appLogger.info("UnconnectedReceiver: thread ending.");
            }

        private PacketQueue  // For output of received packets.
          receiverSignallingQueueOfSockPackets;

        private DatagramSocket   // Unconnected socket receiving packets.
          receiverDatagramSocket;  

        }
      ; // UnconnectedReceiver

    private static class Wrap  // Cyclic/wrap-around operations on longs.
      /* This class is provides some utility methods which
        which perform some wrap-around operations on long integers.
        These operations were created for operating on 
        long times expressed in milliseconds,
        but they can be used in other applications also.
        */
      {
        private static boolean isBeforeB( long x1L, long x2L )
          /* Returns true if x1L is before x2L in the cycle.
            Returns false otherwise.
            It treats times as wrap-around long values
            by subtracting them and comparing the difference to 0.
            */
          {
            return ( ( x1L - x2L ) < 0 );
            }

        private static long laterL( long x1L, long x2L )
          /* Returns the later of times x1L and x2L.  
            It treats times as wrap-around long values.
            */
          {
            return ( isBeforeB( x1L, x2L ) ? x2L : x1L ) ;
            }

        }

    }
