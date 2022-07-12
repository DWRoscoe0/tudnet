package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Properties;

import allClasses.epinode.MapEpiNode;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


/*
  This class implements IP multicast.
   
  From https://en.wikipedia.org/wiki/IP_multicast
    IP multicast is a method of sending Internet Protocol (IP) datagrams 
    to a group of interested receivers in a single transmission. 
    It is the IP-specific form of multicast and is used for 
    streaming media and other network applications. 
    It uses specially reserved multicast address blocks in IPv4 and IPv6.
    
  The two primary applications of IPMulticast are:
  * Distributing data to multiple interested receivers,
    thereby saving a lot of bandwidth when there are many receivers.
  * Creating a distributed virtual server for the purpose of
    communicating the existence of resources on a network.
    An example is SSDP used by UniversalPlugAndPlay aka UPnP protocol.

  This class presently uses multicast for only peer service discovery.
  Presently this is done with 2 types of packets:
  * An MC-QUERY packet is multicast periodically to request
    the IDs of peers on the LAN. 
  * An MC-ALIVE packets are multicast in response to
    to receipt of an MC-QUERY packet.

  After construction and initialization, 
  packets processed by this class pass through the following Queues:
  * receiverToMulticasterNetcasterQueue: packets from the MulticastReceiver.
  * multicasterToConnectionManagerNetcasterQueue: packets to ConnectionManager.
  * netcasterToSenderPacketQueue: packets to the network Sender.

  This class was originally based on Multicaster.java at 
    http://homepages.inf.ed.ac.uk/rmcnally/peerDiscover/Multicaster.java
  but it has changed a lot since then.
  This originally worked using either multicast or broadcast.
  Now it uses only multicast.

  ///enh Multicast on all non-loopback NetworkInterfaces,
  not only the one from which the default gateway can be reached.
   
  ///enh Changes to reduce packet traffic.  No rush on these items
    until # of nodes on a LAN becomes large.

    ///enh Reduce rate of beacons and responses by some combination of:
       * round-robin role changing?
       * delaying by delay time times number of nodes
       * doing in random time slot divided by number of nodes

    ///enh Make more like Simple Service Discovery Protocol (SSDP).
       = MC-ALIVE sent on device-app startup and end of sleep mode. 
       = BYEBYE sent on shutdown, entering sleep mode, or any service termination.
       Because nodes are peers, both clients and servers,
       the above two should be enough to function.
       = MC-QUERY, might be needed, even if above 2 are used for cases when
         * a neighbor disappears and a replacement is needed, 
           unless new neighbors can be found in cache.
         * an MC-ALIVE message was lost.
       ? Add a time parameter to query message to allow
         receiving only updates since the node's most recent BYEBYE.
       ? Storms will not be a problem because
         * Neighboring is the service and neighbors are interchangeable. 
         * Replies are multicast, so pending replies by later replying nodes
           can be aborted after some replies are sent.

    ///enh Or, a more general solution:
      Instead of having separate command packets, have a single one,
      such as MULTICAST-PACKET, and have internal parameters:
      * HELLO: announces arrival.
      * WANT-PEERS
        * number-of-peers-wanted
      * PEER-LIST (cached peers)
        * peer1
        * peer2
        * ...
        * peerN
      * BYEBYE: announces departure.

    ///enh MulticastLoadDistribution.  Goals, eventual:
    * A peer can respond to a query with its own ID plus
      the peers that it has already cached, 
      and maybe their number of connections.
    * If there is no other activity then beacon packets may be sent.
      Beacon packets may contain cached peer data. 
    * Peers with fewer connections should be quicker to send.
    * Packets may include info about other peers, especially
      those that want new neighbors.
    * Peers might want new neighbors because:
      * they need more.
      * they want different ones.
    
  */

