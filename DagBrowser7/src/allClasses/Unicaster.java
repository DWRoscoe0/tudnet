package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Random;

import allClasses.LockAndSignal.Input;

public class Unicaster

	extends Netcaster

  implements Runnable 
  
  /* Each instance of this nested class contains and manages data about 
    one of the peer nodes of which the ConnectionManager is aware.
    Its job is to manage a single Datagram connection
    with another peer node.
    
    This class is not a Thread, but is a Runnable on which to base a Thread.
    The thread will informally implement a state machine
    which manages the connection.
    
    Originally it was planned for each Unicaster thread to 
    send and receive packets using a connected DatagramSocket.
    Different peers would use different DatagramSockets,
    each with different remote addresses, but the same local addresses.
    Bind errors can be avoided by using 
    DatagramSocket.setReuseAddress(true).
    Unfortunately, such connected and bound DatagramSockets
    will not receive any packets if there is 
    an unconnected DatagramSocket bound to the same local address
    used for receiving the initial connection requests.
    Closing the unconnected socket allows the connected ones to work,
    but reopening the unconnected socket disables the 
    connected ones again.
    As a result, connected sockets are not used.
    Instead all packets are received by one unconnected DatagramSocket, 
    and those packets are de-multiplexed and forwarded to 
    the appropriate peer thread.

    The plan is to add functionality to this class in small stages.
    At first peers simply exchanged packets,
    waiting half a period, making no attempt to measure ping times.
    More functionality will be added later.

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

  { // Unicaster.

    // Fields (constant and variales).
    
      // Injected dependency instance variables.
      private final Shutdowner theShutdowner;
      private final UnicasterManager theUnicasterManager;
      private final SubcasterManager theSubcasterManager;

      // Other instance variables.
      private Random theRandom; // For arbitratingYieldB() random numbers.
      private boolean arbitratedYieldingB; // Used to arbitrate race conditions.

			public Unicaster(  // Constructor. 
			  UnicasterManager theUnicasterManager,
			  SubcasterManager theSubcasterManager,
	    	LockAndSignal threadLockAndSignal,
	      NetInputStream theNetInputStream,
	      NetOutputStream theNetOutputStream,
	      IPAndPort remoteIPAndPort,
        DataTreeModel theDataTreeModel,
        Shutdowner theShutdowner
        )
      /* This constructor constructs a Unicaster for the purpose of
        communicating with the node at remoteInetSocketAddress,
        but no response has yet been made.
        Fields are defined in a way to cause an initial response.
        
        ?? Add parameter which controls whether thread first waits for
        a PING or an ECHO, to reduce or eliminate ping-ping conflicts.
        Implement protocol with a state-machine.
        */
      {
        super(
        		threadLockAndSignal,
	  	      theNetInputStream,
	  	      theNetOutputStream,
  	        theDataTreeModel,
  	        remoteIPAndPort,
        		"Unicaster" 
        		);

        // Storing injected dependency constructor arguments.
          this.theShutdowner= theShutdowner;
  			  this.theUnicasterManager= theUnicasterManager;
  			  this.theSubcasterManager= theSubcasterManager;
        }


    public void run()  // Main Unicaster thread.
      /* This method contains the main thread logic in the form of
        a state machine composed of the highest level states.
        The only thing it does now is exchange ping and echo packets
        with the remote peer, first one way, then the other.
        It terminates if it fails to receive a reply to 4 consecutive pings.
        
        ?? This is only temporary because 
        packets which are not either ping or echo are ignored,
        so a connection can do nothing else.
        Later the protocol will be expanded by adding
        more types of packets, or 
        packets will be de-multiplexed by protocol.
        In this case Packets would contain 
        a sub-protocol value for this purpose.
        */
      {
        try { // Operations that might produce an IOException.
	          initializingV();
	          
	          theSubcasterManager.buildAddAndStartSubcaster( // Adding Subcaster.
	              "PING-REPLY." //??? Hard wired at first.
	      	  		);

	          { //??? Uncomment only one of the following method calls.
	          	runWithoutSubcastersV(); // Original code.
	          	// runWithSubcastersV(); // Experiment code with Subcaster.
		          }
	          
	      		if  // Informing remote end whether we are doing Shutdown.
	      		  ( theShutdowner.isShuttingDownB() ) 
		      		{
	      				sendingMessageV("SHUTTING-DOWN"); // Informing peer of shutdown.
	  	          appLogger.info( "SHUTTING-DOWN message sent.");
		      			}
	      		theNetOutputStream.close(); // Closing output stream.
          	}
          catch( IOException e ) {
          	Globals.logAndRethrowAsRuntimeExceptionV( 
          			"run() IOException", e 
          			);
            }

        theSubcasterManager.stoppingEntryThreadsV();
	    	theUnicasterManager.removingV( this ); // Removing self from manager.

        appLogger.info("run() exiting."); // Needed if thread self-terminates.
        }

    protected void initializingV()
	    throws IOException
	    {
      	super.initializingV();
        theRandom= new Random(0);  // Initialize arbitratingYieldB().
	  	  arbitratedYieldingB= arbitratingYieldB();
	      addB( 	RoundTripTimeNamedInteger= new NamedInteger( 
			      		theDataTreeModel, "Round-Trip-Time-ns", 0 
			      		)
	      			);
	  	  
		    addB( theSubcasterManager ); // Adding to our list.
	    	}

    public void runWithSubcastersV() throws IOException //???
      // Does (or will do) full PING-REPLY protocol using a Subcaster.
	    {
			  while (true) // Repeating until thread termination is requested.
				  {
		    		if // Exiting if requested.
		          ( Thread.currentThread().isInterrupted() ) 
		          break;

		    		while ( theNetInputStream.available() > 0 ); //??? send packet.
		    		
		    		//??? Get packets from Subcasters and send to peer.
		    		
		    		netcasterLockAndSignal.doWaitE(); // Waiting for any input.
		      	}
	    	}

    public void runWithoutSubcastersV() throws IOException //??? Needn't be public.
      // Does full PING-REPLY protocol without help of Subcaster.
	    {
	    	int stateI= // Initialize ping-reply protocol state from yield flag. 
	      	  arbitratedYieldingB ? 0 : 1 ;
	      while (true) // Repeating until thread termination is requested.
	        {
	      		if   // Exiting if requested.
	            ( Thread.currentThread().isInterrupted() ) 
	            break;
	      	  switch ( stateI ) { // Decoding alternating state.
	        	  case 0:
	              //appLogger.info(getName()+":\n  CALLING tryingPingSendV() ===============.");
	              tryingPingSendV();
	              stateI= 1;
	              break;
	        	  case 1:
	              //appLogger.info(getName()+":\n  CALLING tryingPingReceiveV() ===============.");
	              tryingPingReceiveV();
	              stateI= 0;
	              break;
	        	  }
	          } // while(true)
	    	}

    private void tryingPingSendV() throws IOException
      /* This method does the sub-protocol of trying to 
        send a ping to the remote peer and receiving an echo response.
        If it doesn't receive an echo packet in response
        within one-half period, it tries again.
        It tries several times before giving up and 
        terminating the current thread.
        If it receives a ping instead of an echo then
        it might give up this sub-protocol.
        */
      {
        LockAndSignal.Input theInput;  // Type of input that ends waits.
        int maxTriesI= 3;
        int triesI= 1;
        pingEchoRetryLoop: while (true) { // Sending ping and receiving echo.
          if  // Checking and exiting if maximum attempts have been done.
            ( triesI > maxTriesI )  // Maximum attempts exceeded.
            { // Terminating thread.
              appLogger.info(
              		"Requesting termination after "
                	+"\n  "+maxTriesI+" ECHO time-outs."
                  );
              Thread.currentThread().interrupt(); // Starting termination.
              break pingEchoRetryLoop;
              }
          sendingMessageV("PING"); // Sending ping packet.
          pingSentNanosL= System.nanoTime(); // Recording ping send time in ns
          long pingSentMsL= System.currentTimeMillis(); // and in ms.
          echoWaitLoop: while (true) { // Handling echo or other conditions.
            theInput=  // Awaiting next input within reply interval.
            		testWaitInIntervalE( pingSentMsL, HalfPeriodMillisL );
            if ( theInput == Input.TIME ) // Exiting echo wait if time-out.
              { appLogger.info( "Time-out waiting for ECHO: "+triesI );
                break echoWaitLoop;  // End wait to send new PING, maybe.
              	}
            if // Handling SHUTTING-DOWN packet or interrupt by exiting.
    	    		( tryingToCaptureTriggeredExitB( ) )
  	    			break pingEchoRetryLoop; // Exit everything.
        		theNetInputStream.mark(0); // Preparing to not consume message.
        		String inString= readAString(); // Reading message.
        		if ( inString.equals( "ECHO" ) ) // Handling echo, maybe.
              { // Handling echo and exiting.
                RoundTripTimeNamedInteger.setValueL(
                		(System.nanoTime() - pingSentNanosL)
                		); // Calculating RoundTripTime.
            		break pingEchoRetryLoop; // Exit everything.
                }
        		if ( inString.equals( "PING" ) ) // Handling ping conflict, maybe.
              { // Handling ping conflict.
                appLogger.info( "PING-PING conflict." );
              	if ( arbitratedYieldingB ) // Arbitrating ping-ping conflict.
                  { // Yielding ping processing to other peer.
                    appLogger.info( "PING ping yield: "+triesI );
                		theNetInputStream.reset(); // Putting message back.
                    break pingEchoRetryLoop; // Yielding by exiting main loop.
                    }
              		else
              		{ appLogger.info( "PING ping not yielding: "+triesI );
              			// Ignoring this PING.
                    break echoWaitLoop;  // End wait to send new PING, maybe.
              			}
              	  }
            //appLogger.debug( 
        		//		"tryingPingSendV(): unexpected: "
        		//	+ inString
        		//	+ " from "
        		//	+ PacketStuff.gettingPacketString( 
        		//			theNetInputStream.getSockPacket().getDatagramPacket()
        		//			)
        		//	);
        		// Ignoring the message that was gotten, whatever it was.
            } // echoWaitLoop:
          triesI++;
          } // pingEchoRetryLoop: 
        }

    private void tryingPingReceiveV() throws IOException
	    /* This method does the sub-protocol of trying to 
		    receive a PING message from the remote peer 
		    to which it replies by sending an ECHO response.
        If a PING is not immediately available then
        it waits up to PeriodMillisL for a PING to arrive,
        after which it gives up and returns.
        If a PING is received it responds immediately by sending an ECHO,
        after which it waits PeriodMillisL while ignoring all
        received messages except SHUTTING-DOWN.
        It will exit immediately if isInterrupted() becomes true
        or a SHUTTING-DOWN message is received.
        */
      {
        LockAndSignal.Input theInput;  // Type of input that ends wait.
        long pingWaitStartMsL= System.currentTimeMillis();
        pingWaitLoop: while (true) { // Processing until something causes exit.
      		if // Handling SHUTTING-DOWN packet or interrupt by exiting.
	    			( tryingToCaptureTriggeredExitB( ) )
	    			break pingWaitLoop;
      		// Note, can't readAString() here because it might not be available.
          if // Handling a received ping if present.
            ( tryingToGetStringB( "PING" ) )
            { // Handling received ping, then exit.
              sendingMessageV("ECHO"); // Sending echo packet as reply.
              long echoSentMsL= System.currentTimeMillis();
              while (true) { // Ignoring packets for a while, then exit.
                theInput=  // Awaiting input within the ignore interval.
                		testWaitInIntervalE( echoSentMsL, PeriodMillisL );
                if ( theInput == Input.TIME ) // Exiting if time limit reached.
                  break pingWaitLoop;  
            		if // Exiting everything if exit has been triggered.
            			( tryingToCaptureTriggeredExitB( ) )
            			break pingWaitLoop;
            		readAString(); // Reading message and ignoring it.
              	} // while (true)
              }
      		tryingToGetString(); // Reading and ignoring any message.

          theInput= testWaitInIntervalE( // Awaiting next input.
          		pingWaitStartMsL, PeriodMillisL + HalfPeriodMillisL
          		);
          if ( theInput == Input.TIME ) // Exiting outer loop if time-out.
	          {
	            appLogger.info( "Time-out waiting for PING." );
	            break pingWaitLoop;  // Exiting to abort wait.
	          	}
          } // pingWaitLoop
        }

    private boolean arbitratingYieldB()
      /* This method arbitrates when this local peer is trying
        to do the same thing as the remote peer.
        When executed on both peers, on one it should return true
        and on the other it should return false.
        It returns true when this peer should yield,
        and false when it should not.

        This method is not designed to be fair, 
        but to simply help resolve conflicts quickly.
        When used it put two connected peers into
        complementary parts of their ping-reply protocols.
        It is used:
        * When two connected Unicaster threads start.
        * To resolve ping-ping conflicts, which is when
          two connected Unicasters somehow end up in
          the same parts of their protocols.

        This method is based on comparing each peer's address information.
        Presently it uses port information.
        ?? It should probably use something more unique,
        such as IP address or other unique ID number,
        but one's own IP address is more difficult to get than port.  
        */
      {
        boolean yieldB;  // Storage for result.
        int addressDifferenceI=  // Calculate port difference.
          getKeyK().getPortI() - PortManager.getLocalPortI();
        if ( addressDifferenceI != 0 )  // Handling ports unequal.
          yieldB= ( addressDifferenceI < 0 );  // Lower ported peer yields.
          else  // Handling rare case of ports equal.
          {
            theRandom.setSeed(System.currentTimeMillis());  // Reseting...
              // ...the random number generator seed with current time.
            yieldB= theRandom.nextBoolean();  // Yield randomly.
            }
        //appLogger.info("arbitratingYieldB() = " + yieldB);
        return yieldB;
        }

    // Receive packet code.  This might be enhanced with streaming.

    public void puttingReceivedPacketV( NetcasterPacket theNetcasterPacket )
      /* This method is used by UnicastReceiver threads.
        The one associated with this Unicaster thread uses this method
        to add theNetcasterPacket to this thread's receive queue.
       */
      {
        //receiverToNetcasterPacketQueue.add(theNetcasterPacket);
    		theNetInputStream.getPacketQueue().add(theNetcasterPacket);
        }

    } // Unicaster.
