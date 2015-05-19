package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Properties;

import static allClasses.Globals.*;  // appLogger;

/* This class uses UDP multicast to discover other peers on the LAN.
  It sends query packets and processes response packets.
  It receives query packets and generates responses.
  It queues all receive packets to the ConnectionManager
  which it uses to create unicast connections with the discovered peers. 
  
  This was based on Multicaster.java at 
    http://homepages.inf.ed.ac.uk/rmcnally/peerDiscover/Multicaster.java
  This originally worked using either multicast or broadcast.
  Now it does only multicast.

  Changes to make?

	  ??? This class works, but is not fast nor robust. It needs work.
	  
	  ??? RoundRobin beaconing and sharing the role of
	  responder to new arrivees.
	  
	  ??? Maybe the thread should be divided into two threads,
	  as in the ConnectionManager's unicast receiver:
	  one thread manages, and 	  one thread does nothing except receive packets.
	  
	  ??? For an unknown reason, 
	  multicast packets which packet sniffer indicates were sent
	  are not always received.
	  It might be timing because it worked when run under Eclipse IDE,
	  but not as a Java app from the command line.
	  So far it seems to affect only one side,
	  so at least one member of every pair can discover the other
	  and can initiate a connection.

  */

public class Multicaster

	///extends MutableList
	extends NetCaster
	
	implements Runnable
	
	{
		private static final byte QUERY_PACKET = 80;  // 050h
		private static final byte RESPONSE_PACKET = 81;  // 051h
	
	  // Injected dependencies.
	  private InetSocketAddress theInetSocketAddress;
		public final SignallingQueue<SockPacket> // Send output.
	    sendQueueOfSockPackets;  // SockPackets for ConnectionManager to send.
	  public final SignallingQueue<SockPacket> // Receive output.
	    receiveQueueOfSockPackets;  // SockPackets for ConnectionManager to note.
	  private DatagramSocket unconnectedDatagramSocket;  // Socket for sending.

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
	
	  private MulticastSocket aMulticastSocket; /* For receiving multicast 
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
	
	  private boolean shouldStop = false;  // For signaling Thread termination.
	
	  public Multicaster (  // Constructor.
	  		DataTreeModel theDataTreeModel,
	  		InetSocketAddress theInetSocketAddress,
	      SignallingQueue<SockPacket> sendQueueOfSockPackets,
	      SignallingQueue<SockPacket> receiveQueueOfSockPackets,
	      DatagramSocket unconnectedDatagramSocket
	      )
	    throws IOException
	    /* Constructs a Multicaster object and prepares it for
	      UDP multicast communications duties.  
	      Those duties are to help peers discover each other by
	      sending and receiving multicast packets on the LAN.
	      The parameters are:
	        * theDataTreeModel: TreeModel to which this object will be added.
	        * sendQueueOfSockPackets: thread-safe queue to receive 
	          multicast packets to be sent.  This how transmitting happens.
	        * receiveQueueOfSockPackets: thread-safe queue to receive 
	          multicast packets received.
	        * unconnectedDatagramSocket: DatagramSocket to use
	          for SockPadkets, that are queued for sending.
	      */
	    {
	      super(  // Superclass NetCaster List constructor with some dependencies. 
		        theDataTreeModel,
		        theInetSocketAddress,
	      		"Multicaster-at-" ///+ peerInetSocketAddress.getAddress()
	          ///new DataNode[]{} // Initially empty of details.
	      		);

	  		// Store remaining injected dependencies.
	  	  this.theInetSocketAddress= theInetSocketAddress;
	      this.sendQueueOfSockPackets= sendQueueOfSockPackets;
	      this.receiveQueueOfSockPackets= receiveQueueOfSockPackets;
	      this.unconnectedDatagramSocket= unconnectedDatagramSocket;
	      }

    public void run()  // Main Multicaster thread.
      // This method contains the main thread logic.  
      {
        try { // Operations that might produce an IOException.
        	///appLogger.info(
	        ///              Thread.currentThread().getName()+": run() beginning."
	        ///   );

  	  		initializeV();  // Do non-injection initialization.
          //initializeChildrenV();

          while (true) // Repeating until thread termination is requested.
            {
              if   // Exiting if requested.
                ( Thread.currentThread().isInterrupted() ) 
                break;
              // Code to be repeated goes here.
            	
  	          while( !shouldStop )  // While thread termination is not requested...
  	            { // Send and receive multicast packets.
  	              try {
  	                DatagramPacket queryDatagramPacket = new DatagramPacket( 
  	                  new byte[] { QUERY_PACKET },1, groupInetAddress, multicastPortI 
  	                  );
  	                SockPacket querySockPacket= new SockPacket(
  	                  unconnectedDatagramSocket,
  	                  queryDatagramPacket
  	                  );
  	                sendQueueOfSockPackets.add( // Queuing query packet for sending.
  	                  querySockPacket
  	                  );
  	                packetsSentNamedInteger.addValueL( 1 );
  	                receivingMulticastPacketsV( ); // Receiving packets until done.
  	                }
  	              catch( SocketException soe ) {
  	                // someone may have called disconnect()
  	                appLogger.info( 
  	                  "Multicaster.run() Terminating loop because of\n  " + soe
  	                  );
  	                shouldStop= true;  // Signalling to terminate loop.
  	                }
  	              } // Send and receive multicast packets.
  	
  	          aMulticastSocket.disconnect();  // Stopping filtering?  Filtering?
  	          aMulticastSocket.close();  // Freeing associated OS resource.
              }

          // Terminating.
	        ///appLogger.info(
	        /// Thread.currentThread().getName()+": run() ending."
	        /// );
          }
        catch( IOException e ) {
          e.printStackTrace();
          }
        }

	  protected void initializeV()
	    throws IOException
	    {
	  		///theInetSocketAddress= new InetSocketAddress(
			  ///		InetAddress.getByName("239.255.0.0"),
			  ///		PortManager.getDiscoveryPortI()
			  ///		);
		    this.groupInetAddress = ///InetAddress.getByName("239.255.0.0");
		    		theInetSocketAddress.getAddress();
		    this.multicastPortI = // Set multicast port #.
		      ///PortManager.getDiscoveryPortI();
		    	theInetSocketAddress.getPort();
		    this.ttl= 1;  // Set Time-To-Live to 1 to discover LAN peers only.
		  
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

	  	  super.initializeV();
	  	  }
		
    private void receivingMulticastPacketsV( ) 
      throws IOException 
      /* This helper method receives and processes multicast packets,
        both query packets and response packets to a previously sent query,
        until no more packets are received for a timeout interval.
        It reports all received packets to the ConnectionManager.
        If any received packets are query packets then
        it sends multicast response packets.
        */
      { // receivingMulticastPacketsV( )
        try { // Exceptions within this block terminate the receive loop.
          while (true) { // Receive response packets until time-out.
            byte[] responseBytes = new byte[ 1 ];
            DatagramPacket rx =  // Construct receiving packet.
              new DatagramPacket( responseBytes, responseBytes.length );
            SockPacket receiveSockPacket=  // Cnstruct SockPacket.
              new SockPacket( aMulticastSocket, rx );
            appLogger.info(
              "Multicaster.run(): waiting for multicast packet\n  "
              + " SR:" + aMulticastSocket.getRemoteSocketAddress()
              + " SL:" + aMulticastSocket.getLocalSocketAddress()
              + " group:" + groupInetAddress
              );
            aMulticastSocket.setSoTimeout( 30000 ); // Set timeout interval.
            aMulticastSocket.receive( rx );
            appLogger.info("Multicaster.run(): received multicast packet\n"
              + " R:" + rx.getSocketAddress()
              + " to L:" + aMulticastSocket.getLocalSocketAddress()
              );
            packetsReceivedNamedInteger.addValueL( 1 );

            if( responseBytes[ 0 ] == QUERY_PACKET )
              { // Create and queue response.
                DatagramPacket responseDatagramPacket= // Constructing response.
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
                sendQueueOfSockPackets.add( // Queuing response for sending.
                  aSockPacket 
                  );

                receiveQueueOfSockPackets.add( // Passing query to CM.
                  receiveSockPacket
                  );
                } // Create and queue response.
            else if( responseBytes[ 0 ] == RESPONSE_PACKET )
              {
                //appLogger.info(
                //  "Multicaster.run(): multicast response address"
                //  + rx.getSocketAddress()
                //  );
                receiveQueueOfSockPackets.add( // Passing reponse to CM.
                  receiveSockPacket
                  );
                }
            else
              appLogger.error("Multicaster.run(): unknown multicast packet.");
            } // Receive response packets until time-out.
          } // Exceptions within this block terminate the receive loop.
        catch (SocketTimeoutException aSocketTimeoutException) {
          appLogger.info( "Multicaster.run(): receive(..) timed out." );
          }
        } // receivingMulticastPacketsV( )
	  
	  }
