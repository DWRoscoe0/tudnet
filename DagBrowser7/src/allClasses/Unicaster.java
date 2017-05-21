package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Timer;

import static allClasses.Globals.appLogger;

//// import allClasses.HelloMachineState.ProcessingFirstHelloState;
import allClasses.LockAndSignal.Input;

public class Unicaster

	extends Netcaster

  implements Runnable 

  /* Each instance of this class manages a a single Datagram connection with
    one of the peer nodes of which the ConnectionManager is aware.
    It uses unicast packets, not multicast packets.
    
    This class is not a Thread, but is a Runnable on which to base a Thread.
    The Runnable contains a loop which:
    * Receives packets from the remote peer.
    * Send packets to the remote peer.
    * Implements several protocols for various purposes, such as:
      * Establishing and shutting down a connection.
	    * Exchanging sequence numbers to measure packet loss and round-trip-time. 
	    * Multiplexing packets from and de-multiplexing packets to other threads
	      which implement their own protocols.
    
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
    but reopening the unconnected socket disables the connected ones again.
    As a result, connected sockets are not used.
    Instead all packets are received by one unconnected DatagramSocket, 
    and those packets are de-multiplexed and forwarded to 
    the appropriate peer thread.
    */

  { // Unicaster.

    // Fields (constant and variales).
    
      // Injected dependency instance variables
      private final UnicasterManager theUnicasterManager;
      private final SubcasterManager theSubcasterManager;
      private final SubcasterQueue subcasterToUnicasterSubcasterQueue;
  		private Timer theTimer;
  		
  		// Other instance variables.
  		private BeforeHelloExchangeState theBeforeHelloExchangeState;
  		private AfterHelloExchangedState theAfterHelloExchangedState;
  		private MultiMachineState theMultiMachineState;
  		private IgnoreAllSubstatesState theIgnoreAllSubstatesState;
  		private TemporaryMainState theTemporaryMainState;
  		
  	public Unicaster(  // Constructor. 
			  UnicasterManager theUnicasterManager,
			  SubcasterManager theSubcasterManager,
	    	LockAndSignal theLockAndSignal,
	      NetcasterInputStream theNetcasterInputStream,
	      NetcasterOutputStream theNetcasterOutputStream,
	      IPAndPort remoteIPAndPort,
	      Shutdowner theShutdowner,
	      SubcasterQueue subcasterToUnicasterSubcasterQueue,
	  		Timer theTimer,
	  		NamedLong retransmitDelayMsNamedLong,
	  		DefaultBooleanLike leadingDefaultBooleanLike
	  		)
	    /* This constructor constructs a Unicaster for the purpose of
	      communicating with the node at remoteInetSocketAddress,
	      but no response has yet been made.
	      Fields are defined in a way to cause an initial response.
	      
	      ?? Add parameter which controls whether thread first waits for
	      a PING or an REPLY, to reduce or eliminate ping-ping conflicts.
	      Implement protocol with a state-machine.
	      */
	    {
	      super(
	      		theLockAndSignal,
	  	      theNetcasterInputStream,
	  	      theNetcasterOutputStream,
	          theShutdowner,
		        remoteIPAndPort,
	      		"Unicaster", 
	          retransmitDelayMsNamedLong, 
	          leadingDefaultBooleanLike
	          );
	
	      // Storing injected dependency arguments not stored in superclass.
				  this.theUnicasterManager= theUnicasterManager;
				  this.theSubcasterManager= theSubcasterManager;
				  this.subcasterToUnicasterSubcasterQueue= 
				  		subcasterToUnicasterSubcasterQueue;
		  		this.theTimer= theTimer;
	      }

    protected void initializeWithIOExceptionV() throws IOException
	    {
    		super.initializeWithoutStreamsV(); // Stream counts are added below in
    		  // one of the sub-state machines.

	  		// Create and start the sub-state machines.

    		// Create and add actual sub-states.
	  	  theBeforeHelloExchangeState= new BeforeHelloExchangeState();
	  	  theBeforeHelloExchangeState.initializeWithIOExceptionV(); //////
	  	  addStateListV( theBeforeHelloExchangeState );
	  	  theAfterHelloExchangedState= new AfterHelloExchangedState();
	  	  theAfterHelloExchangedState.initializeWithIOExceptionV(); //////
	  	  addStateListV( theAfterHelloExchangedState );
	  	  requestSubStateListV( theBeforeHelloExchangeState ); // Initial state.

	  	  theTemporaryMainState= new TemporaryMainState();
	  	  theTemporaryMainState.initializeWithIOExceptionV();
	  	  
	  	  theIgnoreAllSubstatesState= new IgnoreAllSubstatesState();
	  	  theIgnoreAllSubstatesState.initializeWithIOExceptionV();

	  	  theMultiMachineState= new MultiMachineState(
	  				theTimer, 
	  			  theEpiInputStreamI,
	  				theEpiOutputStreamO,
	  				retransmitDelayMsNamedLong,
	  				this
	  	  		);
	  	  theMultiMachineState.initializeWithIOExceptionV();

	  	  theIgnoreAllSubstatesState.addB( theMultiMachineState );

	  	  addB( theTemporaryMainState );
	  	  addB( theMultiMachineState );
	  	  addB( theIgnoreAllSubstatesState );

	  	  theMultiMachineState.finalStateHandlerB(); // Start the machines,
	  	    // by starting their timers, by callinG the main machine handler.
	  	  
	  	  addB( theSubcasterManager );
	    	}

    protected void finalizingV() throws IOException
	    // This is the opposite of initilizingV().
	    {
	    	theMultiMachineState.finalizeV();
	    	theEpiOutputStreamO.close(); // Closing output stream.
	    	}

    public void run()  // Main Unicaster thread.
      /* This method contains the main thread logic.
        */
      {
        try { // Operations that might produce an IOException.
	          initializeWithIOExceptionV();

	          enterV(); 
					  while (true) { // Repeating until termination is requested.
					  	LockAndSignal.Input theInput= 
				  				theLockAndSignal.testingForInterruptE();
			      	if ( theInput != Input.NONE ) break; // Exit if interrupted.
            	finalStateHandlerB(); // Handle things as a state-machine.
				  	  theInput= // Waiting for at least one new input.
			    			  theLockAndSignal.waitingForInterruptOrNotificationE();
				  	  }
	          
	          finalizingV();
	  	    	theUnicasterManager.removingV( this ); // Removing self from manager.
          	}
          catch( IOException e ) {
          	Globals.logAndRethrowAsRuntimeExceptionV( 
          			"run() IOException", e 
          			);
            }

        appLogger.info("run() exiting."); // Needed if thread self-terminates.
        }

    public void overrideStateHandlerV() throws IOException
      /* This method does, or will do itself, or will delegate to Subcasters, 
        all protocols of a Unicaster.  This might include:
        * Doing full PING-REPLY protocol by letting a Subcaster do it, 
        	and forwarding its packets in both directions.
        * Passing packets from Subcasters to the network.
        * Passing Subcaster packets from the network to Subcasters.
        * Doing simple received message decoding.
        * Connection/Hello handshake state machine cycling.
        */
	    { super.orStateHandlerB(); // Behave as an OrState.
	    	}

		private class BeforeHelloExchangeState extends StateList 
	  	/* This class is active before HELLO messages have been exchanged.
	  	  It tries to exchange HELLO messages with the remote peer
	  	  to decide which peer will lead and which will follow.
	  	  It retries using exponential back-off 
	  	  until the acknowledgement is received.
			  */
	  	{
			  public void overrideStateHandlerV() throws IOException ////// Not used yet.
			  	{ if (!processingHellosB()) 
			  			requestStateListV( finalSentinelState ); //// break processing;
			  		requestStateListV( theAfterHelloExchangedState );
			  		}
		  		} // class BeforeHelloExchangeState

		private class AfterHelloExchangedState extends StateList 
			/* This state class is active after HELLO messages are exchanged.
			  It responds to late HELLO messages and enables the other protocols.
			  */
	  	{
		  	public void enterV() throws IOException
				  { super.enterV(); // This is mainly to set background color.
			      theSubcasterManager.getOrBuildAddAndStartSubcaster(
						  "PING-REPLY" ///tmp Hard wired creation at first.  Fix later.
						  ); // Adding Subcaster.
						}
			  public void overrideStateHandlerV() throws IOException ////// Not used yet.
			  	{ while (true) { // Repeating until termination interrupt occurs.
					  	LockAndSignal.Input theInput= 
				  				theLockAndSignal.testingForInterruptE();
			      	if ( theInput != Input.NONE ) break; // Exit if interrupted.
			      	if (tryProcessingOneRemoteMessageB()) continue;
			    			//////// Eventually need this to not ignore + consume.
			      	if // Input and ignore any other message from peer.
			      	  ( theEpiInputStreamI.tryingToGetString() != null ) continue;
			    		if (multiplexingPacketsFromSubcastersB()) continue;
				  	  theInput= // Waiting for at least one new input.
			    			  theLockAndSignal.waitingForInterruptOrNotificationE();
				  	  }
		  	  	} 
				public void exitV() throws IOException
				  { if  // Informing remote end whether app is doing a Shutdown.
		    			( theShutdowner.isShuttingDownB() ) 
			    		{ theEpiOutputStreamO.writingAndSendingV("SHUTTING-DOWN"); // Informing peer.
			          appLogger.info( "SHUTTING-DOWN message sent.");
			    			}
		    		theSubcasterManager.stoppingEntryThreadsV();
						super.exitV(); // This is mainly to set background color.
						}

		  		} // class AfterHelloExchangedState

		class TemporaryMainState extends StateList 
	    {

		    public void overrideStateHandlerV() 
		      /// Does nothing, thereby ignoring all sub-states.
		      {}

		    } // TemporaryMainState

    class IgnoreAllSubstatesState extends StateList
      /* This class holds, but does not activate, 
        any states or machines it holds.
        It is for holding states which either are not finished,
        or are finished but their handlers are 
        being temporarily called in a non-standard way.
       */
	    {

		    public void overrideStateHandlerV() 
		      /// Does nothing, thereby not calling any of the sub-states.
		      {}

		    } // IgnoreAllSubstatesState 
    
    /*  ///dbg
		private void sendTestPacketV() throws IOException
			{
				//appLogger.debug( "Queuing packet with TEST for sending.");
				writingTerminatedStringV( "TEST" );
				endingPacketV(); // Forcing send.
				EpiThread.interruptableSleepB(100); // Letting other threads run,
				  // also limiting bandwidth.
				}
    */ ///dbg
	  
		private boolean multiplexingPacketsFromSubcastersB() throws IOException
		  /* This method tries to input forward one message packet from 
		    any of the Subcasters to a remote peer.
		    It does this by nesting the Subcaster packet data in a NetcasterPacket.
		    It returns true if success, false otherwise.
		    */
			{
			  boolean gotInputB= false;
				process: {  // Process one packet queued from Subcasters.
		      SubcasterPacket theSubcasterPacket= // Getting next SubcasterPacket 
	        		subcasterToUnicasterSubcasterQueue.poll(); // from queue.
	        if (theSubcasterPacket == null) // Exiting if queue empty.
	        	break process; 
	        nestedWritingWithMultiplexHeaderV(theSubcasterPacket);
	        gotInputB= true; // Indicating input processed.
	        } // process: 
				return gotInputB;
			 	}

		private void nestedWritingWithMultiplexHeaderV(
				  SubcasterPacket theSubcasterPacket
				  )
				throws IOException
		  /* This method forwards theSubcasterPacket to the remote peer.
		    It prepends the Subcaster key as a multiplex/demultiplex header.
			  ///opt Improve efficiency by passing a buffer window instead of 
			    doing write(..)?
		    */
			{
	      DatagramPacket theDatagramPacket= // Extracting DatagramPacket.
						theSubcasterPacket.getDatagramPacket();
	      //theEpiOutputStreamO.flush(); // Flushing to prepare new stream buffer.
	      ///writingSequenceNumberV();
	    		theEpiOutputStreamO.writingTerminatedStringV( // Writing key as de-multiplex header. 
	      	theSubcasterPacket.getKeyK() 
	      	);
	      theEpiOutputStreamO.writingTerminatedLongV( 
				  theDatagramPacket.getLength() 
				  );
				theEpiOutputStreamO.write( // Writing nested Subcaster packet.
						theDatagramPacket.getData(),
						theDatagramPacket.getOffset(),
						theDatagramPacket.getLength()
						);
				theEpiOutputStreamO.sendingPacketV(); ///opt? Could this be delayed?
				}

		private boolean tryProcessingOneRemoteMessageB() throws IOException
		  /* This method tries to input and processes 
		    one message from the remote peer.
		    A message might be in a single packet or several packets.
		    If a message begins with a Subcaster key then 
		    the body of the message is forwarded to 
		    the associated Subcaster as a new nested packet. 
		    Otherwise the message is decoded locally.
		    This method returns true if it consumes a message, false otherwise.
		    //// If it encounters a message that it doesn't understand,
		    //// then it consumes the message but also ignores it.
		    //// Make it not such messages.
		    */
		  {
			  boolean successB= (theEpiInputStreamI.available() > 0);
			  if ( successB ) { // Processing some input.
		  	  try 
			  	  { successB= processingRemotePeerMessageB(); }
			    	catch (IllegalArgumentException anIllegalArgumentException)
				    { // Handling any parse errors.
		        	appLogger.warning(
		        			"Packet parsing error: " + anIllegalArgumentException
		        			);
			    		theEpiInputStreamI.emptyingBufferV(); // Consuming remaining 
			    		  // bytes in buffer because using them is impossible.
					    }
			  	} // Processing some input
			  return successB;
				}

		private boolean processingRemotePeerMessageB() throws IOException
		  /* This method processes the next available message.
		    One is assumed to be available.
		    Most will be demultiplexed and passed to Subcasters.
		    Some will be processed in this thread.
		    If the message is recognized and processed then true is returned.
		    If the message is not recognized then 
		    it is returned to the input stream and true is returned.
		   */
			{
			  boolean successB= true;
				theEpiInputStreamI.mark(0); // Prepare to back up if message not known.
				String keyString=  // Reading message key string
						theEpiInputStreamI.readAString();

				process: {
		      Subcaster theSubcaster= // Getting associated Subcaster, if any.
		        theSubcasterManager.tryingToGetDataNodeWithKeyD( keyString );
		      if // Passing remainder of message to associated Subcaster.
		        ( theSubcaster != null )
		        { processMessageToSubcasterV( theSubcaster ); break process; }
		      if ( theMultiMachineState.finalHandleSynchronousInputB(keyString) )
		      	break process;
  			  if ( processHelloB( keyString ) ) // "HELLO"
   			  	break process;
   			  if ( processShuttingDownB(keyString) ) // "SHUTTING-DOWN"
   			  	break process;
    		  if ( keyString.equals( "TEST" ) ) // "TEST" packet.
   			  	break process; // Accepting by breaking.

    		  ////theEpiInputStreamI.reset();
    		  appLogger.warning( 
    		  	"processingRemoteMessageB(): Ignoring unknown remote message: " 
    		  	+ keyString 
    		  	);
					} // process:
				  return successB;
				}

		public boolean processingHellosB()
			throws IOException
		  /* This method tries to establish the Unicaster connection 
		    by exchanging HELLO messages.  This includes:
		    * Sending a HELLO message.
		    * Receiving a HELLO message.
		    * determining which peer should act as the leader when needed.
		      If the local peer decides to be the LEADER then 
		      the remote peer decides to be the follower, and vice versa.
		      This determination is based on the peers' IP addresses.
		    * Ignoring messages other than HELLO.
		    This method should execute in both peers at approximately
		    the same time and is called when the local and remote peers
		    recognize each other's existence.
		    Returns true if HELLO messages were exchanged, meaning
		    the connection may be considered established.
		    Returns false otherwise.
		    
		    ///enh This could be a state machine, but doesn't have to be,
		    because it happens only when a peer-to-peer connection is established.
		    
		    ///enh Replace retry count with a overall time limit?
		    */
			{
				int triesRemainingI= 3; // 3 tries then we give up.
			  boolean successB= false;
			  long helloIimeLimitMsL= retransmitDelayMsNamedLong.getValueL();
			  tryingToConnectByExchangingHellos: while (true) {
          if ( triesRemainingI-- <= 0 ) // Exiting if try limit exceeded. 
          	break tryingToConnectByExchangingHellos;
	        appLogger.info( "Sending HELLO." );
	    		theEpiOutputStreamO.writingTerminatedStringV( "HELLO" );
	    		theEpiOutputStreamO.writingTerminatedStringV(  // Writing other peer's IP address. 
		  				getKeyK().getInetAddress().getHostAddress() 
		  				);
	    		theEpiOutputStreamO.sendingPacketV(); // Forcing send.
          long helloSentMsL= System.currentTimeMillis();
          processingPossibleHelloResponse: while (true) {
            Input theInput=  // Awaiting next input within time interval.
            		waitingForSubnotificationOrIntervalOrInterruptE( 
            				helloSentMsL, helloIimeLimitMsL
            				);
            if // Handling possible exit interrupt.
    	      	( theInput == Input.INTERRUPTION )
  	    			break tryingToConnectByExchangingHellos; // Exit everything.
            if ( theInput == Input.TIME ) // Handling possible time-out.
	            { appLogger.info( "Time-out waiting for HELLO." );
	              break processingPossibleHelloResponse;
	            	}
	    			String keyString= // Reading SUBNOTIFICATION message key string.
	    					theEpiInputStreamI.readAString(); 
	  			  if // Handling possible received HELLO key message. 
	  			    ( processHelloB( keyString ) ) 
	  			  	{ successB= true; break tryingToConnectByExchangingHellos; }
	  			  // Ignoring any other message.
          	} // processingPossibleHelloResponse: while (true)
          helloIimeLimitMsL*=2;  // Doubling time limit.
  			  } // tryingToConnectByExchangingHellos:
				return successB;
				}
  	
  	private boolean processHelloB(String keyString) 
  			throws IOException
  	  /* This method tries to process the Hello message and its arguments.
  	    If keyString is "HELLO" then it processes,
  	    which means parsing the IP address which follows and determining
  	    which peer, the local or remote, will be leader,
  	    by comparing the IP addresses, and setting 
  	    the value for leadingDefaultBooleanLike.
  	    It does not send a reply "HELLO".  
  	    This is assumed to have been done simultaneously elsewhere.
  	    It returns true if HELLO was processed, false otherwise.
  	    The first time this method is called is the only one that counts.
  	    Later calls might produce redundant retransmissions, 
  	    but should have no other effect.
  	   */
	  	{
  		  boolean isKeyB= keyString.equals( "HELLO" ); // Testing key.
  		  if (isKeyB) { // Decoding argument if key is "HELLO".
					String localIpString= theEpiInputStreamI.readAString();
					String remoteIpString= getKeyK().getInetAddress().getHostAddress();
					leadingDefaultBooleanLike.setValueB( 
							localIpString.compareTo(remoteIpString) > 0 
							);
					//leadingDefaultBooleanLike= !leadingDefaultBooleanLike; // Reverse roles for debug test. 
					// Note, the particular ordering of IP address Strings
					// doesn't matter.  What matters is that the ordering is consistent.
	        appLogger.info( 
	        		"HELLO received.  Overriding role to be: "
	        		+ (leadingDefaultBooleanLike.getValueB() ? "LEADER" : "FOLLOWER")
	        		);
				  }
  		  return isKeyB;
	  		}

  	private boolean processShuttingDownB(String keyString)
  	  /* This method sets this thread's interrupt status to cause exit
  	    if keyString is "SHUTTING-DOWN".
  	   */
      {
			  boolean isKeyB= keyString.equals( "SHUTTING-DOWN" ); 
				if (isKeyB) {
	        appLogger.info( "SHUTTING-DOWN message received.");
	        Thread.currentThread().interrupt(); // Interrupting this thread.
	          // This will cause thread termination.
					}
				return isKeyB;
        }

		private void processMessageToSubcasterV( Subcaster theSubcaster )
      throws IOException 
      /* This method sends the remainder of the present message buffer
        to theSubcaster for processing.
			  The message key string is assumed to have already been read
			  and decoded to mean theSubcaster.

			  ///opt Improve efficiency by passing a buffer window instead of 
			    doing a read(..) into a new buffer.
				*/
      {
			  processing: {
					String lengthString= // Reading Subcaster message length String. 
							theEpiInputStreamI.readAString();
					int lengthI= Integer.parseInt( lengthString );
	        SubcasterPacketManager theSubcasterPacketManager= 
	        		theSubcaster.getPacketManagerM();
	        byte[] bufferBytes= // Allocating Subcaster packet buffer. 
	        		theSubcasterPacketManager.produceBufferBytes( lengthI );
	        if // Reading Subcaster message bytes into buffer, or exiting.
	          ( theEpiInputStreamI.read( bufferBytes, 0, lengthI ) < lengthI )
				    { // Handling failure to fit error.
		        	appLogger.error(
		        			"Subcaster packet repacking error: length: "
		        			);
			    		break processing; // Exiting.
					    }
	        //appLogger.debug(
	        //		"processMessageToSubcasterV(): " + 
	        //	new String( bufferBytes, 0, lengthI )
	        //	);
					SubcasterPacket theSubcasterPacket= // Repackaging buffer. 
						theSubcasterPacketManager.produceKeyedPacketE(
									bufferBytes, lengthI
									);
		      theSubcaster.puttingKeyedPacketV( // Passing to Subcaster. 
		      		theSubcasterPacket 
		      		);
					} //processing:
	      }


	} // Unicaster.