public class Multicaster

  extends Netcaster
  
  {
    // Injected dependencies.
    private MulticastSocket theMulticastSocket; /* For receiving multicast 
      datagrams.  Multicast datagrams are sent using 
      a different DatagramSocket, an unconnected DatagramSocket because:
        * That works.
        * It's a way to specify a source/local port number.
      */ 
    private IPAndPort theIPAndPort;
    private final NetcasterQueue // Receive output.
      multicasterToConnectionManagerNetcasterQueue;  // SockPackets for ConnectionManager to note.

    private final NetcasterPacketManager multicastReceiverNetcasterPacketManager;

    public InetAddress groupInetAddress; /* Multicast group IPAddress.   
      This, combined with the port, determines 
      the set of peers that are able to discover each other.
  
      RFC2365, "Administratively Scoped IP Multicast", allocates
      239.0.0.0 to 239.255.255.255 for use local and organizational
      scopes for multicast addresses.  They are not meant to be used
      outside of those scopes.  There are two expandable scopes:
        Local Scope -- 239.255.0.0/16
        Organization Local Scope -- 239.192.0.0/14
      Routers are supposed to block packets outside of these ranges.
      Should be a valid multicast address, 
      i.e.: in the range 224.0.0.1 to 239.255.255.255 inclusive.
      */
  
    public int multicastPortI;  // Multicast port number used with IP.
  
    int ttl;  /* ttl: The time-to-live for multicast packets. 
      0 = restricted to the same host, 
      1 = Restricted to the same subnet, 
      <32 = Restricted to the same site, organization or department, 
      <64 = Restricted to the same region, 
      <128 = Restricted to the same continent,
      <255 = unrestricted
      */
  
    public Multicaster(  // Constructor.
        LockAndSignal netcasterLockAndSignal,
        NetcasterInputStream theNetcasterInputStream,
        NetcasterOutputStream theNetcasterOutputStream,
        Shutdowner theShutdowner,
        IPAndPort theIPAndPort,
        MulticastSocket theMulticastSocket,
        NetcasterQueue multicasterToConnectionManagerNetcasterQueue,
        NetcasterPacketManager multicastReceiverNetcasterPacketManager,
        NamedLong initialRetryTimeOutMsNamedLong
        )
      /* Constructs a Multicaster object and prepares it for
        UDP multicast communications duties.  
        Those duties are to help peers discover each other by
        sending and receiving multicast packets on the LAN.
        */
      {
        super(  // Superclass Netcaster List constructor with some dependencies. 
            netcasterLockAndSignal,
            theNetcasterInputStream,
            theNetcasterOutputStream,
            theShutdowner,
            theIPAndPort,
            "Multicaster",
            initialRetryTimeOutMsNamedLong,
            new DefaultBooleanLike(false)
            );

        // Store remaining injected dependencies.
        this.theMulticastSocket= theMulticastSocket;
        this.theIPAndPort= theIPAndPort;
        this.multicasterToConnectionManagerNetcasterQueue= multicasterToConnectionManagerNetcasterQueue;
        this.multicastReceiverNetcasterPacketManager=
            multicastReceiverNetcasterPacketManager;
        }

    
    /* The following is new code that uses the MulticastReceier thread.  */
    
    private EpiThread theMulticastReceiverEpiThread; // its thread.

    public static class MulticastReceiver

      implements Runnable

      /* This simple thread receives and queues 
        multicast DatagramPackets from a MulticastSocket.
        The packets are queued to a NetcasterQueue for consumption by 
        the main Multicaster thread.
      
        After construction and initialization, 
        data for this thread passes through the following:
        * theMulticastSocket: DatagramPackets from the network.
        * receiverToMulticasterNetcasterQueue: packets to the Multicaster. 
        
        This thread is kept simple because the only known way to guarantee
        fast termination of a multicast receive(..) operation
        is for another thread to close its MulticastSocket.
        Doing this will also terminate this thread.
        */
      {

        // Injected dependency instance variables.
        private final NetcasterQueue receiverToMulticasterNetcasterQueue;
          // Queue which is destination of received packets.
        private final MulticastSocket theMulticastSocket;
        private final NetcasterPacketManager theNetcasterPacketManager;


        MulticastReceiver( // Constructor. 
            NetcasterQueue receiverToMulticasterNetcasterQueue,
            MulticastSocket theMulticastSocket,
            NetcasterPacketManager theNetcasterPacketManager
            )
          /* Constructs an instance of this class from:
              * theMulticastSocket: the socket receiving packets.
              * receiverToMulticasterNetcasterQueue: the output queue.
            */
          {
            this.receiverToMulticasterNetcasterQueue= 
                receiverToMulticasterNetcasterQueue;
            this.theMulticastSocket= theMulticastSocket;
            this.theNetcasterPacketManager= theNetcasterPacketManager;
            }
        

        public void run() 
          /* This method repeatedly waits for and reads 
            multicast DatagramPackets and queues them 
            for consumption by the main multicaster thread.
            */
          {
            theAppLog.info("run() begins.");
            try { // Operations that might produce an IOException.
              while  // Receiving and queuing packets unless termination is
                ( ! EpiThread.testInterruptB() ) // requested.
                { // Receiving and queuing one packet.
                  try {
                    NetcasterPacket receiverNetcasterPacket=
                        theNetcasterPacketManager.produceKeyedPacket();
                    DatagramPacket receiverDatagramPacket= 
                        receiverNetcasterPacket.getDatagramPacket(); 
                    theMulticastSocket.receive( receiverDatagramPacket );
                    PacketManager.logMulticastReceiverPacketV(
                        receiverDatagramPacket
                        );
                    receiverToMulticasterNetcasterQueue.put( // Queuing packet.
                      receiverNetcasterPacket
                      );
                    }
                  catch( SocketException soe ) {
                    theAppLog.info("run(): " + soe );
                    Thread.currentThread().interrupt(); // Translating 
                      // exception into request to terminate this thread.
                    }
                  } // Receiving and queuing one packet.
              }
              catch( IOException e ) {
                Misc.logAndRethrowAsRuntimeExceptionV( 
                    "run() IOException: ", e
                    );
              }
            theAppLog.info("run() ends.");
            }

        } // MulticastReceiver
      
    public void run()  // Main Multicaster thread.
        /* This method contains the main thread logic.
          On start up it announces the presence of 
          this peer node on the LAN by sending multicast packets.
          It will repeat this if no multicast packets 
          are received within the multicast period. 
          It processes received packets queued to it 
          by the MulticastReceiver thread.
          Received packets are forwarded to the ConnectionManager
          which uses them to create or activate associated Unicasters.
          The period timer is reset each time a packet is sent or received.
          After the system settles down,
          it should send and receive one multicast packet per period. 

          ?? This thread will probably be extensively changed,
          but for now, for development on  small networks, it works fine.
           */
        {
          if (theAppLog.testAndLogDisabledB( 
              Config.multicasterThreadsDisableB, 
              "run() multicaster") 
              )
            return;
          EpiThread.interruptibleSleepB( Config.multicastDelayMsL );
            // Initial delay to allow reconnection of Unicast peers.
          theAppLog.info( "run() initial delay ends." );
          try { // Operations that might produce an IOException requiring redo.
            initializeWithIOExceptionV();  // Do non-injection initialization.
            startingMultcastReceiverThreadV();

            while (true) // Repeating until thread termination is requested.
              {
                if   // Exiting if requested.
                  ( EpiThread.testInterruptB() ) 
                  break;
                // Code to be repeated goes here.
                
                while  // Processing while thread termination is not requested...
                  ( ! EpiThread.testInterruptB() )
                  { // Send and receive multicast packets.
                    try {
                      mcSendWordAsMapMessageV("MC-QUERY");
                      receivingPacketsV( ); // Receiving packets until done.
                      }
                    catch( SocketException soe ) {
                      // Someone may have called disconnect() or closed socket.
                      theAppLog.info( 
                        "Multicaster.run() Terminating loop because of" + NL + "  " + soe
                        );
                      Thread.currentThread().interrupt(); // Terminating loop 
                      }
                    } // Send and receive multicast packets.
      
                theMulticastSocket.disconnect();  // Stopping filtering?  Filtering?
                stoppingMulticastReceiverThreadV();
                }
            for(int i=3; i>0; i--){ // Say goodbye 3 times...
              mcSendWordAsMapMessageV("MC-GOODBYE");
              }
            }
          catch( IOException e ) {
            theAppLog.debug("run(): theMulticastSocket not [yet] open:"+e);
            }

          //appLogger.info( Thread.currentThread().getName()+": run() ending." );
          }

    private void startingMultcastReceiverThreadV()
      {
        theMulticastReceiverEpiThread= // Constructing thread.
            AppFactory.makeMulticastReceiverEpiThread(
              theEpiInputStreamI.getNotifyingQueueQ(),
              theMulticastSocket,
               multicastReceiverNetcasterPacketManager
              );
        theMulticastReceiverEpiThread.startV();  // Starting thread.
        }

    private void stoppingMulticastReceiverThreadV()
      {
        theMulticastReceiverEpiThread.stopV();  // Request termination of
          // theMulticastReceiver thread.
        Closeables.closeAndReportNothingV(theMulticastSocket); // This unblocks
          // theMulticastSocket.receive() in that thread.
        theMulticastReceiverEpiThread.joinV();  // Wait for termination of
          // theMulticastReceiver thread.
        }

    private void receivingPacketsV() 
      throws IOException 
      /* This helper method receives and processes multicast message packets,
        both query packets and response packets to a previously sent query,
        until either no more packets are received for a timeout interval
        or a thread exit is requested.
        This method sends no query packets of its own.
        Those must be sent by a caller of this method.
        But if any received packets are query packets then
        this method sends multicast response packets.
        It reports all received packets to the ConnectionManager.
        */
      {
        theAppLog.debug("MC","receivingPacketsV() begins.");
        long delayMsL= // Minimum time between multicasts. 
            // 3600000; // 1 hour for testing to disable multicasting.
            Config.multicastPeriodMsL;
        LockAndSignal.Input theInput;  // Type of input that ends waits.
        processorLoop: while (true) { // Processing messages until exit request.
          { // Processing messages or exiting.
            theAppLog.debug("MC","receivingPacketsV() at processorLoop beginning.");
            long baseTimeMsL= System.currentTimeMillis();
            theInput=  // Awaiting next input.
              waitingForSubnotificationOrIntervalOrInterruptE(
                baseTimeMsL, delayMsL);
            inputDecoder: switch ( theInput ) { // Decoding the input type.
              case TIME: // Handling a time-out.
                multicastActivityLoggerV( false );
                break processorLoop;  // Exiting loop.
              case INTERRUPTION: // Handling a thread exit request.
                break processorLoop;  // Exiting loop.
              case SUBNOTIFICATION: // Handling a message.
                messageDecoder: {
                  MapEpiNode messageMapEpiNode= // Parse a map.
                      MapEpiNode.tryMapEpiNode(theEpiInputStreamI);
                  if (null == messageMapEpiNode) { // If not gotten
                    theAppLog.debug("MC","receivingPacketsV() no MapEpiNode!");
                    break messageDecoder; // exit.
                    }
                  theAppLog.debug(
                      "MC","receivingPacketsV() decoding:" + messageMapEpiNode);
                  MapEpiNode valueMapEpiNode= // Try for "MC-QUERY". 
                      messageMapEpiNode.getMapEpiNode("MC-QUERY");
                  if (null != valueMapEpiNode) // If got it, process.
                    { 
                      mcSendWordAsMapMessageV("MC-ALIVE");
                      processingPossibleNewUnicasterV(); 
                      multicastActivityLoggerV(true);
                      break messageDecoder;
                      }
                  valueMapEpiNode= // Try for "MC-ALIVE". 
                      messageMapEpiNode.getMapEpiNode("MC-ALIVE");
                  if (null != valueMapEpiNode) // If got it, process.
                    { processingPossibleNewUnicasterV(); 
                      multicastActivityLoggerV(true);
                      break messageDecoder; 
                      }
                  // Ignoring anything else.
                  theAppLog.debug( "receivingPacketsV() ignoring unexpected: "
                      + messageMapEpiNode);
                  } // messageDecoder:
                break inputDecoder;
              default: // Handling anything else as an error.
                theAppLog.error( 
                    "receivingPacketsV(): Unknown LockAndSignal.Input" 
                    );
                break inputDecoder;
              } // inputDecoder: 
              theAppLog.debug("MC","receivingPacketsV() have exited inputDecoder.");
            } // Processing packets until exit.
          } // processorLoop:  // Processing packet or exiting.
        theAppLog.debug("MC","receivingPacketsV() ends.");
        }

    private void mcSendWordAsMapMessageV(String mcString) throws IOException
      /* This method sends mcString in a YAML-like map packet.
       */
      {
        MapEpiNode messageMapEpiNode= 
            MapEpiNode.makeSingleEntryEmptyMapEpiNode(mcString);
        messageMapEpiNode.writeV(theEpiOutputStreamO); // Write complete map.
        theEpiOutputStreamO.endBlockAndSendPacketV();
        }

    private void processingPossibleNewUnicasterV() throws IOException
      /* This method passes the present packet of theNetcasterInputStream
       to the ConnectionManager.
       */
      {
        NetcasterPacket theNetcasterPacket= // Copying packet from this InputStream.
            theEpiInputStreamI.getKeyedPacketE();
        multicasterToConnectionManagerNetcasterQueue.put( // Passing to CM.
            theNetcasterPacket
            );
        }

    public void initializeWithIOExceptionV()
      throws IOException
      {
        super.initializeV();

        NetworkInterface gatewayNetworkInterface=
          determineGatewayNetworkInterface();
        this.groupInetAddress = theIPAndPort.getInetAddress();
        this.multicastPortI = theIPAndPort.getPortI();
        this.ttl= 1;  // Set Time-To-Live to 1 to discover LAN peers only.
      
        { // Fix MulticastSocket.setTimeToLive() bug for IPv4 + IPv6 systems.
          Properties props = System.getProperties();
          props.setProperty( "java.net.preferIPv4Stack", "true" );
          System.setProperties( props );
          }
        theMulticastSocket.setLoopbackMode( true );  // Disable loopback.
        theMulticastSocket.setTimeToLive( ttl );
        theMulticastSocket.setNetworkInterface(gatewayNetworkInterface);
        theAppLog.debug("MC",
            "initializeWithIOExceptionV()" + NL
            + "  gatewayNetworkInterface= " + gatewayNetworkInterface 
            );
        theMulticastSocket.joinGroup( // To receive multicast packets, join...
            groupInetAddress  // ...this group IPAddress.
            );
          }

    private NetworkInterface determineGatewayNetworkInterface()
      /* This method tries to determine the single best NetworkInterface
        on which to do multicasting.  This is assumed to be the interface
        through which a packet would be routed to an Internet routeable 
        IP-address.
        
        ///enh  Make multicasting more general.
          It should be able to discover nodes on all NetworkInterfaces.
        ///org Note, AppInstanceManager.logLocalHostV() 
          does not do the same thing.  
          What constitutes the local host is not clear.
       */
      {
        NetworkInterface theNetworkInterface= null;
        InetAddress theInetAddress= null;
        try {
          try ( DatagramSocket theDatagramSocket= new DatagramSocket(); )
            {
              theDatagramSocket.connect(InetAddress.getByAddress(
                  new byte[]{1,1,1,1}), 0); // Internet route-able address.
              theInetAddress= theDatagramSocket.getLocalAddress();
              theNetworkInterface= 
                  NetworkInterface.getByInetAddress(theInetAddress);
              }
        } catch (Exception e) {
          theAppLog.exception( "determineGatewayNetworkInterfaceV(): ", e);
        }
        theAppLog.debug("MC",
            "determineGatewayNetworkInterfaceV()" + NL + "  "
            + "theInetAddress= " + theInetAddress 
            + ", theNetworkInterface= " + theNetworkInterface 
            );
        return theNetworkInterface;
        }

    private boolean multicastActiveB= false;
    
    private void multicastActivityLoggerV( boolean activeB )
      /* This method logs when bidirectional multicast communication
        either begins or ends.  Activity ending is defined as
        no received multicast packets for the multicast period.
       */
      {
        if ( multicastActiveB != activeB ) // Has activity changed? 
          { // Yes.
            multicastActiveB= activeB; // Record new activity state.
            theAppLog.debug("MC", // Log new state. 
                "multicastActivityLoggerV(..) multicastActiveB= " + multicastActiveB );
            }
        }
    
    }
