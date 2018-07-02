package allClasses;

import java.io.IOException;
import java.util.Timer;

import static allClasses.Globals.appLogger;

//// import allClasses.AppLog.LogLevel;
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
    
    ///tmp This class contains a lot of disabled code,
      versions of which are being reimplemented, 
      starting with LinkedMachineState.
      Eventually this disabled code must be all be reimplemented or deleted.
    */

  { // Unicaster.

    // Fields (constant and variales).
    
      // Injected dependency instance variables
      private final UnicasterManager theUnicasterManager;
      private final SubcasterManager theSubcasterManager;
      private final TCPCopier.TCPClient theTCPClient;
      private final Timer theTimer;
  		
  		// Other instance variables.
  		private LinkedMachineState theLinkedMachineState;
  		
  	public Unicaster(  // Constructor. 
			  UnicasterManager theUnicasterManager,
			  SubcasterManager theSubcasterManager,
	    	LockAndSignal theLockAndSignal,
	      NetcasterInputStream theNetcasterInputStream,
	      NetcasterOutputStream theNetcasterOutputStream,
	      IPAndPort remoteIPAndPort,
	      TCPCopier.TCPClient theTCPClient,
	      Shutdowner theShutdowner,
	      SubcasterQueue subcasterToUnicasterSubcasterQueue, ///elim Subcasters?
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
				  this.theTCPClient= theTCPClient;
		  		this.theTimer= theTimer;
	      }

    protected void initializeWithIOExceptionV() throws IOException
	    {
    		super.initializeWithoutStreamsV(); // Stream counts are added below in
    		  // one of the sub-state machines.

	  		// Create and start the sub-state machines.

    		{ // Create and add actual sub-states.
    			
    			LinkMeasurementState theLinkMeasurementState= 
    				 new LinkMeasurementState( 
  		    				theTimer, 
  		    				theEpiInputStreamI,
  			  				theEpiOutputStreamO,
  			  				retransmitDelayMsNamedLong 
  			      		);
    			theLinkMeasurementState.initializeWithIOExceptionStateList();

  				theLinkedMachineState= new LinkedMachineState();
  				theLinkedMachineState.initializeWithIOExceptionLinkedMachineState(
		  				theTimer, 
		  			  theEpiInputStreamI,
		  				theEpiOutputStreamO,
		  				retransmitDelayMsNamedLong,
		  				theTCPClient,
		  				this,
		  				new StateList[] { theLinkMeasurementState }
		  	  		);
  				addStateListV( theLinkedMachineState );

  				} // Create and add actual sub-states.

	  	  addAtEndB( theSubcasterManager );
	  	  
	  	  // propagateIntoSubtreeB( LogLevel.TRACE ); ///dbg /// tmp
	  	  }

    protected void finalizingV() throws IOException
	    // This is the opposite of initilizingV().
	    {
	    	///elim theMultiMachineState.finalizeV();
	    	theEpiOutputStreamO.close(); // Closing output stream.
	    	}

    public void run()  // Main Unicaster thread.
      /* This method contains the main thread logic.
        It contains initialization, finalization, with
        a mesaage processing loop in between.
        It also contains an IOException handler.
        */
      {
    		appLogger.info("run() begins.");
        try { // Operations that might produce an IOException.
        		appLogger.info("run() initializing root state machine.");
	          initializeWithIOExceptionV();
	      		appLogger.info("run() init done, activating root state machine.");
	          doOnEntryV(); // Recursively activate all states that should be. 
        		appLogger.info("run() machine activated, doing first display.");
	      		theDataTreeModel.displayTreeModelChangesV(); // Display our arrival.

	      	  runLoop(); // Do actual input processing in a loop.

	      	  finalizingV();
	  	    	theUnicasterManager.removingV( this ); // Removing self from tree.
	      		appLogger.info("run() after remove and before final display.");
	      		theDataTreeModel.displayTreeModelChangesV(); // Display removal.
          	}
          catch( IOException e ) {
          	Globals.logAndRethrowAsRuntimeExceptionV( 
          			"run() IOException", e 
          			);
            }
    		appLogger.info("run() ends.");
        }

	  private void runLoop() throws IOException
	    /* This method contains the message processing loop.
		    It basically just reads event messages from the input stream
		    and passes them to its state machine
		    until it receives a signal to exit.
		    */
			{
	  		appLogger.info("runLoop() begins.");
		    processUntilTerminated: while (true) {
			    processAllAvailableInput: while (true) {
			    	while (doOnInputsB()) ; // Process inputs in this state-machine.
			    	  // This will also start the state machine timers the first time.
			    	if ( getDiscreteInputString() != null ) { ///dbg
			  			appLogger.warning("runLoop() unconsumed input= "+getDiscreteInputString());
			  			resetDiscreteInputV();  // consume it.
			  			///fix  Legitimate input is sometimes not consumed!  Fix.
			    		}
		    		if ( theEpiInputStreamI.available() <= 0 ) // No input available. 
		    			break processAllAvailableInput; // Exit available input loop.
	    	  	String inString=  // Get next stream string.
	    	  			theEpiInputStreamI.readAString();
	      		setDiscreteInputV( inString );
		      	} // processAllAvailableInput:
					///dbg appLogger.warning("runLoop() before wait.");
			  	LockAndSignal.Input theInput= // Waiting for new input.
		  			  theLockAndSignal.waitingForInterruptOrNotificationE();
					///dbg appLogger.warning("runLoop() after wait.");
		    	if ( theInput == Input.INTERRUPTION ) break processUntilTerminated; 
		     	} // processUntilTerminated: 
      	{ // Inform state machine of termination request.
    			appLogger.info("runLoop() loop interrupted.  Terminating.");
      		Thread.currentThread().interrupt(); // Restore interrupt.
        	while (doOnInputsB()) ; // Let state machine run until processed.
	      	}
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
		  	    	appLogger.info( 
		  	    			"Unicaster.onInputsV() unprocessed message: "
		  	    			+ eventMessageString 
		  	    			);
			    		break process;
		      		}
	      	appLogger.info( 
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



	} // Unicaster.
