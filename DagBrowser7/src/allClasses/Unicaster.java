package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Random;

///import allClasses.LockAndSignal.Input;

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
        a PING or an ECHO, to reduce or eliminate ping-ping conflicts.
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
        a sub-protocol value for this purpose.
        */
      {
        try { // Operations that might produce an IOException.
	          initializingV();
	          
	          theSubcasterManager.getOrBuildAddAndStartSubcaster(
	              "PING-REPLY" //// Hard wired creation at first.
	      	  		); // Adding Subcaster.

	          { //// Uncomment only one of the following method calls.
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

    protected void initializingV()
	    throws IOException
	    {
      	super.initializingV();
        theRandom= new Random(0);  // Initialize arbitratingYieldB().
	  	  arbitratedYieldingB= arbitratingYieldB( getKeyK().getPortI() );
	  	  
		    addB( theSubcasterManager ); // Adding to our list.
	    	}

    public void runWithSubcastersV() throws IOException ////
      /* Does (or will do) full PING-REPLY protocol 
        by letting a Subcaster do it, 
        and forwarding its packets in both directions.
        */
	    {
			  while (true) // Repeating until termination is requested.
				  {
	    			streamcasterLockAndSignal.doWaitE(); // Waiting for any new input.
	  			  //// This wait doesn't work at front of loop!  Check LockAndSignal.

        		///if // Exiting everything if exit has been triggered.
	      		///	( tryingToCaptureTriggeredExitB( ) )
	      		///	break;
        		if // Exiting if thread termination requested.
		          ( Thread.currentThread().isInterrupted() ) 
		          break;

		    		while (processingRemoteMessagesB() ) ;

		    		multiplexingPacketsFromSubcastersV();
		    		
		      	}
    		if  // Informing remote end whether we are doing Shutdown.
    			( theShutdowner.isShuttingDownB() ) 
	    		{
	  				writingNumberedPacketV("SHUTTING-DOWN"); // Informing peer of shutdown.
	          appLogger.info( "SHUTTING-DOWN message sent.");
	    			}
	    	}

		private void multiplexingPacketsFromSubcastersV() throws IOException
		  /* This method forwards messages from Subcasters to remote peers.
		    Presently it simply repackages the SubcasterPacket as 
		    a NetcasterPacket.
		    */ ////
			{
	      while (true) {  // Process all packets queued from Subcasters.
		      SubcasterPacket theSubcasterPacket= // Getting next SubcasterPacket 
	        		subcasterToUnicasterSubcasterQueue.poll(); // from queue.
	        if (theSubcasterPacket == null) break; // Exiting if queue empty.

	        queueWithMultiplexHeaderV(theSubcasterPacket);
	        }
			 	}

		private void queueWithMultiplexHeaderV(SubcasterPacket theSubcasterPacket) 
				throws IOException
		  /* This method forwards theSubcasterPacket to the remote peer.
		    It prepends a PING-REPLY multiplex header.
			  //// Improve efficiency by passing a buffer window instead of 
			    doing write(..).
		    */
			{
	      DatagramPacket theDatagramPacket= // Extracting DatagramPacket.
						theSubcasterPacket.getDatagramPacket();
	      theEpiOutputStreamO.flush(); // Flushing to prepare new stream buffer.
	      writingSequenceNumberV();
	      writingTerminatedStringV( // Writing key as de-multiplex header. 
	      	theSubcasterPacket.getKeyK() 
	      	);
				writingTerminatedStringV( // Writing length of Subcaster packet. 
				  theDatagramPacket.getLength() + "" 
				  );
				theEpiOutputStreamO.write( // Writing Subcaster packet to OutputStream.
						theDatagramPacket.getData(),
						theDatagramPacket.getOffset(),
						theDatagramPacket.getLength()
						);
			  theEpiOutputStreamO.flush(); // Flushing again to send it all.
				}

		private boolean processingRemoteMessagesB() throws IOException //////
		  /* This method processes one message from the remote peer,
		    if one is available.
		    If a messages is a Subcaster key then 
		    the remainder of the packet is forwarded to the associated Subcaster. 
		    ////Presently it simply repackages NetcasterPackets as SubcasterPackets
		    ////for the PING-REPLY Subcaster.
		    It returns true if a message was processed, false otherwise.
		    */
		  {
			  boolean messageToProcessB= ( theEpiInputStreamI.available() > 0 ); 
				if// Processing if there is anything to process. 
					( messageToProcessB )
					{ // Processing one message.
						String keyString= readAString(); // Reading message key string
            Subcaster theSubcaster= // Getting associated Subcaster, if any.
              theSubcasterManager.tryingToGetDataNodeWithKeyD( keyString );
              
            if // Passing remainder of message to associated Subcaster if any.
              ( theSubcaster != null )
              processMessageToSubcasterV( theSubcaster );
            else if // Trying to get and convert SHUTTING-DOWN packet to interrupt status. 
		          ( keyString.equals( "SHUTTING-DOWN" ) )
			        {
			          appLogger.info( "SHUTTING-DOWN message received.");
			          Thread.currentThread().interrupt(); // Interrupting this thread.
			          }
		        ; // Ignoring other messages, for now.  //////
						}
				return messageToProcessB;
			  }

		/*  ////
		private void OLDprocessMessageToSubcasterV( Subcaster theSubcaster )////
      throws IOException 
      /* Processes one message to theSubcaster.
			  The message key string is assumed to have already been read.
				*/
		/*  ////
      {
  			NetcasterPacket theNetcasterPacket= // Getting Unicaster packet. 
						///theEpiInputStreamI.getKeyedPacketE();
						theEpiInputStreamI.readKeyedPacketE();
				DatagramPacket theDatagramPacket= // Extracting DatagramPacket.
					theNetcasterPacket.getDatagramPacket();
        SubcasterPacketManager theSubcasterPacketManager= 
        		theSubcaster.getPacketManagerM();
				SubcasterPacket theSubcasterPacket= // Repackaging packet. 
						theSubcasterPacketManager.produceKeyedPacketE( 
								theDatagramPacket 
								);
	      theSubcaster.puttingKeyedPacketV( // Passing to Subcaster. 
	      		theSubcasterPacket 
	      		);
	      }
		*/  ////

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
					int lengthI;
			    try { // Converting length string to number, or exiting.
				      lengthI= Integer.parseInt( lengthString );
				    	}
				    catch (NumberFormatException aNumberFormatException)
					    { // Handling parse error.
			        	appLogger.warning(
			        			"Subcaster message length: " + aNumberFormatException
			        			);
				    		theEpiInputStreamI.emptyingBufferV(); // Consuming remaining 
				    		  // bytes in buffer because interpretation is impossible.
				    		break processing; // Exiting.
						    }
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

