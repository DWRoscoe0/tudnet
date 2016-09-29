package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;

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

  /* 
    This is the base class DataNode for UDP packet streaming.
    It doesn't know anything about packet IP addresses or ports.
    That is handled by its subclass Netcaster.
    But it does know about UDP packet boundaries and flushing.
    
    This class is extended by:
    * Netcaster for data streams which are communicated within real UDP packets.
    * Subcaster for data streams which are nested within Netcaster streams.
     
    */

	{
  
	  // Constants.
		private final char delimiterChar= '!';
		
	  protected final long PeriodMillisL= // Period between handshakes.
	    //4000;   // 4 seconds.
	  	//100;
	  	//200;
	  	//400;
	  	//1000;
	    2000;
	  protected final long HalfPeriodMillisL= // Half of period.
	    PeriodMillisL / 2;  

    protected final LockAndSignal streamcasterLockAndSignal;
			// LockAndSignal for inputs to this thread.  It is used in
      // the construction of the following queue. 

	  protected final O theEpiOutputStreamO;
		protected final I theEpiInputStreamI;

    protected final Shutdowner theShutdowner;

		protected long pingSentNanosL; // Time the last ping was sent.

    // Detail-containing child sub-objects.
      protected NamedInteger roundTripTimeNamedInteger;
        // This is an important value.  It can be used to determine
        // how long to wait for a message acknowledgement before
        // re-sending a message.

    // Other variables.
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
      // Adds a DataNode for displaying round trip time.
      //// This might be eliminated if roundTripTimeNamedInteger is moved.
      // It's not presently calculated.
	    {
	      addB( 	roundTripTimeNamedInteger= new NamedInteger( 
	      		theDataTreeModel, "Round-Trip-Time-ns", -1
	      		)
	  			);
	
        appLogger.info( 
        		"This Streamcasteer has been given the role of: "
        		+ (leadingB ? "LEADER" : "FOLLOWER")
        		);
		    }

		public void pingReplyProtocolV() throws IOException
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
        send a ping to the remote peer and receiving a REPLY in response.
        If it doesn't receive a REPLY message in response
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
        pingEchoRetryLoop: while (true) { // Sending PING and receiving REPLY.
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
          replyWaitLoop: while (true) { // Handling echo or other conditions.
            theInput=  // Awaiting next input within reply interval.
            		testWaitInIntervalE( pingSentMsL, HalfPeriodMillisL );
            if ( theInput == Input.TIME ) // Exiting echo wait if time-out.
              { appLogger.info( "Time-out waiting for REPLY: "+triesI );
                break replyWaitLoop;  // End wait to send new PING, maybe.
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
      			    //appLogger.debug( "Got REPLY." );
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
                    break replyWaitLoop;  // End wait to send new PING, maybe.
              			}
              	  }
        		ignoringOrLoggingStringV(inString); // Ignoring any other string.
            } // replyWaitLoop:
          triesI++;
          } // pingEchoRetryLoop: 
        }

		private void ignoringOrLoggingStringV()
		  throws IOException
		  /* This method is similar to ignoringOrLoggingStringV(String inString)
		    but it tries to read the string which is to be ignored.
		    If there is not string available for reading then it is logged as null.
		    */
		  {
			  String inString= tryingToGetString( ); // Reading string to ignore.
			  ignoringOrLoggingStringV(inString); // Ignoring it.
			  }

		private void ignoringOrLoggingStringV(String inString)
				 throws IOException
		  /* This method does several things:
		    * It ignores String inString which supposedly was just read
		      from the InputStream and can't be processed for some reason.
		    * It logs the fact of ignoring inString, for debugging, if enabled.
		    * It documents with its calls where input is ignored.
		    */
			{
		    appLogger.debug( 
						"ignoringOrLoggingStringV(..) : "
					+ inString
					+ " from "
					+ PacketManager.gettingPacketString( 
							theEpiInputStreamI.getKeyedPacketE().getDatagramPacket()
							)
					);
				}

    protected void tryingPingReceiveV() throws IOException
	    /* This method does the sub-protocol of trying to 
		    receive a PING message from the remote peer 
		    to which it replies by sending a REPLY response.
        If a PING is not immediately available then
        it waits up to PeriodMillisL for a PING to arrive,
        after which it gives up and returns.
        If a PING is received it responds immediately by sending a REPLY,
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
	          theInput= testWaitInIntervalE( // Awaiting next input.
	          		pingWaitStartMsL, PeriodMillisL + HalfPeriodMillisL
	          		);
	          if ( theInput == Input.TIME ) // Exiting all if time-out.
		          {
		            appLogger.warning( "Time-out waiting for PING." );
		            break all;  // Exiting to abort wait.
		          	}
	      		if // Handling exit request interrupt by exiting.
		    			( tryingToCaptureTriggeredExitB( ) )
		    			break all;
	      		// Note, can't readAString() here because it might not be available?
            //appLogger.debug( "Testing for PING." );
            AppLog.testingForPingB= true;
	          if // Handling a received ping if present.
	            ( tryingToGetStringB( "PING" ) )
		          {
	          		AppLog.testingForPingB= false;
		            //appLogger.debug( "tryingPingReceiveV() got PING." );
		          	break pingWaitLoop;  // Exiting pingWaitLoop only.
		          	}
        		AppLog.testingForPingB= false;
        		ignoringOrLoggingStringV(); // Ignoring any other string.
	          } // pingWaitLoop
	        while(true) { // Sending and confirming receipt of REPLY.
	          writingPacketV("REPLY");
	          long echoSentMsL= System.currentTimeMillis();
	          postReplyPause: while (true) {
	            theInput=  // Awaiting input within the ignore interval.
	            		testWaitInIntervalE( echoSentMsL, PeriodMillisL );
	            if ( theInput == Input.TIME ) // Exiting if time limit reached.
	              break all;  
	        		if // Exiting everything if exit has been triggered.
	        			( tryingToCaptureTriggeredExitB( ) )
	        			break all;
		          if // Handling a repeat received ping if present.
		        	  ( tryingToGetStringB( "PING" ) )
			          { 
			            appLogger.warning( "tryingPingReceiveV(): got repeat PING." );
			          	break postReplyPause;  // Exiting pingWaitLoop only.
			            }
	        		// Change following to use: ignoringOrLoggingStringV(); // Ignoring any other string.
	        		String aString= readAString(); // Reading message to ignore it.
	            appLogger.warning( 
	        			"tryingPingReceiveV(): unexpected " + aString + ", ignoring"
	        			);
	          	} // postReplyPause: 
	          }
    			} // all:
        }

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
            	break process; 
            	}
    	  	theInput= // Doing general wait for anything else. 
    	  	  streamcasterLockAndSignal.doWaitWithTimeOutE( remainingMsL );
	        if // Handling thread interrupt signal.
	          ( EpiThread.exitingB() )
	          { theInput= Input.INTERRUPTION; break; }
    	  	} // process: while (true) 

    	  return theInput;
	      }

    protected boolean tryingToCaptureTriggeredExitB( ) throws IOException
      /* This method tests whether exit has been triggered, meaning:
        * The current thread's isInterrupted() is true.
		    This method returns true if exit is triggered, false otherwise.

        It no longer does the following additional check:
        * The next packet, if any, at the head of 
          the receiverToStreamcasterNotifyingQueueQ,
		    	contains "SHUTTING-DOWN", indicating the remote node is shutting down.
		    	If true then the packet is consumed and
		    	the current thread's isInterrupted() status is set true.
		    */
      { 
	      return EpiThread.exitingB();
        }


    // String reading methods.

    protected boolean tryingToGetStringB( String aString ) throws IOException
      /* This method tries to get a particular String aString.
        It consumes the String and returns true 
        if the desired string is there, 
        otherwise it does not consume the String and returns false.
        The string is considered to be not there if either:
        * There are no characters available in the input stream buffer.
        * The characters available in the input stream buffer are
          not the desired string.
        */
      {
  			boolean gotStringB= false;
    		theEpiInputStreamI.mark(0); // Marking stream position.
    		String inString= tryingToGetString();
		    //appLogger.debug( "tryingToGetStringB(): inString= "+ inString );
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
  		throws IOException
  		/* This method reads and returns one int number 
  		  converted from a String ending in the delimiterChar.
  		  from theEpiInputStreamI.
  		  This means it could not be used for floating point numbers.  
  		  It blocks if a full number is not available.
  		 */
			{
				String numberString= readAString();
	      int numberI= Integer.parseInt( numberString );
			  return numberI;
				}
		


    // String writing and packet sending code.

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
      /* This flushes the stream.
        It guarantees a packet boundary by sending a packet.
        This version simply flushes that EpiOutputStream,
        but an overridden version could do things such as
    		adding a sequence number.
    		*/
      {
	  		theEpiOutputStreamO.flush(); // Sending it by flushing.
        }

    protected void writingTerminatedStringV( String aString ) 
    		throws IOException
      /* This method writes to the EpiOutputStream the following:
        * aString 
        * delimiterChar as the terminator character on the end.
        It doesn't flush() the stream.
        */
      { 
	  		writingStringV( aString );
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
