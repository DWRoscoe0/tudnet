package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
////import java.util.Random;




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
		////private final char delimiterChar= '.';
		////private final char delimiterChar= '$';
		private final char delimiterChar= '!';
		
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
		////int packetIDI; // Sequence number for sent packets.

    // Detail-containing child sub-objects.
      protected NamedInteger roundTripTimeNamedInteger;
        // This is an important value.  It can be used to determine
        // how long to wait for a message acknowledgement before
        // re-sending a message.
        //// This is probably better measured in Unicaster class.

    // Other variables.
    ////protected Random theRandom; // For arbitratingYieldB() tie-breaking.
    protected boolean leadingB= false; // Used to settle race conditions.
      
	  public Streamcaster(  // Constructor.
	      DataTreeModel theDataTreeModel,
	      String nameString,
        Shutdowner theShutdowner,
        boolean leadingB,
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
        this.leadingB= leadingB;
	      }

    protected void initializingV() throws IOException
      //// This might be eliminated if roundTripTimeNamedInteger is moved.
	    {

	      addB( 	roundTripTimeNamedInteger= new NamedInteger( 
	      		theDataTreeModel, "Round-Trip-Time-ns", -1
	      		)
	  			);
	
        appLogger.info( 
        		"This Streamcasteer is starting in the role of: "
        		+ (leadingB ? "LEADER" : "FOLLOWER")
        		);
		    }

		public void pingReplyProtocolV() throws IOException //// Needn't be public.
      // Does full PING-REPLY protocol.  Will terminate thread if requested.
	    {
	    	int stateI= // Initialize protocol state from leadership flag. 
	      	  leadingB ? 0 : 1 ;
	      while (true) // Repeating until thread termination is requested.
	        {
	      		if ( EpiThread.exitingB() ) break;  // Exiting if requested.
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
        int maxTriesI= 5;
        int triesI= 1;
        pingEchoRetryLoop: while (true) { // Sending ping and receiving echo.
          if  // Checking and exiting if maximum attempts have been done.
            ( triesI > maxTriesI )  // Maximum attempts exceeded.
            { // Terminating thread.
              appLogger.info(
              		"Requesting termination after "
                	+"\n  "+maxTriesI+" REPLY time-outs."
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
              { appLogger.info( "Time-out waiting for REPLY: "+triesI );
                break echoWaitLoop;  // End wait to send new PING, maybe.
              	}
            if // Handling SHUTTING-DOWN packet or interrupt by exiting.
    	    		( tryingToCaptureTriggeredExitB( ) )
  	    			break pingEchoRetryLoop; // Exit everything.
            theEpiInputStreamI.mark(0); // Preparing to not consume message.
        		String inString= readAString(); // Reading message.
        		if ( inString.equals( "REPLY" ) ) // Handling echo, maybe.
              { // Handling echo and exiting.
                roundTripTimeNamedInteger.setValueL(
                		(System.nanoTime() - pingSentNanosL)
                		); // Calculating RoundTripTime.
      			    appLogger.debug( "Got REPLY." );
            		break pingEchoRetryLoop; // Exit everything.
                }
        		if ( inString.equals( "PING" ) ) // Handling ping conflict, maybe.
              { // Handling ping conflict.
                appLogger.info( "PING-PING conflict." );
              	if ( ! leadingB ) // Arbitrating ping-ping conflict.
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
			  ///processing: {
			    // Ignoring particular expected strings.
					///if ( inString.equals("N") ) break processing;
					///if ( inString.matches("[0-9]+") ) // Ignoring numbers. 
					/// 		break processing;
					/// 	if ( inString.equals("PING-REPLY") ) break processing;
			  	
			  	// Logging anything else.
			    appLogger.debug( 
							"tryingPingSendV(): unexpected: "
						+ inString
						+ " from "
						+ PacketManager.gettingPacketString( 
								theEpiInputStreamI.getKeyedPacketE().getDatagramPacket()
								)
						);
					///} // processing:
				}

    protected void tryingPingReceiveV() throws IOException
	    /* This method does the sub-protocol of trying to 
		    receive a PING message from the remote peer 
		    to which it replies by sending an REPLY response.
        If a PING is not immediately available then
        it waits up to PeriodMillisL for a PING to arrive,
        after which it gives up and returns.
        If a PING is received it responds immediately by sending an REPLY,
        after which it waits PeriodMillisL while ignoring all
        received messages except SHUTTING-DOWN,
        or a repeat PING, which elicits a repeat REPLY.
        It will exit immediately if isInterrupted() becomes true
        or a SHUTTING-DOWN message is received.
        */
      {
    	  all: { // Block of entire method, for exiting.
	        LockAndSignal.Input theInput;  // Type of input that ends wait.
	        long pingWaitStartMsL= System.currentTimeMillis();
	        pingWaitLoop: while (true) { // Processing until exit triggered.
	      		if // Handling SHUTTING-DOWN packet or interrupt by exiting.
		    			( tryingToCaptureTriggeredExitB( ) )
		    			break all;
	      		// Note, can't readAString() here because it might not be available.
            appLogger.debug( "Testing for PING." );
            AppLog.testingForPingB= true;
	          if // Handling a received ping if present.
	            ( tryingToGetStringB( "PING" ) )
		          {
	          		AppLog.testingForPingB= false;
		            appLogger.debug( "tryingPingReceiveV() got PING." );
		          	break pingWaitLoop;  // Exiting pingWaitLoop only.
		          	}
        		AppLog.testingForPingB= false;
	          theInput= testWaitInIntervalE( // Awaiting next input.
	          		pingWaitStartMsL, PeriodMillisL + HalfPeriodMillisL
	          		);
	          if ( theInput == Input.TIME ) // Exiting all if time-out.
		          {
		            appLogger.warning( "Time-out waiting for PING." );
		            break all;  // Exiting to abort wait.
		          	}
	       		tryingToGetString(); ////// Reading and ignoring any message.
	          } // pingWaitLoop
	        while(true) { // Sending and confirming receipt of REPLY.
	          writingPacketV("REPLY");
	          long echoSentMsL= System.currentTimeMillis();
	          postReplyPause: while (true) {
	            theInput=  // Awaiting input within the ignore interval.
	            		testWaitInIntervalE( echoSentMsL, PeriodMillisL );
	            ///appLogger.debug( "tryingPingReceiveV(): theInput= " + theInput );
	            if ( theInput == Input.TIME ) // Exiting if time limit reached.
	              break all;  
	        		if // Exiting everything if exit has been triggered.
	        			( tryingToCaptureTriggeredExitB( ) )
	        			break all;
		          if // Handling a repeat received ping if present.
		        	  ( tryingToGetStringB( "PING" ) )
			          { ////
			            appLogger.warning( "tryingPingReceiveV(): got repeat PING." );
			          	break postReplyPause;  // Exiting pingWaitLoop only.
			            }
	        		String aString= readAString(); // Reading message to ignore it.
	            appLogger.warning( 
	        			"tryingPingReceiveV(): unexpected " + aString + ", ignoring"
	        			///+ " from "
		        		///+ PacketManager.gettingPacketString( 
		        		///	theEpiInputStreamI.getKeyedPacketE().getDatagramPacket()
		        		///	)
	        			);
	          	} // postReplyPause: 
	          }
    			} // all:
        }

    /*//////////////
    protected boolean arbitratingYieldB( int remotePortI )
      /* This method arbitrates when this local peer is trying
        to do the same thing as the remote peer.
        When executed on both peers, on one it should return true
        and on the other it should return false.
        It returns true when this peer should yield,
        and false when it should not.

        This method is not designed to be fair, 
        but to simply help resolve conflicts quickly.
        When used it puts two connected peers into
        complementary parts of their ping-reply protocols.
        It is used:
        * When two connected Unicaster threads start.
        * To resolve ping-ping conflicts, which is when
          two connected Unicasters somehow end up in
          the same parts of their protocols,
          usually after an error occurs.

        This method is based on comparing each peer's address information.
        Presently it uses port information.
        ?? It should probably use something more unique,
        such as IP address or other unique ID number,
        but one's own IP address is more difficult to get than port.  
        */
    /*//////////////
      {
        boolean yieldB;  // Storage for result.
        int addressDifferenceI=  // Calculate difference in...
          ///getKeyK().getPortI() - PortManager.getLocalPortI(); // ports.
        	remotePortI - PortManager.getLocalPortI(); // ports.
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
    */ //////////////

    protected Input testWaitInIntervalE( long startMsL, long lengthMsL) 
    		throws IOException
      /* This is a special test-and-wait method with 
        different input priorities than the LockAndSignal wait methods.
        The priority order used here is:
		      TIME
		      NOTIFICATION
		      INTERRUPTION
       */
	    {
    		LockAndSignal.Input theInput;

    		process: while (true) {
	        final long remainingMsL= 
	          streamcasterLockAndSignal.intervalRemainingMsL( 
	          		startMsL, lengthMsL 
	          		);
	        if // Exiting if time before, or after, time interval.
	          ( remainingMsL == 0 )
	          { theInput= Input.TIME; break process; }
	        if // Exiting if a notification from InputStream is ready.
	          ( theEpiInputStreamI.available() > 0 )
		        { theInput= Input.NOTIFICATION; 
            	///appLogger.debug( "testWaitInIntervalE(..): available() > 0" );
            	break process; 
            	}
    	  	theInput= // Doing general wait for anything else. 
    	  	  streamcasterLockAndSignal.doWaitWithTimeOutE( remainingMsL );
	        if // Handling thread interrupt signal.
	          ( EpiThread.exitingB() )
	          { theInput= Input.INTERRUPTION; break; }
    	  	////break process; ////
    			} // process: while (true) 

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
	      return EpiThread.exitingB();
        }


    // String reading methods.

    protected boolean tryingToGetStringB( String aString ) throws IOException
      /* This method tries to get a particular String aString.
        It consumes the String and returns true 
        if the desired string is there, 
        otherwise it does not consume the String and returns false.
        */
      {
  			boolean gotStringB= false;
    		theEpiInputStreamI.mark(0); // Marking stream position.
    		String inString= tryingToGetString();
		    appLogger.debug( "tryingToGetStringB(): inString= "+ inString );
    	  gotStringB=  // Testing for desired string.
    	  		aString.equals( inString );
		    if ( ! gotStringB ) // Resetting position if String is not correct.
    	  	theEpiInputStreamI.reset(); // Putting String back into stream.
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
  		  delimiterChar from theEpiInputStreamI.  It blocks if needed.
  		  The String returned does not include the delimiter.
  		 */
			{
				String readString= "";
				while (true) { // Reading and accumulating all bytes in string.
					if ( theEpiInputStreamI.available() <= 0 ) // debug.
						{
							readString+="!NO-DATA-AVAILABLE!";
		          appLogger.error( "readAString(): returning " + readString );
							break;
	  					}
					int byteI= theEpiInputStreamI.read();
					if ( delimiterChar == byteI ) break; // Exiting if terminator seen.
					readString+= (char)byteI;
					}
				return readString;
				}

		protected int readANumberI()
  		throws IOException //// , NumberFormatException
  		/* This method reads and returns one number int
  		  converted from a String ending in the delimiterChar.
  		  from theEpiInputStreamI.
  		  This means it could not be used for floating point numbers.  
  		  It blocks if needed.
  		 */
			{
				String numberString= readAString();
	      int numberI= Integer.parseInt( numberString );
			  return numberI;
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
	  		flush(); // Sending it by flushing.
        }

    protected void flush() throws IOException
      // This is shorthand for flushing the EpiOutputStream.
      {
	  		theEpiOutputStreamO.flush(); // Sending it by flushing.
        }

    protected void writingSequenceNumberV() throws IOException
      /* This method increments and writes the packet ID (sequence) number
        to the EpiOutputStream.
        It doesn't flush().
        */
      {
	  		writingTerminatedStringV( "N" );
	  		////writingTerminatedStringV( (++packetIDI) + "" );
	  		writingTerminatedLongV( 
	  				(theEpiOutputStreamO.getCounterNamedInteger().getValueL()) 
	  				////  + "" 
	  				);
        }

    protected void writingTerminatedStringV( String aString ) 
    		throws IOException
      /* This method writes aString with delimiterChar
        as the terminator character on the end to the EpiOutputStream.
        It doesn't flush().
        */
      { 
	  		writingStringV( aString );
	  		////writingStringV( "." );
	  		writingStringV( String.valueOf(delimiterChar) );
        }

    protected void writingTerminatedLongV( long theL ) 
    		throws IOException
      /* This method writes theL with delimiterChar
        as the terminator character on the end to the EpiOutputStream.
        It doesn't flush().
        */
      { 
	  		writingTerminatedStringV( theL + "" ); // Converting to String.
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
