package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Random;

import allClasses.LockAndSignal.Input;

public class Unicaster

	extends NetCaster

  implements Runnable ///, JobStatus  // JobStatus code is no longer needed?? 
  
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
    
      // Constants.
      private final long PeriodMillisL=  // Period between sends or receives.
        4000;   // 4 seconds.
      private final long HalfPeriodMillisL= // Half of period.
        PeriodMillisL / 2;  

      // Injected dependency instance variables.
      private final PacketQueue sendQueueOfSockPackets;
        // Queue to receive SockPackets to be sent to Unicaster.
    	@SuppressWarnings("unused") // ??
    	private InputQueue<Unicaster> cmInputQueueOfUnicasters;
      private final ConnectionManager theConnectionManager;
      private final Shutdowner theShutdowner;

      // Other instance variables.
      LockAndSignal peerLockAndSignal;  // LockAndSignal for this thread.
				// LockAndSignal for inputs to this thread.  It is used in
        // the construction of the following queue. 
      private final PacketQueue receiveQueueOfSockPackets;
        // Queue for SockPackets from unconnected receiver thread.
        /// References to this for input are being replaces by 
        /// InputStream references.
      int packetIDI; // Sequence number for sent packets.
      private Random theRandom; // For arbitratingYieldB() random numbers.
      private long pingSentNanosL; // Time the last ping was sent.
      private boolean arbitratedYieldingB; // Used to arbitrate race conditions.

      private String cachedString= "";  // Used for parsing packet data.
        // This is a copy of the next string in the NetInputStream.

      // Detail-containing child sub-objects.
	      private NamedInteger RoundTripTimeNamedInteger;
	        // This is an important value.  It is used to determine
	        // how long to wait for a message acknowledgement before
	        // re-sending a message.

	    NetOutputStream theNetOutputStream;
			NetInputStream theNetInputStream;


			public Unicaster(  // Constructor. 
        InetSocketAddress remoteInetSocketAddress,
        PacketQueue sendQueueOfSockPackets,
        InputQueue<Unicaster> cmInputQueueOfUnicasters,
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
          this.cmInputQueueOfUnicasters= cmInputQueueOfUnicasters;
          this.theConnectionManager= theConnectionManager;
          this.theShutdowner= theShutdowner;

        packetIDI= 0; // Setting starting packet sequence number.
        
        theRandom= new Random(0);  // Initialize arbitratingYieldB().

        peerLockAndSignal= new LockAndSignal(false);

        receiveQueueOfSockPackets=
          new PacketQueue( peerLockAndSignal );
            // For SockPackets from ConnectionManager.

    	  theNetInputStream= new NetInputStream(
  	    		receiveQueueOfSockPackets, 
  	    		remoteInetSocketAddress.getAddress(),
            remoteInetSocketAddress.getPort()
  	    		);

  	    theNetOutputStream= new NetOutputStream(
  	    		sendQueueOfSockPackets, 
  	    		remoteInetSocketAddress.getAddress(),
            remoteInetSocketAddress.getPort()
  	    		);
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
	      				sendingMessageV("SHUTTING-DOWN"); // Sending SHUTTING-DOWN packet.
	  	          appLogger.info( "SHUTTING-DOWN packet sent.");
		      			}
	      		theNetOutputStream.close(); // Closing output stream.
          	}
          catch( IOException e ) {
            appLogger.info("run() IOException: "+e );
            throw new RuntimeException(e);
            // Try reestablishing connection??
            }

        theConnectionManager.removingV( this );

        //appLogger.info("Unicaster.run() exiting.");
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

    private void tryingPingSendV() throws IOException
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
          sendingMessageV("PING"); // Sending ping packet.
          pingSentNanosL= System.nanoTime(); // Recording ping send time in ns
          long pingSentMsL= System.currentTimeMillis(); // and in ms.
          echoWaitLoop: while (true) { // Handling echo or other conditions.
            theInput=  // Awaiting next input within reply interval.
            		testWaitInIntervalE( pingSentMsL, HalfPeriodMillisL );
            if ( theInput == Input.TIME ) // Exiting echo wait if time-out.
              { appLogger.info( "Time-out waiting for ECHO: "+triesI );
                break echoWaitLoop;  // Exiting wait loop to send PING.
              	}
            else if // Handling SHUTTING-DOWN packet or interrupt by exiting.
    	    		( tryingToCaptureTriggeredExitB( ) )
  	    			break pingEchoRetryLoop; // Exit everything.
        		else if ( tryingToGetMessageB( "ECHO" ) ) // Handling echo, maybe.
              { // Handling echo and exiting.
                RoundTripTimeNamedInteger.setValueL(
                		(System.nanoTime() - pingSentNanosL)
                		); // Calculating RoundTripTime.
                // This should handle unwanted received packets.
    	    			break pingEchoRetryLoop; // Exit everything.
                }
        		else if ( testingMessageB( "PING" ) ) // Handling ping conflict, maybe.
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
              			tryingToConsumeOneMessageB();  // Consuming PING.
              			}
              	  }
        		else 
        			tryingToConsumeOneMessageB();  // Consume any other packet.
            } // echoWaitLoop:
          triesI++;
          } // pingEchoRetryLoop: 
        }

    private void tryingPingReceiveV() throws IOException
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
        long pingWaitStartMsL= System.currentTimeMillis();
        ///long pingTimeMillisL= // Calculating latest okay ping receive time.
	      ///  pingWaitStart
	      /// + PeriodMillisL
	      /// + HalfPeriodMillisL;
        pingWaitLoop: while (true) { // Processing until something causes exit.
      		if // Handling SHUTTING-DOWN packet or interrupt by exiting.
	    			( tryingToCaptureTriggeredExitB( ) )
	    			break pingWaitLoop;
          if // Handling a received ping if present.
            ( tryingToGetMessageB( "PING" ) )
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
            		tryingToConsumeOneMessageB(); // Ignore any other packet.
              	} // while (true)
              }
      		tryingToConsumeOneMessageB();  // Consume any other packet.
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

    /* ???
    private Input testWaitWithTimeOutE( long timeOutMsL) throws IOException
      /* This is a special test-and-wait method which will return immediately 
        with Input.NOTIFICATION if a received packet 
        is available for processing,
        otherwise it will do a LockAndSignal.doWaitWithTimeOutE(..).
        So it might block, or it might not.
        NOTIFICATION has priority over TIME,
        even if a time-out has already occurred.
        This is not the normal way the LockAndSignal wait methods work.
       */
    /* ???
	    {
    		LockAndSignal.Input theInput;

    		if ( testingMessageB( ) ) 
	        theInput= Input.NOTIFICATION;
    	  else
    	  	theInput=
    	  	  peerLockAndSignal.doWaitWithTimeOutE( 
	        		timeOutMsL
	        		);

    	  return theInput;
	      }
	  ??? */

    private Input testWaitInIntervalE( long startMsL, long lengthMsL) 
    		throws IOException
      /* ??? Being converted to give TIME priority.
       * 
       * This is a special test-and-wait method which will return immediately 
        with Input.NOTIFICATION if a received packet 
        is available for processing,
        otherwise it will do a LockAndSignal.doWaitWithIntervalE(..).
        So it might block, or it might not.
        NOTIFICATION has priority over TIME,
        even if the time limit has already passed.
        This is not the normal way the LockAndSignal wait methods work.
       */
	    {
    		LockAndSignal.Input theInput;

    		process: {
	        final long remainingMsL= 
	        		peerLockAndSignal.intervalRemainingMsL( startMsL, lengthMsL ); 
	        if // Exiting if time before, but more likely after, time interval.
	          ( remainingMsL == 0 )
	          { theInput= Input.TIME; break process; } // Exiting loop.
	    		if ( testingMessageB( ) ) ///
		        { theInput= Input.NOTIFICATION; break process; }
    	  	theInput= // Doing general wait. 
		    	///	  peerLockAndSignal.doWaitWithIntervalE(
		    	///  	    startMsL,
		    	///     		lengthMsL
		    	///     		);
    	  			peerLockAndSignal.doWaitWithTimeOutE( remainingMsL );
    			}

    	  return theInput;
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
      /* This method is used by the UnicastReceiver threads
        which is associated with this Unicaster thread
        to add theSockPacket to this thread's receive queue.
       */
      {
        receiveQueueOfSockPackets.add(theSockPacket);
        }

    private boolean tryingToCaptureTriggeredExitB( ) throws IOException
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
          ( tryingToGetMessageB( "SHUTTING-DOWN" ) )
	        {
	          appLogger.info( "SHUTTING-DOWN packet received.");
	          Thread.currentThread().interrupt(); // Converting to thread interrupt.
	          }
	      return Thread.currentThread().isInterrupted();
        }

    private boolean tryingToGetMessageB( String aString ) throws IOException
      /* This method tries to get a packet, if any,
        at the head of the receiveQueueOfSockPackets,
        contains aString.
        It consumes the packet and returns true if there is a packet and
        it is aString, false otherwise.
        */
      {
    	  boolean gotPacketB=  // Testing for packet with desired string.
    	  	testingMessageB( aString );
    	  if ( gotPacketB ) // Consuming packet if desired one is there.
        	tryingToConsumeOneMessageB();
    	  return gotPacketB;
      	}

    private boolean tryingToConsumeOneMessageB() throws IOException /// Packet.
      /* This method consumes one packet, if any,
        at the head of the queue.
        It returns true if a packet was consumed,
        false if there was none to consume.
        */
      { /// Marker
	      /* ??
	      boolean processingPacketB= 
	      	( peekingMessageString( ) != null );
	      if ( processingPacketB ) // Consuming the packet if there is one.
	        {
	        	receiveQueueOfSockPackets.poll(); // Removing head of queue.
	        	cachedString= ""; // Empty the string cache/flag.
	          packetsReceivedNamedInteger.addValueL( 1 );  // Count the packet.
	          }
	  	  return processingPacketB;
	  	  ??? */
    	
	      return ( 
	      		getOrTestString( null, true)
	      		!= 
	      		null 
	      		);
	      }

    private String peekingMessageString( ) throws IOException
      /* This method returns the String in the next received packet
        in the queue, if there is one.  
        If there's no packet then it returns null.
        As a side-effect it stores the string, or null, in cachedString.  
        */
      { /// Marker
    	  /* ???
        calculatingString: {
  				if ( cachedString != "" ) // Exiting if String is cached. 
  				  break calculatingString;
          SockPacket receivedSockPacket= // Testing queue for a packet.
            receiveQueueOfSockPackets.peek();
          if (receivedSockPacket == null) // Exiting if no packet.
            break calculatingString;
          DatagramPacket theDatagramPacket= // Getting DatagramPacket.
            receivedSockPacket.getDatagramPacket();
          cachedString= // Calculating and caching String from packet.
            PacketStuff.gettingPacketString( theDatagramPacket );
          } // calculatingString: 
	    	return cachedString; // Returning whatever is now in cache.
	    	??? */

	      return getOrTestString( null, false );
	      }

      private boolean testingMessageB( String aString ) throws IOException
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
	          	peekingMessageString( );
	          if ( packetString == null ) // Exiting if no packet or no string.
	            break decodingPacket;  // Exiting with false.
	          if   // Exiting if the desired String is not in packet String.
	            ( ! packetString.contains( aString ) )
	            break decodingPacket;  // Exiting with false.
	          resultB= true;  // Changing result because Strings are equal.
	          } // decodingPacket:
	        return resultB;  // Returning the result.
	        }

      private boolean testingMessageB( ) throws IOException
        /* This method tests whether a packet, if any,
          at the head of the receiveQueueOfSockPackets,
          is available.
          It returns true if there is a packet available, false otherwise.
          */
        { /// Marker
          return ( 
          		//receiveQueueOfSockPackets.peek() 
          		getOrTestString( null, false )
          		!= 
          		null 
          		);
          }

      @SuppressWarnings("unused") ///
      private String NEWgetOrTestString( String desiredString, boolean consumeB) 
      		throws IOException
        /* ??? This method is being converted to use NetInputStream.

          This is a new, possibly temporary method,
          through which all packet reading will pass
          at least during the transition from packet io to stream io.

          This method tries to get or test for desiredString in the input.
          consumeB means consume any acceptable string, 
          otherwise do a test only.
          
          If no bytes are available then it returns null.
          Otherwise it reads an entire string, blocking if necessary.
          If desiredString==null then it returns the read string.
          If desiredString!=null and the read string contains desiredString
          then it returns desiredString, null otherwise. 
          if consumedB is true and a string is returned,
          then the read string is consumed and can not be read
          from the stream again.

          As a side-effect it stores the read string, or null, in cachedString.  
          */
	      {
      	  String returnString= null;
          parsing: {
	  				theNetInputStream.mark(0); // Marking now  in case we reset() later.
	  				if ( cachedString != "" ) // Exiting if String already cached. 
	  				  break parsing;
  					if // Exiting if no bytes available. 
						  ( 0 >= theNetInputStream.available() )
							break parsing;
	  				while (true) { // Reading all bytes in string.
	  					int byteI= theNetInputStream.read();
	  					cachedString+= (char)byteI;
	  					if ( '.' == byteI ) break; // Exiting if terminator seen.
	  				  }
	         	} // parsing: 
	        testing: {
	          if ( cachedString == "" ) // Exiting if no packet or no string.
	            break testing; // Exiting with null.
	          if ( desiredString == null ) // Exiting if any string is acceptable.
		          { returnString= cachedString; // Using read string as result.
		            break testing;  // Exiting with string.
		            }
	          if   // Exiting if the desired String is the one read.
	            ( cachedString.contains( desiredString ) )
		          { returnString= desiredString; // Using desired string as result.
		            break testing;  // Exiting with string.
		            }
	          } // testing:
	        consuming: {
	          if ( ! consumeB ) // Exiting if consuming not requested.
	            {
			  				theNetInputStream.reset(); // Backup stream to start.
		          	break consuming; // Exiting 
		          	}
	          if ( returnString == null) // Exiting if no string to consume. 
	          	break consuming;
	        	// Stream has already been advanced.
            appLogger.debug( "consuming from InputStream: "+cachedString );
	        	cachedString= ""; // Emptying the string cache.
	          packetsReceivedNamedInteger.addValueL( 1 );  // Counting the packet.
	          } // consuming:
	      	return returnString;
	      	}

      private String getOrTestString( String desiredString, boolean consumeB)
        /* This is a new, possibly temporary method,
          through which all packet reading will pass
          at least during the transition from packet io to stream io.

          This method tries to get or test for desiredString in the input.
          consumeB means consume any acceptable packet, otherwise test only.
          Returns reference to desired string, or null if desired string
          was not seen.
          If desiredStringB==null then any input string is acceptable.  
        	As a side-effect it stores the string, or null, in cachedString.  
         */
	      {
      	  String returnString= null;
          parsing: {
	  				if ( cachedString != "" ) // Exiting if String is cached. 
	  				  break parsing;
	          SockPacket receivedSockPacket= // Testing queue for a packet.
	            receiveQueueOfSockPackets.peek();
	          if (receivedSockPacket == null) // Exiting if no packet.
	            break parsing;
	          DatagramPacket theDatagramPacket= // Getting DatagramPacket.
	            receivedSockPacket.getDatagramPacket();
	          cachedString= // Calculating and caching String from packet.
	            PacketStuff.gettingPacketString( theDatagramPacket );
	          } // parsing: 
	        testing: {
	          if ( cachedString == "" ) // Exiting if no packet or no string.
	            break testing; // Exiting with null.
	          if ( desiredString == null ) // Exiting if any string is acceptable.
		          { returnString= cachedString; // Using cached string as result.
		            break testing;  // Exiting with string.
		            }
	          if   // Exiting if the desired String is in packet String.
	            ( cachedString.contains( desiredString ) )
		          { returnString= desiredString; // Using desired string as result.
		            break testing;  // Exiting with string.
		            }
	          } // testing:
	        consuming: {
	          if ( ! consumeB ) // Exiting if consuming not requested.
	          	break consuming;
	          if ( returnString == null) // Exiting if nothing to consume. 
	          	break consuming;
            appLogger.debug( "consuming from packet: "+cachedString );
	        	receiveQueueOfSockPackets.poll(); // Removing head of queue.
	        	cachedString= ""; // Emptying the string cache.
	          packetsReceivedNamedInteger.addValueL( 1 );  // Counting the packet.
	          } // consuming:
	      	return returnString;
	      	}

    // Send packet code.  This might be enhanced with streaming.

      private void sendingMessageV( String aString ) throws IOException
        /* This method sends a packet containing aString to the peer.
          It does NOT use NetOutputStream.  It accesses packets directly.
          It prepends a packet ID number.
          */
        {
      	  ///newSendingPacketV( aString );
      	  String payloadString= ((packetIDI++) + ":" + aString);
          //appLogger.info( "sendingMessageV(): " + payloadString );
          byte[] buf = payloadString.getBytes();
          DatagramPacket packet = new DatagramPacket(
            buf, 
            buf.length, 
            remoteInetSocketAddress.getAddress(),
            remoteInetSocketAddress.getPort()
            );
          SockPacket aSockPacket= new SockPacket(packet);
          sendQueueOfSockPackets.add( // Queuing packet for sending.
              aSockPacket
              );

          packetsSentNamedInteger.addValueL( 1 );
          }

      @SuppressWarnings("unused") ///
      private void NEWsendingMessageV( String aString ) throws IOException
        /* This method sends a packet containing aString to the peer.
          It uses NetOutputStream instead of accessing packets directly.
          It prepends a packet ID number.
          It does it using a NetOutputStream.
          */
        {
      		appLogger.debug( "sending: "+aString );
      		
          String payloadString= ((packetIDI++) + ":" + aString);
          //appLogger.info( "sendingMessageV(): " + payloadString );
          byte[] buf = payloadString.getBytes();
          
          theNetOutputStream.write(buf); // Writing it to memory.
          theNetOutputStream.write('.'); // Writing terminator.
          theNetOutputStream.flush(); // Sending it in packet.

          packetsSentNamedInteger.addValueL( 1 );
          }


    /* ???
    // interface JobStatus code.  Not presently used.
    
    private boolean jobDoneB= false;
    
  	public boolean getJobDoneB() { return jobDoneB; }
  	
  	public void setJobDoneV( Boolean jobDoneB ) { this.jobDoneB= jobDoneB; }
  	??? */
  	
    } // Unicaster.
