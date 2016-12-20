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
		//% private final char delimiterChar= Config.delimiterChar; //////
		
	  protected final long PeriodMillisL= // PING-REPLY handshake period.
	    //4000;   // 4 seconds.
	  	//100;
	  	//200;
	  	//400;
	  	//1000;
	    Config.pingReplyHandshakePeriod2000MsL; // 2000;
	  protected final long HalfPeriodMillisL= // Half of period.
	    PeriodMillisL / 2;  

	  // Injected dependencies.
	  protected final LockAndSignal theLockAndSignal;
			// LockAndSignal for inputs to this thread.  It is used in
      // the construction of the following queue. 
	  protected final O theEpiOutputStreamO;
		protected final I theEpiInputStreamI;
    protected final Shutdowner theShutdowner;
    protected NamedLong retransmitDelayMsNamedLong;

    // Detail-containing child sub-objects.  None.

    // Other variables.
    protected boolean leadingB= false; // Used to settle race conditions.
      
	  public Streamcaster(  // Constructor.
	      DataTreeModel theDataTreeModel,
	      String nameString,
        Shutdowner theShutdowner,
        boolean leadingB,
	  		K theKeyK,
	  		LockAndSignal theLockAndSignal,
	      I theEpiInputStreamI,
	      O theEpiOutputStreamO,
	      NamedLong retransmitDelayMsNamedLong
	  		)
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableList superclass.
		        theDataTreeModel,
		        nameString, // Type name but not entire name.
		        theKeyK
	      		);

	      this.theLockAndSignal= theLockAndSignal;
	      this.theEpiInputStreamI= theEpiInputStreamI;
	      this.theEpiOutputStreamO= theEpiOutputStreamO;
        this.theShutdowner= theShutdowner;
        this.leadingB= leadingB;
        this.retransmitDelayMsNamedLong= retransmitDelayMsNamedLong;
	    	}

    protected void initializingV() throws IOException
	    {
	  	  addB( retransmitDelayMsNamedLong );
	  	  
        appLogger.info( 
        		"This Streamcaster has been given the role of: "
        		+ (leadingB ? "LEADER" : "FOLLOWER")
        		);
		    }

		public void pingReplyProtocolV() throws IOException
      /* This method does the full PING-REPLY protocol.  
        It will terminate this thread if requested.
        Except for this method, this protocol code 
        is written as a thread, not as a state machine.
        Control stays within it until Unicaster termination.
       */
	    {
	  	  if (! leadingB) // Start with ping-receive if not leading. 
	  	  	tryingPingReceiveV();
	      while (true) // Repeating until thread termination is requested.
	        {
	      		if ( EpiThread.exitingB() ) break;  // Exiting if requested.
	  	      tryingPingSendV();
	  	  		if ( EpiThread.exitingB() ) break;  // Exiting if requested.
	  	    	tryingPingReceiveV();
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
        long retransmitDelayMsL= retransmitDelayMsNamedLong.getValueL();
        pingReplyLoop: while (true) { // Sending PING and receiving REPLY.
          if  // Checking and exiting if maximum attempts have been done.
            ( triesI > maxTriesI )  // Maximum attempts exceeded.
            { // Terminating thread.
              appLogger.info(
              		"Requesting termination after "
                	+"\n  "+maxTriesI+" REPLY time-outs."
                  );
              Thread.currentThread().interrupt(); // Starting termination.
              break pingReplyLoop;
              }
          theEpiOutputStreamO.writingAndSendingV("PING"); // Sending ping packet.
          long pingSentMsL= System.currentTimeMillis();
          replyWaitLoop: while (true) { // Handling echo or other conditions.
            theInput=  // Awaiting next input within reply interval.
            		waitingForSubnotificationOrIntervalOrInterruptE( 
            				//%pingSentMsL, HalfPeriodMillisL
            				pingSentMsL, retransmitDelayMsL
            				);
            if ( theInput == Input.TIME ) // Exiting echo wait if time-out.
              { appLogger.info( "Time-out waiting for REPLY: "+triesI );
                break replyWaitLoop;  // End wait to send new PING, maybe.
              	}
            if // Handling thread interrupt by exiting.
		          ( theInput == Input.INTERRUPTION )
  	    			break pingReplyLoop; // Exit everything.
            theEpiInputStreamI.mark(0); // Preparing to not consume message.
        		String inString=  // Reading message.
        				theEpiInputStreamI.readAString();
        		if ( inString.equals( "REPLY" ) ) // Handling echo, maybe.
              { // Handling echo and exiting.
      			    //appLogger.debug( "Got REPLY." );
            		break pingReplyLoop; // Exit everything.
                }
        		if ( inString.equals( "PING" ) ) // Handling ping conflict, maybe.
              { // Handling ping conflict.
                appLogger.info( "PING-PING conflict." );
              	if ( ! leadingB ) // Arbitrating ping-ping conflict.
                  { // Yielding ping processing to other peer.
                    appLogger.info( "PING ping yield: "+triesI );
                    theEpiInputStreamI.reset(); // Putting message back.
                    break pingReplyLoop; // Yielding by exiting main loop.
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
          retransmitDelayMsL*= 2; // Doubling the time-out delay.
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
	          theInput= waitingForSubnotificationOrIntervalOrInterruptE( // Awaiting next input.
	          		pingWaitStartMsL, PeriodMillisL + HalfPeriodMillisL
	          		);
	      		if // Handling thread interrupt by exiting.
		          ( theInput == Input.INTERRUPTION )
		    			break all;
	          if ( theInput == Input.TIME ) // Exiting all if time-out.
		          {
		            appLogger.warning( "Time-out waiting for PING." );
		            break all;  // Exiting to abort wait.
		          	}
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
	        	theEpiOutputStreamO.writingAndSendingV("REPLY");
	          long echoSentMsL= System.currentTimeMillis();
	          postReplyPause: while (true) {
	            theInput=  // Awaiting input within the ignore interval.
	            		waitingForSubnotificationOrIntervalOrInterruptE( echoSentMsL, PeriodMillisL );
	            if ( theInput == Input.TIME ) // Exiting if time limit reached.
	              break all;  
	        		if // Exiting everything if exit has been triggered.
			          ( theInput == Input.INTERRUPTION )
	        			break all;
		          if // Handling a repeat received ping if present.
		        	  ( tryingToGetStringB( "PING" ) )
			          { 
			            appLogger.warning( "tryingPingReceiveV(): got repeat PING." );
			          	break postReplyPause;  // Exiting pingWaitLoop only.
			            }
	        		// Change following to use: ignoringOrLoggingStringV(); // Ignoring any other string.
	        		String theString=  // Reading message to ignore it.
	        				theEpiInputStreamI.readAString();
	            appLogger.warning( 
	        			"tryingPingReceiveV(): unexpected " + theString + ", ignoring"
	        			);
	          	} // postReplyPause: 
	          }
    			} // all:
        }

    protected Input waitingForSubnotificationOrIntervalOrInterruptE( 
    			long startMsL, long lengthMsL
    			) 
    		throws IOException
      /* This is a special wait method which uses 
        its own set of Input types and priority order, which is:
			      SUBNOTIFICATION
	          TIME
			      INTERRUPTION
		    This priority order is useful in those situations when
		    it is important to finish processing all other pending inputs
		    before processing an INTERRUPTION, which usually means a
		    thread termination request.

			  It is assumed that all SUBNOTIFICATION input data,
			  which is data being read from theEpiInputStreamI,
			  has been, or will be, preceded by NOTIFICATION input.
        */
	    {
    		LockAndSignal.Input theInput;
    		loopWithoutResult: while (true) {
	        if // Exiting if a datum from InputStream is ready.
	          ( theEpiInputStreamI.available() > 0 )
		        { theInput= Input.SUBNOTIFICATION; break loopWithoutResult; }
	        loopWithoutNotification: while (true) {
		        final long remainingMsL= theLockAndSignal.timeCheckedDelayMsL( 
          		startMsL, lengthMsL 
          		);
					  theInput= theLockAndSignal.testingRemainingDelayE( remainingMsL );
		      	if ( theInput != Input.NONE ) break loopWithoutResult;
		      	theInput= theLockAndSignal.testingForInterruptE();
		      	if ( theInput != Input.NONE ) break loopWithoutResult;
		      	theLockAndSignal.waitingForInterruptOrDelayOrNotificationV(
			      		remainingMsL
			      		);
		      	theInput= theLockAndSignal.testingForNotificationE();
		      	if ( theInput != Input.NONE ) break loopWithoutNotification;
	        	} // loopWithoutNotification: while(true)
    	  	} // loopWithoutResult: while (true) 
    	  return theInput;
	      }


    // String reading methods.

    protected boolean tryingToGetStringB( String theString ) throws IOException
      /* This method tries to get a particular String theString.
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
    	  		theString.equals( inString );
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
	    	  inString= theEpiInputStreamI.readAString();
	    	  }
  	  return inString;
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
				String numberString= theEpiInputStreamI.readAString();
	      int numberI= Integer.parseInt( numberString );
			  return numberI;
				}

		protected String XreadAString() //////
  		throws IOException
  		/* This method reads and returns one String ending in the first
  		  delimiterChar from theEpiInputStreamI.  
  		  The String returned does not include the delimiter.
  		  It does not block.
				If a complete string, including delimiter is not available,
				then logs and returns an empty string.
  		 */
			{
			  /*////
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
				*/ ////
				return theEpiInputStreamI.readAString();
				}
		


    // String writing and packet sending code.

    protected void XwritingAndSendingV( String theString ) throws IOException //////
      /* This method writes theString to theEpiOutputStream
        and then sends it and anything else that has been written 
        to the stream in a packet.
        */
      {
    		//% theEpiOutputStreamO.writingTerminatedStringV( theString );
    		//% theEpiOutputStreamO.sendingPacketV();
    		theEpiOutputStreamO.writingAndSendingV( theString );
        }

    protected void XsendingPacketV() throws IOException //////
      /* This method forces what has been written to the stream to be sent.
        It also guarantees a packet boundary at this point in the stream.
        It is equivalent to endingPacketV( 0 );
    		*/
      {
	  		//%theEpiOutputStreamO.flush(); // Sending it by flushing stream.
    		//%endingPacketV( 0 );
    		//% theEpiOutputStreamO.doOrScheduleSendB( 0 );
  			theEpiOutputStreamO.sendingPacketV();
        }

    protected void XendingPacketV( long delayMsL ) //% throws IOException  //////
      /* This method forces what has been written to the stream to be sent
        within delayMsL milliseconds.
        It also guarantees a packet boundary at this point in the stream.
    		*/
      {
	  		theEpiOutputStreamO.doOrScheduleSendB( delayMsL );
        }

    protected void XwritingTerminatedStringV( String theString ) ////// 
    		throws IOException
      /* This method writes to the EpiOutputStream 
        theString followed by the delimiterChar.
        But it doesn't force a flush().
        */
      { 
    		theEpiOutputStreamO.writingTerminatedStringV( theString );
	    	//% theEpiOutputStreamO.writingStringV( theString );
    		//% theEpiOutputStreamO.writingStringV( String.valueOf(delimiterChar) );
        }

    protected void XwritingTerminatedLongV( long theL ) ////// 
    		throws IOException
      /* This method writes theL long int 
        followed by the delimiterChar to the EpiOutputStream.  
        But it doesn't force a flush().
        */
      { 
  			//% theEpiOutputStreamO.writingTerminatedStringV( theL + "" ); // Converting to String.
    		theEpiOutputStreamO.writingTerminatedLongV( theL );
        }

    protected void XwritingStringV( String theString ) throws IOException //////
      /* This method writes theString to the EpiOutputStream.
        But it doesn't force a flush().
        */
      { 
    		///byte[] buf = theString.getBytes(); // Getting byte buffer from String
        ///theEpiOutputStreamO.write(buf); // Writing it to stream memory.
        theEpiOutputStreamO.writingStringV(theString);
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
    	  //%theStreamcasterQueueQ.add(theKeyedPacketE);
    	  theStreamcasterQueueQ.put(theKeyedPacketE);
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
