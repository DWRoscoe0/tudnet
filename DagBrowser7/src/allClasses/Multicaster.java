package allClasses;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Properties;

import static allClasses.Globals.*;  // appLogger;

/* Each peer node use this class to use UDP multicast to:
  * announce its arrival on a LAN; and
  * to discover other peers already on the LAN.
  It does these things by sending and receiving query and response packets.
  It receives query packets and generates responses.
  It informs the ConnectionManager of new peers that it discovers.
  
  This was based on Multicaster.java at 
    http://homepages.inf.ed.ac.uk/rmcnally/peerDiscover/Multicaster.java
  but it has changed a lot since then.
  This originally worked using either multicast or broadcast.
  Now it does only multicast.

  Changes to make.  No rush until # nodes on a LAN becomes large.

	  ?? RoundRobin beaconing and sharing the role of
	  responder to new arrivees.

  	?? Make more SSDP-like.
  	   = ALIVE on startup and end of sleep mode. 
  	   = BYEBYE, on shutdown or sleep mode (might be detectable).
  	   = DISCOVERY, might be needed, even if above 2 are used,
  	     * If neighbors disappear, unless new neighbors can be found in cache.
  	     * If ALIVE message become lost.
  	   ? Add time parameter for updates since BYEBYE.
  	   ? Storms will not be a problem because:
  	     * Of interchangeability of nodes, because neighboring is the service.
  	     *  Replies are multicast, so replies can be aborte after some are seen.

    ?? MulticastLoadDistribution.  Goals, eventual:
    * at least 3 immediate connection-available responses to every query,
      if available, so new peers can get merged/connected quickly.
    * if there is no other activity then beacon packets are sent.
    * peers with fewer connections should be quicker to send.
    * packets may include info about other peers, especially
      those that want new neighbors.
    * peers might want new neighbors because:
      * they need more.
      * they want different ones.
    
  */

