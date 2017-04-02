package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
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
  		private LinkMeasurementState theLinkMeasurementState;
  		private MultiMachineState theMultiMachineState;
  		
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
    		NamedLong retransmitDelayMsNamedLong
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
            retransmitDelayMsNamedLong 
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
	          initializeWithIOExceptionV();

	          { // Uncomment only one of the following method calls.
	          	runWithSubcastersV(); // Code which uses Subcasters.
	          	//runWithoutSubcastersV(); // Original code without Subcasters.
		          }
	          
	          finalizingV();
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

    protected void initializeWithIOExceptionV() throws IOException
	    {
    		super.initializeWithoutStreamsV(); // We do the stream counts below.

    		theLinkMeasurementState= new LinkMeasurementState( 
	  		    theDataTreeModel,
    				theTimer, 
    				theEpiInputStreamI,
    				theEpiOutputStreamO, 
    				retransmitDelayMsNamedLong 
	      		);
	  	  theLinkMeasurementState.initializeWithIOExceptionV();
	  	  addB( theLinkMeasurementState ); // This includes stream counts.

	  	  theMultiMachineState= new MultiMachineState();
	  	  theMultiMachineState.initializeWithIOExceptionV();
	  	  addB( theMultiMachineState );

	  	  addB( theSubcasterManager );
	    	}

    protected void finalizingV() throws IOException
      // This is the opposite of initilizingV().
	    {
	    	theLinkMeasurementState.finalizeV();
	    	theEpiOutputStreamO.close(); // Closing output stream.
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
		    		////if (theLinkMeasurementState.cycleMachineB()) continue;
			  	  theInput= // Waiting for at least one new input.
		    			  theLockAndSignal.waitingForInterruptOrNotificationE();
			  	  }
	    		if  // Informing remote end whether app is doing a Shutdown.
	    			( theShutdowner.isShuttingDownB() ) 
		    		{ theEpiOutputStreamO.writingAndSendingV("SHUTTING-DOWN"); // Informing peer.
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
			  ///theEpiOutputStreamO.flush(); // Flushing to send it as a packet.
				theEpiOutputStreamO.sendingPacketV(); //// Could this be delayed?
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
				String keyString=  // Reading message key string
						theEpiInputStreamI.readAString();

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
		      if ( theLinkMeasurementState.handleInputB(keyString) ) ////
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
		      This determination is based on the peers' IP addresses.
		    * Ignoring messages other than HELLO.
		    This method should execute in both peers at approximately
		    the same time and is called when the local and remote peers
		    recognize each other's existence.
		    Returns true if HELLO messages were exchanged, meaning
		    the connection may be considered established.
		    Returns false otherwise.
		    
		    //// This could be a state machine, but doesn't have to be,
		    because it happens only when a peer-to-peer connection is established.
		    
		    //// Replace retry count with a overall time limit?
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
					String localIpString= theEpiInputStreamI.readAString();
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

		public void runWithoutSubcastersV()  //// Needn't be public. 
			throws IOException
      // Does PING-REPLY protocol in this thread, not in a Subcaster.
	    {
				pingReplyProtocolV();
		    }



	} // Unicaster.
