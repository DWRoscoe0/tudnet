package allClasses;

import java.awt.Color;
import java.io.IOException;
import java.util.Timer;

import static allClasses.AppLog.theAppLog;


public class Unicaster

	extends Netcaster

  implements Runnable 

  /* Each instance of this class manages a a single Datagram connection with
    one of the peer nodes of which the ConnectionManager is aware.
    It uses unicast packets, not multicast packets.
    
    Each Unicaster is associated with a remote IPAndPort
    which can be retrieved with the superclass method getKeyK().
    The UnicasterManager maintains a map from IPAndPort to Unicaster.
    So the reference to IPAndPort is duplicated.
    
    This class is not a Thread, but is a Runnable on which to base a Thread.
    The Runnable contains a loop which:
    * Receives packets from the remote peer.
    * Send packets to the remote peer.
    * Implements several protocols for various purposes, such as:
      * Establishing and shutting down a connection.
	    * Exchanging sequence numbers to measure packet loss and round-trip-time. 
	    * Multiplexing packets from and de-multiplexing packets to other threads
	      which implement their own protocols.  This is not presently used.

    
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
    
    ///tmp This class contains a lot of disabled code,
      versions of which are being reimplemented, 
      starting with LinkedMachineState.
      Eventually this disabled code must be all be reimplemented or deleted.
    */

  { // Unicaster.

    // Fields (constants and variables).
    
      // Injected dependency instance variables
      private final UnicasterManager theUnicasterManager;
      private final SubcasterManager theSubcasterManager;
      private final TCPCopier theTCPCopier;
      private final Timer theTimer;
      private final Persistent thePersistent;
      private PeersCursor thePeersCursor;
      private NotifyingQueue<String> unicasterNotifyingQueueOfStrings;
      private NotifyingQueue<MapEpiNode> toUnicasterNotifyingQueueOfMapEpiNodes;
      private NotifyingQueue<MapEpiNode> toConnectionManagerNotifyingQueueOfMapEpiNodes;
      
  		// Other instance variables.
      private EpiThread theEpiThread;
  		private LinkedMachineState theLinkedMachineState;

  	  Color getBackgroundColor( Color defaultBackgroundColor )
  	    /* This method is a kludge to return the background color from 
  	      the theLinkedMachineState without needing to add code to 
  	      AndState or AndOrState.
  	     */
  	    {
  	      return theLinkedMachineState.getBackgroundColor( defaultBackgroundColor );
  	      }
  		
  	public Unicaster(  // Constructor. 
			  UnicasterManager theUnicasterManager,
			  SubcasterManager theSubcasterManager,
	    	LockAndSignal theLockAndSignal,
	      NetcasterInputStream theNetcasterInputStream,
	      NetcasterOutputStream theNetcasterOutputStream,
	      IPAndPort remoteIPAndPort,
	      TCPCopier theTCPCopier,
	      Shutdowner theShutdowner,
	      SubcasterQueue subcasterToUnicasterSubcasterQueue, ///opt Subcasters?
	  		Timer theTimer,
	  		Persistent thePersistent,
	  		PeersCursor thePeersCursor,
	  		NamedLong initialRetryTimeOutMsNamedLong,
	  		DefaultBooleanLike leadingDefaultBooleanLike,
	  		NotifyingQueue<String> unicasterNotifyingQueueOfStrings,
        NotifyingQueue<MapEpiNode> toConnectionManagerNotifyingQueueOfMapEpiNodes
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
	          initialRetryTimeOutMsNamedLong, 
	          leadingDefaultBooleanLike
	          );
	
	      // Storing injected dependency arguments not stored in superclass.
				  this.theUnicasterManager= theUnicasterManager;
				  this.theSubcasterManager= theSubcasterManager;
				  this.theTCPCopier= theTCPCopier;
		  		this.theTimer= theTimer;
		  		this.thePersistent= thePersistent;
		      this.thePeersCursor= thePeersCursor;
	        this.unicasterNotifyingQueueOfStrings= unicasterNotifyingQueueOfStrings;
          this.toConnectionManagerNotifyingQueueOfMapEpiNodes=
              toConnectionManagerNotifyingQueueOfMapEpiNodes;
	      }

    protected void initializeWithIOExceptionV( 
        EpiThread theEpiThread
        ) 
      throws IOException
	    {
    		super.initializeWithoutStreamsV(); // Stream counts are added below in
    		  // one of the sub-state machines.

    		this.theEpiThread= theEpiThread;

        this.toUnicasterNotifyingQueueOfMapEpiNodes= // Make empty EmpNode queue. 
            new NotifyingQueue<MapEpiNode>(theLockAndSignal, 5);

	  		// Create and start the sub-state machines.

    		{ // Create and add actual sub-states.
    			
    			LinkMeasurementState theLinkMeasurementState= 
    				 new LinkMeasurementState( 
  		    				theTimer, 
  		    				theEpiInputStreamI,
  			  				theEpiOutputStreamO,
  			  				initialRetryTimeOutMsNamedLong,
  			  				this
  			      		);
    			theLinkMeasurementState.initializeWithIOExceptionStateList();

  				theLinkedMachineState= new LinkedMachineState();
  				theLinkedMachineState.initializeWithIOExceptionLinkedMachineState(
		  				theTimer, 
		  			  theEpiInputStreamI,
		  				theEpiOutputStreamO,
		  				initialRetryTimeOutMsNamedLong,
		  				theTCPCopier,
		  				this,
		  				thePersistent,
		          thePeersCursor,
		  				theLinkMeasurementState,
		  	      toConnectionManagerNotifyingQueueOfMapEpiNodes
		  	  		);
  				addStateListV( theLinkedMachineState );

  				} // Create and add actual sub-states.

	  	  addAtEndB( theSubcasterManager );
	  	  
	  	  // propagateIntoSubtreeB( LogLevel.TRACE ); ///dbg /// tmp
	  	  
	  	  }

    public EpiThread getEpiThread()
      {
        return theEpiThread;
        }

    protected void finalizingV() throws IOException
	    // This is the opposite of initilizingV().
	    {
	    	theEpiOutputStreamO.close(); // Closing output stream.
	    	super.finalizeV();
	    	}

    public void run()  // Main Unicaster thread.
      /* This method contains the main thread logic.
        It contains initialization, finalization, with
        a mesaage processing loop in between.
        It also contains an IOException handler.
        */
      {
        if (theAppLog.testAndLogDisabledB( Config.unicasterThreadsDisableB, 
            "run() unicasters") 
            )
          return;
        /// appLogger.info("run() begins.");
        try { // Operations that might produce an IOException.
            /// appLogger.info("run() activating root state machine.");
	          doOnEntryV(); // Recursively activate all states that should be. 
	          /// appLogger.info("run() machine activated, doing first display.");
        		theDataTreeModel.displayTreeModelChangesV(); // Display tree after arrival.

	      	  runLoop(); // Do actual input processing in a loop.

	          processInputB( "Shutdown" ); // Make state machine process shutdown message.
	      	  finalizingV();
	  	    	theUnicasterManager.removingV( this ); // Removing self from tree.
	  	    	  // This isn't really needed, but is a good test of display logic.
	  	    	/// appLogger.info("run() after remove and before final display.");
	      		Nulls.fastFailNullCheckT(theDataTreeModel);
	      		theDataTreeModel.displayTreeModelChangesV(); // Display tree after removal.
          	}
          catch( IOException theIOException) {
          	Misc.logAndRethrowAsRuntimeExceptionV( 
          			"run() IOException", theIOException 
          			);
            }
        /// appLogger.info("run() ends.");
        }

	  private void runLoop() throws IOException
	    /* This method contains the Unicaster input message processing loop.
		    It reads event messages from various sources and processes them.
		    It does this until it receives a signal to exit.
		    
		    Input message sources now include:
		    * The EpiInputStream.  These are passed to the superclass state machine. 
		      /// This is being changed so any leftover is sent as a MapEpiNode
		      to the ConnectionManager.
		    * The String queue.  These are passed to the superclass state machine.
		    * the EpiNode queue.  
		      These come from the ConnectionManager.
		      They are serialized to a packet and sent to the remote peer.

        ///fix  Legitimate input is sometimes not consumed!
		    */
			{
	  		theAppLog.info("runLoop() begins.");
        doOnInputsB(); // This first call guarantees that state machine timers start.
	      processingLoop: while (true) {
	        if (EpiThread.testInterruptB()) break processingLoop; // Exit if requested.
          // theAppLog.info("runLoop() before processPacketStreamInputV().");
          processPacketStreamInputV();
          // theAppLog.info("runLoop() before processNotificationStringInputV().");
          processNotificationStringInputV();
          processNotificationMapEpiNodeInputV();
          // theAppLog.info("runLoop() before waitingForInterruptOrNotificationE().");
          theLockAndSignal.waitingForInterruptOrNotificationE();
	      	} // processingLoop:
  			theAppLog.info("runLoop() loop interrupted, stopping state machine.");
  			// ? theTimer.cancel(); // Cancel all Timer events for debug tracing, ///dbg
        while (doOnInputsB()) ; // Cycle state machine until nothing remains to be done.
				}

    private void processNotificationMapEpiNodeInputV() throws IOException
      /* This method gets all MapEpiNodes, if any,
        from the toUnicasterNotifyingQueueOfMapEpiNodes,
        and sends them, 1 packet each, to the remote peer.
        */
      {
        while (true) {
          MapEpiNode theMapEpiNode= toUnicasterNotifyingQueueOfMapEpiNodes.poll();
          if (theMapEpiNode == null) break; // Exit if queue empty.

          // Convert queue element to a packet and send.
          theMapEpiNode.writeV(theEpiOutputStreamO);
          theEpiOutputStreamO.endBlockAndSendPacketV(); // Forcing send.
          }
        }

    private void processNotificationStringInputV() throws IOException
      /* This method gets all Strings, if any,
        from the unicasterNotifyingQueueOfStrings,
        and passes them to the Unicaster state machine,
        and cycles the machine until each String is fully processed.
        */
      {
        while (true) {
          String notificationString= unicasterNotifyingQueueOfStrings.poll();
          if (notificationString == null) break; // Exit if no string.
          setOfferedInputV(notificationString); // Offer String to state machine.
          while (doOnInputsB()) ; // Cycle state machine until nothing remains to be done.
          }
        }

    private void processPacketStreamInputV() throws IOException
      /* This method gets all available Strings, if any,
        from the theEpiInputStreamI,
        and passes them to the Unicaster state machine,
        and cycles the machine until each String is fully processed.
        Any input that remains, if any, if processed by processUnprocessedInputV().
        */
      {
        while (theEpiInputStreamI.available() > 0) { // Try parsing more of packet stream.
          String inString= theEpiInputStreamI.readAString(); // Get next token.
          setOfferedInputV( inString ); // Offer it to state machine.
          while (doOnInputsB()) ; // Cycle state machine until processing stops.
          processUnprocessedInputV(); // Handle any left-over input.
          }
        }

    public void connectToPeerV()
      // This method tells the state-machine to connect.
      {
        theAppLog.info("Unicaster.connectToPeerV() executing, queuing 'Connect'.");
        unicasterNotifyingQueueOfStrings.put("Connect");
        }
    
    private void processUnprocessedInputV() throws IOException
      /* This method is called to process any offered input that
        the Unicaster state machine was unable to process.
        Unprocessed Debug messages are silently ignored.
        Other messages are logged, converted to MapEpiNodes, 
        and added to the toConnectionManagerNotifyingQueueOfMapEpiNodes
        for processing by the ConnectionManager.
        The ConnectionManager is the processor of last resort.
        */
      {
          String offeredString= getOfferedInputString();
        toReturn: { 
        toConsumeInput: { 
          if ( offeredString == null ) break toReturn; // Input string consumed, exit.
          if (offeredString.equals("DEBUG")) { // Ignore DEBUG messages
            theEpiInputStreamI.readAString(); // and their message count arguments.
            break toConsumeInput; // Finished up.
            }
          { // Log any other input and log OrState states. 
            theAppLog.info("processUnprocessedInputV() input= "+offeredString);
            /// logOrSubstatesB("processUnprocessedInputV()"); // Log active sub-states.
            }
          MapEpiNode theMapEpiNode= // Get any left-over input as a MapEpiNode.
              theEpiInputStreamI.tryMapEpiNode();
          if (theMapEpiNode == null) break toConsumeInput; // Clean up if no MapEpiNode.
          theAppLog.info( // Log the EpiNode.
              "processUnprocessedInputV() EpiNode= " + theMapEpiNode.toString());
          if (isAboutThisUnicasterB(theMapEpiNode)) { // Ignore if about this Unicaster.
            theAppLog.info(
                "processUnprocessedInputV() ignoring above data about this Unicaster.");
            break toConsumeInput; // Ignoring to prevent self-reference message storm.
            }
          toConnectionManagerNotifyingQueueOfMapEpiNodes.put(theMapEpiNode); // Send...
        } // toConsumeInput: 
          resetOfferedInputV();  // consume unprocessed state machine String input.
        } // toReturn:
          return;
        }

    private boolean isAboutThisUnicasterB(MapEpiNode otherMapEpiNode)
      /* This method returns true if theMapEpiNode contains information about
        this Unicaster, based on IP, Port, and PeerIdentity.
        It returns false otherwise.
        */
      {
          boolean resultB= false; // Assume something is different.
        goReturn: {
          MapEpiNode thisMapEpiNode= thePeersCursor.getSelectedMapEpiNode();

          EpiNode testEpiNode= otherMapEpiNode.getEpiNode("IP");
          if (testEpiNode == null) break goReturn;
          if (! testEpiNode.equals(thisMapEpiNode.getEpiNode("IP"))) break goReturn;

          testEpiNode= otherMapEpiNode.getEpiNode("Port");
          if (testEpiNode == null) break goReturn;
          if (! testEpiNode.equals(thisMapEpiNode.getEpiNode("Port"))) break goReturn;

          testEpiNode= otherMapEpiNode.getEpiNode("PeerIdentity"); 
          if (testEpiNode == null) break goReturn;
          if (! testEpiNode.equals(thisMapEpiNode.getEpiNode("PeerIdentity"))) 
            break goReturn;

          resultB= true; // All is the same, so return true.
        } // goReturn:
          return resultB;
        }

    public boolean onInputsV() throws IOException
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
	    	process: {
		    	if ( super.orStateOnInputsB() ) // Try processing by sub-states first. 
		    		break process;
		    	String eventMessageString= theEpiInputStreamI.tryingToGetString();
	      	if ( eventMessageString != null ) // There is an unprocessed message.
		      	{ // Log and ignore the message.
		  	    	theAppLog.info( 
		  	    			"Unicaster.onInputsV() unprocessed message: "
		  	    			+ eventMessageString 
		  	    			);
			    		break process;
		      		}
	      	theAppLog.info( 
  	    			"Unicaster.onInputsV() no message to process!"
  	    			+ eventMessageString 
  	    			);
	    		} // process:
        return false;
				}

    public String getSummaryString( )
      /* Returns a string indicating whether 
        this Unicaster is presently connected or disconnected.
       	*/
      {
    	  return isConnectedB() ? "Connected" : "Disconnected"; 
        }
		
		public boolean isConnectedB()
		  /* This method returns true if this Unicaster is connected to its peer,
		    false otherwise.
		   	*/
			{
				return theLinkedMachineState.isConnectedB(); 
				}

    public NotifyingQueue<String> getNotifyingQueueOfStrings()
      /* This method returns it NotifyingQueueOfStrings to allow callers
        to add strings to it.
        */
      {
        return unicasterNotifyingQueueOfStrings;
        }

    public void putV(MapEpiNode theMapEpiNode)
      /* This method queues theEpiNode for this Unicaster.  */
      {
        toUnicasterNotifyingQueueOfMapEpiNodes.put(theMapEpiNode);
        }
    
	} // Unicaster.
