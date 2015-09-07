package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

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
    UDP uses only the local (IP,port) pair when it demultiplexes.
    So to prevent BindExceptions on sockets bound to
    the same local ports, each peer of a given node would need 
    to connect to the remote peer using a different remote number.
    This is an unacceptable complication.
    
    So, only 2 Datagram sockets are used: 
    * one socket for multicast receiving and 
    * another socket for every thing else.
    Their ports must be different/unique.

    There are several types of thread classes defined in this file
    or in other java files.
    * ConnectionManager: the main thread for this class.
      It manages everything.  
    * UnconnectedReceiver: receives packets for ConnectionManager.
    * Unicaster: manages one connection.  
      ? Maybe rename to ConnectionManager?
    * Multicaster: sends and receives multicast packets
      used for discovering other peers on the LAN.
    * MulticastReceiver: used to receive multicast packets for Multicaster.

    An IOException can be caused by external events,
    such as a link going down, the computer going to sleep, etc.
    These are handled by closing the socket, opening another,
    and retrying the operation.
    In the case of the MulticastReceiver and UnconnectedReceiver,
    new DatagramSockets are passed to the constructors because
    closing the socket is the only way to terminate receive() operations. 

    ?? Presently the unconnected unicast receiver and multicast receiver
    threads can not recover from an IOException.
    This is related to the fact that their sockets are created by
    other threads and injected so that closing the socket by
    the connection manager can be used to immediately terminate
    their waiting on receive() and terminate those threads.
    Once closed the sockets can not be reused.
    There needs to be a better way of doing this which is recoverable.
    It can't recover on its own if socket is injected into constructor.
    ? Could I use setDatagramSocketImplFactory(..) and
      class DatagramSocketImpl?
    ? Maybe put [re]creation of receiver threads in main processing loop. 
    */

  { // class ConnectionManager.

  
    // Injected instance variables, all private.
	    
	    private ConnectionsFactory theConnectionsFactory;
	  	private UnicasterManager theUnicasterManager;
      private Shutdowner theShutdowner;

    // Other instance variables, all private.
		  private MulticastSocket theMulticastSocket; // For multicast receiver. 
  		private Multicaster theMulticaster;
	    private EpiThread multicasterEpiThread ; // Its thread.
		
	    public DatagramSocket unconnectedDatagramSocket; // For UDP io.
	      // It is used for receiving unicast packets and sending both types.

	    private UnconnectedReceiver theUnconnectedReceiver; // Receiver and
	    private EpiThread theUnconnectedReceiverEpiThread ; // its thread.

	    private Sender theSender; // Sender and
	    private EpiThread theSenderEpiThread ; // its thread.
	
	    // Inputs to the Sender thread.
      private LockAndSignal senderLockAndSignal;
	    private PacketQueue senderInputQueueOfSockPackets;

	    // Inputs to the connection manager thread.
	    private LockAndSignal cmLockAndSignal;  // LockAndSignal for this thread.
	      /* This single object is used to synchronize communication between 
	        the ConnectionManager and all threads providing data to it.
	        It is used by the input queues that follow.
	        It can also be used separately to signal asynchronous inputs
	        such as the socket open/closed state.
	        The old way of synchronizing inputs used 
	        an Objects for a lock and a separate boolean signal.
	        */
	    private PacketQueue multicastSignallingQueueOfSockPackets; 
	      // Queue receiving multicast packets received.
	    private PacketQueue cmUnicastInputQueueOfSockPackets;
	    	// Queue receiving unconnected unicast packets received.
	    private JobQueue<Unicaster> cmJobQueueOfUnicasters;
	      // Queue receiving unicasters beginning or end.


    public ConnectionManager(   // Constructor.
        ConnectionsFactory theConnectionsFactory,
        DataTreeModel theDataTreeModel,
        UnicasterManager theUnicasterManager,
        Shutdowner theShutdowner
        )
      {
        super(  // Constructing base class.
          theDataTreeModel, // For receiving tree change notifications.
          "Connections", // DataNode (not thread) name.
          new DataNode[]{} // Initially empty List of Peers.
          );

        // Storing other dependencies injected into this class.
        this.theConnectionsFactory= theConnectionsFactory;
        this.theUnicasterManager= theUnicasterManager;
        this.theShutdowner= theShutdowner;
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
        } // Connections.

    private void initilizeV()
      {
		    // Setting all input queues empty.

	    	senderLockAndSignal= new LockAndSignal(false);  // Sender signaler.
	    	senderInputQueueOfSockPackets=
		      new PacketQueue(senderLockAndSignal);
	
	      cmLockAndSignal= new LockAndSignal(false);  // CM signaler.
		    multicastSignallingQueueOfSockPackets=
		      new PacketQueue(cmLockAndSignal);
		    cmUnicastInputQueueOfSockPackets=
		      new PacketQueue(cmLockAndSignal);
		    cmJobQueueOfUnicasters=
		      new JobQueue<Unicaster>(cmLockAndSignal);
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
        startingAllThreads();

        processingInputsAndExecutingEventsV(); // Until thread termination...
          // ...is requested.

        stoppingAllThreadsV();
        }

    private void processingInputsAndExecutingEventsV()
      /* This method contains the main processing loop which
        does various types of work.  The work consists of:
          * Processing inputs it gets from several thread-safe queues.
          - It was also processing scheduled jobs whose times have come,
            but these have been moved to other threads, at least for now.
        When there is no more work, the loop blocks until 
        the next bit of work arrives, when it repeats the process.

        The loop continues until thread termination is requested
        by setting the Interrupt Status Flag with Thread.interrupt().
        This flag is preserved by this method,
        so it may be tested by its callers.

				The work of many of the queue processing methods that were called below
				were moved to separate threads with their own queues.
				The 2 that remain are for miscellaneous packets from the Multcaster
				and UnconnectedReceiver threads.
        */
      {
    	  toReturn: {
	    		while (true) { // Repeating until thread termination requested.
	      		if // Exiting loop if  thread termination is requested.
	      		  ( Thread.currentThread().isInterrupted() ) break toReturn;
		      		
		    		// toLoopBack: 
		    		{ // Processing inputs and waiting for more.
		          processingUnconnectedSockPacketsB();
		          processingMulticasterSockPacketsB();

		          /* At this point, all inputs that arrived before 
		            the last notification signal should have been processed, 
		            and maybe a few more inputs that arrived after that.  
		            */

		          cmLockAndSignal.doWaitE();  // Waiting for next signal of inputs.
	      			} // toLoopBack
		        } // while (true)
	    		} // toReturn.
        return;
        }

    private void startingAllThreads()
      {
    	  startingSenderThreadV();
        startingMulticasterThreadV();
        startingUnicastReceiverThreadV();
        }

    private void stoppingAllThreadsV()
      {
        theUnicasterManager.stoppingPeerThreadsV();

        stoppingUnicastReceiverThreadV(); // reverse??
        stoppingMulticasterThreadV(); // reverse??
    	  stoppingSenderThreadV();
        }

    private void startingSenderThreadV()
      // Needs work??
      {
	      theSender=  // Constructing thread.
	          new Sender( 
	            unconnectedDatagramSocket,
	            senderInputQueueOfSockPackets,
	            senderLockAndSignal
	            );
        theSenderEpiThread= new EpiThread( 
        	theSender,
          "Sender"
          );
        theSenderEpiThread.startV();  // Starting thread.
	      }

    private void startingUnicastReceiverThreadV()
      // Needs work??
      {
	      theUnconnectedReceiver=  // Constructing thread.
	          new UnconnectedReceiver( 
	            unconnectedDatagramSocket,
	            cmUnicastInputQueueOfSockPackets,
	            theUnicasterManager
	            );
        theUnconnectedReceiverEpiThread= new EpiThread( 
          theUnconnectedReceiver,
          "UcRcvr"
          );
        theUnconnectedReceiverEpiThread.startV();  // Starting thread.
	      }

    private void stoppingUnicastReceiverThreadV()
	    {
	      theUnconnectedReceiverEpiThread.stopV();  // Requesting termination of
			    // theUnconnectedReceiver thread.
			  unconnectedDatagramSocket.close(); // Causing immediate unblock of
			    // unconnectedDatagramSocket.receive() in that thread.
			  theUnconnectedReceiverEpiThread.joinV();  // Waiting for termination of
	        // theUnconnectedReceiver thread.
	      }

    private void stoppingSenderThreadV()
	    {
	      theSenderEpiThread.stopV(); // Requesting termination of Sender thread.
			  // Note, DatagramSocket should already be closed.
	      theSenderEpiThread.joinV(); // Waiting for termination of Sender thread.
	      }

    private void startingMulticasterThreadV()
      {
        try { // PeerDiscoery
          theMulticastSocket = new MulticastSocket(  // Create MulticastSocket...
			      PortManager.getDiscoveryPortI()  // ...bound to Discovery port.
			      );
          // Need a better way to resupply theMulticastSocket on error ??
          //new Multicaster(  // Construct Multicaster...
        	theMulticaster= theConnectionsFactory.makeMulticaster(
     	  		theMulticastSocket
     	  		,senderInputQueueOfSockPackets // ...with Sender queue,...
            ,multicastSignallingQueueOfSockPackets // ...receive queue,...
            ,unconnectedDatagramSocket // ...and socket to use.
    		  	,theUnicasterManager
            );  // Multicaster will start its own thread.
					addB( theMulticaster );  // Add to DataNode List.
          multicasterEpiThread= new EpiThread( 
            theMulticaster,
            "Multicaster" //+peerInetSocketAddress
            );
          multicasterEpiThread.startV();
          } catch (IOException e) {
            appLogger.error("startingMulticasterThreadV():"+e);
          }
        }

    private void stoppingMulticasterThreadV()
	    {
    		multicasterEpiThread.stopV();  // Requesting termination of thread.
	      theMulticastSocket.close(); // Causing immediate unblock of
			    // DatagramSocket.receive() in the thread.
	      multicasterEpiThread.joinV();  // Waiting for termination of thread.
	      }

    private boolean processingMulticasterSockPacketsB() 
      /* This method processes packets received by 
        the Multicaster Thread and forwarded here,
        by adding the nodes that sent them to the known connections.
        It handles each differently depending on whether it is
        a query packet or a response.
        It returns true if any packets were processed, false otherwise.
        */
      {
        boolean packetsProcessedB= false;
        SockPacket theSockPacket;

        while (true) {  // Process all received packets.
          theSockPacket= // Try getting next packet from queue.
            multicastSignallingQueueOfSockPackets.poll();
          if (theSockPacket == null) break;  // Exit if no more packets.
      		createAndPassToUnicasterV( theSockPacket );
          packetsProcessedB= true;
          }
          
        return packetsProcessedB;
        }

    public synchronized void addingV( Unicaster thisUnicaster ) 
      /* This method's job is to add the thisUnicaster to
	      the ConnectionManager's inherited MutableList.  
	      It should already be in the Map.
        This method is called when thisUnicaster is starting.
        */
	    {
	    	if  // Adding to DataNode List if it's not there.
	      	( ! addB( thisUnicaster ) )
	      	appLogger.error("CM:startingUnicasterV(): Already added.");
	      // There is no need to add to Map.  It is there already.
	      }

    public synchronized void removingV( Unicaster thisUnicaster ) 
      /* This method's job is to remove the thisUnicaster from
	      the inherited MutableList and the Map.
        It is called when thisUnicaster is terminating.
        */
	    {
	    	if  // Removing to DataNode List if it's there.
	      	( ! removeB( thisUnicaster ) )
	      	appLogger.error("CM:stoppingUnicasterV(): removeB(..) failed");

	    	theUnicasterManager.removeV(thisUnicaster); // Move to Unicaster??
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
              cmUnicastInputQueueOfSockPackets.poll();
            }

          if (theSockPacket == null) break;  // Exit if no more packets.
          
          //appLogger.info(
          //  "ConnectionManager.processingUnconnectedSockPacketsB()\n  "
          //  + theSockPacket.getSocketAddressesString()
          //  );
          createAndPassToUnicasterV( theSockPacket );

          packetsProcessedB= true;
          }
          
        return packetsProcessedB;
        }

    private void createAndPassToUnicasterV(SockPacket theSockPacket)
      /* This method processes one packet received from a peer.
        The peer is assumed to be at the packet's remote address and port.
        It adds a Unicaster to the Unicaster data structures
        if the appropriate Unicaster doesn't already exist.
        If the entry does exist then it updates it.
        This should work with both unicast and multicast packets
        provided that their remote addresses are those of the remote peer.
        */
      {
        //appLogger.info(
        //  "ConnectionManager.createAndPassToUnicasterV(..)\n  "
        //  + theSockPacket.getSocketAddressesString()
        //  );
	      Unicaster theUnicaster=  // Get or create Unicaster.
	          theUnicasterManager.tryGettingExistingUnicaster( theSockPacket );
	      if ( theUnicaster == null )
		      {
			      theUnicaster=  // Get or create Unicaster.
			          getOrBuildAndStartUnicaster( theSockPacket );
			      }
	      theUnicaster.puttingReceivedPacketV( // Giving packet to Unicaster.  
	      		theSockPacket
	      		);
        }

    private Unicaster getOrBuildAndStartUnicaster( SockPacket theSockPacket )
      /* Gets or creates the Unicaster associated with theSockPacket.
        It adds the Unicaster to the appropriate data structures.
        It returns the found or created Unicaster.
        
        ?? An InetSocketAddress can be built only with new-operator.
        This makes fast testing difficult.
        Maybe define a new class and use it instead as the key value 
        in the peerSocketAddressConcurrentHashMap.  
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
          getOrBuildAndStartUnicaster(peerInetSocketAddress);
        return theUnicaster;
        }

    private Unicaster getOrBuildAndStartUnicaster(
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
          theUnicasterManager.getNetCasterValue(peerInetSocketAddress);
        if (resultNetCasterValue == null) // Adding peer if not stored already.
          {
		        appLogger.info( "Creating new Unicaster." );
            final NetCasterValue newNetCasterValue=  // Building new peer. 
              theConnectionsFactory.makeUnicasterValue(
                peerInetSocketAddress,
                senderInputQueueOfSockPackets,
                cmJobQueueOfUnicasters,
                unconnectedDatagramSocket,
                this, // theConnectionManager
                theShutdowner
                );
            theUnicasterManager.putV( // Adding to HashMap with...
              peerInetSocketAddress,  // ...the SocketAddress as the key and...
              newNetCasterValue  // ...the NetCasterValue as the value.
              );
            newNetCasterValue.getEpiThread().startV(); // Start peer's thread.
            resultNetCasterValue= newNetCasterValue;  // Using new peer as result.
            }
        return resultNetCasterValue.getUnicaster();
        }

    } // class ConnectionManager.
