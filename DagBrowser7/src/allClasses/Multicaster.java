package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Properties;

import static allClasses.Globals.*;  // appLogger;

/* Each peer node used this class to use UDP multicast to:
  * announce its arrival on a LAN; and
  * to discover other peers already on the LAN.
  It does these things by sending and receiving query and response packets.
  It receives query packets and generates responses.
  It informs the ConnectionManager of new peers that it discovers.

  After construction and initialization, 
  data for this thread passes through the following:
  * receiverToMulticasterPacketQueue: packets from the MulticastReceiver.
  * multicasterToConnectionManagerPacketQueue: packets to ConnectionManager.
  * netcasterToSenderPacketQueue: packets to the network Sender.
  
  This was originally based on Multicaster.java at 
    http://homepages.inf.ed.ac.uk/rmcnally/peerDiscover/Multicaster.java
  but it has changed a lot since then.
  This originally worked using either multicast or broadcast.
  Now it does only multicast.

  ?? Changes to make.  No rush until # nodes on a LAN becomes large.

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
	  // Injected dependencies.
	  private MulticastSocket theMulticastSocket; /* For receiving multicast 
	    datagrams.  Multicast datagrams are sent using 
	    a different DatagramSocket, an unconnected DatagramSocket because:
	      * That works.
	      * It's a way to specify a source/local port number.
	    */ 
	  private InetSocketAddress theInetSocketAddress;
	  private final PacketQueue // Receive output.
	    multicasterToConnectionManagerPacketQueue;  // SockPackets for ConnectionManager to note.
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
	      LockAndSignal netcasterLockAndSignal,
	      NetInputStream theNetInputStream,
	  		NetOutputStream theNetOutputStream,
	  		DataTreeModel theDataTreeModel,
	  		InetSocketAddress theInetSocketAddress,
	  		MulticastSocket theMulticastSocket,
	      PacketQueue multicasterToConnectionManagerPacketQueue,
		  	UnicasterManager theUnicasterManager
	      )
	    /* Constructs a Multicaster object and prepares it for
	      UDP multicast communications duties.  
	      Those duties are to help peers discover each other by
	      sending and receiving multicast packets on the LAN.
	      */
	    {
	      super(  // Superclass NetCaster List constructor with some dependencies. 
	          netcasterLockAndSignal,
	  	      theNetInputStream,
	  	      theNetOutputStream,
	  	  		theDataTreeModel,
		        theInetSocketAddress,
		        "Multicaster"
	      		);

	  		// Store remaining injected dependencies.
	  		this.theMulticastSocket= theMulticastSocket;
	  	  this.theInetSocketAddress= theInetSocketAddress;
	      this.multicasterToConnectionManagerPacketQueue= multicasterToConnectionManagerPacketQueue;
		  	this.theUnicasterManager= theUnicasterManager;
	      }

	  
	  /* The following is new code that uses the MulticastReceier thread.  */
	  
    private EpiThread theMulticastReceiverEpiThread; // its thread.

    public static class MulticastReceiver

      implements Runnable

      /* This simple thread receives and queues 
        multicast DatagramPackets from a MulticastSocket.
        The packets are queued to a PacketQueue for consumption by 
        the main Multicaster thread.
			
			  After construction and initialization, 
			  data for this thread passes through the following:
        * theMulticastSocket: DatagramPackets from the network.
        * receiverToMulticasterPacketQueue: packets to the Multicaster. 
        
        This thread is kept simple because the only known way to guarantee
        fast termination of a multicast receive(..) operation
        is for another thread to close its MulticastSocket.
        Doing this will also terminate this thread.
        */
      {

        // Injected dependency instance variables.
        private PacketQueue receiverToMulticasterPacketQueue;
          // Queue which is destination of received packets.
        private MulticastSocket theMulticastSocket;


        MulticastReceiver( // Constructor. 
            PacketQueue receiverToMulticasterPacketQueue,
            MulticastSocket theMulticastSocket
            )
          /* Constructs an instance of this class from:
              * theMulticastSocket: the socket receiving packets.
              * receiverToMulticasterPacketQueue: the output queue.
            */
          {
	          this.receiverToMulticasterPacketQueue= 
	          		receiverToMulticasterPacketQueue;
	          this.theMulticastSocket= theMulticastSocket;
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
	                  DatagramPacket theDatagramPacket= 
	                  		receiveSockPacket.getDatagramPacket(); 
	                  theMulticastSocket.receive( theDatagramPacket );
	                  //appLogger.debug(
	                  //		"run() received: "
	                  //+ PacketStuff.gettingPacketString(theDatagramPacket)
	                  //);
	                  receiverToMulticasterPacketQueue.add( // Queuing packet.
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
				  			Globals.logAndRethrowAsRuntimeExceptionV( 
				  					"run() IOException: ", e
				  					);
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
          	initializingV();  // Do non-injection initialization.
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
      {
    		theMulticastReceiverEpiThread= // Constructing thread.
    				AppGUIFactory.makeMulticastReceiverEpiThread(
	            theNetInputStream.getPacketQueue(),
	            theMulticastSocket
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
    		long delayMsL= // Minimum time between multicasts. 
    				// 3600000; // 1 hour for testing to disable multicasting.
    				10000; // 10 seconds to make multicast happen more often.
    		    // 40000; // 40 seconds for normal use.
    		LockAndSignal.Input theInput;  // Type of input that ends waits.
      	long querySentMsL= System.currentTimeMillis();
        processorLoop: while (true) { // Processing messages until exit.
        	{ // Processing messages or exiting.
          	theInput=  // Awaiting next input within reply interval.
          		testWaitInIntervalE( querySentMsL, delayMsL );
	          switch ( theInput ) {  // Handling the input type.
	          	case TIME: // Handling a time-out.
	              multicastConnectionLoggerV( false ); // Comm. ended.
	              break processorLoop;  // Exiting loop.
	            case INTERRUPTION: // Handling a thread's interruption.
	              break processorLoop;  // Exiting loop.
	            case NOTIFICATION:  // Handling a message input.
	            	processor: {
	            		String inString= readAString(); // Reading message.
		          		//if ( testingMessageB( "DISCOVERY" ) ) // Handling query, maybe.
	            		if ( inString.equals( "DISCOVERY" ) ) // Handling query, maybe.
			        			{ sendingMessageV("ALIVE"); // Sending response.
				              processingPossibleNewUnicasterV();
				              break processor;
				              }
			        		//if ( testingMessageB( "ALIVE" ) ) // Handling response, maybe.
			        		if ( inString.equals( "ALIVE" ) ) // Handling response, maybe.
				            { processingPossibleNewUnicasterV(); break processor; }
		          		// Ignoring anything else.
			            //appLogger.warning(
			        		//		"receivingPacketsV(): unexpected: "
			        		// //+ getAndConsumeOneMessageString()
			        		//  + inString 
			        		//+ " from "
			        		//+ PacketStuff.gettingPacketAddressString( 
			        		//		theNetInputStream.getSockPacket().getDatagramPacket()
			        		//	  )
			        		//);
	            		}
	            }
            } // processor: // Processing packets until exit.
        	} // processorLoop:  // Processing packet or exiting.
        }

    private void processingPossibleNewUnicasterV( ) throws IOException
	    {
	      SockPacket theSockPacket= // Copying packet from queue.
	      		theNetInputStream.getSockPacket();
	      Unicaster theUnicaster= // Testing for associated Unicaster.
	      		theUnicasterManager.tryGettingUnicaster( theSockPacket );
	      if ( theUnicaster == null )  // Processing if Unicaster does not exists.
	       	{
       			//appLogger.debug(
	       		//	"Multicaster.processingPossibleNewUnicasterV():\n  queuing: "
		       	//	+PacketStuff.gettingPacketString(theSockPacket.getDatagramPacket())
	       		//);
          	multicasterToConnectionManagerPacketQueue.add( // Passing to CM.
			    		theSockPacket
			        );
       	    }
	      }

	  protected void initializingV()
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

	  	  super.initializingV();
	  	  }
	  
	  private boolean multicastActiveB= false;
	  
    private void multicastConnectionLoggerV( boolean activeB )
      /* This method logs when bidirectional multicast communication
        either begins or ends.  Activity ending is defined as
        no received multicast packets for the multicast period.
       */
    	{
    	  if ( multicastActiveB != activeB ) {
    	  	multicastActiveB= activeB;
          appLogger.info("multicastActiveB= " + multicastActiveB );
          }
    	  }
    
	  }
