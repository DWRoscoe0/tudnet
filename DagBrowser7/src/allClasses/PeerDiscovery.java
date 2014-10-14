package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Properties;
//import java.util.concurrent.BlockingQueue;

import static allClasses.Globals.*;  // appLogger;

/* This class uses UDP multicast to discover other peers on the LAN.
  
  ??? This class works, but is not fast nor robust. It needs much work.
  This was based on PeerDiscovery.java at 
    http://homepages.inf.ed.ac.uk/rmcnally/peerDiscover/PeerDiscovery.java
  This originally worked using either multicast or broadcast.
  Now it does only multicast.
  
  ??? RoundRobin beaconing and sharing the role of
  responder to new arrivees.
  
  ??? Maybe the thread should be divided into two threads,
  as in the Connection, one thread manages, and
  one thread does nothing except receive packets.
  
  ??? For an unknown reason, 
  multicast packets which packet sniffer indicates were sent
  are not always received.
  It might be timing because it worked when run under Eclipse IDE,
  but not as a Java app from the command line.
  So far it seems to affect only one side,
  so at least one member of every pair can discover the other
  and can initiate a connection.
  */

public class PeerDiscovery
{
	private static final byte QUERY_PACKET = 80;  // 050h
	private static final byte RESPONSE_PACKET = 81;  // 051h

  public final InetAddress groupInetAddress; /* Multicast group IPAddress.   
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

  public final int multicastPortI;  // Multicast port number used with IP.

  public final SignallingQueue<SockPacket> // Send output.
    sendQueueOfSockPackets;  // SockPackets for ConnectionManager to send.

  public final SignallingQueue<SockPacket> // Receive output.
    receiveQueueOfSockPackets;  // SockPackets for ConnectionManager to note.

  private DatagramSocket unconnectedDatagramSocket;  // Socket for sending.

  private final MulticastSocket aMulticastSocket; /* For receiving multicast 
    datagrams.  Multicast datagrams are sent using 
    a different DatagramSocket, an unconnected DatagramSocket because:
      * That works.
      * It's a way to specify a source/local port number.
    */ 

  int ttl;  /* ttl: The time-to-live for multicast packets. 
    0 = restricted to the same host, 
    1 = Restricted to the same subnet, 
    <32 = Restricted to the same site, organisation or department, 
    <64 = Restricted to the same region, 
    <128 = Restricted to the same continent,
    <255 = unrestricted
    */

  private boolean shouldStop = false;  // For signalling Thread termination.

  public ExceptionHandler rxExceptionHandler = new ExceptionHandler();
    // Redefine this to be a different handler if desired.  null means no-op.

  public PeerDiscovery (  // Constructor.
      SignallingQueue<SockPacket> sendQueueOfSockPackets,
      SignallingQueue<SockPacket> receiveQueueOfSockPackets,
      DatagramSocket unconnectedDatagramSocket
      )
    throws IOException
    /* Constructs a PeerDiscovery object and prepares it for
      UDP multicast communications duties.  
      Those duties are to help peers discover each other by
      sending and receiving multicast packets on the LAN.
      The parameters are:
        * sendQueueOfSockPackets: thread-safe queue of 
          multicast packets to be sent.
        * receiveQueueOfSockPackets: thread-safe queue of 
          multicast packets received.
        * unconnectedDatagramSocket: DatagramSocket to use
          for SockPadkets, which are what are actually queued.
      */
    {
      this.sendQueueOfSockPackets= sendQueueOfSockPackets;
      this.receiveQueueOfSockPackets= receiveQueueOfSockPackets;
      this.unconnectedDatagramSocket= unconnectedDatagramSocket;

      this.groupInetAddress = InetAddress.getByName("239.255.0.0");
      this.ttl= 1;  // Set Time-To-Live to 1 to discover LAN peers only.
      this.multicastPortI = // Set multicast port #.
        PortManager.getDiscoveryPortI();
    
      { // Fix MulticastSocket.setTimeToLive() bug for IPv4 + IPv6 systems.
        Properties props = System.getProperties();
        props.setProperty( "java.net.preferIPv4Stack", "true" );
        System.setProperties( props );
        }
      aMulticastSocket = new MulticastSocket(  // Create MulticastSocket...
        PortManager.getDiscoveryPortI()  // ...bound to Discovery port.
        );
      aMulticastSocket.joinGroup(  // To receive multicast packets, join...
        groupInetAddress  // ...this group IPAddress.
        );
      aMulticastSocket.setLoopbackMode( true );  // Disable loopback.
      aMulticastSocket.setTimeToLive( ttl );
      mcastListen.setDaemon( true );  // Don't prevent exit for this thread.
      mcastListen.start();  // Start the thread.
      }

  private MCThread mcastListen =  // This thread does multicast listening.
    new MCThread( "MulticastDiscovery" );

  private class MCThread extends Thread {  // That thread defined.

    MCThread( String nameString ) // Constructor.
      { 
        super( nameString );  // Name here because setName() not reliable.
        }

