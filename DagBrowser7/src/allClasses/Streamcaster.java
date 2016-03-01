package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Random;

import allClasses.LockAndSignal.Input;

public class Streamcaster< 
    K,
		E extends KeyedPacket<K>,
		Q extends NotifyingQueue<E>,
    M extends PacketManager<K,E>,
    I extends EpiInputStream<K,E,Q,M>,
    O extends EpiOutputStream<K,E,Q,M>
		>

	extends DataNodeWithKey< K >

  /* This is the base class is for streaming with UDP packets, but
    it doesn't know anything about packet IP addresses or ports.
    That is handled by subclass Netcaster.
    */

	{
  
	  // Constants.
	  protected final long PeriodMillisL=  // Period between sends or receives.
	    4000;   // 4 seconds.
	  protected final long HalfPeriodMillisL= // Half of period.
	    PeriodMillisL / 2;  

    protected final LockAndSignal streamcasterLockAndSignal;
			// LockAndSignal for inputs to this thread.  It is used in
      // the construction of the following queue. 

	  protected final O theEpiOutputStreamO;
		protected final I theEpiInputStreamI;

    protected final Shutdowner theShutdowner;

		protected long pingSentNanosL; // Time the last ping was sent.
		int packetIDI; // Sequence number for sent packets.

    // Detail-containing child sub-objects.
      protected NamedInteger RoundTripTimeNamedInteger;
        // This is an important value.  It can be used to determine
        // how long to wait for a message acknowledgement before
        // re-sending a message.

    // Other variables.
    protected Random theRandom; // For arbitratingYieldB() random numbers.
    protected boolean arbitratedYieldingB; // Used to arbitrate race conditions.
      
	  public Streamcaster(  // Constructor.
	      DataTreeModel theDataTreeModel,
	      String nameString,
        Shutdowner theShutdowner,
	  		K theKeyK,
	  		LockAndSignal netcasterLockAndSignal,
	      I theEpiInputStreamI,
	      O theEpiOutputStreamO
	  		)
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableList superclass.
		        theDataTreeModel,
		        nameString, // Type name but not entire name.
		        theKeyK
	      		);

	      this.streamcasterLockAndSignal= netcasterLockAndSignal;
	      this.theEpiInputStreamI= theEpiInputStreamI;
	      this.theEpiOutputStreamO= theEpiOutputStreamO;
        this.theShutdowner= theShutdowner;
	      }

    protected void initializingV()
	    throws IOException
	    {

	      addB( 	RoundTripTimeNamedInteger= new NamedInteger( 
	      		theDataTreeModel, "Round-Trip-Time-ns", 0 
	      		)
	  			);
	
		    }

		public void pingReplyProtocolV() throws IOException //// Needn't be public.
      // Does full PING-REPLY protocol.  Will terminate thread if requested.
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

    protected void tryingPingSendV() throws IOException
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
          writingPacketV("PING"); // Sending ping packet.
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
            theEpiInputStreamI.mark(0); // Preparing to not consume message.
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
                    theEpiInputStreamI.reset(); // Putting message back.
                    break pingEchoRetryLoop; // Yielding by exiting main loop.
                    }
              		else
              		{ appLogger.info( "PING ping not yielding: "+triesI );
              			// Ignoring this PING.
                    break echoWaitLoop;  // End wait to send new PING, maybe.
              			}
              	  }
        		ignoringOrLoggingStringV(inString);
        			// Ignoring the message that was gotten, whatever it was.
            } // echoWaitLoop:
          triesI++;
          } // pingEchoRetryLoop: 
        }

		private void ignoringOrLoggingStringV(String inString)
				 throws IOException
			{
			  processing: {
			    // Ignoring particular expected strings.
					if ( inString.equals("N") ) break processing;
			  	if ( inString.matches("[0-9]+") ) // Ignoring numbers. 
			  		break processing;
			  	if ( inString.equals("PING-REPLY") ) break processing;
			  	
			  	// Logging anything else.
			    appLogger.debug( 
							"tryingPingSendV(): unexpected: "
						+ inString
						+ " from "
						+ PacketManager.gettingPacketString( 
								theEpiInputStreamI.getKeyedPacketE().getDatagramPacket()
								)
						);
					} // processing:
				}

    protected void tryingPingReceiveV() throws IOException
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
              writingPacketV("ECHO"); // Sending echo packet as reply.
              long echoSentMsL= System.currentTimeMillis();
              while (true) { // Ignoring packets for a while, then exit.
                theInput=  // Awaiting input within the ignore interval.
                		testWaitInIntervalE( echoSentMsL, PeriodMillisL );
                if ( theInput == Input.TIME ) // Exiting if time limit reached.
                  break pingWaitLoop;  
            		if // Exiting everything if exit has been triggered.
            			( tryingToCaptureTriggeredExitB( ) )
            			break pingWaitLoop;
            		String aString= readAString(); // Reading message and ignoring it.
                appLogger.debug( 
            				"tryingPingReceiveV(): unexpected: "
            			+ aString
            			+ " from "
            			+ PacketManager.gettingPacketString( 
            					theEpiInputStreamI.getKeyedPacketE().getDatagramPacket()
            					)
            			);
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

    protected boolean arbitratingYieldB( int remotePortI )
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
          ///getKeyK().getPortI() - PortManager.getLocalPortI();
        	remotePortI - PortManager.getLocalPortI();
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

    protected Input testWaitInIntervalE( long startMsL, long lengthMsL) 
    		throws IOException
      /* This is a special test-and-wait method with 
        different input priorities than the LockAndSignal wait methods.
        The priorities used here are:
		      TIME
		      NOTIFICATION
		      INTERRUPTION
       */
	    {
    		LockAndSignal.Input theInput;

    		process: {
	        final long remainingMsL= 
	          streamcasterLockAndSignal.intervalRemainingMsL( 
	          		startMsL, lengthMsL 
	          		);
	        if // Exiting if time before, or more likely after, time interval.
	          ( remainingMsL == 0 )
	          { theInput= Input.TIME; break process; }
	        if // Exiting if a notification from InputStream is ready.
	          ( theEpiInputStreamI.available() > 0 )
		        { theInput= Input.NOTIFICATION; break process; }
    	  	theInput= // Doing general wait for anything else. 
    	  	  streamcasterLockAndSignal.doWaitWithTimeOutE( remainingMsL );
    			} // process:

    	  return theInput;
	      }

    protected boolean tryingToCaptureTriggeredExitB( ) throws IOException
      /* This method tests whether exit has been triggered, meaning either:
        * The current thread's isInterrupted() is true.
		    This method returns true if exit is triggered, false otherwise.

        It no longer checks for:
        * The next packet, if any, at the head of 
          the receiverToStreamcasterNotifyingQueueQ,
		    	contains "SHUTTING-DOWN", indicating the remote node is shutting down.
		    	If true then the packet is consumed and
		    	the current thread's isInterrupted() status is set true.
		    */
      { 
    	  /*////
        if // Trying to get and convert SHUTTING-DOWN packet to interrupt status. 
          ( tryingToGetStringB( "SHUTTING-DOWN" ) )
	        {
	          appLogger.info( "SHUTTING-DOWN message received.");
	          Thread.currentThread().interrupt(); // Interrupting this thread.
	          }
	      */ ////
	      return Thread.currentThread().isInterrupted();
        }


    // String reading methods.

    protected boolean tryingToGetStringB( String aString ) throws IOException
      /* This method tries to get a particular String aString.
        It consumes the String and returns true if the desired string is there, 
        otherwise it does not consume the message and returns false.
        */
      {
  			boolean gotStringB= false;
    		theEpiInputStreamI.mark(0); // Marking stream position.
    		String inString= tryingToGetString();
    	  gotStringB=  // Testing for desired string.
    	  		aString.equals( inString );
    	  if ( ! gotStringB ) // Resetting position if String is not correct.
    	  	theEpiInputStreamI.reset();
    	  return gotStringB;
      	}

    protected String tryingToGetString( ) throws IOException
    /* This method tries to get any String.
      It returns a String if there is one available, null otherwise.
      */
    {
			String inString= null;
			if // Overriding if desired string is able to be read. 
			  ( theEpiInputStreamI.available() > 0 )
				{
	    	  inString= readAString();
	    	  }
  	  return inString;
    	}

		protected String readAString()
  		throws IOException
  		/* This method reads and returns one String ending in the first
  		  delimiterChar='.' from theEpiInputStreamI.  It blocks if needed.
  		  The String returned includes the delimiter.
  		 */
			{
			  final char delimiterChar= '.';
				String readString= "";
				while (true) { // Reading and accumulating all bytes in string.
					if ( theEpiInputStreamI.available() <= 0 ) // debug.
						{
							readString+="!NOT-AVAILABLE!";
		          appLogger.error( "readAString(): returning " + readString );
							break;
	  					}
					int byteI= theEpiInputStreamI.read();
					if ( delimiterChar == byteI ) break; // Exiting if terminator seen.
					readString+= (char)byteI;
					}
				return readString;
				}


    // String writing and packet sending code.

    protected void writingNumberedPacketV( String aString ) 
    		throws IOException
      /* This method is like writingPacketV(..) but
        it prepends a packet ID / sequence number.
        */
      {
    		writingSequenceNumberV();
    		writingPacketV(aString); // Writing string into buffer.
        }

    protected void writingPacketV( String aString ) throws IOException
      /* This method writes aString to the EpiOutputStream.
        It does it using a EpiOutputStream.
        It ends with a flush() so that the message is actually sent
        and sent quickly.
        */
      {
	  		writingTerminatedStringV( aString );
	  		theEpiOutputStreamO.flush(); // Sending it by flushing.
        }

    protected void writingSequenceNumberV() throws IOException
      /* This method increments and writes the packet ID (sequence) number
        to the EpiOutputStream.
        It doesn't flush().
        */
      {
	  		writingTerminatedStringV( "N" );
	  		writingTerminatedStringV( (packetIDI++) + "" );
        }

    protected void writingTerminatedStringV( String aString ) 
    		throws IOException
      /* This method writes aString with terminator character "."
        on the end to the EpiOutputStream.
        It doesn't flush().
        */
      { 
	  		writingStringV( aString );
	  		writingStringV( "." );
        }

    protected void writingStringV( String aString ) throws IOException
      /* This method write aString to the EpiOutputStream.
        It doesn't flush().
        */
      { 
    		byte[] buf = aString.getBytes(); // Getting byte buffer from String
        theEpiOutputStreamO.write(buf); // Writing it to stream memory.
        }
    
    
    // Miscellaneous methods.

    public void puttingKeyedPacketV( E theKeyedPacketE )
      /* This method is used by various Receiver threads
        to add a KeyedPacket to the queue of this Streamcaster's
        EpiInputStream.  It is from these packets that
        the InputStream's data comes.
        */
      {
    	  Q theStreamcasterQueueQ= theEpiInputStreamI.getNotifyingQueueQ();
    	  theStreamcasterQueueQ.add(theKeyedPacketE);
        }

    public M getPacketManagerM()
      /* Returns the PacketManager that can be used to create packets
        for the InputStream.  It returns the one the OutputStream uses
        to allocate its packets.
	      */
	    {
	    	return theEpiOutputStreamO.getPacketManagerM();
	    	}
	}
