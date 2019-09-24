package allClasses;

import java.io.IOException;
import java.util.Timer;

import static allClasses.AppLog.theAppLog;


public class LinkedMachineState

	extends OrState
	
	/* This class contains a [hierarchical] state machine 
	  that processes the HELLO handshake that is supposed to happen
	  at the beginning of a Unicaster connection,
	  and the GOODBYE message that ends a Unicaster connection.
	  
	  This state machine needs redesign to be general enough
	  to handle all of the following inputs:
	  * HELLO
	  * GOODBYE
    * Any unicaster packet from the remote peer.
    * Any multicaster packet from the remote peer.
    * interrupt() request used to terminate it.
    
	  */

	{	
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong retransmitDelayMsNamedLong;
		private TCPCopier theTCPCopier;
		private Timer theTimer; ///opt.  use function parameter only. 
		private Unicaster theUnicaster;
		private Persistent thePersistent; 
    private PeersCursor thePeersCursor;

		// Other variables.
		private String thePeerIdentityString= null;
		
		// Sub-state-machine instances.
    private DisconnectedState theDisconnectedState;
    private ExponentialRetryConnectingState theExponentialRetryConnectingState;
    private SlowPeriodicRetryConnectingState theSlowPeriodicRetryConnectingState;
    private ConnectedState theConnectedState;
    
		LinkedMachineState(  // Constructor.
				)
			throws IOException
	  	{
			  }

	  public synchronized LinkedMachineState 
	  	initializeWithIOExceptionLinkedMachineState(
					Timer theTimer, 
				  NetcasterInputStream theNetcasterInputStream,
					NetcasterOutputStream theNetcasterOutputStream,
					NamedLong retransmitDelayMsNamedLong,
					TCPCopier theTCPCopier,
					Unicaster theUnicaster,
					Persistent thePersistent, 
					PeersCursor thePeersCursor,
					LinkMeasurementState theLinkMeasurementState
		  		)
				throws IOException
		  {
	  		super.initializeWithIOExceptionStateList();

  	  	// Injected dependencies.
			  this.theTimer= theTimer;
			  this.theNetcasterInputStream= theNetcasterInputStream;
			  this.theNetcasterOutputStream= theNetcasterOutputStream;
			  this.retransmitDelayMsNamedLong= retransmitDelayMsNamedLong;
				this.theTCPCopier= theTCPCopier;
				this.theUnicaster= theUnicaster;
				this.thePersistent= thePersistent; 
        this.thePeersCursor= thePeersCursor;

	  		// Adding measurement count.

    		// Construct all sub-states of this state machine.
        theDisconnectedState= new DisconnectedState();
				theExponentialRetryConnectingState= new ExponentialRetryConnectingState();
        theSlowPeriodicRetryConnectingState= new SlowPeriodicRetryConnectingState();
        theConnectedState= new ConnectedState();

        // Initialize add to DAG all sub-states of this state machine.
        // Not all sub-states get default initialization.
        initAndAddStateListV(theDisconnectedState);
        initAndAddStateListV(theExponentialRetryConnectingState);
        initAndAddStateListV(theSlowPeriodicRetryConnectingState);
        addStateListV(theConnectedState.initializeWithIOExceptionStateList(
            theLinkMeasurementState));
        
        theConnectedState.setTargetDisconnectStateV(theSlowPeriodicRetryConnectingState);

        setFirstOrSubStateV( theDisconnectedState ); // Initial state is Disconnected.
    		
	  	  theTimerInput= // Creating our timer and linking it to this state. 
			  		new TimerInput(  ///? Move to factory or parent?
			  				this.theTimer,
			  				this
			  				);

	  	  /*  ///dbg
	  	  { ///dbg adjust logging for debugging.
					propagateIntoSubtreeB( LogLevel.TRACE );
					theConnectedState.propagateIntoDescendantsV(LogLevel.INFO);
		  	  }
	  	  */  ///dbg

	  	  return this;
			  }
	
    public synchronized void finalizeV() throws IOException
	    // This method processes any pending loose ends before app shutdown.
		  {
	  	  super.finalizeV();
	  		theTimerInput.cancelingV(); // To stop our timer.
	      }

    private long debugMessageCountL= 0;
    
    private void sendDebugCountV()
        throws IOException
      /* This method sends a DEBUG message 
        followed by the debug message count to the remote peer.
        Its purpose is to make logs of packets easier to interpret during debugging.
        It does not flush the buffer to send a packet.
        It should be called before a regular message which does flush the buffer.
        The receiver should silently ignore this message and its argument.
        */
      {
        theNetcasterOutputStream.writingTerminatedStringV( "DEBUG" );
        theNetcasterOutputStream.writingTerminatedLongV(debugMessageCountL);
        debugMessageCountL++;
        }

  	public void onEntryV() throws IOException
		  { 
  	    super.onEntryV();
			  retryTimeOutMsL=   // Initializing retry time-out.
			  		retransmitDelayMsNamedLong.getValueL();
				}

	  public boolean onInputsB() throws IOException 
	    /* This input handler method is mainly concerned with
	      disconnecting its Unicaster from the one running on the peer node.
	      It lets its descendant states handle everything else.
	      It interprets a thread interrupt as a termination request
	      and responds by requesting a sub-state change to 
	      theDisconnectedState if that state is not already active.
	      */
		  {
        if // Process local termination request, if present.
          ( tryInputB("Shutdown") )
          { // Process shutdown request by saving connection status, then disconnecting.
            theAppLog.debug( 
                "LinkedMachineState.onInputsB() isConnectedB()="+ isConnectedB());
            thePeersCursor.putFieldV( "wasConnected", isConnectedB());

            processInputB("Disconnect"); // Now cause disconnect.
            }
        boolean returnB= // Try processing in OrState machine of superclass.
          super.onInputsB();
		  	return returnB;
		  	}

		public void onExitV() throws IOException
		  // Cancels acknowledgement timer.
		  { 
				theTimerInput.cancelingV();
				super.onExitV();
				}

		// Other variables.
	  private TimerInput theTimerInput;
	  private long retryTimeOutMsL;
		
		public boolean isConnectedB()
		  /* This method returns true if this Unicaster is connected to its peer,
		    false otherwise.
		   	*/
			{
				return (getpresentSubStateList() == theConnectedState) ;
				}

		
		/* The following are the states used for managing Unicaster connecting. 
		  There are 2 states worthy of note
		  * InitiatingConnectState: The machine is put into this state
		    when a Unicaster is created for the first time,
		    in response to a multicast message from the peer.
		    It makes use of an aggressive exponential back-off of
		    HELLO messages in an effort to connect to the other peer.
		  */

    private class DisconnectedState extends StateList

      /* This is an important state.
        It is the first state after app startup.
        It is the last state before app shutdown.
        
        This state does not use a timer, 
        so it make no attempt on its own to end the state.
        
        This state will end only when it receives a request to do so,
        which could be:
        * A HELLO message received from the peer.
        * A local Connect request received from ConnectionManager.
        
        This state will silently ignore any received GOODBYE message.
        */

      {

        public void onEntryV() throws IOException
          { 
            super.onEntryV();
            }

        public void onInputsToReturnFalseV() throws IOException
          /* This method does nothing except test for HELLO messages.
            If it receives one then it sends a HELLO in response
            and transitions to the ConnectedState. 
            */
          {
            if (tryReceivingHelloB(this))
              {
                sendHelloV(this); // Send a response HELLO.
                requestAncestorSubStateV( // Switch to ConnectedState.
                    theConnectedState
                    );
                }
            else if ( tryInputB("Connect") ) { // Connect requested, at startup.
              theAppLog.info("Connecting to previous connectee.");
              sendHelloV(this); // Send initial HELLO.
              requestAncestorSubStateV( theExponentialRetryConnectingState );
              }
            else if ( tryInputB("GOODBYE") ) { // Ignore any redundant GOODBYE message.
              // appLogger.info("GOODBYE received and ignored while in DisconnectedState.");
              }
            }

        } // class DisconnectedState


    private class ExponentialRetryConnectingState extends StateList 

      /* This class assumes that a HELLO message 
        has already been sent to the remote peer
        and tries to receive one from the remote peer.
        It retransmits HELLOs if a HELLO is not received,
        using exponential back-off.
        It will give up trying if no HELLO is received. 
        */

      {

        public void onEntryV() throws IOException
          {
            super.onEntryV();
            theTimerInput.scheduleV(retryTimeOutMsL);
            }
    
        public void onInputsToReturnFalseV() throws IOException
          {
            if (tryReceivingHelloB(this)) // Try to process HELLO.
              requestAncestorSubStateV( // Success.  Request connected state.
                  theConnectedState );
            else if // Try handling time-out?
              (testAndLogIfTrueB(theTimerInput.testInputArrivedB(),
                  "exponential HELLO retry time-out,")) 
              {
                boolean limitReachedB= // Reschedule time-out with exponential back-off
                    (theTimerInput.rescheduleB(
                        Config.maxTimeOutMsL)); // up to this limit.
                if (! limitReachedB)
                  sendHelloV(this); // Not at max time-out so re-send hello and
                    // stay in this state.
                  else
                  { // End exponential backup by switching to another state.
                    theAppLog.info(
                        "Time-out limit reached in"+getFormattedStatePathString());
                    //// sendHelloV(this); // Initial HELLO for next state.
                    requestAncestorSubStateV( // Switch to different type of retrying.
                        theSlowPeriodicRetryConnectingState);
                    }
                }
            }
  
        } // class ExponentialRetryConnectingState


    private class SlowPeriodicRetryConnectingState extends StateList 

      /* This class assumes that a HELLO message 
        has already been sent to the remote peer
        and tries to receive one from the remote peer.
        It does not retransmit HELLOs if a HELLO is not received.
        It will give up trying if no HELLO is received. 
        */

      {

        public void onEntryV() throws IOException
          {
            super.onEntryV();
            sendHelloV(this); // Initial HELLO for this state.  Done here because
              // some predecessor states (ConnectedState sub-states) can't do it.
            theTimerInput.scheduleV(Config.slowPeriodicRetryTimeOutMsL);
            }
    
        public void onInputsToReturnFalseV() throws IOException
          {
            if (tryReceivingHelloB(this)) // Try to process HELLO.
              { // Success.  Move to ConnectedState.
                sendHelloV(this); // Send one in case HELLO came from peer in same state.
                requestAncestorSubStateV( theConnectedState ); // Request connected state.
                }
            else if (theTimerInput.testInputArrivedB()) // Time-out? 
              { // Send another HELLO and keep waiting.
                sendHelloV(this);
                theTimerInput.scheduleV( // Restart timer.
                    Config.slowPeriodicRetryTimeOutMsL);
                //// requestAncestorSubStateV( this ); // Continue by requesting this state. 
                }
            }
  
          } // class SlowPeriodicRetryConnectingState
		

		private class ConnectedState extends AndState

	  	/* This is a special state.
	  	  Being in this state means we are connected to the peer node.
	  	  This is the state in which inter-peer communication happens.
	  	  
	  	  This state handles the reception of any extra HELLO messages,
	  	  which are HELLO messages received after the first one.
	  	  Receiving HELLO messages here means that the remote peer
	  	  did not receive the HELLO or HELLOs sent by us earlier.
	  	  
	  	  This state also contains the sub-state LinkedMeasurementState.
	  	  This is used to monitor the health of the link to the peer,
	  	  measuring packet losses and round-trip-time.
	  	  
	  	  Finally this state handles several conditions which can cause
	  	  the state machine to exit this state.
	  	  */

	  	{
		    private LinkMeasurementState theLinkMeasurementState;
		    
				public StateList initializeWithIOExceptionStateList(
				    LinkMeasurementState theLinkMeasurementState)
			    throws IOException
			    /* This method initializes the sub-states,
			      building the sub-state-machine.  
			      Presently there is only one.  */
					{
				    this.theLinkMeasurementState= theLinkMeasurementState;

						super.initializeWithIOExceptionStateList();
						
            addStateListV(this.theLinkMeasurementState);
						
						return this;
						}
			
				public void setTargetDisconnectStateV(
				    StateList theBrokenConnectionState)
				  /* This method sells the LinkMeasurementState what state to request
				    it it appears that the communication link to the peer is broken. 
				    */
    			{
				    theLinkMeasurementState.setTargetDisconnectStateV(
				        theSlowPeriodicRetryConnectingState);
				    }

	    	public void onEntryV() throws IOException
		  	  /* Informs TCPCopier about this new connection,
		  	    which means a possible TCPServer to try.
		  	    It also records peer information in the Persistent storage
		  	    for faster connecting after app restart.
		  	    */
		  	  {
	    	    //// appLogger.debug( "Entering"+ getFormattedStatePathString() );
	    	    theAppLog.debug( "Connecting" );
            super.onEntryV();
	    			IPAndPort remoteIPAndPort= theUnicaster.getKeyK();
		    		theTCPCopier.queuePeerConnectionV(remoteIPAndPort);
		    		thePeersCursor.addInfoUsingPeersCursor(
		    		    remoteIPAndPort, thePeerIdentityString);
		  	  	}
	      
        private boolean sentHelloB= true; 
          // True means previous HELLO was sent by us, not received by us.
          // It is used by onInputsB() to prevent HELLO message storms.

			  public boolean onInputsB() throws IOException
			  	/* This method sends HELLO messages 
			  	  in response to extra received HELLO messages.
			  	  It calls the sub-state handler for message processing.
			  	  It also decodes various messages such as GOODBYE 
			  	  that can cause the state machine to exit this state.
            To prevent HELLO storms a response is made 
            to only every other received HELLO.
            This 1/2 ratio might be overkill.  1/3 or 1/4 might be better.
			  	  */
			  	{
			  			boolean signalB= true; // Assume signal will be produced.
			  		goReturn: {
				  		if ( super.onInputsB() ) // Try processing in sub-state machine.
				  			break goReturn; // Return with signal true.
				  		if (tryReceivingHelloB(this)) { // Try to process an extra HELLO.
					        theAppLog.debug( "Extra HELLO received." );
					  			if  // If we received a HELLO and 
					  			  ( sentHelloB^= true ) { // we didn't send one last time,
					  				sendHelloV(this); // send a HELLO this time.
		                requestAncestorSubStateV(this); // Reenter this to reset sub-states.
					  			  }
					  			break goReturn; // Return with signal true.
					  			}
              if ( tryInputB("GOODBYE") ) { // Peer disconnected itself by saying goodbye?
                sayGoodbyesV(); // Respond to peer with our own goodbyes.
                requestAncestorSubStateV( theDisconnectedState); // Disconnect ourselves.
                break goReturn; // Return with signal true.
                }
              if ( tryInputB("Disconnect") ) { // Disconnect requested (for our shutdown)?
                sayGoodbyesV(); // Inform peer.
                requestAncestorSubStateV( theDisconnectedState); // Disconnect ourselves.
                break goReturn; // Return with signal true.
                }
              if ( tryInputB("Skipped-Time") ) { // We just woke up from sleep.
                sendHelloV(this); // send a HELLO to reestablish connection.
                requestAncestorSubStateV( // Yes, so assume connection broke
                  theExponentialRetryConnectingState); // and try to reestablish.
                break goReturn; // Return with signal true.
                }
				  		signalB= false; // Everything failed.  Set no signal.
			  		} // goReturn: 
			  			return signalB;
					  }

		    public void onExitV() throws IOException
		      { 
		        super.onExitV();
            theAppLog.debug( "Disconnecting" );
		        }
		    
	  		} // class ConnectedState


    private void sayGoodbyesV() throws IOException
      /* This method sends 3 GOODBYEs, each in a separate packet.  */
      {
        for (int i=0; i<3; i++) { // Send 3 GOODBYE packets.
          theNetcasterOutputStream.writingTerminatedStringV( "GOODBYE" );
          theNetcasterOutputStream.sendingPacketV(); // Forcing send.
          }
        }


  	private boolean tryReceivingHelloB(StateList subStateList) 
  			throws IOException
  	  /* This method tries to receive and process the Hello message,
  	    including its arguments.
  	    This method is part of this state class, 
  	    but is not called by its own code.
  	    It is called by one of its sub-states, 
  	    which must be specified with the parameter subStateList.

  	    If the next input to the sub-state is "HELLO" then it processes it,
  	    which means parsing the IP address which follows and determining
  	    which peer, the local or remote, will be leader,
  	    by comparing the IP addresses, and setting 
  	    the value for leadingDefaultBooleanLike.
				Note, the particular ordering of IP address Strings doesn't matter.  
				What matters is that the ordering is consistent.
				One peer will decide it is the leader; the other peer, the follower.

  	    This method does not send a reply "HELLO".  Either
  	    * a "HELLO" must be sent after this method returns, or
  	    * a "HELLO" has already been sent before this method was called.

  	    This method returns true if HELLO is received and processed, 
  	    false otherwise.
  	    
  	    ///pos This method could also select a leader based on PeerIdentity
  	    instead of IP address.  Presently PeerIdentity is read but discarded.
  	    */
	  	{
  		  boolean gotKeyB= subStateList.tryInputB("HELLO");
  		  if (gotKeyB) { // Decoding argument if input is "HELLO".
					String localIpString= theNetcasterInputStream.readAString();
					thePeerIdentityString= theNetcasterInputStream.readAString(); 
					String remoteIpString= 
							theUnicaster.getKeyK().getInetAddress().getHostAddress();
					theUnicaster.leadingDefaultBooleanLike.setValueB( // Decide who leads.
							localIpString.compareTo(remoteIpString) > 0 
							);
					///dbg leadingDefaultBooleanLike= !leadingDefaultBooleanLike; // Reverse roles for debug test. 
	        theAppLog.info( 
	        		"HELLO received.  Setting or overriding role to be: "
	        		+(	theUnicaster.leadingDefaultBooleanLike.getValueB() 
	        				? "LEADER" 
	        				: "FOLLOWER"
	        				)
	        		);
				  }
  		  return gotKeyB;
	  		}


  	private void sendHelloV(StateList subStateList)
  			throws IOException
  	  /* This method sends a HELLO message to the remote peer
  	    from state subStateList, and logs that it has done so.
  	    The HELLO message includes the IP address of the remote peer.
  	    */
	  	{
    	  sendDebugCountV();
    	  theNetcasterOutputStream.writingTerminatedStringV( "HELLO" );
		    theNetcasterOutputStream.writingTerminatedStringV( 
						theUnicaster.getKeyK().getInetAddress().getHostAddress() 
						);  // Writing IP address of remote peer.
		    theNetcasterOutputStream.writingTerminatedStringV( 
						thePersistent.getDefaultingToBlankString("PeerIdentity")); 
		    theNetcasterOutputStream.sendingPacketV(); // Forcing send.
	  		}

		} // class LinkedMachineState
