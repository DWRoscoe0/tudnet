package allClasses;

import java.io.IOException;
////import java.net.DatagramPacket;
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
      ////private final SubcasterQueue subcasterToUnicasterSubcasterQueue;
  		private Timer theTimer;
  		
  		// Other instance variables.
  		private LinkedMachineState theLinkedMachineState;
  		
  	public Unicaster(  // Constructor. 
			  UnicasterManager theUnicasterManager,
			  SubcasterManager theSubcasterManager,
	    	LockAndSignal theLockAndSignal,
	      NetcasterInputStream theNetcasterInputStream,
	      NetcasterOutputStream theNetcasterOutputStream,
	      IPAndPort remoteIPAndPort,
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
				  ////this.subcasterToUnicasterSubcasterQueue= 
				  ////		subcasterToUnicasterSubcasterQueue;
		  		this.theTimer= theTimer;
	      }

    protected void initializeWithIOExceptionV() throws IOException
	    {
    		super.initializeWithoutStreamsV(); // Stream counts are added below in
    		  // one of the sub-state machines.

	  		// Create and start the sub-state machines.

    		{ // Create and add actual sub-states.
    			/*  ////
	    		initAndAddStateListV(
	    				theBeforeHelloExchangeState= new BeforeHelloExchangeState());
	    		initAndAddStateListV(
	    				theAfterHelloExchangedState= new AfterHelloExchangedState());
	    		setFirstOrSubStateV( theBeforeHelloExchangeState ); // Initial state.
	
		  	  initAndAddStateListV(theTemporaryMainState= new TemporaryMainState());
		  	  
		  	  initAndAddStateListV(theIgnoreAllSubstatesState= 
		  	  		new IgnoreAllSubstatesState());
	
		  	  theMultiMachineState= new MultiMachineState(
		  				theTimer, 
		  			  theEpiInputStreamI,
		  				theEpiOutputStreamO,
		  				retransmitDelayMsNamedLong,
		  				this
		  	  		);
		  	  theMultiMachineState.initializeWithIOExceptionV();

	
		  	  ///tmp Note, the following are added but not as States.
		  	  theIgnoreAllSubstatesState.addB( theMultiMachineState );
		  	  addB( theTemporaryMainState );
		  	  addB( theMultiMachineState );
		  	  addB( theIgnoreAllSubstatesState );
		  	  */   ////
  	  	  //// addStateListV( 
    			
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
		  				this,
		  				new StateList[] { theLinkMeasurementState }
		  	  		);
  				addStateListV( theLinkedMachineState );

    			} // Create and add actual sub-states.

	  	  //// theMultiMachineState.startRootMachineV(); // Start the multi-machines,
	  	    // by starting their timers, by callinG the main machine handler.

	  	  addAtEndB( theSubcasterManager );
	    	}

    protected void finalizingV() throws IOException
	    // This is the opposite of initilizingV().
	    {
	    	//// theMultiMachineState.finalizeV();
	    	theEpiOutputStreamO.close(); // Closing output stream.
	    	}

    public void run()  // Main Unicaster thread.
      /* This method contains the main thread logic.
        It basically just reads event messages from the input stream
        and passes them to its state machine
        until it receives a signal to exit.
        Most state-machines only need to read from the input stream
        for event data, not the main event message itself.
        */
      {
    		appLogger.info("run() begins.");
        try { // Operations that might produce an IOException.
	          initializeWithIOExceptionV();
	          doOnEntryV(); // Simulate an actual state entry. 
	      		theDataTreeModel.displayTreeModelChangesV(); // Display our arrival.

	          while (true) { // Repeating until termination is requested.
            	doOnInputsB(); // Process inputs in this state-machine.
            	  // This will start the state machine timers the first time.
					  	LockAndSignal.Input theInput= // Waiting for a new input.
			    			  theLockAndSignal.waitingForInterruptOrNotificationE();
			      	if ( theInput == Input.INTERRUPTION ) // Process exit request. 
				      	{ // Inform state machine of pending exit, then do so.
			      			appLogger.info("run() interrupted, exiting.");
				      		Thread.currentThread().interrupt(); // Restore interrupt.
		            	while (doOnInputsB()) ; // Make state machine process it.
		            	break;
					      	}
			      	if // Transfer any synchronous input from stream to machine.
			      	  ( ( theEpiInputStreamI.available() > 0 ) // New is available.
			      	  	&& ( getSynchronousInputString() == null ) // Old is consumed.
			      	  	)
			      		setSynchronousInputV( theEpiInputStreamI.readAString() );
		 	      	} // while (true)
	          
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

    public void onInputsV() throws IOException
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
	    	////appLogger.info( "Unicaster.onInputsV() starting." );
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
	    	////appLogger.info( "Unicaster.onInputsV() ending." );
				}



	} // Unicaster.
