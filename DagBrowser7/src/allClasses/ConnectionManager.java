package allClasses;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import allClasses.LockAndSignal.Input;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

public class ConnectionManager 

  extends MutableList

  implements Runnable

  /* This class manages connections, both Unicast and Multicast connections,
    with other Infogora peer nodes.
    It makes use of the class UnicasterManager as a helper.

    It extends MutableList to manage what it displays.
    Because this list is accessed by a TreeModel for a GUI JTree,
    the List is accessed synchronously on the Event Dispatch Thread (EDT).

    ?? This class makes use of other threads 
    to manage sending and receiving of network packets
    and the individual connections receiving or sending those packets.
    Packets are passed between threads via thread-safe queues.
    ///org Some work is needed to better manage these threads,
    because DatagramSockets must be closed to terminate these threads,
    and recovery from an IOException on these sockets is troublesome.

    Originally the plan was to use a separate 
    connected DatagramSocket for each peer of a node.
    Unfortunately, unlike TCP, which can demultiplex packets
    based on both local and remote (IP,port) pairs,
    UDP uses only the local (IP,port) pair when it demultiplexes.
    So to prevent BindExceptions on sockets bound to
    the same local ports, each peer of a given node would need 
    to connect to the remote peer using a different remote port number.
    This is an unacceptable complication.
    
    So, only 2 Datagram sockets are used: 
    * one socket for multicast receiving and 
    * another socket for unicast receiving and 
      both multicast and unicast sending.
    Their ports of these 2 sockets must be different.

    There are several types of thread classes defined in this file
    or in other java files.
    * ConnectionManager: the main thread for this class.
      It manages everything.  
    * UnconnectedReceiver: receives unicast packets for ConnectionManager.
    * Unicaster: there is one of these for each peer connection.  
    * MulticastReceiver: used to receive multicast packets for Multicaster.
    * Multicaster: sends and receives multicast packets.
      This is used for discovering other peers on LANs.
    * Sender: does the actual calls to send(..) to send all packets.

    An IOException can be caused by external events,
    such as a link going down, the computer going to sleep and waking up, etc.
    These are handled by closing the socket, opening another,
    and retrying the operation.
    In the case of the MulticastReceiver and UnconnectedReceiver,
    new DatagramSockets are passed to the constructors because
    closing the socket is the only way to terminate receive() operations. 
    ///org There should be a better way of doing this so 
    the threads don't need to be completely reconstructed ??

    ///org Presently the unconnected unicast receiver and multicast receiver
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
	    
			private AppGUIFactory theAppGUIFactory;
	    
		  private final Persistent thePersistent;

    	private PortManager thePortManager;

    	private UnicasterManager theUnicasterManager;

	    private LockAndSignal cmThreadLockAndSignal;
        /* This single object is used to synchronize communication between 
          the ConnectionManager and all threads providing data to it.
  				It should be the same LockAndSignal instance used in the construction
  				of the input queues that follow.
          It can also be used separately to signal asynchronous inputs
          such as the socket open/closed state.
          The old way of synchronizing inputs used 
          an Object for a lock and a separate boolean signal.
          */

      // Synchronized inputs to the connection manager's thread.
	    private NetcasterQueue multicasterToConnectionManagerNetcasterQueue; 
	      // Queue of multicast packets received from Multicaster.
	    private NetcasterQueue unconnectedReceiverToConnectionManagerNetcasterQueue;
	    	// Queue of unconnected unicast packets received from Unicasters.
      private NotifyingQueue<String> toConnectionManagerNotifyingQueueOfStrings;
        // For inputs in the form of Strings.
      private NotifyingQueue<MapEpiNode> toConnectionManagerNotifyingQueueOfMapEpiNodes;
        // For inputs in the form of MapEpiNodes.
      
      private TextStream theTextStream= null;

    // Other instance variables, all private.

      private MulticastSocket theMulticastSocket; // For multicast receiver. 
	    private EpiThread multicasterEpiThread ; // Its thread.
  		private InetAddress multicastInetAddress;
		
	    public DatagramSocket unconnectedDatagramSocket; // For UDP io.
	      // It is used for receiving unicast packets and sending both types.

	    private EpiThread theUnconnectedReceiverEpiThread; // its thread.

	    private EpiThread theSenderEpiThread ; // its thread.
	


    public ConnectionManager(  // Constructor.
    		AppGUIFactory theAppGUIFactory,
    	  Persistent thePersistent,
    	  PortManager thePortManager,
    	  UnicasterManager theUnicasterManager,
    		LockAndSignal cmThreadLockAndSignal,
    		NetcasterQueue multicasterToConnectionManagerNetcasterQueue,
    		NetcasterQueue unconnectedReceiverToConnectionManagerNetcasterQueue,
        NotifyingQueue<String> toConnectionManagerNotifyingQueueOfStrings,
        NotifyingQueue<MapEpiNode> toConnectionManagerNotifyingQueueOfMapEpiNodes,
        TextStream theTextStream
    		)
      {
      	super.initializeV(  // Constructing base class.
          "Connection-Manager", // DataNode (not thread) name.
          emptyListOfDataNodes()
          );

        // Storing other dependencies injected into this class.
  	    this.theAppGUIFactory= theAppGUIFactory;
  	    this.thePersistent= thePersistent;
      	this.thePortManager= thePortManager;
  	    this.theUnicasterManager= theUnicasterManager; 
  	    this.cmThreadLockAndSignal= cmThreadLockAndSignal;
  	    this.multicasterToConnectionManagerNetcasterQueue=
  	    		multicasterToConnectionManagerNetcasterQueue;
  	    this.unconnectedReceiverToConnectionManagerNetcasterQueue=
  	    		unconnectedReceiverToConnectionManagerNetcasterQueue;
        this.toConnectionManagerNotifyingQueueOfStrings=
            toConnectionManagerNotifyingQueueOfStrings;
        this.toConnectionManagerNotifyingQueueOfMapEpiNodes=
            toConnectionManagerNotifyingQueueOfMapEpiNodes;
        this.theTextStream= theTextStream;

        }


    public void run()  // Main ConnectionManager thread logic.
      /* This method creates unconnectedDatagramSocket 
        for its use and uses it and does other things
        by calling settingThreadsAndDoingWorkV().
        This is inside of a try-catch block in a loop
        to recover and retry after SocketException-s.
        Because DatagramSockets can't be reused after IOExceptions,
        all the threads that use it are recreated if that occurs.
        */
      {
    		initializeV();  // Doing non-injection initialization.

    		restartPreviousUnicastersV();

        processingInputsAndExecutingEventsV(); // Until thread termination...
          // ...is requested.

        stoppingAllThreadsV();
        }

    public void initializeV()
    	// This method does non-injection initialization.
      {
		    addAtEndB( theUnicasterManager ); // Adding UnicasterManager to our list.

				try { // Doing this here is a bit of a kludge.
					  multicastInetAddress= InetAddress.getByName("239.255.0.0"); }
				  catch ( UnknownHostException e ) { 
          	Misc.logAndRethrowAsRuntimeExceptionV( "initializeV()", e );
				  }
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
    		while (true) { // Repeating until thread termination requested.
      		maintainingDatagramSocketAndDependentThreadsV( );
      		maintainingMulticastSocketAndDependentThreadsV( );

          LockAndSignal.Input theInput= // Waiting for any new inputs. 
        		cmThreadLockAndSignal.waitingForNotificationOrInterruptE();

      		if // Exiting loop if  thread termination is requested.
      		  ( theInput == Input.INTERRUPTION )
      			break;

          processingUnconnectedSockPacketsB();
          processingMulticasterSockPacketsB();
          processingLocalStringMessagesB();
          processPeerDataMessagesV();

          /* At this point, at least the inputs that arrived before 
            the last notification signal should have been processed. 
            */
	        } // while (true)
        return;
        }

		private void restartPreviousUnicastersV()
			/* This method attempts to restore Unicaster peer connections
			  which were active immediately before the previous local app shutdown.

        Because an app start up is triggering these events,
        the Unicaster is started in a state to cause 
        its state machine to do a reconnect and not a connect.
			 	*/
			{
        if (theAppLog.testAndLogDisabledB( Config.unicasterThreadsDisableB, 
            "restartPreviousUnicastersV()") 
            )
          return;
        
      	theAppLog.info("ConnectionManager.restartPreviousUnicastersV() begins.");
	    	PeersCursor thePeersCursor= // Used for iteration. 
	    	    PeersCursor.makeOnFirstEntryPeersCursor( thePersistent );
			  while // Process all peers in peer list. 
			  	( ! thePeersCursor.getEntryKeyString().isEmpty() ) 
			  	{ // Process one peer in peer list.
            tryToStartUnicasterV(thePeersCursor);
					    thePeersCursor.nextKeyString(); // Advance cursor.
					  }
      	theAppLog.info("ConnectionManager.restartPreviousUnicastersV() ends.");
				}

    private void tryToStartUnicasterV(PeersCursor thePeersCursor)
      /* This method tries to start the Unicaster,
        the data of which thePeersCursor points.
        */
      {
        toReturn: {
          if (thePeersCursor.testB("ignorePeer")) // This peer is supposed to be ignored?
            break toReturn;  // Yes, ignore this peer by exiting now.

          String peerIPString= thePeersCursor.getFieldString("IP");
          String peerPortString= thePeersCursor.getFieldString("Port");
          IPAndPort theIPAndPort= new IPAndPort(peerIPString, peerPortString);
          Unicaster theUnicaster= 
              theUnicasterManager.tryGettingAndLoggingPreexistingUnicaster(theIPAndPort);
          if ( theUnicaster == null ) // Unicaster does not yet exist.
            { // Create it, start it, and maybe connect it.
              String theIdString= thePeersCursor.getFieldString("PeerIdentity");
              theUnicaster= theUnicasterManager.buildAddAndStartUnicaster(
                  theIPAndPort, theIdString); // Restore peer with Unicaster.
              if // Reconnect to peer if it was connected at shutdown.
                (thePeersCursor.testB("wasConnected"))
                theUnicaster.connectToPeerV(); // Message state-machine to connect.
              }
        } // toReturn:
          return;
        }

    private void stoppingAllThreadsV()
      /* This method stops all the threads started by the ConnectionManager.
        It is called at shutdown time.
        The order is important so that new threads will not be started.
      	///org Possibly use a different stop order??
       	*/
      {
    		stoppingMulticasterThreadV(); 
        theUnicasterManager.stoppingEntryThreadsV(); // Stop Unicasters threads.

        stoppingSenderThreadV(); // Stops only after queued packets are sent.
        stoppingUnicastReceiverThreadV();
        }

	  private void maintainingDatagramSocketAndDependentThreadsV()
      /* This method creates the DatagramSocket and 
        the threads which depend on it.  It does this when either
        * the socket has not been opened yet, or
        * the socket had been open but was closed for some reason,
          such as an IOException.  
       */
	    { 
    	  if // Preparing socket and dependencies if socket not working.
    	    ( EpiDatagramSocket.isNullOrClosedB( unconnectedDatagramSocket ) )
	    	  preparingAll: { // Preparing socket and dependencies.
            theAppLog.info(
                "ConnectionManager.maintainingDatagramSocketAndDependentThreadsV()"
                + " has begun the renewal of threads and sockets.");
        	  stoppingSenderThreadV();
            stoppingUnicastReceiverThreadV();
    	    	preparingSocketLoop: while (true) {
              if ( EpiThread.testInterruptB() )
              	break preparingAll;
    	  	  	prepareDatagramSocketV();
    	  	  	if ( ! EpiDatagramSocket.isNullOrClosedB( 
    	  	  			unconnectedDatagramSocket 
    	  	  			) )
    	  	  		break preparingSocketLoop;
    	  	  	} // preparingSocketLoop:
	      	  startingSenderThreadV();
	          startingUnicastReceiverThreadV();
            theAppLog.info(
                "ConnectionManager.maintainingDatagramSocketAndDependentThreadsV()"
                + " has finished the renewal of threads and sockets.");
	    	  	} // preparingAll: 
	    	}
    
	  private void prepareDatagramSocketV()
	    // Makes one attempt to create the unconnectedDatagramSocket.
		  {
		    try { // Creating a new unconnected DatagramSocket and using it.
		      unconnectedDatagramSocket= // Construct socket for UDP io.
		      		theAppGUIFactory.makeDatagramSocket((SocketAddress)null);
		      unconnectedDatagramSocket.setReuseAddress(true);
		      unconnectedDatagramSocket.bind( // Binding socket to...
		      	AppGUIFactory.makeInetSocketAddress(
		          thePortManager.getNormalPortI()  // ...app's local port.
		          ) // Note, the IP is not defined.
		        );
		      }
		    catch ( SocketException e ) { // Handling SocketException.
		      theAppLog.error("unconnectedDatagramSocket:"+e);
		      if ( unconnectedDatagramSocket != null )
		        unconnectedDatagramSocket.close();
		      EpiThread.interruptibleSleepB(  // Don't hog CPU in error loop.
		      	Config.errorRetryPause1000MsL
		      	);
		      }
		    finally {
		      }
		  	}

    private void startingSenderThreadV()
      { 
    		theSenderEpiThread= theAppGUIFactory.makeSenderEpiThread( 
    				unconnectedDatagramSocket 
            );
        theSenderEpiThread.startV();  // Starting thread.
	      }

    private void startingUnicastReceiverThreadV()
      {
    		theUnconnectedReceiverEpiThread= 
    				theAppGUIFactory.makeUnconnectedReceiverEpiThread( 
    						unconnectedDatagramSocket 
    						);
        theUnconnectedReceiverEpiThread.startV();  // Starting thread.
	      }

    private boolean processingUnconnectedSockPacketsB()
      /* This method processes unconnected unicast packets 
        that are received by the UnconnectedReceiver Thread 
        and forwarded here.  It does this
        by adding the nodes that sent them to the known connections.
        It returns true if any packets were processed, false otherwise.
        */
      {
        boolean packetsProcessedB= false;
        NetcasterPacket theNetcasterPacket;

        while (true) {  // Process all received unconnected packets.
          theNetcasterPacket= // Try getting next packet from queue.
            unconnectedReceiverToConnectionManagerNetcasterQueue.poll();

          if (theNetcasterPacket == null) break;  // Exit if no more packets.
          
          //appLogger.info(
          //  "ConnectionManager.processingUnconnectedSockPacketsB()" + NL + "  "
          //  + theKeyedPacket.getSocketAddressesString()
          //  );
          passToUnicasterV( theNetcasterPacket ); ///rev disabled for testing.

          packetsProcessedB= true;
          }
          
        return packetsProcessedB;
        }

    private boolean processingLocalStringMessagesB()
      /* This method processes local messages that are received by the ConnectionManager
        and replicated and distributed to each Unicaster by the UnicasterManager.
        It returns true if any messages were processed, false otherwise.

        Presently the only message being passed is the Skipped-Time message.
        A skipped time period can cause a flurry of retransmissions and log entries
        when the period starts, apparently because 
        the network interface is shutdown before the CPU. 
        Similarly, there will be a flurry of HELLO reconnect message retransmissions 
        and log entries at the end of the skipped time period
        in response to the Skipped-Time message sent at that time,
        apparently because the network interface is restored after the CPU. 
        */
      {
        boolean itemsProcessedB= false;
        String theMessageString;

        while (true) {  // Process all messages.
          theMessageString= // Try getting next message from queue.
              toConnectionManagerNotifyingQueueOfStrings.poll();

          if (theMessageString == null) break;  // Exit if no more messages
          
          theAppLog.debug(
            "ConnectionManager.processingLocalStringMessagesB()" + theMessageString);
          theUnicasterManager.passToUnicastersV( theMessageString ); ///rev disabled for testing.

          itemsProcessedB= true;
          }
          
        return itemsProcessedB;
        }

    private void stoppingUnicastReceiverThreadV()
      /* This method stops the receiver thread by closing its socket,
        in addition to the usual way of interrupting the thread.
        This must be done because the DatagramSocket.receive(DatagramPacket)
        method is not interruptible by any method except
        closing the associated socket.
        */
	    {
        theAppLog.info( // Note this special situation in log.
            "ConnectionManager.stoppingUnicastReceiverThreadV()."
            + NL + "  This may take several seconds for Socket to close.");
        EpiDatagramSocket.closeIfNotNullV( // Close socket to allow termination.
    				unconnectedDatagramSocket
    				); // Strangely, closing can be immediate, take seconds, or even minutes!
    		EpiThread.stopAndJoinIfNotNullV(theUnconnectedReceiverEpiThread);
        }

    private void stoppingSenderThreadV()
	    {
    		EpiThread.stopAndJoinIfNotNullV(theSenderEpiThread);
		  	}

		private void maintainingMulticastSocketAndDependentThreadsV( )
      /* This method creates the MulticastSocket and 
        the thread which depends on it
        if the socket has not been opened yet.
        It does this again if the socket was open but has been closed.  
        */
	    { 
    	  if // Preparing socket and dependencies if socket not working.
    	    ( EpiDatagramSocket.isNullOrClosedB( theMulticastSocket ) )
	    	  preparingAll: { // Building or rebuilding socket and dependencies. 
            theAppLog.debug(
                "ConnectionManager.maintainingMulticastSocketAndDependentThreadsV() "
                + " has begun the renewal of thread and socket.");
            stoppingMulticasterThreadV();
    	    	preparingSocketLoop: while (true) {
              if ( EpiThread.testInterruptB() )
              	break preparingAll;
    	  	  	prepareMulcicastSocketV();
    	  	  	if ( ! EpiDatagramSocket.isNullOrClosedB( theMulticastSocket ) )
    	  	  		break preparingSocketLoop;
    	        EpiThread.interruptibleSleepB(  // Don't hog CPU in error loop.
    	            Config.errorRetryPause1000MsL
    	            );
              theAppLog.debug(
                  "maintainingMulticastSocketAndDependentThreadsV() loopng.");
    	  	  	} // preparingSocketLoop:
            startingMulticasterThreadV();
            theAppLog.info(
                "ConnectionManager.maintainingMulticastSocketAndDependentThreadsV()"
                + " has finished the renewal of thread and socket.");
	    	  	} // preparingAll: 
	    	}

	  private void prepareMulcicastSocketV()
    /* Makes one attempt to create theMulticastSocket.
      If there an error then it closes the socket.
      */
	  {
	    try { // Creating a new unconnected DatagramSocket and using it.
	    	theMulticastSocket= theAppGUIFactory.makeMulticastSocket(
		      thePortManager.getMulticastPortI()  // ...bound to Discovery port.
		      );
	      }
	    catch ( IOException e ) { // Handling SocketException.
	      theAppLog.error("theMulticastSocket:"+e);
	      if ( theMulticastSocket != null )
	      	theMulticastSocket.close();
	      }
	    finally {
	      }
	  	}

    private void startingMulticasterThreadV()
      {
    		Multicaster theMulticaster= theAppGUIFactory.makeMulticaster(
		      theMulticastSocket
		      ,multicasterToConnectionManagerNetcasterQueue // ...receive queue,...
		      ,multicastInetAddress
		      );
        theAppLog.debug(
            "startingMulticasterThreadV() adding theMulticaster and starting thread.");
    		addAtEndB( theMulticaster );  // Add to DataNode List.
        multicasterEpiThread= AppGUIFactory.makeEpiThread( 
            theMulticaster,
            "Multicaster"
            );
        multicasterEpiThread.startV();
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
        NetcasterPacket theNetcasterPacket;

        while (true) {  // Process all received packets.
          theNetcasterPacket= // Try getting next packet from queue.
            multicasterToConnectionManagerNetcasterQueue.poll();
          if (theNetcasterPacket == null) break;  // Exit if no more packets.
          // /*  ///rev disabled for testing.
  				Unicaster theUnicaster= theUnicasterManager.getOrBuildAddAndStartUnicaster( 
	      		theNetcasterPacket 
	      		);
          // */  ///rev
  				if (! theUnicaster.isConnectedB()) { // Become connected if not already.
  	        theAppLog.info(
  	            "processingMulticasterSockPacketsB() connecting to peer.");
  				  theUnicaster.connectToPeerV();
  				  }

          packetsProcessedB= true;
          }
          
        return packetsProcessedB;
        }

    private void stoppingMulticasterThreadV()
	    {
				EpiDatagramSocket.closeIfNotNullV(  // Causing unblock and termination.
						theMulticastSocket
						); ///org could this be moved to multicasterEpiThread?
				EpiThread.stopAndJoinIfNotNullV(multicasterEpiThread);
		    }


    private void passToUnicasterV( NetcasterPacket theNetcasterPacket )
      /* This method passes theNetcasterPacket to the Unicaster 
        associated with the remote peer that sent the packet.
        The peer is assumed to be at the packet's remote address and port.
        If the Unicaster doesn't exist then it will be created first.
        Normally the Unicaster will not exist when this method is called.
        This method should work with packets received by 
        either the unicast receiver or the multicast receiver. 
        */
      {
        if (theAppLog.testAndLogDisabledB( Config.unicasterThreadsDisableB, 
            "passToUnicasterV(..)") 
            )
          return;

        //appLogger.info(
        //  "ConnectionManager.createAndPassToUnicasterV(..)" + NL + "  "
        //  + theKeyedPacket.getSocketAddressesString()
        //  );
    		Unicaster theUnicaster=  // Getting the appropriate Unicaster.
    				theUnicasterManager.getOrBuildAddAndStartUnicaster( 
		      		theNetcasterPacket 
		      		);
        if (! theUnicaster.isConnectedB()) { // Become connected if not already.
          theAppLog.info(
              "passToUnicasterV() connecting to peer.");
          theUnicaster.connectToPeerV();
          }
	      theUnicaster.puttingKeyedPacketV( // Giving to Unicaster as its first? packet.  
	      		theNetcasterPacket
	      		);
        }

    
    /*  ///
      PeerDataExchange: The comments which follow are from a brainstorming session.
        Not all of the material is being used or will be used.

      PeerTerminology:
        Peer: any peer.
        LocalPeer: Self-explanatory.
        RemotePeer: A second peer that is not the LocalPeer.
        SubjectPeer: A third peer about which LocalPeer and RemotePeer 
          communicate.
    
      RemotePeer/Unicaster variables.  
        Note, all time variables increase monotonically.
        * long lastModifiedTime  Updated with present time when 
          any of the following variables of the same RemotePeer change: 
          * boolean wasConnected.
          The lastModifiedTime of no 2 peers will be equal.
          This uniqueness simplifies coding.
        * boolean wasConnected // Changes when associated RemotePeer 
          connects to or disconnects from the LocalPeer.
        * long sentLastLastModifiedTime: the lastModifiedTime of 
          the SubjectPeer data packet most recently sent to the RemotePeer.
          This is also the largest value to be sent.
        * long acknowledgedLastLastModifiedTime: this is a copy of 
          the last sentLastLastModifiedTime send by the RemotePeer and
          received by the LocalPeer.  It, along with lastSendTime,
          can be used to decide whether to retransmit the previous data.
          It is always less than or equal to sentLastLastModifiedTime.
        * long lastSendTime: This is the last real time that
          data was sent to the RemotePeer.
          This is used to determine the next time out, if any.
          
        */
    
    private void processPeerDataMessagesV()
      /* This method process all MapEpiNode messages about peer state changes.
        */
      {
        /// theAppLog.debug("ConnectionManager.processPeerDataMessagesV() called.");
        MapEpiNode messageMapEpiNode;
        while (true) {  // Process all messages.
          messageMapEpiNode= // Try getting next message from queue.
              toConnectionManagerNotifyingQueueOfMapEpiNodes.poll();
          if (messageMapEpiNode == null) break;  // Exit if no more messages
          theAppLog.debug( 
              "ConnectionManager.processPeerDataMessagesV() dequeued peerMapEpiNode:"
              + messageMapEpiNode.toString(2));
          decodePeerMapEpiNodeV(messageMapEpiNode);
          }
        }

    private void decodePeerMapEpiNodeV(MapEpiNode messageMapEpiNode)
      /* This method decodes the received single-entry messageMapEpiNode, 
        based on the value of the key of that entry.
        The value of the entry is another MapEpiNode 
        containing actual data about a subject peer.
        The message could be from a local Unicaster about a change in its connection state,
        or it could be from a remote peer about the possibly changed state
        of another peer.
        */
      {
          theAppLog.debug("ConnectionManager.decodePeerMapEpiNodeV(..) begins, "
            + "messageMapEpiNode=" + NL + "  " + messageMapEpiNode);
          MapEpiNode valueMapEpiNode;
        goReturn: {
          if (tryProcessingByTextStreamB(messageMapEpiNode)) // Try TextStream processing.
            { break goReturn; } // Success, so exit.
            
          valueMapEpiNode= messageMapEpiNode.getMapEpiNode("LocalNewState");
          if (valueMapEpiNode != null) 
            { processLocalNewStateV(valueMapEpiNode); break goReturn; }
          
          valueMapEpiNode= messageMapEpiNode.getMapEpiNode("RemoteNewState");
          if (valueMapEpiNode != null) 
            { processRemoteStateV(valueMapEpiNode); break goReturn; }
          
          valueMapEpiNode= messageMapEpiNode.getMapEpiNode("RemoteCurrentState");
          if (valueMapEpiNode != null) 
            { processRemoteStateV(valueMapEpiNode); break goReturn; }
          
          theAppLog.debug("ConnectionManager.decodePeerMapEpiNodeV(..) ignoring"
            + NL + "  " + messageMapEpiNode); // Report message being ignored.
        } // goReturn:
          theAppLog.debug("ConnectionManager.decodePeerMapEpiNodeV(..) ends.");
          return;
        }

    private void processRemoteStateV(MapEpiNode subjectPeerMapEpiNode)
      /* This method does what is needed to process subjectPeerMapEpiNode, 
        the MapEpiNode body of a RemoteNewState and RemoteCurrentState messages.
        They have slightly different meanings, but are processed the same way.
        Each node contains data about whether a peer is active and
        therefore possibly able to accept connections.
        If it can accept connections, a connection is initiated.
        This implements a promiscuous connection behavior.
        */
      {
        theAppLog.debug("ConnectionManager.processRemoteUpdatedStateV(..) begins."
            + NL + "  subjectPeerMapEpiNode=" + subjectPeerMapEpiNode);
        toReturn: {
          if // Exit if subject peer is not connected to the remote peer.
            (! subjectPeerMapEpiNode.testB("isConnected")) 
            break toReturn;

          // Subject peer is active and connected to somebody.  
          // Do we want to connect to it also?

          PeersCursor thePeersCursor= // Create piterator for use. 
              PeersCursor.makeOnNoEntryPeersCursor( thePersistent );
          thePeersCursor.findOrAddPeerV(subjectPeerMapEpiNode); // Locate subject peer
            // data in Persistent storage, or create new data.
          if (thePeersCursor.testB("ignorePeer")) // This peer is supposed to be ignored?
            break toReturn; // so exit.
          if (thePeersCursor.testB("isConnected")) // We're already connected to this peer
            break toReturn; // so exit.
          if // Same IDs, so subject peer is actually the local peer
            ( thePeersCursor.getEmptyOrString("PeerIdentity").equals(
                thePersistent.getTmptyOrString("PeerIdentity")))
            break toReturn; // so exit.

          theAppLog.debug("ConnectionManager.processRemoteUpdatedStateV(MapEpiNode) "
            + "connecting to subject peer!!!!!!!!!!!");
          String peerIPString= thePeersCursor.getFieldString("IP");
          String peerPortString= thePeersCursor.getFieldString("Port");
          IPAndPort theIPAndPort= new IPAndPort(peerIPString, peerPortString);
          String theIdString= thePeersCursor.getFieldString("PeerIdentity");
          Unicaster theUnicaster= 
              theUnicasterManager.getOrBuildAddAndStartUnicaster(theIPAndPort,theIdString);
          theUnicaster.connectToPeerV(); // Message state-machine to connect.
        } // toReturn:
          theAppLog.debug("ConnectionManager.processRemoteUpdatedStateV(..) ends.");
          return;
        }

    private void processLocalNewStateV(MapEpiNode subjectPeerMapEpiNode)
      /* This method does what is needed to process the subjectPeerMapEpiNode
        received in a LocalNewState message about a subject peer. 
        */
      {
        notifyPeersAboutPeerV(subjectPeerMapEpiNode); // To our peers,
          // send information about the status change of the subject peer.
        
        notifyPeerAboutPeersV(subjectPeerMapEpiNode); // To the subject peer,
          // send the statuses of all our [other] peers.
        }

    private void notifyPeersAboutPeerV(MapEpiNode messagePeerMapEpiNode)
      /* This method notifies all connected peers about
        the changed connection status of the peer described by messagePeerMapEpiNode.
        */
      {
          theAppLog.debug( "ConnectionManager.notifyPeersAboutPeerV() called.");
          PeersCursor scanPeersCursor= // Used for iteration. 
              PeersCursor.makeOnNoEntryPeersCursor( thePersistent );
        peerLoop: while (true) { // Process all peers in my peer list. 
          if (scanPeersCursor.nextKeyString().isEmpty() ) // Try getting next scan peer. 
            break peerLoop; // There are no more peers, so exit loop.
          theAppLog.appendToFileV("(to-peers?)"); // Log that peer is being considered.
          if (! scanPeersCursor.testB("isConnected")) // This peer is not connected 
            continue peerLoop; // so loop to try next peer.
          String peerIPString= scanPeersCursor.getFieldString("IP");
          String peerPortString= scanPeersCursor.getFieldString("Port");
          IPAndPort theIPAndPort= new IPAndPort(peerIPString, peerPortString);
          Unicaster scanUnicaster= // Try getting associated Unicaster.
              theUnicasterManager.tryingToGetUnicaster(theIPAndPort);
          if (scanUnicaster == null) { // Unicaster of scan peer doesn't exist
            theAppLog.error(
                "ConnectionManager.notifyPeersAboutPeerV() non-existent Unicaster.");
            continue peerLoop; // so loop to try next peer.
            }
          theAppLog.appendToFileV("(YES!)"); // Log that we're sending data.
          scanUnicaster.putV( // Queue scan peer data to Unicaster of scan peer
            MapEpiNode.makeSingleEntryMapEpiNode( // wrapped in a RemoteNewState map.
              "RemoteNewState", messagePeerMapEpiNode)
            );
        } // peerLoop: 
          theAppLog.appendToFileV("(end of peers)"+NL); // Mark end of list with new line.
        }

    private void notifyPeerAboutPeersV(MapEpiNode messagePeerMapEpiNode)
      /* This method notifies the peer described by messagePeerMapEpiNode
        about the status of all [other] peers.
        This should be called when a peer connects.
        This produces a lot of packets, but shouldn't run often.
        */
      {
        toReturn: {
          theAppLog.debug("ConnectionManager.notifyPeerAboutPeersV() called.");
          if (! messagePeerMapEpiNode.testB("isConnected")) // Message peer not connected 
            break toReturn; // so end processing.
          String theIPString= messagePeerMapEpiNode.getString("IP");
          String thePortString= messagePeerMapEpiNode.getString("Port");
          IPAndPort theIPAndPort= new IPAndPort(theIPString, thePortString);
          Unicaster messageUnicaster= 
              theUnicasterManager.tryingToGetUnicaster(theIPAndPort);
          if (messageUnicaster== null) // Peer in message has no Unicaster
            break toReturn; // so end processing.
          PeersCursor scanPeersCursor= // Create cursor to be used for peer iteration. 
              PeersCursor.makeOnFirstEntryPeersCursor( thePersistent );
        peerLoop: while (true) { // Process all peers in my peer list. 
          if (scanPeersCursor.nextKeyString().isEmpty() ) // Try getting next scan peer. 
            break peerLoop; // There are no more peers, so exit loop.
          theAppLog.appendToFileV("(to-peer?)"); // Log that we're considering peer.
          if (! scanPeersCursor.testB("isConnected")) // This peer is not connected 
            continue peerLoop; // so loop to try next peer.
          theAppLog.appendToFileV("(YES!)"); // Log that we're sending data.
          messageUnicaster.putV( // Queue peer data for sending
              MapEpiNode.makeSingleEntryMapEpiNode( // wrapped in another map.
                "RemoteCurrentState", scanPeersCursor.getSelectedMapEpiNode())
              );
        } // peerLoop: 
          theAppLog.appendToFileV("(end of peers)"+NL); // Mark end of list with new line.
        } // toReturn:
          return;
        }


    // TextStream message processing.

    private boolean tryProcessingByTextStreamB(MapEpiNode theMapEpiNode)
      /* Returns true if TextStream was able to process, false otherwise.
        */
      {
        return theTextStream.tryProcessingMapEpiNodeB(theMapEpiNode);
        }

    } // class ConnectionManager.
