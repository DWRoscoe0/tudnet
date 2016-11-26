package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.TimerTask; 
import java.util.Timer;

import static allClasses.Globals.appLogger;


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
    * Implement several protocols for various purposes, such as:
      * Establishing and shutting down a connection.
	    * Exchanging sequence numbers to measure packet loss. 
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

    */

  { // Unicaster.

    // Fields (constant and variales).
    
      // Injected dependency instance variables
      private final UnicasterManager theUnicasterManager;
      private final SubcasterManager theSubcasterManager;
      private final SubcasterQueue subcasterToUnicasterSubcasterQueue;
  		private Timer theTimer;

      // Local variables for containing, measuring, and displaying stats.
      // Some are DataNode children.

      // Variables for managing incoming packets and 
      // their sequence numbers.  They all start at 0.
      private DefaultLongLike newIncomingPacketsSentDefaultLongLike;
	    	// This is set to the value of the sequence number argument of the
			  // most recently received "PS" message plus 1.  
			  // When sent this argument was the remote end's 
			  // EpiOutputStream packet counter.
      	// This value usually increases, but can decrease 
        // if an "PS" is carried by an out-of-order packets.
      private NamedInteger oldIncomingPacketsSentNamedInteger;
	    	// A difference between this and newIncomingPacketsSentDefaultLongLike 
        // indicates a new received sequence number needs to be processed.
      private NamedInteger newIncomingPacketsReceivedNamedInteger;
      	// This is a copy of the local EpiInputStream packet counter.
      	// It represents the latest count of locally received packets.
      	// This value can only increase.
  		private DefaultLongLike oldIncomingPacketsReceivedDefaultLongLike;
  			// A difference between this and newIncomingPacketsReceivedNamedInteger 
  			// indicates a new packet has been received and needs to be processed.
  		private NamedFloat incomingPacketLossNamedFloat;
        // Floating representation of the fraction of incoming packets lost.
      LossAverager incomingPacketLossAverager;
      
      // Variables for managing outgoing packets and their acknowledgement.
      private NamedInteger newOutgoingPacketsSentNamedInteger;
	    	// This is a copy of the local EpiOutputStreamO packet counter.
      	// This value can only increase.
      private NamedInteger newOutgoingPacketsSentEchoedNamedInteger;
	    	// This is the local EpiOutputStreamO packet counter after 
        // being returned from remote.  It might lag the real counter by RTT.
      	// This value can only increase.
      private DefaultLongLike oldOutgoingPacketsSentDefaultLongLike;
	    	// A difference between this and newOutgoingPacketsSentNamedInteger 
	      // indicates new packets have been sent and need to be processed.
      private NamedInteger newOutgoingPacketsReceivedNamedInteger;
	    	// This value comes from the most recently received "PA" message.  
			  // When sent this argument was the remote end's 
			  // EpiInputStream packet counter, 
        // counting the number of packets recevied.
	    	// This value normally increases, but can decrease because of 
        // out-of-order "PA" packets. 
      private DefaultLongLike oldOutgoingPacketsReceivedDefaultLongLike;
	    	// A difference between this and newOutgoingPacketsReceivedNamedInteger 
	      // indicates a new packet has been acknowledged and 
  			// needs to be processed.
  		private NamedFloat outgoingPacketLossNamedFloat;
      	// Floating representation of the fraction of outgoing packets lost.
      private LossAverager outgoingPacketLossLossAverager;

  		// Other instance variables.

      /*////
      private long timeToSendNextSequenceNumberMsL= 
       	System.currentTimeMillis()+1000;  /* Delay sending first PS.
            This is done to let startup logging settle down so
            it won't interfere with measurement of first Round-Trip-Time. 
            */ 

  		////private TimerInput theTimerInput;  // For PS-PS RTT timing. 
  		private RTTMeasurer theRTTMeasurer;
  		
			public Unicaster(  // Constructor. 
			  UnicasterManager theUnicasterManager,
			  SubcasterManager theSubcasterManager,
	    	LockAndSignal theLockAndSignal,
	      NetcasterInputStream theNetcasterInputStream,
	      NetcasterOutputStream theNetcasterOutputStream,
	      IPAndPort remoteIPAndPort,
        DataTreeModel theDataTreeModel,
        Shutdowner theShutdowner,
        SubcasterQueue subcasterToUnicasterSubcasterQueue,
    		Timer theTimer
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
  	        theDataTreeModel,
  	        remoteIPAndPort,
        		"Unicaster" 
        		);

        // Storing injected dependency arguments not stored in superclass.
  			  this.theUnicasterManager= theUnicasterManager;
  			  this.theSubcasterManager= theSubcasterManager;
  			  this.subcasterToUnicasterSubcasterQueue= 
  			  		subcasterToUnicasterSubcasterQueue;
  	  		this.theTimer= theTimer;

        }


    public void run()  // Main Unicaster thread.
      /* This method contains the main thread logic.
        */
      {
        try { // Operations that might produce an IOException.
	          initializingV();

	          { // Uncomment only one of the following method calls.
	          	runWithSubcastersV(); // Code which uses Subcasters.
	          	//runWithoutSubcastersV(); // Original code without Subcasters.
		          }
	          
	      		theEpiOutputStreamO.close(); // Closing output stream.
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

    protected void initializingV() throws IOException
	    {
    		super.initializingWithoutStreamsV(); // We do the streams below.

    		//% theTimer= new Timer(); ////
    		////theTimerInput=  new TimerInput( theLockAndSignal, theTimer ); ////
    		theRTTMeasurer= new RTTMeasurer( this ); ////

    		// Adding incoming packet statistics children and related trackers.
	  	  newIncomingPacketsSentDefaultLongLike= new DefaultLongLike(0);
	  	  addB( oldIncomingPacketsSentNamedInteger= new NamedInteger(
	      		theDataTreeModel, "Incoming-Packets-Sent", 0 
	      		)
	  			);
		    addB( newIncomingPacketsReceivedNamedInteger=
		    		theEpiInputStreamI.getCounterNamedInteger()
		    		);
	  	  oldIncomingPacketsReceivedDefaultLongLike= new DefaultLongLike(0);
	  	  addB( incomingPacketLossNamedFloat= new NamedFloat( 
	      		theDataTreeModel, "Incoming-Packet-Loss", 0.0F  
	      		)
	  			);
	  	  incomingPacketLossAverager= new LossAverager(
	  	  				oldIncomingPacketsSentNamedInteger,
	  	  				oldIncomingPacketsReceivedDefaultLongLike,
	  	  				incomingPacketLossNamedFloat
	  	  				);

	  	  // Adding outgoing packet statistics children and related trackers.
    		newOutgoingPacketsSentNamedInteger=
    				theEpiOutputStreamO.getCounterNamedInteger(); 
		    addB( newOutgoingPacketsSentNamedInteger );
		    addB( newOutgoingPacketsSentEchoedNamedInteger= new NamedInteger(
		    		theDataTreeModel, "Outgoing-Packets-Sent-Echoed", 0 
		    		)
		    	); 
		    oldOutgoingPacketsSentDefaultLongLike= new DefaultLongLike(0);
		    addB( newOutgoingPacketsReceivedNamedInteger= new NamedInteger( 
			      theDataTreeModel, "Outgoing-Packets-Received", 0 
			      )
		    	);
	  	  addB( outgoingPacketLossNamedFloat= new NamedFloat( 
	      		theDataTreeModel, "Outgoing-Packet-Loss", 0.0f 
	      		)
	  			);
	  	  oldOutgoingPacketsReceivedDefaultLongLike= new DefaultLongLike(0);
	  	  outgoingPacketLossLossAverager= new LossAverager(
	  	  		oldOutgoingPacketsSentDefaultLongLike,
	  	  		oldOutgoingPacketsReceivedDefaultLongLike,
	  	  		outgoingPacketLossNamedFloat
	  	  		);

	  	  addB( theSubcasterManager );
	    	}

    public void runWithSubcastersV() throws IOException
      /* This method does, or will do itself, or will delegate to Subcasters, 
        all protocols of a Unicaster.  This might include:
        * Doing full PING-REPLY protocol by letting a Subcaster do it, 
        	and forwarding its packets in both directions.
        * Passing packets from Subcasters to the network.
        * Passing Subcaster packets from the network to Subcasters.
        * Doing simple received message decoding.
        * Connection/Hello handshake state machine cycling.
        */
	    {
    	  processing: {
	    		if (!processingHellosB()) break processing;
		      theSubcasterManager.getOrBuildAddAndStartSubcaster(
					  "PING-REPLY" //// Hard wired creation at first.  Fix later.
					  ); // Adding Subcaster.
				  while (true) { // Repeating until termination is requested.
				  	LockAndSignal.Input theInput= 
			  				theLockAndSignal.testingForInterruptE();
		      	if ( theInput != Input.NONE ) break;
		      	if (processingMessagesFromRemotePeerB()) continue;
		    		if (multiplexingPacketsFromSubcastersB()) continue;
		    		if (theRTTMeasurer.cycleMachineB()) continue;
			  	  theInput= // Waiting for at least one new input.
		    			  theLockAndSignal.waitingForInterruptOrNotificationE();
			  	  }
	    		if  // Informing remote end whether app is doing a Shutdown.
	    			( theShutdowner.isShuttingDownB() ) 
		    		{ writingAndSendingV("SHUTTING-DOWN"); // Informing peer.
		          appLogger.info( "SHUTTING-DOWN message sent.");
		    			}
    			} // processing:
	    	}
	  
    /*////
		private void sendTestPacketV() throws IOException
			{
				//appLogger.debug( "Queuing packet with TEST for sending.");
				writingTerminatedStringV( "TEST" );
				endingPacketV(); // Forcing send.
				//%Thread.yield(); // Letting other threads run.
				EpiThread.interruptableSleepB(100); // Letting other threads run,
				  // also limiting bandwidth.
				}
    */ ////
	  
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
			  //// Improve efficiency by passing a buffer window instead of 
			    doing write(..)?
		    */
			{
	      DatagramPacket theDatagramPacket= // Extracting DatagramPacket.
						theSubcasterPacket.getDatagramPacket();
	      //theEpiOutputStreamO.flush(); // Flushing to prepare new stream buffer.
	      ///writingSequenceNumberV();
	      writingTerminatedStringV( // Writing key as de-multiplex header. 
	      	theSubcasterPacket.getKeyK() 
	      	);
	  		writingTerminatedLongV( 
				  theDatagramPacket.getLength() 
				  );
				theEpiOutputStreamO.write( // Writing nested Subcaster packet.
						theDatagramPacket.getData(),
						theDatagramPacket.getOffset(),
						theDatagramPacket.getLength()
						);
			  ///theEpiOutputStreamO.flush(); // Flushing to send it as a packet.
				endingPacketV(); //// Could this be delayed?
				}

		private boolean processingMessagesFromRemotePeerB() throws IOException
		  /* This method tries to input and processes 
		    one message from the remote peer.
		    A message might be in a single packet or several packets.
		    If a message begins with a Subcaster key then 
		    the body of the message is forwarded to 
		    the associated Subcaster as a new nested packet. 
		    Otherwise the message is decoded locally.
		    This method returns true if success, false otherwise.
		    */
		  {
			  boolean gotInputB= (theEpiInputStreamI.available() > 0);
			  if ( gotInputB ) { // Processing some input.
		  	  try 
			  	  { processingRemoteMessageV(); } // Doing only one.
			    	catch (IllegalArgumentException anIllegalArgumentException)
				    { // Handling any parse errors.
		        	appLogger.warning(
		        			"Packet parsing error: " + anIllegalArgumentException
		        			);
			    		theEpiInputStreamI.emptyingBufferV(); // Consuming remaining 
			    		  // bytes in buffer because interpretation is impossible.
					    }
			  	} // Processing some input
			  return gotInputB;
				}

		private void processingRemoteMessageV() throws IOException
		  /* This method processes the next available message.
		    Most will be demultiplexed and passed to Subcasters.
		   */
			{
				String keyString= readAString(); // Reading message key string

				process: {
		      Subcaster theSubcaster= // Getting associated Subcaster, if any.
		        theSubcasterManager.tryingToGetDataNodeWithKeyD( keyString );
		      if // Passing remainder of message to associated Subcaster.
		        ( theSubcaster != null )
		        { processMessageToSubcasterV( theSubcaster ); break process; }

		      // Process non-Subcaster message.
		      //if ( processPacketSequenceNumberB(keyString) ) // "PS"
   			  //	break process;
		      //if ( processPacketAcknowledgementB(keyString) ) // "PA"
		      if ( theRTTMeasurer.processMeasurementMessageB(keyString) ) ////
		      	break process;
  			  if ( processHelloB( keyString ) ) // "HELLO"
   			  	break process;
   			  if ( processShuttingDownB(keyString) ) // "SHUTTING-DOWN"
   			  	break process;
    		  if ( keyString.equals( "TEST" ) ) // "TEST" packet.
   			  	break process; // Accepting by breaking.

          appLogger.warning( "Ignoring remote message: " + keyString );
					} // process:
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
		      This determination is based on the peer's IP addresses.
		    * Ignoring messages other than HELLO.
		    This method should execute in both peers at approximately
		    the same time and is called when the local and remote peers
		    recognize each other's existence.
		    Returns true if HELLO messages were exchanged, meaning
		    the connection may be considered established.
		    Returns false otherwise.
		    */
			{
				int triesRemainingI= 3; // 3 tries then we give up.
			  boolean successB= false;
			  long helloIimeLimitMsL= roundTripTimeNamedInteger.getValueL();
			  tryingToConnectByExchangingHellos: while (true) {
          if ( triesRemainingI-- <= 0 ) // Exiting if try limit exceeded. 
          	break tryingToConnectByExchangingHellos;
	        appLogger.info( "Sending HELLO." );
		  		writingTerminatedStringV( "HELLO" );
		  		writingTerminatedStringV(  // Writing other peer's IP address. 
		  				getKeyK().getInetAddress().getHostAddress() 
		  				);
		  		endingPacketV(); // Forcing send.
          long helloSentMsL= System.currentTimeMillis();
          processingPossibleHelloResponse: while (true) {
            Input theInput=  // Awaiting next input within time interval.
            		waitingForSubnotificationOrIntervalOrInterruptE( 
            				//%helloSentMsL, 5000
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
	    				readAString(); 
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
  	    the value for leadingB.
  	    It does not send a reply "HELLO".  
  	    This is assumed to have been done simultaneously elsewhere.
  	    It returns true if HELLO was processed, false otherwise.
  	    The first time this method is called is the only one that counts.
  	    Later calls might redundant retransmissions, 
  	    but should have no other effect.
  	   */
	  	{
  		  boolean isKeyB= keyString.equals( "HELLO" ); // Testing key.
  		  if (isKeyB) { // Decoding argument if key is "HELLO".
					String localIpString= readAString();
					String remoteIpString= getKeyK().getInetAddress().getHostAddress();
					leadingB= localIpString.compareTo(remoteIpString) > 0;
					//leadingB= !leadingB; // Reverse roles for debug test. 
					// Note, the particular ordering of IP address Strings
					// doesn't matter.  What matters is that the ordering is consistent.
					theSubcasterManager.setLeadingV( leadingB );
	        appLogger.info( 
	        		"HELLO received.  Overriding role to be: "
	        		+ (leadingB ? "LEADER" : "FOLLOWER")
	        		);
				  }
  		  return isKeyB;
	  		}

  	/*////
    protected void endingPacketV() throws IOException
      /* This method writes a packet sequence number 
        into the stream if it is time for it.
        Next it forces what has been written to the stream, 
        if anything, to be sent.
        This method overrides the Streamcaster version which only flushes.
        
        Because this method is called after other messages are written,
        sequence numbers are always piggy-backed on those messages.
        Sequence numbers are not sent in packet by themselves. 
    		*/
  	/*////
      {
    	  if // Write next packet sequence number if it's time for it. 
    	    ( System.currentTimeMillis() - timeToSendNextSequenceNumberMsL  >= 0 )
	    	  {
	    	  	////sendingSequenceNumberV();
    	  		////lastPacketSequenceSentMsL= // Saving when sequence number sent. 
    	    	////		System.currentTimeMillis(); 
    	  		appLogger.debug( "endingPacketV() PS written, RTT start.");
	    	    do // Increment next time until it's in the future.
		    	  	timeToSendNextSequenceNumberMsL+= 1000; // Add 1 second to next time.
	    	    	while 
	    	    		( System.currentTimeMillis() - timeToSendNextSequenceNumberMsL  >= 0 );
	    	    }
	    	  else
		  		super.endingPacketV();
	  		}
  	*/ ////

    
    protected void sendingSequenceNumberV() throws IOException
      /* This method increments and writes the packet ID (sequence) number
        to the EpiOutputStream.
        It doesn't flush().
        */
      {
    	  long sequenceNumberL= 
    	  		(theEpiOutputStreamO.getCounterNamedInteger().getValueL()); 
        appLogger.debug( "sendingSequenceNumberV() " + sequenceNumberL);
	    	writingTerminatedStringV( "PS" );
	  		writingTerminatedLongV( sequenceNumberL );
	  		super.endingPacketV();
	  		theRTTMeasurer.signalSequenceNumberSentV();
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
					}
				return isKeyB;
        }

		private void processMessageToSubcasterV( Subcaster theSubcaster )
      throws IOException 
      /* This method sends the remainder of the present message buffer
        to theSubcaster for processing.
			  The message key string is assumed to have already been read
			  and decoded to mean theSubcaster.

			  //// Improve efficiency by passing a buffer window instead of 
			    doing a read(..) into a new buffer.
				*/
      {
			  processing: {
					String lengthString= // Reading Subcaster message length String. 
							readAString();
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

		public void runWithoutSubcastersV()  //// Needn't be public. 
			throws IOException
      // Does PING-REPLY protocol in this thread, not in a Subcaster.
	    {
				pingReplyProtocolV();
		    }
		
		/*/////
		private boolean rttStateMachineB() ////
			{
			  boolean inputArrivedB= false;
				if (theTimerInput.getInputArrivedB())
					{
						inputArrivedB= true;
						}
					else if (! theTimerInput.getInputScheduledB())
						theTimerInput.scheduleV(1000);
				return inputArrivedB;
				}
		*//////
		
		////static // This class must be static because it contains a static enum.
		static class RTTMeasurer // Being developed for PS-PS RTT timing.
		  /* This class is a state machine that uses PS and PA to 
		    measure Round-Trip-Time.  
		    It is not thread-safe and must be called only from 
		    within the Unicaster thread.
		   */
			{	
			  
			  // Injected dependencies.
				private Unicaster theUnicaster; //// temporary cyclic dependency.

				// Other variables.
				long timeOutMsL;
			
				RTTMeasurer( Unicaster theUnicaster ) // Constructor.
			  	{
					  this.theUnicaster= theUnicaster; //// temporary cyclic dependency.
					  
					  statisticsTimerInput= 
					  		new TimerInput(theUnicaster.theLockAndSignal,theUnicaster.theTimer);
					  }
		
				// This is for theStateI. 
				// This can't be a static enum because we are in a non-static class.
					private static final int PAUSING= 1;
					private static final int SENDING_AND_WAITING=2; 
			  
					private int theStateI= PAUSING; // Initial state of the machine.
		
     	  private volatile boolean machineCyclingB= false;
			  private TimerInput statisticsTimerInput; 
			  private long sendSequenceNumberTimeMsL;
	      ////private long lastPacketSequenceSentMsL;
			  private boolean sequenceNumberSentB= false;
				private boolean acknowledgementReceivedB= false;

        public boolean processMeasurementMessageB(String keyString) 
      		throws IOException
	        {
		        boolean successB= true;
		    	  beforeExit: {
			        if ( processPacketSequenceNumberB(keyString) ) // "PS"
			        	break beforeExit;
			        if ( processPacketAcknowledgementB(keyString) ) // "PA"
			        	break beforeExit;
			        successB= false;
		        	} // beforeExit:
		        return successB;
		        }

      	private boolean processPacketSequenceNumberB(String keyString) 
      			throws IOException
      	  /* This method processes the packet sequence number that follows "PS",
      	    which comes from the remote peer EpiOutputStreamO packet count.
      	    From this and the local EPIInputStreamI packet count it calculates 
      	    a new value of the local peer's incoming packet loss ratio.
      	    This ratio is accurate when calculated because
      	    the values it uses in the calculation are synchronized.

      	    It also sends an "PA" message back to the remote peer with same numbers
      	    so the remote peer can calculate the same ratio, 
      	    which for the remote peer is called the outgoing packet loss ratio.
      	    By sending and using both numbers the remote peer's calculation is
      	    not affected by variations in Round-Trip-Time (RTT).

      	    Every packet has a sequence number, but 
      	    not every packet needs to contain its sequence number.
      	    The reception of an "PS" message with its sequence number means that
      	    the remote has sent at least that number of packets.
      	    The difference of the highest sequence number received and
      	    the number of packets received is the number of packets lost.
      	    A new difference and a loss ratio average can be calculated
      	    each time a new sequence number is received.
      	    In fact that is how reception of a sequence number can be interpreted.

    	  		//// Sequence numbers and other numbers eventually need to be converted 
    	  		  to use modulo (wrap-around) arithmetic.
    	  	  */
    	  	{
      		  boolean isKeyB= keyString.equals( "PS" ); 
      		  if (isKeyB) {
    	  		  int sequenceNumberI= theUnicaster.readANumberI(); // Reading # from packet.
    				  theUnicaster.newIncomingPacketsSentDefaultLongLike.setValueL( // Recording.
    							sequenceNumberI + 1
    							); // Adding 1 to convert sequence # to remote sent packet count.
    	  			theUnicaster.incomingPacketLossAverager.recordPacketsReceivedOrLostV(
    	  					theUnicaster.newIncomingPacketsSentDefaultLongLike,
    	  					theUnicaster.newIncomingPacketsReceivedNamedInteger
    					  );
    	  			theUnicaster.writingTerminatedStringV( "PA" );
    	  			theUnicaster.writingTerminatedLongV( // The remote sequence number.
    		  				//newIncomingPacketsSentDefaultLongLike.getValueL()
    		  				sequenceNumberI
    		  				);
    	  			long receivedPacketCountL= 
    	  			  theUnicaster.newIncomingPacketsReceivedNamedInteger.getValueL(); 
    	  			theUnicaster.writingTerminatedLongV( // The local received packet count.
    	  					receivedPacketCountL 
    		  				);
    	  			theUnicaster.endingPacketV(); // Flushing now for minimum RTT.
    	        appLogger.debug( "processPacketSequenceNumberB(..) PS:"
    		  		  +sequenceNumberI+","
    	        	+receivedPacketCountL
    		  		  );
      				}
      		  return isKeyB;
    	  		}

      	private boolean processPacketAcknowledgementB(String keyString) 
      			throws IOException
      	  /* This method processes the "PA" sequence number 
      	    feedback message, which the remote peer sends
      	    in response to receiving an "PS" sequence number message.
      	    The "PA" is followed by:
      	    * a copy of the sent packet sequence number received by the remote peer,
      	    * the remote peers received packet count.
      	    From these two values it calculates 
      	    the packet loss ratio in the remote peer receiver.
      	    By having "PA" include both values, its calculation is RTT-immune.

    				See processSequenceNumberB(..) about "PS" for more information.
    	  	  */
    	  	{
    	  	  boolean isKeyB= keyString.equals( "PA" ); 
    			  if (isKeyB) {
    			  	theUnicaster.roundTripTimeNamedInteger.setValueL(
              		(System.currentTimeMillis() - sendSequenceNumberTimeMsL)
              		);
              signalPacketAcknowledgementReceivedV();
    		  		int sequenceNumberI= theUnicaster.readANumberI(); // Reading echo of sequence #.
    		  		int packetsReceivedI= theUnicaster.readANumberI(); // Reading packets received.
    	        appLogger.debug( "processPacketAcknowledgementB() PA:"
    		  		  +sequenceNumberI+","
    	        	+packetsReceivedI+";TO="
    		  		  +theUnicaster.roundTripTimeNamedInteger.getValueL()
    		  		  );
    		  		theUnicaster.newOutgoingPacketsSentEchoedNamedInteger.setValueL(
    			    		sequenceNumberI + 1 // Convert sequence # to sent packet count.
    			    		);
    		  		theUnicaster.newOutgoingPacketsReceivedNamedInteger.setValueL(packetsReceivedI);
    		  		theUnicaster.outgoingPacketLossLossAverager.recordPacketsReceivedOrLostV(
    		  				theUnicaster.newOutgoingPacketsSentEchoedNamedInteger,
    		  				theUnicaster.newOutgoingPacketsReceivedNamedInteger
    						  );
    			  	}
    			  return isKeyB;
    	  		}

        public void signalSequenceNumberSentV() throws IOException
          { 
        		sendSequenceNumberTimeMsL= System.currentTimeMillis();
        		//appLogger.debug("signalSequenceNumberSentV().");
	        	sequenceNumberSentB= true;
	        	////runMachineB(); Recursive race condition?
	          }

        public void signalPacketAcknowledgementReceivedV()  throws IOException
        	{ 
        		//appLogger.debug("signalPacketAcknowledgementReceivedV()");
        		acknowledgementReceivedB= true; 
	        	//cycleMachineB(); ////////
	          }

		    public boolean cycleMachineB() throws IOException
		      /* Decodes the state by calling associated handler method once.
		        It true if the machine can run more cycles,
		        false if it is waiting for the next input.
		        */
		      { 
		    		boolean stillRunningB= true;
		    	  if (!machineCyclingB) { // Cycling only if not already doing so.
		    	  	machineCyclingB= true;
			    	  switch (theStateI) { // Repeatedly decoding state.
				    		case PAUSING: 
				    			stillRunningB= handlingPausingB(); break;
				    		case SENDING_AND_WAITING: 
				    			stillRunningB= handleSendingAndWaiting(); break;
			 					default: 
			 						stillRunningB= false; break;
			    			}
		    	  	machineCyclingB= false;
		    	  	}
		    	  return stillRunningB;
		    		}
		
		    // State handler methods.
		
		    private boolean handlingPausingB()
		      // This state method generates the pause between PS-PA handshakes.
		    	{ 
		    	  boolean runningB= true;
		    	  beforeExit: {
			    		if (! statisticsTimerInput.getInputScheduledB()) { 
		    			  statisticsTimerInput.scheduleV( Delay.handshakePause5000MsL );
		    			  //appLogger.debug("handlingPauseB() scheduling pause end input.");
				    		break beforeExit;
				    	  }
			    		if (! statisticsTimerInput.getInputArrivedB()) {
		    				runningB= false; // This means the machine is waiting.
		    				//appLogger.debug("handlingPauseB() pause end input scheduled.");
				    		break beforeExit;
				    	  }
			    		{ // Changing state at end of pause. 
		    				statisticsTimerInput.cancelingV();
		    			  timeOutMsL= theUnicaster.roundTripTimeNamedInteger.getValueL()*2;
		    			  theStateI= SENDING_AND_WAITING;
		    			  //appLogger.debug("handlingPauseB() pause end input arrived.");
		    			  }
		    	  } // beforeExit:
		    		  return runningB; 
		    		}

		    private boolean handleSendingAndWaiting() throws IOException
		    	// This state method handles the PS-PA handshakes, and retrying.
		    	{
		    	  	boolean runningB= true;
		    	  beforeExit: { 
		    	  beforeStartPausing: { 
		    	  beforeWaiting: { 
			    		if (! statisticsTimerInput.getInputScheduledB()) { 
			    			statisticsTimerInput.scheduleV(timeOutMsL);
			    			//appLogger.debug("handleSendingAndWaiting() scheduling "+timeOutMsL);
				    		theUnicaster.sendingSequenceNumberV();
				    		break beforeExit;
					    	}
		        	if (! sequenceNumberSentB) break beforeWaiting;
		        	if (acknowledgementReceivedB) { // Handling PA acknowledgement.
		        		//appLogger.debug("handleSendingAndWaiting() PA acknowledgement and resets.");
		        		acknowledgementReceivedB= false;  // Resetting signal variable,
		        		sequenceNumberSentB= false; // and the other one.
		    				statisticsTimerInput.cancelingV();
				    		break beforeStartPausing; // Going to Pausing state.
					    	}
		    	  } // beforeWaiting:
			    		if (! statisticsTimerInput.getInputArrivedB()) {
				    	  runningB= false; // This means the machine is waiting.
				    	  //appLogger.debug("handleSendingAndWaiting() time-out scheduled.");
				    		break beforeExit;
					    	}
		        	// Time-out.   
			    		//appLogger.debug("handleSendingAndWaiting() time-out occurred.");
	    				statisticsTimerInput.cancelingV();
	    			  if ( timeOutMsL <= Delay.maxTimeOut5000MsL ) { // Handling limit not reached.
	    				  timeOutMsL*=2;  // Doubling time limit for retrying.
	    					break beforeExit;
	  					  }
	    			  //appLogger.debug("handleSendingAndWaiting() last time-out.");
		    	  } // beforeStartPausing:
    			  	theStateI= PAUSING; // Doing state switch to pausing. 
    			  	break beforeExit; 
	    			} // beforeExit:
		    			return runningB;
		    		}
		
				} // RTTMeasurer

		static class TimerInput // Being developed for PS-PS RTT timing.
		  /* This class functions as an input to 
		    processes modeled as threads.
		    It uses the LockAndSignal.notifyingV() method,
		    so it is guaranteed to be quick.
		    The presence of an active input can be tested 
		    with the getInputArrivedB() method.
		    This class is not meant to be used with 
		    processes modeled as state-machines.
		    
		    //// This class's documentation needs better terminology.
		   */
			{	
			  // Injected dependencies.
				private LockAndSignal theLockAndSignal;
			  private Timer theTimer;
			  
				// Other variables.
				private TimerTask theTimerTask= null;
				private boolean inputArrivedB= false; 
		
		    TimerInput( // Constructor.
			  		LockAndSignal theLockAndSignal,
			  		Timer theTimer
			  		)
			  	{
				  	this.theLockAndSignal= theLockAndSignal;
				  	this.theTimer= theTimer;
				  	}
		
		    public boolean getInputArrivedB() 
		      // Returns whether or not this input has been activated.
		      { return inputArrivedB; }
		
		    public boolean getInputScheduledB() 
		      // Returns whether or not this input has been scheduled.
		      { return theTimerTask != null; }
		    
		    public synchronized void scheduleV( long delayMsL )
		      /* Schedules this timer for input activation after delayML milliseconds.
		        If this timer object is already scheduled or active 
		        then the old scheduled activation is cancelled first.
		       */
			    {
		    		cancelingV(); // Canceling any older input.
			    	theTimerTask= new TimerTask() {
			        public void run()
			          // Activates this as an input and notifies interested thread.
				        {
			        		inputArrivedB= true;
			        	  theLockAndSignal.notifyingV();
				          }
			    		};
			    	theTimer.schedule(theTimerTask, delayMsL);
			    	}
		
		    public synchronized void cancelingV()
		      /* Cancels future generation of a timer input.
		        This will cancel both inputs that have been scheduled
		        but not yet arrived, and inputs that have arrived.
		
		        An earlier version returned true if cancellation was successful, 
		        false otherwise, but it could not be trusted.
		        */
			    {
		    		if (theTimerTask != null) // Handling timer created.
			    		{
		    				inputArrivedB= false; // Erasing any arrived input.
			    			theTimerTask.cancel(); // Canceling timer.
					    	theTimerTask= null; // Recording no longer scheduled.
			    			}
			    	}	 
		
			}


	} // Unicaster.
