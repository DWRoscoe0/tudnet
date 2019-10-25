package allClasses;

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
      
  		// Other instance variables.
      private EpiThread theEpiThread;
  		private LinkedMachineState theLinkedMachineState;
  		
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
	  		NotifyingQueue<String> unicasterNotifyingQueueOfStrings
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
	      }

    protected void initializeWithIOExceptionV( 
        EpiThread theEpiThread
        ) 
      throws IOException
	    {
    		super.initializeWithoutStreamsV(); // Stream counts are added below in
    		  // one of the sub-state machines.

    		this.theEpiThread= theEpiThread;

	  		// Create and start the sub-state machines.

    		{ // Create and add actual sub-states.
    			
    			LinkMeasurementState theLinkMeasurementState= 
    				 new LinkMeasurementState( 
  		    				theTimer, 
  		    				theEpiInputStreamI,
  			  				theEpiOutputStreamO,
  			  				initialRetryTimeOutMsNamedLong 
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
		  				theLinkMeasurementState
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
        		if // Reconnect if we were connected.
        		  (thePeersCursor.getFieldB("wasConnected"))
        		  connectToPeerV(); // Tell state-machine to connect.

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
          	Globals.logAndRethrowAsRuntimeExceptionV( 
          			"run() IOException", theIOException 
          			);
            }
        /// appLogger.info("run() ends.");
        }

	  private void runLoop() throws IOException
	    /* This method contains the message processing loop.
		    It basically just reads event messages from the input stream
		    and the local message queue,
		    and passes them to its state machine until it receives a signal to exit.

        The state machine timers will start with the first calls to doOnInputsB().

        ///fix  Legitimate input is sometimes not consumed!
		    */
			{
	  		theAppLog.info("runLoop() begins.");
	      processingLoop: while (true) {
	        if (EpiThread.testInterruptB()) // Exit loop if thread termination requested. 
	          break processingLoop;
          if (doOnInputsB()) // Do some pending state machine work...
            continue processingLoop; // ...and loop until its all done.
          processUnprocessedInputV(); // Handle any left-over input.
		    	if (theEpiInputStreamI.available() > 0) { // Try parsing more packet stream.
            String inString= theEpiInputStreamI.readAString(); // Get next token.
            setOfferedInputV( inString ); // Offer it to state machine.
	    		  continue processingLoop; // Loop to process offered token.
	    		  }
	    		String localMessageString= // Try getting local message String. 
	    		    unicasterNotifyingQueueOfStrings.poll();
	    		if (localMessageString != null) { // If gotten, process it.
            setOfferedInputV( localMessageString ); // Offer it to state machine.
            continue processingLoop; // Loop to process offered message.
	    		  }
          ///dbg appLogger.debug("runLoop() before wait.");
          theLockAndSignal.waitingForInterruptOrNotificationE();
          ///dbg appLogger.debug("runLoop() after wait.");
	      	} // processingLoop:
  			theAppLog.info("runLoop() loop interrupted, stopping state machine.");
  			// ? theTimer.cancel(); // Cancel all Timer events for debug tracing, ///dbg
        while (doOnInputsB()) ; // Cycle state machine until nothing remains to be done.
				}
	  
    public void connectToPeerV()
      // This method tells the state-machine to connect.
      {
        theAppLog.info("connectToPeerV() executing.");
        try {
          processInputB( "Connect" ); // Make state machine process connect message.
        } catch( IOException theIOException) {
          Globals.logAndRethrowAsRuntimeExceptionV( 
              "Unicaster.connectToPeerV() IOException", theIOException 
              );
          }
        }
    
    private void processUnprocessedInputV() throws IOException
      /* This method is called to process any offered input that
        the Unicaster state machine was unable to process.
        Debug messages are silently ignored.
        Other messages are logged and ignored.
        ///enh Display all OrState state-machine states.
       */
      {
          String inputString= getOfferedInputString();
        toReturn: { 
        toConsumeInput: { 
          if ( inputString == null ) break toReturn;
          if (inputString.equals("DEBUG")) { // Ignore any debug message
            theEpiInputStreamI.readAString(); // and its message count argument.
            break toConsumeInput;
            }
          { // Log any other input, plus all OrState states. 
            theAppLog.info("processUnprocessedInputV() input= "+inputString);
            logOrSubstatesB(); // Log active OrState sub-states.
            }
        } // toConsumeInput: 
          resetOfferedInputV();  // consume unprocessed input.
        } // toReturn:
          return;
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

    public String getValueString( )
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

	} // Unicaster.
