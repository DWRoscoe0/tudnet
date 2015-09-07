package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Random;

import allClasses.LockAndSignal.Input;

public class Unicaster

	extends NetCaster

  implements Runnable, JobStatus  // JobStatus code is no longer needed?? 
  
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
    an unonnected DatagramSocket bound to the same local address
    used for receiving the initial connection requests.
    Closing the unconnected socket allows the connected ones to work,
    but reopening the unconnected socket disables the 
    connected ones again.
    As a result, connected sockets are not used.
    Instead all packets are received by one unconnected DatagramSocket, 
    and those packets are demultiplexed and forwarded to 
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
    
      // Constants.
      private final long PeriodMillisL=  // Period between sends or receives.
        4000;   // 4 seconds.
      private final long HalfPeriodMillisL= // Half of period.
        PeriodMillisL / 2;  

      // Injected dependency instance variables.
      private final PacketQueue sendQueueOfSockPackets;
        // Queue to receive SockPackets to be sent to Unicaster.
    	@SuppressWarnings("unused") // ??
    	private JobQueue<Unicaster> cmJobQueueOfUnicasters;
      private final DatagramSocket unconnectedDatagramSocket;
      private final ConnectionManager theConnectionManager;
      private final Shutdowner theShutdowner;

      // Other instance variables.
      LockAndSignal peerLockAndSignal;  // LockAndSignal for this thread.
      int packetIDI; // Sequence number for sent packets.
      private Random theRandom; // For arbitratingYieldB() random numbers.
      private final PacketQueue receiveQueueOfSockPackets;
        // Queue for SockPackets from ConnectionManager.
      private long pingSentAtNanosL; // Time the last ping was sent.
      private boolean arbitratedYieldingB; // Used to arbitrate race conditions.

      // Detail-containing child sub-objects.
	      private NamedInteger RoundTripTimeNamedInteger; 


    public Unicaster(  // Constructor. 
        InetSocketAddress remoteInetSocketAddress,
        PacketQueue sendQueueOfSockPackets,
        JobQueue<Unicaster> cmJobQueueOfUnicasters,
        DatagramSocket unconnectedDatagramSocket, // No longer used??
        DataTreeModel theDataTreeModel,
        ConnectionManager theConnectionManager,
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
  	        theDataTreeModel,
  	        remoteInetSocketAddress,
        		"Unicaster-at-" 
        		);

        // Storing injected dependency constructor arguments.
          this.sendQueueOfSockPackets= sendQueueOfSockPackets;
          this.cmJobQueueOfUnicasters= cmJobQueueOfUnicasters;
          this.unconnectedDatagramSocket= unconnectedDatagramSocket;
          this.theConnectionManager= theConnectionManager;
          this.theShutdowner= theShutdowner;

        peerLockAndSignal= new LockAndSignal(false);

        packetIDI= 0; // Setting starting packet sequence number.
        
        theRandom= new Random(0);  // Initialize arbitratingYieldB().

        receiveQueueOfSockPackets=
          new PacketQueue( peerLockAndSignal );
            // For SockPackets from ConnectionManager.
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
        a protocol value for this purpose.
        */
      {
        theConnectionManager.addingV( this );

      	int stateI= // Initialize ping-reply protocol state from yield flag. 
    	  		arbitratedYieldingB ? 0 : 1 ;
        try { // Operations that might produce an IOException.
          initializeV();

          while (true) // Repeating until thread termination is requested.
            {
          		recordStatusV( 0 ); // ??
              if   // Exiting if requested.
	              ( Thread.currentThread().isInterrupted() ) 
	              break;
          	  switch ( stateI ) { // Decoding alternating state.
	          	  case 0:
	                //appLogger.info(getName()+":\n  CALLING tryingPingSendV() ===============.");
	                tryingPingSendV();
	                stateI= 1;
	          	  	
	          	  case 1:
	                //appLogger.info(getName()+":\n  CALLING tryingPingReceiveV() ===============.");
	                tryingPingReceiveV();
	                stateI= 0;
	          	  }
              }
	      		if  // Informing remote end if we are doing Shutdown.
	      		  ( theShutdowner.isShuttingDownB() ) 
		      		{
	      				sendingPacketV("SHUTTING-DOWN"); // Sending SHUTTING-DOWN packet.
	  	          appLogger.info( "SHUTTING-DOWN packet sent.");
		      			}
          	}
          catch( IOException e ) {
            appLogger.info("run() IOException: "+e );
            throw new RuntimeException(e);
            }

        theConnectionManager.removingV( this );

        appLogger.info("Unicaster run() exitting.");
        }

    protected void initializeV()
	    throws IOException
	    {
      	super.initializeV();
	      addB( 	RoundTripTimeNamedInteger= new NamedInteger( 
			      		theDataTreeModel, "Round-Trip-Time-ns", 0 
			      		)
	      			);
	  	  arbitratedYieldingB= arbitratingYieldB();
	    	
	    	}

    private void recordStatusV( int conditionI )
      /* This method records the connection status.
        It logs or displays the status if it has changed significantly.
        */
    	{}

    private void tryingPingSendV()
      /* This method tries to send a ping to the remote peer
        and receive an echo response.
        If it doesn't receive an echo packet in response
        within one-half period, it tries again.
        It tries several times before giving up and 
        terminating the current thread.
        */
      {
        LockAndSignal.Input theInput;  // Type of input that ends waits.
        int maxTriesI= 3;
        int triesI= 1;
        pingEchoRetryLoop: while (true) { // Sending ping getting echo.
          if  // Checking and exiting if maximum retries were exceeded.
            ( triesI > maxTriesI )  // Maximum attempts exceeded.
            { // Terminating thread.
              appLogger.info(
                	Thread.currentThread().getName()+
                	  ": requesting termination after "+maxTriesI+" ECHO"+
                	  "  \n  time-outs."
                  );
              Thread.currentThread().interrupt(); // Starting termination.
              break pingEchoRetryLoop;
              }
          sendingPacketV("PING"); // Sending ping packet.
          pingSentAtNanosL= System.nanoTime(); // Recording ping send time.
          echoWaitLoop: while (true) { // Handling echo or other conditions.
            theInput=  // Awaiting next input.
            		testWaitWithTimeOutE( HalfPeriodMillisL );
            if ( theInput == Input.TIME ) // Exiting echo wait if time-out.
              { appLogger.info( "Time-out waiting for ECHO: "+triesI );
                break echoWaitLoop;  // Exiting wait loop to send PING.
              	}
            else if // Handling SHUTTING-DOWN packet or interrupt by exiting.
    	    		( tryingToCaptureTriggeredExitB( ) )
  	    			break pingEchoRetryLoop; // Exit everything.
        		else if ( tryingToGetPacketB( "ECHO" ) ) // Handling echo, maybe.
              { // Handling echo and exiting.
                RoundTripTimeNamedInteger.setValueL(
                		(System.nanoTime() - pingSentAtNanosL)
                		); // Calculating RoundTripTime.
                // This should handle unwanted received packets.
    	    			break pingEchoRetryLoop; // Exit everything.
                }
        		else if ( testingPacketB( "PING" ) ) // Handling ping conflict, maybe.
              { // Handling ping conflict.
                appLogger.info( "PING-PING conflict." );
              	if ( arbitratedYieldingB ) // Arbitrating ping-ping conflict.
                  { // Yielding ping processing to other peer.
                    appLogger.info( "PING ping yield: "+triesI );
                    break pingEchoRetryLoop; // Yielding by exiting loop.
                    // Ping packet remains in queue.
                    }
              		else
              		{ appLogger.info( "PING ping not yielding: "+triesI );
              			tryingToConsumeOnePacketB();  // Consuming PING.
              			}
              	  }
        		else 
        			tryingToConsumeOnePacketB();  // Consume any other packet.
            } // echoWaitLoop:
          triesI++;
          } // pingEchoRetryLoop: 
        }

    private void tryingPingReceiveV()
      /* This method tries to process a received PING packet 
        from the remote peer to which it replies by sending an ECHO response.
        If a PING is not immediately available then
        it waits up to PeriodMillisL for a PING to arrive,
        after which it gives up and returns.
        If a PING is received it responds immediately by sending an ECHO,
        after which it waits PeriodMillisL while ignoring all
        received packets except Shutdowner.
        It will exit immediately if isInterrupted() becomes trun
        or a Shutdowner packet is received.
        */
      {
        LockAndSignal.Input theInput;  // Type of input that ends wait.
        long pingTimeMillisL= // Calculating latest okay ping receive time.
          System.currentTimeMillis()
          + PeriodMillisL
          + HalfPeriodMillisL;
        pingWaitLoop: while (true) { // Processing until something causes exit.
      		if // Handling SHUTTING-DOWN packet or interrupt by exiting.
	    			( tryingToCaptureTriggeredExitB( ) )
	    			break pingWaitLoop;
          if // Handling a received ping if present.
            ( tryingToGetPacketB( "PING" ) )
            { // Handling received ping, then exit.
              sendingPacketV("ECHO"); // Sending echo packet as reply.
              while (true) { // Ignoring packets until end time, then exit.
                theInput=  // Awaiting input.
                		testWaitWithTimeOutE( PeriodMillisL );
                if ( theInput == Input.TIME ) // Exiting if time limit reached.
                  break pingWaitLoop;  
            		if // Exiting everything if exit has been triggered.
            			( tryingToCaptureTriggeredExitB( ) )
            			break pingWaitLoop;
            		tryingToConsumeOnePacketB();  // Consume any other packet.
              	} // while (true)
              }
      		tryingToConsumeOnePacketB();  // Consume any other packet.
          theInput= testWaitWithTimeOutE( // Awaiting next input.
          		pingTimeMillisL - System.currentTimeMillis()
          		);
          if ( theInput == Input.TIME ) // Exiting outer loop if time-out.
	          {
	            appLogger.info( "Time-out waiting for PING." );
	            break pingWaitLoop;  // Exiting to abort wait.
	          	}
          } // pingWaitLoop
        }
    
    
    
    private Input testWaitWithTimeOutE( long timeOutMsL)
      /* This is a special test-and-wait method which will return immediately 
        with Input.NOTIFICATION if a received packet available for processing,
        otherwise it will do a LockAndSignal.doWaitWithTimeOutE(..).
        So it might block, or it might not.
       */
	    {
	      return ( testingPacketB( ) ) // Returning the appropriate thing.
	        ? Input.NOTIFICATION // Returning immediately because packet is ready.
	        : peerLockAndSignal.doWaitWithTimeOutE( // Returning from normal wait. 
	        		timeOutMsL
	        		);
	      }

    private boolean arbitratingYieldB()
      /* This method arbitrates when this local peer is trying
        to do the same thing as the remote peer.
        When executed on both peers, on one it should return true
        and on the other it should return false.
        It returns true when this peer should yield,
        and false when it should not.

        This method is not designed to be fair, but to simply resolve conflicts.
        It is used to resolve ping-ping conflicts,
        putting two connected peers in complementary parts of their
        ping-reply protocols.

        It is based on comparing each peer's address information.
        Presently it uses port information.
        ?? It should probably use something more unique,
        such as IP address or other unique ID number.  
        */
      {
        boolean yieldB;  // Storage for result.
        int addressDifferenceI=  // Calculate port difference.
          remoteInetSocketAddress.getPort() - PortManager.getLocalPortI();
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

    public void puttingReceivedPacketV( SockPacket theSockPacket )
      /* This method is used by source threads, actually the UnicastReceiver,
        to add theSockPacket to this Unicaster's receive queue.
       */
      {
        receiveQueueOfSockPackets.add(theSockPacket);
        }

    private boolean tryingToCaptureTriggeredExitB( )
      /* This method tests whether exit has been triggered, meaning either:
        * The current thread's isInterrupted() is true, or
        * The next packet, if any, at the head of the receiveQueueOfSockPackets,
		    	contains "Shutdowner", indicating the remote node is shutting down.
		    	If true then the packet is consumed and
		    	the current thread's isInterrupted() status is set true.
		    This method returns true if exit is triggered, false otherwise.
		    */
      {
        if // Trying to get and convert SHUTTING-DOWN packet to interrupt status. 
          ( tryingToGetPacketB( "SHUTTING-DOWN" ) )
	        {
	          appLogger.info( "SHUTTING-DOWN packet received.");
	          Thread.currentThread().interrupt(); // Converting to thread interrupt.
	          }
	      return Thread.currentThread().isInterrupted();
        }

    private boolean tryingToGetPacketB( String aString )
      /* This method tries to get a packet, if any,
        at the head of the receiveQueueOfSockPackets,
        contains aString.
        It consumes the packet and returns true if there is a packet and
        it is aString, false otherwise.
        */
      {
    	  boolean gotPacketB=  // Testing for packet with desired string.
    	  	testingPacketB( aString );
    	  if ( gotPacketB ) // Consuming packet if desired one is there.
        	tryingToConsumeOnePacketB();
    	  return gotPacketB;
      	}

    private boolean tryingToConsumeOnePacketB()
      /* This method consumes one packet, if any,
        at the head of the queue.
        It returns true if a packet was consumed,
        false if there was none to consume.
        */
      {
    	  boolean processingPacketB= 
    	  	( peekingPacketString( ) != null );
        if ( processingPacketB ) // Consuming the packet if there is one.
	        {
	        	receiveQueueOfSockPackets.poll(); // Removing head of queue.
	        	cachedPacketString= null; // Empty the string cache/flag.
	          packetsReceivedNamedInteger.addValueL( 1 );  // Count the packet.
	          }
    	  return processingPacketB;
        }

    private String cachedPacketString= null;

    private String peekingPacketString( )
      /* This method returns the String in the next received packet
        in the queue, if there is one.  
        If there's no packet then it returns null. 
        */
      {
        calculatingString: {
  				if ( cachedPacketString != null ) // Exiting if String is cached. 
  				  break calculatingString;
          SockPacket receivedSockPacket= // Testing queue for a packet.
            receiveQueueOfSockPackets.peek();
          if (receivedSockPacket == null) // Exiting if no packet.
            break calculatingString;
          DatagramPacket theDatagramPacket= // Getting DatagramPacket.
            receivedSockPacket.getDatagramPacket();
          cachedPacketString= // Calculating and caching String from packet.
            PacketStuff.gettingPacketString( theDatagramPacket );
          } // calculatingString: 
	    	return cachedPacketString; // Returning whatever is now in cache.
	      }

      private boolean testingPacketB( String aString )
        /* This method tests whether the packet, if any,
          at the head of the receiveQueueOfSockPackets,
          contains aString.
          It returns true if there is a packet and
          it is aString, false otherwise.
          The packet, if any, remains in the queue.
          */
        {
        boolean resultB= false;  // Assuming aString is not present.
        decodingPacket: {
          String packetString= // Getting string from packet if possible. 
          	peekingPacketString( );
          if ( packetString == null ) // Exiting if no packet or no string.
            break decodingPacket;  // Exiting with false.
          if   // Exiting if the desired String is not in packet String.
            ( ! packetString.contains( aString ) )
            break decodingPacket;  // Exiting with false.
          resultB= true;  // Changing result because Strings are equal.
          } // decodingPacket:
        return resultB;  // Returning the result.
        }

      private boolean testingPacketB( )
        /* This method tests whether a packet, if any,
          at the head of the receiveQueueOfSockPackets,
          is available.
          It returns true if there is a packet available, false otherwise.
          */
        {
          return ( receiveQueueOfSockPackets.peek() != null );
          }

    
    // Send packet code.  This might be enhanced with streaming.

    private void sendingPacketV( String aString )
      /* This method sends a packet containing aString to the peer.
        */
      {
        String payloadString= ((packetIDI++) + ":" + aString);
        //appLogger.info( "sendingPacketV(): " + payloadString );
        byte[] buf = payloadString.getBytes();
        DatagramPacket packet = new DatagramPacket(
          buf, 
          buf.length, 
          remoteInetSocketAddress.getAddress(),
          remoteInetSocketAddress.getPort()
          );
        SockPacket aSockPacket=
          new SockPacket( 
            unconnectedDatagramSocket,
            packet 
            );
        sendQueueOfSockPackets.add( // Queuing packet for sending.
            aSockPacket
            );

        packetsSentNamedInteger.addValueL( 1 );
        }


    // interface JobStatus code.  Not presently used.
    
    private boolean jobDoneB= false;
    
  	public boolean getJobDoneB() { return jobDoneB; }
  	
  	public void setJobDoneV( Boolean jobDoneB ) { this.jobDoneB= jobDoneB; }
  	
    } // Unicaster.