    @Override
    public void run() 
      /* This method continuously sends a multicast discovery/query packet,
        and then receives multicast packets until there aren't any more.
        The received packets might be queries from other peers,
        or replies to this peers query.
        */
      {
        appLogger.info(getName()+" thread starting.");

        try { // Operations that might produce an IOException.

          while( !shouldStop )  // While thread termination is not requested...
            { // Send and receive multicast packets.
              try {
                DatagramPacket queryDatagramPacket = new DatagramPacket( 
                  new byte[] { QUERY_PACKET },1, groupInetAddress, multicastPortI 
                  );
                SockPacket querySockPacket= new SockPacket(
                  ///BoundUDPSockets.getDatagramSocket(),
                  unconnectedDatagramSocket,
                  queryDatagramPacket
                  );
                sendQueueOfSockPackets.add(  // Queue multicast query.
                  querySockPacket
                  );
                receiveAllMulticastPacketsV( ); // Receive packets until they end.
                }
              catch( SocketException soe ) {
                // someone may have called disconnect()
                appLogger.info( 
                  "PeerDiscovery.run() Terminating loop because of\n" + soe
                  );
                shouldStop= true;  // Terminate loop.
                }
              } // Send and receive multicast packets.

          aMulticastSocket.disconnect();  // Stop filtering, though there is none.
          aMulticastSocket.close();  // Free associated OS resource.
          }
        catch( IOException e ) {
          if( rxExceptionHandler != null )
            {
              rxExceptionHandler.handle( e );
            }
          }
        }

    private void receiveAllMulticastPacketsV( ) 
      throws IOException 
      /* This helper method receives and processes multicast packets,
        both query packets and respons packets to a previously sent query,
        until none are received for a timeout interval.
        It reports all received packets to the ConnectionManager.
        If any received packets are query packets then
        it sends multicast response packets.
        */
      {
        try { // Exceptions within this block terminate the receive loop.
          while (true) { // Receive response packets until time-out.
            byte[] responseBytes = new byte[ 1 ];
            DatagramPacket rx =  // Construct receiving packet.
              new DatagramPacket( responseBytes, responseBytes.length );
            SockPacket receiveSockPacket=  // Cnstruct SockPacket.
              new SockPacket( aMulticastSocket, rx );
            //appLogger.info(
            //  "PeerDiscovery.run(): waiting for multicast packet\n  "
            //  + " SR:" + aMulticastSocket.getRemoteSocketAddress()
            //  + " SL:" + aMulticastSocket.getLocalSocketAddress()
            //  + " group:" + groupInetAddress
            //  );
            aMulticastSocket.setSoTimeout( 30000 ); // Set timeout interval.
            aMulticastSocket.receive( rx );
            //appLogger.info("PeerDiscovery.run(): received multicast packet\n"
            //  + " R:" + rx.getSocketAddress()
            //  + " to L:" + aMulticastSocket.getLocalSocketAddress()
            //  );

            if( responseBytes[ 0 ] == QUERY_PACKET )
              { // Create and queue response.
                DatagramPacket responseDatagramPacket= // Construct packet.
                  new DatagramPacket(
                    new byte[] { RESPONSE_PACKET }, 
                    1, 
                    groupInetAddress, 
                    multicastPortI 
                    );
                SockPacket aSockPacket= new SockPacket( 
                    //BoundUDPSockets.getDatagramSocket(),
                    unconnectedDatagramSocket,
                    responseDatagramPacket
                    );
                sendQueueOfSockPackets.add(  // Queue multicast response.
                  aSockPacket 
                  );

                receiveQueueOfSockPackets.add(  // Report peer.
                  receiveSockPacket
                  );
                } // Create and queue response.
            else if( responseBytes[ 0 ] == RESPONSE_PACKET )
              { // Just report peer's existence.
                //appLogger.info(
                //  "PeerDiscovery.run(): multicast response address"
                //  + rx.getSocketAddress()
                //  );
                receiveQueueOfSockPackets.add(  // Report peer.
                  receiveSockPacket
                  );
                } // Just report peer's existence.
            else
              appLogger.info("PeerDiscovery.run(): unknown multicast packet.");
            } // Receive response packets until time-out.
          } // Exceptions within this block terminate the receive loop.
        catch (SocketTimeoutException aSocketTimeoutException) {
          //appLogger.info( "PeerDiscovery.run(): receive(..) timed out." );
          }
        }

    }; // mcastListen / MCThread

  public void stop()  // Thread terminator.
    /* This method terminates the thread and releases all
      associated resources.  This call will block
      until everything's timed out and closed etc.
      */
    {
      appLogger.info("PeerDiscovery.disconnect().");

      shouldStop = true;  // Request thread termination.

      DatagramSocket sock = aMulticastSocket ;
      sock.close();  // Free associated OS resource.
      sock.disconnect();  // Stop filtering, though there is none.

      Thread listen = mcastListen;
      try {
        listen.join();  // Wait for actual thread termination.
        }
      catch( InterruptedException e ) {
        e.printStackTrace();
        }
      }

  public class ExceptionHandler  // For receiver thread.
    /**
     * Handles an exception.
     * 
     * @author ryanm
     */
    {
      /**
       * Called whenever an exception is thrown from the listen thread. The
       * listen thread should now be dead
       * 
       * @param e
       */
      public void handle( Exception e )
      {
        e.printStackTrace();
      }
    }

  }
