package allClasses;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import allClasses.LockAndSignal.Input;

import static allClasses.Globals.*;  // appLogger;

public class ConnectionManager 

  extends MutableList

  implements Runnable

  /* This class manages connections, both Unicast and Multicast,
    with other Infogora peer nodes.
    It makes use of the class UnicasterManager as a helper.

    It extends MutableList to manage its list of connected peers.
    Because this list is accessed by a TreeModel for a GUI JTree,
    the List is synchronously on the Event Dispatch Thread (EDT).

    ?? This class makes use of other threads 
    to manage the individual connections.
    It receives inputs from those threads via thread-safe queues.
    Some work is needed to better manage these threads,
    because DatagramSockets must be closed to terminate these threads,
    and recovery from an IOException on these sockets is troublesome.

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
    * another socket for unicast receiving and all sending.
    Their ports must be different/unique.

    There are several types of thread classes defined in this file
    or in other java files.
    * ConnectionManager: the main thread for this class.
      It manages everything.  
    * UnconnectedReceiver: receives packets for ConnectionManager.
    * Unicaster: manages one connection.  
      ? Maybe rename to ConnectionManager?
    * MulticastReceiver: used to receive multicast packets for Multicaster.
    * Multicaster: sends and receives multicast packets
      used for discovering other peers on the LAN.
    * Sender: does the actual calls to send(..) to send all packets.

    An IOException can be caused by external events,
    such as a link going down, the computer going to sleep, etc.
    These are handled by closing the socket, opening another,
    and retrying the operation.
    In the case of the MulticastReceiver and UnconnectedReceiver,
    new DatagramSockets are passed to the constructors because
    closing the socket is the only way to terminate receive() operations. 
    There should be a better way of doing this so 
    the threads don't need to be completely reconstructed ??

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
	    
			private AppGUIFactory theAppGUIFactory;
	    
	  	private UnicasterManager theUnicasterManager;

	    private LockAndSignal cmThreadLockAndSignal;  // LockAndSignal for this thread.
      /* This single object is used to synchronize communication between 
        the ConnectionManager and all threads providing data to it.
				It should be the same LockAndSignal instance used in the construction
				of the input queues that follow.
        It man also be used separately to signal asynchronous inputs
        such as the socket open/closed state.
        The old way of synchronizing inputs used 
        an Objects for a lock and a separate boolean signal.
        */
	    private NetcasterQueue multicasterToConnectionManagerNetcasterQueue; 
      // Queue of multicast packets received from Multicaster.

	    // Inputs to the connection manager thread.
	    private NetcasterQueue unconnectedReceiverToConnectionManagerNetcasterQueue;
	    	// Queue of unconnected unicast packets received from Unicasters.

    // Other instance variables, all private.
		  private MulticastSocket theMulticastSocket; // For multicast receiver. 
	    private EpiThread multicasterEpiThread ; // Its thread.
  		private InetAddress multicastInetAddress;
		
	    public DatagramSocket unconnectedDatagramSocket; // For UDP io.
	      // It is used for receiving unicast packets and sending both types.

	    private EpiThread theUnconnectedReceiverEpiThread ; // its thread.

	    private EpiThread theSenderEpiThread ; // its thread.
	


    public ConnectionManager(  // Constructor.
    		AppGUIFactory theAppGUIFactory,
    		UnicasterManager theUnicasterManager,
        DataTreeModel theDataTreeModel,
    		LockAndSignal cmThreadLockAndSignal,
    		NetcasterQueue multicasterToConnectionManagerNetcasterQueue,
    		NetcasterQueue unconnectedReceiverToConnectionManagerNetcasterQueue
    		)
      {
        super(  // Constructing base class.
          theDataTreeModel, // For receiving tree change notifications.
          "Connection-Manager", // DataNode (not thread) name.
          emptyListOfDataNodes()
          );

        // Storing other dependencies injected into this class.
  	    this.theAppGUIFactory= theAppGUIFactory;
  	    this.theUnicasterManager= theUnicasterManager; 
  	    this.cmThreadLockAndSignal= cmThreadLockAndSignal;
  	    this.multicasterToConnectionManagerNetcasterQueue=
  	    		multicasterToConnectionManagerNetcasterQueue;
  	    this.unconnectedReceiverToConnectionManagerNetcasterQueue=
  	    		unconnectedReceiverToConnectionManagerNetcasterQueue;
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
    		initializingV();  // Doing non-injection initialization.

        processingInputsAndExecutingEventsV(); // Until thread termination...
          // ...is requested.

        stoppingAllThreadsV();
        }

    private void initializingV()
    	// This method does non-injection initialization.
      {
		    addB( theUnicasterManager ); // Adding UnicasterManager to our list.

				try { // Doing this here is a bit of a kludge.
					  multicastInetAddress= InetAddress.getByName("239.255.0.0"); }
				  catch ( UnknownHostException e ) { 
          	Globals.logAndRethrowAsRuntimeExceptionV( "initializingV()", e );
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

          /* At this point, at least the inputs that arrived before 
            the last notification signal should have been processed. 
            */
	        } // while (true)
        return;
        }

    private void stoppingAllThreadsV()
      // Possibly use a different stop order??
      {
        theUnicasterManager.stoppingEntryThreadsV();

        stoppingUnicastReceiverThreadV();
        stoppingMulticasterThreadV(); 
    	  stoppingSenderThreadV();
        }

	  private void maintainingDatagramSocketAndDependentThreadsV( )
      /* This method creates the DatagramSocket and 
        the threads which depend on it
        if the socket has not been opened yet.
        It does this again if the socket was open but has been closed.  
       */
	    { 
    	  if // Preparing socket and dependencies if socket not working.
    	    ( EpiDatagramSocket.isNullOrClosedB( unconnectedDatagramSocket ) )
	    	  preparingAll: { // Preparing socket and dependencies.
        	  stoppingSenderThreadV();
            stoppingUnicastReceiverThreadV();
    	    	preparingSocketLoop: while (true) {
              if ( EpiThread.exitingB() )
              	break preparingAll;
    	  	  	prepareDatagramSocketV();
    	  	  	if ( ! EpiDatagramSocket.isNullOrClosedB( 
    	  	  			unconnectedDatagramSocket 
    	  	  			) )
    	  	  		break preparingSocketLoop;
    	  	  	} // preparingSocketLoop:
	      	  startingSenderThreadV();
	          startingUnicastReceiverThreadV();
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
		          PortManager.getLocalPortI()  // ...app's local port.
		          ) // Note, the IP is not defined.
		        );
		      }
		    catch ( SocketException e ) { // Handling SocketException.
		      appLogger.error("unconnectedDatagramSocket:"+e);
		      if ( unconnectedDatagramSocket != null )
		        unconnectedDatagramSocket.close();
		      EpiThread.interruptableSleepB(  // Don't hog CPU in error loop.
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
          //  "ConnectionManager.processingUnconnectedSockPacketsB()\n  "
          //  + theKeyedPacket.getSocketAddressesString()
          //  );
          passToUnicasterV( theNetcasterPacket );

          packetsProcessedB= true;
          }
          
        return packetsProcessedB;
        }

    private void stoppingUnicastReceiverThreadV()
	    {
    		EpiDatagramSocket.closeIfNotNullV(  // Causing unblock and termination.
    				unconnectedDatagramSocket
    				);
    		EpiThread.stopAndJoinIfNotNullV(theUnconnectedReceiverEpiThread);
	      }

    private void stoppingSenderThreadV()
	    {
	  		EpiDatagramSocket.closeIfNotNullV(  // Causing unblock and termination.
	  				unconnectedDatagramSocket
	  				);
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
	    	  preparingAll: { // Preparing socket and dependencies. 
            stoppingMulticasterThreadV();
    	    	preparingSocketLoop: while (true) {
              if ( EpiThread.exitingB() )
              	break preparingAll;
    	  	  	prepareMulcicastSocketV();
    	  	  	if ( ! EpiDatagramSocket.isNullOrClosedB( theMulticastSocket ) )
    	  	  		break preparingSocketLoop;
    	  	  	} // preparingSocketLoop:
            startingMulticasterThreadV();
	    	  	} // preparingAll: 
	    	}

	  private void prepareMulcicastSocketV()
    // Makes one attempt to create theMulticastSocket.
	  {
	    try { // Creating a new unconnected DatagramSocket and using it.
	    	theMulticastSocket= theAppGUIFactory.makeMulticastSocket(
		      PortManager.getDiscoveryPortI()  // ...bound to Discovery port.
		      );
	      }
	    catch ( IOException e ) { // Handling SocketException.
	      appLogger.error("theMulticastSocket:"+e);
	      if ( theMulticastSocket != null )
	      	theMulticastSocket.close();
	      EpiThread.interruptableSleepB(  // Don't hog CPU in error loop.
	      		Config.errorRetryPause1000MsL
	      		);
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
    		addB( theMulticaster );  // Add to DataNode List.
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
  				theUnicasterManager.getOrBuildAddAndStartUnicaster( 
	      		theNetcasterPacket 
	      		);

          packetsProcessedB= true;
          }
          
        return packetsProcessedB;
        }

    private void stoppingMulticasterThreadV()
	    {
				EpiDatagramSocket.closeIfNotNullV(  // Causing unblock and termination.
						theMulticastSocket
						);
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
        //appLogger.info(
        //  "ConnectionManager.createAndPassToUnicasterV(..)\n  "
        //  + theKeyedPacket.getSocketAddressesString()
        //  );
    		Unicaster theUnicaster=  // Getting the appropriate Unicaster.
    				theUnicasterManager.getOrBuildAddAndStartUnicaster( 
		      		theNetcasterPacket 
		      		);
	      theUnicaster.puttingKeyedPacketV( // Giving to it its first packet.  
	      		theNetcasterPacket
	      		);
        }

    } // class ConnectionManager.
