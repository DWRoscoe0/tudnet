package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
////import java.net.InetAddress;
////import java.util.Random;


import allClasses.LockAndSignal.Input;

public class Unicaster

	extends Netcaster

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
    
      // Injected dependency instance variables.
      private final UnicasterManager theUnicasterManager;
      private final SubcasterManager theSubcasterManager;

      // Local child variables for containing and displaying stats.

			protected DefaultLongLike newIncomingPacketsSentDefaultLongLike;
	    	// This comes from the remote end via the "N" message.
			  // It should match the remote ends OutputStream packet counter
			  // and match the local count of received plus lost packets.
      protected NamedInteger oldIncomingPacketsSentNamedInteger;
      	// This eventually becomes the remote EpiInputStream packet count.
        // plus 1.  It comes via the "N" message.
        // It represents the latest count of received plus lost packets.
      protected NamedInteger newIncomingPacketsReceivedNamedInteger;
      	// This is the local EpiInputStream packet counter.
      	// It represents the latest count of received packets only.
  		private DefaultLongLike oldIncomingPacketsReceivedDefaultLongLike;
			  // This is used to detect gaps in incoming packets
	  		// by detecting changes in newIncomingPacketsReceivedNamedInteger.
  			// It represents the previous count of received packets only.
      protected NamedFloat incomingPacketLossNamedFloat;
        // String representation of incomingPacketLossFractionF.

      protected NamedInteger newOutgoingPacketsSentNamedInteger;
    	// This is the local EpiOutputStreamO packet counter.
      // It is the sum of the lost and received outgoing packets.
      protected DefaultLongLike oldOutgoingPacketsSentDefaultLongLike;
    	// This follows and is used to detect changes in
      // newOutgoingPacketsSentNamedInteger
      protected NamedInteger newOutgoingPacketsReceivedNamedInteger;
	    	// This comes from the remote EpiInputStream packet count
	      // in the "NFB" message.
  		protected DefaultLongLike oldOutgoingPacketsReceivedDefaultLongLike;
			  // This follows and is used to detect changes in
  			// newOutgoingPacketsReceivedNamedInteger.
      protected NamedFloat outgoingPacketLossNamedFloat;
        // This is calculated from outgoing packet differences. 

  		private final float smoothingFactorF= 0.1F; // 0.01; 

  		// Other instance variables.
      private final SubcasterQueue subcasterToUnicasterSubcasterQueue;

			public Unicaster(  // Constructor. 
			  UnicasterManager theUnicasterManager,
			  SubcasterManager theSubcasterManager,
	    	LockAndSignal threadLockAndSignal,
	      NetcasterInputStream theNetcasterInputStream,
	      NetcasterOutputStream theNetcasterOutputStream,
	      IPAndPort remoteIPAndPort,
        DataTreeModel theDataTreeModel,
        Shutdowner theShutdowner,
        SubcasterQueue subcasterToUnicasterSubcasterQueue
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
        		threadLockAndSignal,
	  	      theNetcasterInputStream,
	  	      theNetcasterOutputStream,
	          theShutdowner,
  	        theDataTreeModel,
  	        remoteIPAndPort,
        		"Unicaster" 
        		);

        // Storing injected dependency constructor arguments.
  			  this.theUnicasterManager= theUnicasterManager;
  			  this.theSubcasterManager= theSubcasterManager;
  			  this.subcasterToUnicasterSubcasterQueue= 
  			  		subcasterToUnicasterSubcasterQueue;
        }


    public void run()  // Main Unicaster thread.
      /* This method contains the main thread logic.
				////////
        
        */
      {
        try { // Operations that might produce an IOException.
	          initializingV();

	          { // Uncomment only one of the following method calls.
	          	//////runWithoutSubcastersV(); // Original code.
	          	runWithSubcastersV(); // Experimental code with Subcaster.
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

		    ////theRandom= new Random(0);  // Initialize fpr arbitratingYieldB().
	  	  ////leadingB= arbitratingYieldB( getKeyK().getPortI() );

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
	      		theDataTreeModel, "IncomingPacketLoss", 0.0F ////"%" 
	      		)
	  			);

	  	  // Adding outgoing packet statistics children and related trackers.
		    addB( newOutgoingPacketsSentNamedInteger=
		    		theEpiOutputStreamO.getCounterNamedInteger() 
		    		);
		    oldOutgoingPacketsSentDefaultLongLike= new DefaultLongLike(0);
		    addB( newOutgoingPacketsReceivedNamedInteger= new NamedInteger( 
			      theDataTreeModel, "OutgoingPacketsReceived", 0 
			      )
		    	);
	  	  addB( outgoingPacketLossNamedFloat= new NamedFloat( 
	      		theDataTreeModel, "OutgoingPacketLoss", 0.0f 
	      		)
	  			);
	  	  oldOutgoingPacketsReceivedDefaultLongLike= new DefaultLongLike(0);

	  	  addB( theSubcasterManager ); // Adding to our list.
	    	}

    public void runWithSubcastersV() throws IOException
      /* This method does, or will do, or will delegate, 
        all protocols of a Unicaster.  This might include:
        * Doing full PING-REPLY protocol by letting a Subcaster do it, 
        	and forwarding its packets in both directions.
        * Passing packets from Subcasters to the network.
        * Passing subcaster packets from the network to Subcasters.
        * Doing simple received message decoding.
        * Connection/Hello handshake state machine cycling.
        */
	    {
    	  processing: {
	    		if (!processingHellosB()) break processing;
		      theSubcasterManager.getOrBuildAddAndStartSubcaster(
		          "PING-REPLY" //// Hard wired creation at first.
		  	  		); // Adding Subcaster.
				  while (true) // Repeating until termination is requested.
					  {
		    			streamcasterLockAndSignal.doWaitE(); // Waiting for new input.
	        		if ( EpiThread.exitingB() ) break;
			    		processingRemoteMessagesV(); // Includes de-multiplexing.
			    		multiplexingPacketsFromSubcastersV();
			      	}
	    		if  // Informing remote end whether app is doing a Shutdown.
	    			( theShutdowner.isShuttingDownB() ) 
		    		{
		  				writingNumberedPacketV("SHUTTING-DOWN"); // Informing peer.
		          appLogger.info( "SHUTTING-DOWN message sent.");
		    			}
    			} // processing:
	    	}
	  
		private void multiplexingPacketsFromSubcastersV() throws IOException
		  /* This method forwards messages from Subcasters to remote peers.
		    It does this by nesting the Subcaster packet data in
		    a NetcasterPacket.
		    */
			{
				while (true) {  // Process all packets queued from Subcasters.
		      SubcasterPacket theSubcasterPacket= // Getting next SubcasterPacket 
	        		subcasterToUnicasterSubcasterQueue.poll(); // from queue.
	        if (theSubcasterPacket == null) break; // Exiting if queue empty.

	        nestedWritingWithMultiplexHeaderV(theSubcasterPacket);
	        }
			 	}

		private void nestedWritingWithMultiplexHeaderV(
				  SubcasterPacket theSubcasterPacket
				  ) 
				throws IOException
		  /* This method forwards theSubcasterPacket to the remote peer.
		    It prepends the Subcaster key as a multiplex/demultiplex header.
			  //// Improve efficiency by passing a buffer window instead of 
			    doing write(..).
		    */
			{
	      DatagramPacket theDatagramPacket= // Extracting DatagramPacket.
						theSubcasterPacket.getDatagramPacket();
	      //theEpiOutputStreamO.flush(); // Flushing to prepare new stream buffer.
	      writingSequenceNumberV();
	      writingTerminatedStringV( // Writing key as de-multiplex header. 
	      	theSubcasterPacket.getKeyK() 
	      	);
	  		writingTerminatedLongV( 
				////writingTerminatedStringV( // Writing length of Subcaster packet. 
				  theDatagramPacket.getLength() ////+ "" 
				  );
				theEpiOutputStreamO.write( // Writing nested Subcaster packet.
						theDatagramPacket.getData(),
						theDatagramPacket.getOffset(),
						theDatagramPacket.getLength()
						);
			  theEpiOutputStreamO.flush(); // Flushing to send it as a packet.
				}

		private void processingRemoteMessagesV() throws IOException
		  /* This method processes all available messages from the remote peer.
		    These messages might not be in a single packet.
		    If a message begins with a Subcaster key then 
		    the body of the message is forwarded to 
		    the associated Subcaster as a new packet. 
		    Otherwise the message is decoded locally.
		    */
		  {
			  while ( theEpiInputStreamI.available() > 0 ) {
		  	  try 
			  	  { processingRemoteMessageV(); }
			    	catch (IllegalArgumentException anIllegalArgumentException)
				    { // Handling any parse errors.
		        	appLogger.warning(
		        			"Packet parsing error: " + anIllegalArgumentException
		        			);
			    		theEpiInputStreamI.emptyingBufferV(); // Consuming remaining 
			    		  // bytes in buffer because interpretation is impossible.
					    }
			  	}
				}

		private void processingRemoteMessageV() throws IOException
		  // This method processes the next available message.
			{
				String keyString= readAString(); // Reading message key string

				process: {
		      Subcaster theSubcaster= // Getting associated Subcaster, if any.
		        theSubcasterManager.tryingToGetDataNodeWithKeyD( keyString );
		      if // Passing remainder of message to associated Subcaster if any.
		        ( theSubcaster != null )
		        { processMessageToSubcasterV( theSubcaster ); break process; }
		      if ( processSequenceNumberB(keyString) ) 
		      	break process;  // "N"
		      if ( processSequenceFeedbackB(keyString) ) 
		      	break process; // "NFB"
  			  if ( processHelloB( keyString ) ) 
  			   	break process; // "HELLO"
   			  if ( processShuttingDownB(keyString) ) 
		      	break process; // "SHUTTING-DOWN"
		      ; // Ignoring any other message, for now.  //////
          appLogger.warning( "Ignoring remote message: " + keyString );
					} // process:
				}
  	
		public boolean processingHellosB() //////////////// 
			throws IOException
		  /* This method establishes the connection 
		    by exchanging HELLO messages. 
		    This includes determining which peer should act as the leader
		    when needed.  This determination is based on their IP addresses.
		    */
			{
				int triesRemainingI= 3; // 3 tries then we give up.
			  boolean successB= false;
			  exchangingHellos: 
			  	while (true) // Repeating until connected or terminated.
				  {
	          if ( triesRemainingI-- <= 0 ) break exchangingHellos;
			  		writingTerminatedStringV( "HELLO" );
			  		writingTerminatedStringV( 
			  				getKeyK().getInetAddress().getHostAddress() 
			  				); // Writing what will be IP./////
			  		flush();
	          long helloSentMsL= System.currentTimeMillis();
	          awaitingResponseHello: while (true) {
	            Input theInput=  // Awaiting next input within reply interval.
	            		testWaitInIntervalE( helloSentMsL, 5000 );
	            if // Handling SHUTTING-DOWN packet or interrupt by exiting.
	    	    		( tryingToCaptureTriggeredExitB( ) )
	  	    			break exchangingHellos; // Exit everything.
	            if ( theInput == Input.TIME ) // Handling time-out.
		            { appLogger.info( "Time-out waiting for HELLO." );
		              break awaitingResponseHello;
		            	}
		    			String keyString= readAString(); // Reading message key string
		  			  if ( processHelloB( keyString ) ) 
		  			  	{ successB= true; break exchangingHellos; }
		  			  // Ignoring any other input.
	          	} // awaitingResponseHello:
	  			  } // exchangingHellos:
				return successB;
				}
  	
  	private boolean processSequenceNumberB(String keyString) 
  			throws IOException
  	  /* This method processes packet sequence numbers,
  	    which come from the remote EpiOutputStreamO packet count.
  	    This is mostly about calculate the incomingPacketLossFractionF
  	    using exponential smoothing.
  	    Each call to this method represents a received packet.
  	    Each gap in sequence numbers represents one or most lost packets.
	  		//// Sequence numbers eventually need to be converted 
	  		  to use modulo arithmetic.
	  	  */
	  	{
  		  boolean isKeyB= keyString.equals( "N" ); 
  		  if (isKeyB) {
	  		  int sequenceNumberI= readANumberI(); // Reading # from packet.
				  newIncomingPacketsSentDefaultLongLike.setValueL( // Recording.
							sequenceNumberI + 1 // Adding 1 to convert sequence # to count.
							);
		      processPacketLossesV( 
		      		newIncomingPacketsSentDefaultLongLike,
			  			oldIncomingPacketsSentNamedInteger,
			  			newIncomingPacketsReceivedNamedInteger,
			  			oldIncomingPacketsReceivedDefaultLongLike,
			  			incomingPacketLossNamedFloat
		      		);
		  		writingTerminatedStringV( "NFB" );
		  		writingTerminatedLongV( 
		  				newIncomingPacketsReceivedNamedInteger.getValueL() 
		  				);
  				}
  		  return isKeyB;
	  		}
  	
  	private boolean processHelloB(String keyString) 
  			throws IOException
  	  /* This method tries to process the Hello message and its arguments.
  	    If keyString is "HELLO" then it processes,
  	    which means parsing its IP address and determining
  	    which peer, the local or remote, will be leader,
  	    by comparing the IP addresses, and setting
  	    the value for leadingB.
  	    It returns true if HELLO is processed, false otherwise.
  	    The first time processing is the only one that counts.
  	    Later uses handle redundant retransmissions.
  	   */
	  	{
  		  boolean isKeyB= keyString.equals( "HELLO" ); // Testing key.
  		  if (isKeyB) { // Decode argument if key is "HELLO".
					String localIpString= 
							readAString();
					String remoteIpString= 
							getKeyK().getInetAddress().getHostAddress();
					leadingB= localIpString.compareTo(remoteIpString) > 0;
					leadingB= !leadingB; //// Reverse roles for debugging. 
					// Note, the particular ordering of IP address Strings
					// doesn't matter.  What matters is that there is an ordering.
					theSubcasterManager.setLeadingV( leadingB );
	        appLogger.info( 
	        		"This node has assumed the role of: "
	        		+ (leadingB ? "LEADER" : "FOLLOWER")
	        		);
				  }
  		  return isKeyB;
	  		}
  	
  	private boolean processSequenceFeedbackB(String keyString) 
  			throws IOException
  	  /* This method processes the "NFB" sequence number 
  	    feedback message.  This should reproduce [approximately]
  	    the incomingPacketLossFractionF in the receiver.
        The ratios might not be exactly the same, 
        if the processing orders of received vs. missed packets 
        are different, but the ratios should be close.
        
        Note, one is added to the number argument,
        not because a sequence number is being converted to
        a sequence number, it is already both a sequence and a count.
        It is added to prevent an initial lost packet.
        This is needed because NFBs are not self-flushing.  
        They are flushed later by other messages which do flush().
        This was done so there wouldn't be a bunch of
        empty packets containing nothing but N and NFB messages.
        So there are 2 NFBs in the same packet.
        So when the first of a pair is processed,
        it appears as 1 received packet and one lost packet.
        This is temporarily corrected by incrementing
        the NFB number argument by one.
        
        Eventually packet losses will be calculated with a moving average
        and this issue will disappear.
	  	  */
	  	{
	  	  boolean isKeyB= keyString.equals( "NFB" ); 
			  if (isKeyB) {
		  		int numberI= readANumberI(); // Reading # from packet.
			      newOutgoingPacketsReceivedNamedInteger.setValueL(
							numberI + 1 // Adding one prevent initial single packet loss.
							);
		      processPacketLossesV( 
			  			newOutgoingPacketsSentNamedInteger,
			  			oldOutgoingPacketsSentDefaultLongLike,
			  			newOutgoingPacketsReceivedNamedInteger,
			  			oldOutgoingPacketsReceivedDefaultLongLike,
			  			outgoingPacketLossNamedFloat
		      		);
			  	}
			  return isKeyB;
	  		}

  	private void processPacketLossesV( 
  			LongLike newPacketsSentLongLike,
  			LongLike oldPacketsSentLongLike,
  			LongLike newPacketsReceivedLongLike,
  			LongLike oldPacketsReceivedLongLike,
  			NamedFloat packetLossNamedFloat
  			)
  	  /* This method does some packet statistics calculations,
  	    mainly calculating packet losses.
  	    It operates on its argument variables as follows.
  	    * newPacketsSentLongLike: 
  	      This is the latest number of packets which 
  	      have actually been sent, which is also 
  	      the sum of packets received and packets lost.
  	      It is not changed by this method.
  	    * oldPacketsSentLongLike:
  	      This is the number of packets actually sent
  	      at the time of this method's previous call.
  	      It is incremented in parallel with oldPacketsReceivedLongLike 
  	      until it equals newPacketsSentIDefaultLongLike.
  	      See oldPacketsReceivedLongLike for more info.
  	    * newPacketsReceivedLongLike:
  	      This is number of packets which have actually been received.
  	      This excludes lost packets.
  	      It is not changed by this method.
  	    * oldPacketsReceivedLongLike: 
  	      This is the number of packets actually received 
  	      at the time of this method's previous call.
  	      It is incremented in parallel with oldPacketsSentLongLike 
  	      until it equals newPacketsReceivedLongLike.
  	      When oldPacketsReceivedLongLike and oldPacketsSentLongLike
  	      are incremented together it represents a received packet. 
  	      When oldPacketsSentLongLike is incremented alone
  	      it represents a lost packet.
  	    * packetLossNamedFloat:
  	      This is the fraction of sent packets that are lost.
  	      It is calculated by using a process called 
  	      "exponential smoothing" which merges in
  	      0.0 for each received packet and 1.0 for each lost packet.
  	      The numbers of lost and received packets are determined by
  	      the incrementing of oldPacketsReceivedLongLike and 
  	      oldPacketsSentLongLike described above.
  	   	*/
	  	{
		    while (true) {
		    	if // Exiting if all sent packet counts processed.
			    	( oldPacketsSentLongLike.getValueL() >= 
			    	  newPacketsSentLongLike.getValueL() )
		    		break;
					if // Processing whether or not packet received. 
					  ( oldPacketsReceivedLongLike.getValueL() < 
							newPacketsReceivedLongLike.getValueL() 
							)
						{ // Processing packet received.
						  smoothWithV( packetLossNamedFloat, 0 );
			      	oldPacketsReceivedLongLike.addDeltaL(1); // Incrementing.
			      	}
			      else 
			      { // Processing packet lost.
			      	smoothWithV( packetLossNamedFloat, 1 );
			      	}
					oldPacketsSentLongLike.addDeltaL(1); // Incrementing.
		      }
	  		}

  	private void smoothWithV( NamedFloat theNamedFloat, float valueF )
  	  /* This method combines incomingPacketLossNamedFloat
  	    with new values from valueF according to the smoothingFactorF. 
  	    */
	  	{
  			theNamedFloat.setValueF(
  				( smoothingFactorF * valueF ) + 
  				( ( 1 - smoothingFactorF ) * 
  						theNamedFloat.getValueF()
						)
	  			);
        //appLogger.debug( 
        //		"smoothWithV( "+theNamedFloat.getValueF()+", "+valueF+" )" 
        //		);
	  		}

  	private boolean processShuttingDownB(String keyString)
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
      /* Processes one message to theSubcaster.
			  The message key string is assumed to have already been read.
			  //// Improve efficiency by passing a buffer window instead of 
			    doing read(..).
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
	        appLogger.debug(
	        		"processMessageToSubcasterV(): " + 
	        		new String( bufferBytes, 0, lengthI )
	        		);
					SubcasterPacket theSubcasterPacket= // Repackaging buffer. 
						theSubcasterPacketManager.produceKeyedPacketE(
									bufferBytes, lengthI
									);
		      theSubcaster.puttingKeyedPacketV( // Passing to Subcaster. 
		      		theSubcasterPacket 
		      		);
					} //processing:
	      }

		public void runWithoutSubcastersV() throws IOException //// Needn't be public.
      // Does full PING-REPLY protocol without help of Subcaster.
	    {
				pingReplyProtocolV();
		    }

    } // Unicaster.

