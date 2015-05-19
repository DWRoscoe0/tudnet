package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
///import java.net.SocketException;
import java.util.Random;

import allClasses.ConnectionManager.PacketQueue;

public class Unicaster

  ///extends MutableList
	extends NetCaster

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
      private SignallingQueue<Unicaster> terminationQueueOfUnicasters;
      private final DatagramSocket unconnectedDatagramSocket;
      ///private DataTreeModel theDataTreeModel;

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
        SignallingQueue<Unicaster> terminationQueueOfUnicasters,
        DatagramSocket unconnectedDatagramSocket,
        DataTreeModel theDataTreeModel
        )
      /* This constructor constructs a Unicaster for the purpose of
        communicating with the node at remoteInetSocketAddress,
        but no response has yet been made.
        Fields are defined in a way to cause an initial response.
        
        ??? Add parameter which controls whether thread first waits for
        a PING or an ECHO, to reduce or eliminate ping-ping conflicts.
        Implement protocol with a state-machine.
        */
      {
        super( 
  	        theDataTreeModel,
  	        remoteInetSocketAddress,
        		"Unicaster-at-" /// + 
		  	      ///remoteInetSocketAddress.getAddress() +
		  	      /// ":" + remoteInetSocketAddress.getPort()
        		);

        // Storing injected dependency constructor arguments.
          ///this.remoteInetSocketAddress= remoteInetSocketAddress;
          this.sendQueueOfSockPackets= sendQueueOfSockPackets;
          this.terminationQueueOfUnicasters= terminationQueueOfUnicasters;
          this.unconnectedDatagramSocket= unconnectedDatagramSocket;
	        ///this.theDataTreeModel= theDataTreeModel;

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
        ///appLogger.info(
		    ///  Thread.currentThread().getName()+": run() beginning."
		    ///   );
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
          }
          catch( IOException e ) {
            appLogger.threadInfo("run() IOException: "+e );
            throw new RuntimeException(e);
          }

        // Terminating.
	      ///appLogger.info(
	      /// Thread.currentThread().getName()+": run() ending."
	      /// );
        }

    protected void initializeV()
	    throws IOException
	    {
      	super.initializeV();
	      add( 	RoundTripTimeNamedInteger= new NamedInteger( 
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
        int maxTriesI= 3;
        LockAndSignal.Input theInput;  // Type of input that ends waits.
        retryLoop: for // Sending pings until something stops us.
          ( int triesI= 1; ; triesI++ ) // Retry counter.
          { // Trying ping send and echo receive one time, or exiting.
            if  // Checking and exiting if retries were exceeded.
              ( triesI > maxTriesI )  // Maximum attempts exceeded.
              { // Terminating thread.
	              appLogger.info(
	                	Thread.currentThread().getName()+
	                	  ": requesting termination after "+maxTriesI+" ECHO"+
	                	  "  \n  time-outs."
	                  );
            	  terminationQueueOfUnicasters.add( // Queuing for termination.
            	  		this
            	  		);
                break;
                }
            //long pingMillisL= System.currentTimeMillis();
            sendingPacketV("PING"); // Sending ping packet.
            pingSentAtNanosL= System.nanoTime(); // Recording send time.
            long waitMillisL=  // Calculating half-period wait time.
              System.currentTimeMillis()+HalfPeriodMillisL;
            waitLoop: while (true) { // Flushing for pause duration.
              theInput= // Awaiting next input and storing its type.
                peerLockAndSignal.doWaitUntilE( // Awaiting input or...
                  waitMillisL  // ...maximum wait time.
                  );
              switch ( theInput ) {  // Handling the input type.
                case INTERRUPTION: // Handling a thread's interruption.
                  break retryLoop; // Exiting to terminate thread.
                case TIME: // Handling a time-out.
                  //appLogger.info(
                  //  	Thread.currentThread().getName()+
                  //  	  ": time-out waiting for ECHO: "+triesI
                  //    );
                  break waitLoop;  // Exiting wait loop to send PING.
                case NOTIFICATION:  // Handling packet inputs.
                  while (true) { // Handling possible multiple packets.
                    if ( ! testingPacketB( ) ) // Handling empty queue.
                      break;  // Exiting packet NOTIFICATION loop.
                    if // Handling echo packet, maybe.
                      ( testingPacketB( "ECHO" ) )
                      { // Handling echo and exiting.
	                      RoundTripTimeNamedInteger.setValueL(
	                      		(System.nanoTime() - pingSentAtNanosL)
	                      		); // Calculating RoundTripTime.
                        consumingOnePacketV(); // Consuming echo packet.
                        // This should handle unwanted received packets.
                        break retryLoop; // Finishing by exiting loop.
                        }
                    if // Handling ping-ping conflict, maybe.
                      ( testingPacketB( "PING" ) )
	                    { // Handling ping-ping conflict.
	                      appLogger.info(
	                        	Thread.currentThread().getName()+
	                        	  ": PING ping conflict."
	                          );
                      	if // Handling ping-ping conflict.
	                        ( arbitratedYieldingB ) // Let arbiter decide.
	                        { // Yielding ping processing to other peer.
	                          appLogger.info(
	                          	Thread.currentThread().getName()+
	                          	  ": PING ping yield: "+triesI
	                            );
	                          break retryLoop; // Yielding by exiting loop.
	                          // Ping packet remains in queue.
	                          }
	                    	}
                    ignoringOnePacketV(); // Ignoring any other packet value.
                    }
                  // Dropping through to exit switch and continue waitLoop.
                  }
              }
            }
        }

    private void tryingPingReceiveV()
      /* This method tries to receive a ping from the remote peer
        to which it replies by sending an echo response.
        It waits up to PeriodMillisL for a ping to arrive,
        after which it gives up and returns.
        If a ping is received it responds immediately by sending an echo,
        after which it waits PeriodMillisL while ignoring all
        received packets.
        */
      {
        LockAndSignal.Input theInput;  // Type of input that ends wait.
        long pingMillisL=  // Calculating latest ping receive time.
          System.currentTimeMillis()
          + PeriodMillisL
          + HalfPeriodMillisL;
        pingLoop: while (true) { // Inputing until one is acceptable.
          if // Handling a received ping if present.
            ( testingPacketB( "PING" ) )
            { // Handling received ping.
              consumingOnePacketV(); // Consuming ping packet.
              sendingPacketV("ECHO"); // Sending echo packet.
              long pauseMillisL=  // Calculating end of post-echo pause.
                System.currentTimeMillis()+PeriodMillisL;
              while (true) { // Ignoring packets for pause duration.
                theInput= // Awaiting next input and storing its type.
                  peerLockAndSignal.doWaitUntilE( pauseMillisL );
                  switch ( theInput ) {  // Handling input type.
                    case INTERRUPTION: // Handling thread interruption.
                      break pingLoop;  // Exiting outer loop.
                    case TIME: // Handling end of post-echo pause.
                      break pingLoop;  // Exiting outer loop.
                    case NOTIFICATION:  // Handling input packet(s).
                      while (true) { // Ignoring all input packets.
                        if ( ! testingPacketB( ) ) // Handling none left.
                          break;  // Exiting loop.
                        ignoringOnePacketV(); // Flushing one packet.
                        }
                    }
                }
              }
          ignoringOnePacketV(); // Ignoring non-ping packet.
          //appLogger.info(getName()+":\n  ignoring non-ping.");
          theInput= // Awaiting next input and storing its type.
            peerLockAndSignal.doWaitUntilE( // Awaiting input or...
              pingMillisL  // ...ping time limit.
              );
          switch ( theInput ) {  // Handling input based on type.
            case INTERRUPTION: // Handling a thread's interruption.
              break pingLoop;  // Exiting to terminate thread.
            case TIME: // Handling the ping time-out.
              appLogger.info(
                	Thread.currentThread().getName()+
                	  ": time-out waiting for PING."
                  );
              break pingLoop;  // Exiting to abort wait.
            case NOTIFICATION:  // Handling a received packet.
              // Dropping through to exit switch, go to pingLoop, and decode it.
            }
          } // pingLoop
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
        ??? It should probably use something more unique,
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

    /* ???
    private int packBytesToI(byte[] bytes)
      /* This method packs an array of 4 bytes into an int.
        It could be used for converting IP addresses to ints.
        */
    /*
	    {
	      int resultI= 0;
	      for (int I= 0; I < bytes.length; I++) {
	      	resultI= (resultI << 8) | (bytes[I] & 0xff);
	      	}
	      return resultI;
	    	}
	  ??? */

    // Receive packet code.

    public void puttingReceivedPacketV( SockPacket theSockPacket )
      // This method adds theSockPacket to the peer's receive queue.
      {
        receiveQueueOfSockPackets.add(theSockPacket);
        }

    private void consumingOnePacketV()
      /* This method consumes one packet, if any,
        at the head of the queue.
        */
      {
        receiveQueueOfSockPackets.poll();  // Consuming the packet,..
          // ...if there was one at head of queue.

        packetsReceivedNamedInteger.addValueL( 1 );
        }

    private void ignoringOnePacketV()
      /* This method logs that the present one packet, if any,
        at the head of the queue, is being ignored,
        and then consumes it.
        */
      {
        testingPacketB( "(ignoring)" );  // Log that its ignored, sort of.
        consumingOnePacketV();  // So it won't be seen again.
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
          SockPacket receivedSockPacket= // Testing queue for a packet.
            receiveQueueOfSockPackets.peek();
          if (receivedSockPacket == null) // Handling no packet.
            {
              //appLogger.info("testingPacketB() no packet is not "+aString);
              break decodingPacket;  // Exiting with false.
              }
          DatagramPacket theDatagramPacket= // Getting DatagramPacket.
            receivedSockPacket.getDatagramPacket();
          String dataString=  // Creating String from packet data.
            new String(
              theDatagramPacket.getData()
              ,theDatagramPacket.getOffset()
              ,theDatagramPacket.getLength()
              );
          if   // Handling unequal Strings.
            ( ! dataString.contains( aString ) )
            {
              //appLogger.info(
              //  "testingPacketB() packet "+dataString+" is NOT "+aString
              //  );
              break decodingPacket;  // Exiting with false.
              }
          //appLogger.info(
          //  "testingPacketB() packet "+dataString+" IS "+aString
          //  );
          resultB= true;  // Changing result because Strings are equal.
          }
        return resultB;  // Returning the result.
        }

    
    // Send packet code.

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

    } // Unicaster.