public class Multicaster

	extends NetCaster
	
	implements Runnable
	
	{
		public static final byte QUERY_PACKET = 80;  // 050h 'P'
		public static final byte RESPONSE_PACKET = 81;  // 051h 'Q'
	
	  // Injected dependencies.
	  private MulticastSocket theMulticastSocket; /* For receiving multicast 
	    datagrams.  Multicast datagrams are sent using 
	    a different DatagramSocket, an unconnected DatagramSocket because:
	      * That works.
	      * It's a way to specify a source/local port number.
	    */ 
	  private InetSocketAddress theInetSocketAddress;
	  public final InputQueue<SockPacket> // Receive output.
	    cmUnicastInputQueueOfSockPackets;  // SockPackets for ConnectionManager to note.
  	private UnicasterManager theUnicasterManager;

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
	
	  public Multicaster (  // Constructor.
	  		DataTreeModel theDataTreeModel,
	  		InetSocketAddress theInetSocketAddress,
	  		MulticastSocket theMulticastSocket,
	  		InputQueue<SockPacket> sendQueueOfSockPackets,
	      InputQueue<SockPacket> cmUnicastInputQueueOfSockPackets,
		  	UnicasterManager theUnicasterManager
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
	        * cmUnicastInputQueueOfSockPackets: thread-safe queue to receive 
	          multicast packets received.
	        * unconnectedDatagramSocket: DatagramSocket to use
	          for SockPadkets, that are queued for sending.
	      */
	    {
	      super(  // Superclass NetCaster List constructor with some dependencies. 
		        theDataTreeModel,
		        theInetSocketAddress,
		        sendQueueOfSockPackets,
		        "Multicaster-at-"
	      		);

	  		// Store remaining injected dependencies.
	  		this.theMulticastSocket= theMulticastSocket;
	  	  this.theInetSocketAddress= theInetSocketAddress;
	      this.cmUnicastInputQueueOfSockPackets= cmUnicastInputQueueOfSockPackets;
		  	this.theUnicasterManager= theUnicasterManager;
	      }

	  
	  /* The following is new code that uses the MulticastReceier thread.  */
	  
    private MulticastReceiver theMulticastReceiver; // Receiver and
    private EpiThread theMulticastReceiverEpiThread; // its thread.

    private class MulticastReceiver

      implements Runnable

      /* This simple thread receives and queues 
        multicast DatagramPackets from an MulticastSocket.
        The packets are queued to a PacketQueue for consumption by 
        the main Multicaster thread.
        This thread is kept simple because the only known way to guarantee
        fast termination of the a multicast receive(..) operation
        is for another thread to close its MulticastSocket.
        Doing this will also terminate this thread.
        */
      {

        // Injected dependency instance variables.
        
      	@SuppressWarnings("unused")
    		private DatagramSocket receiverDatagramSocket;
          // Unconnected socket which is source of packets.

        private PacketQueue receiverSignallingQueueOfSockPackets;
          // Queue which is destination of received packets.


        MulticastReceiver( // Constructor. 
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
        

        public void run() 
          /* This method repeatedly waits for and reads 
            DatagramPackets and queues them 
            for consumption by another thread.
            */
          {
        		//appLogger.debug("run() begin.");
	          try { // Operations that might produce an IOException.
	            while  // Receiving and queuing packets unless termination is
	              ( ! Thread.currentThread().isInterrupted() ) // requested.
	              { // Receiving and queuing one packet.
	                try {
	                  SockPacket receiveSockPacket=  // Construct SockPacket.
	                  		PacketStuff.makeSockPacket( );
	                  theMulticastSocket.receive( 
	                  		receiveSockPacket.getDatagramPacket() 
	                  		);
	                  receiverSignallingQueueOfSockPackets.add( // Queuing packet.
	                  		receiveSockPacket
	                  		);
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
        		//appLogger.debug("run() end.");
            }

        } // MulticastReceiver
      
    public void run()  // Main Multicaster thread.
        /* This method contains the main thread logic.
          It announces the presence of this peer node on the LAN
          by sending multicast packets.
          It also processes packets queued to it 
          by the MulticastReceiver thread.
          Some of the received packets are forwarded to the ConnectionManager
          which uses them to create or activate Unicasters.

          ?? This thread will probably be extensively changed,
          but for small networks it works fine.
         	*/
        {
	      	try { // Operations that might produce an IOException.
          	initializeV();  // Do non-injection initialization.
            //initializeChildrenV();
  	      	startingMultcastReceiverThreadV();

            while (true) // Repeating until thread termination is requested.
              {
                if   // Exiting if requested.
                  ( Thread.currentThread().isInterrupted() ) 
                  break;
                // Code to be repeated goes here.
              	
                while  // Processing while thread termination is not requested...
                	( ! Thread.currentThread().isInterrupted() )
    	            { // Send and receive multicast packets.
    	              try {
    		      				sendingMessageV("DISCOVERY"); // Sending query.
    	                receivingPacketsV( ); // Receiving packets until done.
    	                }
    	              catch( SocketException soe ) {
    	                // Someone may have called disconnect() or closed socket.
    	                appLogger.info( 
    	                  "Multicaster.run() Terminating loop because of\n  " + soe
    	                  );
                      Thread.currentThread().interrupt(); // Terminating loop 
    	                }
    	              } // Send and receive multicast packets.
    	
    	          theMulticastSocket.disconnect();  // Stopping filtering?  Filtering?
    	          theMulticastSocket.close();  // Freeing associated OS resource.
    	          stoppingMulticastReceiverThreadV();
    	          }

            }
          catch( IOException e ) {
            appLogger.error(Thread.currentThread().getName()+"run(): " + e );
            }

          //appLogger.info( Thread.currentThread().getName()+": run() ending." );
          }

    private void startingMultcastReceiverThreadV()
      // Needs work??
      {
	      theMulticastReceiver=  // Constructing thread.
	          new MulticastReceiver( 
	          	theMulticastSocket,
	            receiveQueueOfSockPackets
	            );
        theMulticastReceiverEpiThread= new EpiThread( 
          theMulticastReceiver,
          "McRcvr"
          );
        theMulticastReceiverEpiThread.startV();  // Starting thread.
	      }

    private void stoppingMulticastReceiverThreadV()
	    {
	      theMulticastReceiverEpiThread.stopV();  // Requesting termination of
			    // theMulticastReceiver thread.
	      theMulticastSocket.close(); // Causing immediate unblock of
			    // theMulticastSocket.receive() in that thread.
			  theMulticastReceiverEpiThread.joinV();  // Waiting for termination of
	        // theMulticastReceiver thread.
	      }

    private void receivingPacketsV( ) 
      throws IOException 
      /* This helper method receives and processes multicast packets,
        both query packets and response packets to a previously sent query,
        until no more packets are received for a timeout interval.
        It reports all received packets to the ConnectionManager.
        If any received packets are query packets then
        it sends multicast response packets.
        */
      {
    		LockAndSignal.Input theInput;  // Type of input that ends waits.
      	long querySentMsL= System.currentTimeMillis(); // and in ms.
        processorLoop: while (true) { // Processing packets until exit.
        	{ // Processing packet or exiting.
        		long delayMsL= 40000;
          	theInput=  // Awaiting next input within reply interval.
          		testWaitInIntervalE( querySentMsL, delayMsL );
	          switch ( theInput ) {  // Handling the input type.
	          	case TIME: // Handling a time-out.
	              multicastConnectionLoggerV( false ); // Comm. ended.
	              break processorLoop;  // Exiting loop.
	            case INTERRUPTION: // Handling a thread's interruption.
	              break processorLoop;  // Exiting loop.
	            case NOTIFICATION:  // Handling packet inputs.
	          		if ( testingMessageB( "DISCOVERY" ) ) // Handling query, maybe.
		        			{ sendingMessageV("ALIVE"); // Sending response.
			              processingPossibleNewUnicasterV();
			              } // Create and queue response.
		        		else if ( testingMessageB( "ALIVE" ) ) // Handling response, maybe.
			            processingPossibleNewUnicasterV();
			          else
				          {
				            appLogger.warning(
				            		"receivingPacketsV(): unexpected: "
				            		+ PacketStuff.gettingPacketString( 
				            				theNetInputStream.getSockPacket().getDatagramPacket()
				            				//receivedSockPacket.getDatagramPacket() 
				            				)
				            		);
			        		  tryingToConsumeOneMessageB();
				            }
	            }
            } // processor: // Processing packets until exit.
        	} // processorLoop:  // Processing packet or exiting.
        }

    private void processingPossibleNewUnicasterV( ) throws IOException
    {
      SockPacket streamSockPacket= // Copying packet from queue.
      		theNetInputStream.getSockPacket();
    	processingPossibleNewUnicasterV(streamSockPacket);
    	tryingToConsumeOneMessageB();
      }

	  protected void processingPossibleNewUnicasterV( SockPacket theSockPacket )
	    {
	      Unicaster theUnicaster= // Testing for associated Unicaster.
	      		theUnicasterManager.tryGettingExistingUnicaster( 
	      				theSockPacket 
	      				);
	      if ( theUnicaster != null )  // Ignoring if Unicaster exists already.
	      	; // Ignoring.
       	else // Associated Unicaster does not exist.
	       	{
       			//appLogger.debug(
	       		//	"Multicaster.processingPossibleNewUnicasterV():\n  queuing: "
		       	//	+PacketStuff.gettingPacketString(theSockPacket.getDatagramPacket())
	       		//);
          	cmUnicastInputQueueOfSockPackets.add( // Passing to CM.
			    		theSockPacket
			        );
       	    }
	      }

	  protected void initializeV()
	    throws IOException
	    {
		    this.groupInetAddress = theInetSocketAddress.getAddress();
		    this.multicastPortI = theInetSocketAddress.getPort();
		    this.ttl= 1;  // Set Time-To-Live to 1 to discover LAN peers only.
		  
		    { // Fix MulticastSocket.setTimeToLive() bug for IPv4 + IPv6 systems.
		      Properties props = System.getProperties();
		      props.setProperty( "java.net.preferIPv4Stack", "true" );
		      System.setProperties( props );
		      }
		    theMulticastSocket.joinGroup(  // To receive multicast packets, join...
		      groupInetAddress  // ...this group IPAddress.
		      );
		    theMulticastSocket.setLoopbackMode( true );  // Disable loopback.
		    theMulticastSocket.setTimeToLive( ttl );

	  	  super.initializeV();
	  	  }
	  
	  private boolean multicastActiveB= false;
	  
    private void multicastConnectionLoggerV( boolean activeB )
      /* This method logs when bidirectional multicast communication
        either begins or ends.  Activity ending is defined as
        no received multicast packets for 40 seconds or more.
       */
    	{
    	  if ( multicastActiveB != activeB ) {
    	  	multicastActiveB= activeB;
          appLogger.info("multicastActiveB= " + multicastActiveB );
          }
    	  }
    
	  }