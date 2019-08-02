package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Timer;

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
		//// private StateList[] theLinkedStateLists;
		//// private LinkMeasurementState theLinkMeasurementState;
		private Persistent thePersistent; 

		// Sub-state-machine instances.
    private InitiatingConnectState theInitiatingConnectState;
    private CompletingConnectState theCompletingConnectState;
    private InitiatingReconnectState theInitiatingReconnectState;
    private CompletingReconnectState theCompletingReconnectState;
    private ConnectedState theConnectedState;
    private DisconnectedState theDisconnectedState;
    private BrokenConnectionState theBrokenConnectionState;
    
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
					//// StateList[] theLinkedStateLists
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
				//// this.theLinkedStateLists= theLinkedStateLists;
				//// this.theLinkMeasurementState= theLinkMeasurementState;
				this.thePersistent= thePersistent; 

	  		// Adding measurement count.

    		// Construct all sub-states of this state machine.
				theInitiatingConnectState= new InitiatingConnectState();
        theCompletingConnectState= new CompletingConnectState();
        theInitiatingReconnectState= new InitiatingReconnectState();
        theCompletingReconnectState= new CompletingReconnectState();
        theConnectedState= new ConnectedState();
        theDisconnectedState= new DisconnectedState();
        theBrokenConnectionState= new BrokenConnectionState();

        // Initialize add to DAG all sub-states of this state machine.
        // Not all sub-states get default initialization.
        initAndAddStateListV(theInitiatingConnectState);
        initAndAddStateListV(theCompletingConnectState);
        initAndAddStateListV(theInitiatingReconnectState);
        initAndAddStateListV(theCompletingReconnectState);
        //// addStateListV(theConnectedState.initializeWithIOExceptionStateList(
        //// this.theLinkedStateLists));
        addStateListV(theConnectedState.initializeWithIOExceptionStateList(
            theLinkMeasurementState));
        initAndAddStateListV(theDisconnectedState);
        initAndAddStateListV(theBrokenConnectionState);
        
        theConnectedState.setTargetDisconnectStateV(theBrokenConnectionState);

    		setFirstOrSubStateV( theInitiatingConnectState );
    		
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
	
    public void setForReconnectV(boolean connectB)
      { 
        setFirstOrSubStateV( connectB 
          ? theInitiatingReconnectState
          : theInitiatingConnectState );
        }

    public synchronized void finalizeV() throws IOException
	    // This method processes any pending loose ends before app shutdown.
		  {
	  	  super.finalizeV();
	  		theTimerInput.cancelingV(); // To stop our timer.
	      }

  	public void onEntryV() throws IOException
		  { 
			  retryTimeOutMsL=   // Initializing retry time-out.
			  		retransmitDelayMsNamedLong.getValueL();

			  super.onEntryV();
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
          (EpiThread.testInterruptB())
          { // Process local termination request by requesting disconnect.
            requestSubStateChangeIfNeededB( theDisconnectedState );
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
		  * 
		 * */

    private class InitiatingConnectState extends StateList 

      /* This state is used to initiate a new connection,
        which means a connection to a peer for the first time,
        and that peer is believed to be on-line now. 
        It sends a HELLO message to the remote peer
        and then transitions to the CompletingConnectState.
        */

      {
    
        public void onInputsToReturnFalseV() throws IOException
          {
            sendHelloV(this);
            requestAncestorSubStateV(theCompletingConnectState);
            }
  
        } // class InitiatingConnectState

    private class CompletingConnectState extends StateList 

      /* This class assumes that a HELLO message 
        has already been sent to the remote peer
        and tries to receive one from the remote peer.
        It retransmits HELLOs if a HELLO is not received,
        using exponential back-off.
        It will give up trying if no HELLO is received. 
        It compares IP addresses to decide
        which peer will lead and which will follow.
        */

      {

        public void onEntryV() throws IOException
          {
            theTimerInput.scheduleV(retryTimeOutMsL);
            }
    
        public void onInputsToReturnFalseV() throws IOException
          {
            if (tryReceivingHelloB(this)) // Try to process first HELLO.
              requestAncestorSubStateV( // Success.  Request connected state.
                  theConnectedState );
            else if (theTimerInput.getInputArrivedB()) // Try Time-out? 
              {
                sendHelloV(this); // Resend hello.
                if // Reschedule time-out with exponential back-off. 
                  (theTimerInput.rescheduleB(Config.maxTimeOutMsL))
                  requestAncestorSubStateV( // Give up if max delay reached by
                    theBrokenConnectionState); // going to broken connection.
                }
            }
  
          } // class CompletingConnectState

    private class InitiatingReconnectState extends StateList 

      /* This state is used to restore an old connection.
        It sends a HELLO message to the remote peer
        and then transitions to the CompletingReconnectState.
        */

      {
    
        public void onInputsToReturnFalseV() throws IOException
          {
            sendHelloV(this);
            requestAncestorSubStateV(theCompletingReconnectState);
            }
  
        } // class InitiatingReconnectState

    private class CompletingReconnectState extends StateList 

      /* This class assumes that a HELLO message 
        has already been sent to the remote peer
        and tries to receive one from the remote peer.
        It does not retransmit HELLOs if a HELLO is not received.
        It will give up trying if no HELLO is received. 
        */

      {

        public void onEntryV() throws IOException
          {
            theTimerInput.scheduleV(Config.maxTimeOutMsL);
            }
    
        public void onInputsToReturnFalseV() throws IOException
          {
            if (tryReceivingHelloB(this)) // Try to process HELLO.
              requestAncestorSubStateV( // Success.  Request connected state.
                  theConnectedState );
            else if (theTimerInput.getInputArrivedB()) // Try Time-out? 
              {
                requestAncestorSubStateV( // Give up by
                    theBrokenConnectionState); // going to broken connection.
                }
            }
  
          } // class CompletingConnectState
		
		private class ConnectedState extends AndState

	  	/* This state means we are connected to the peer node.
	  	  This state handles the reception of any extra HELLO messages,
	  	  which are HELLO messages received after the first one.
	  	  Receiving HELLO messages here means that the remote peer
	  	  did not receive the HELLO or HELLOs sent by us earlier.
	  	  */

	  	{
		    private LinkMeasurementState theLinkMeasurementState;
		    
				public StateList initializeWithIOExceptionStateList(
						//// StateList[] theLinkedStateLists)
				    LinkMeasurementState theLinkMeasurementState)
			    throws IOException
			    /* This method initializes the sub-states,
			      building the sub-state-machine.  
			      Presently there is only one.  */
					{
				    this.theLinkMeasurementState= theLinkMeasurementState;

						super.initializeWithIOExceptionStateList();
						
            addStateListV(this.theLinkMeasurementState);
            /*  ////
						for // Add each child state from array.
						  ( StateList theStateList : theLinkedStateLists ) 
							{ 
								addStateListV(theStateList); // Add it as sub-state.
								}
					  */  ////
						
						return this;
						}
			
				public void setTargetDisconnectStateV(
				    BrokenConnectionState theBrokenConnectionState) 
    			{
				    theLinkMeasurementState.setTargetDisconnectStateV(
	            theBrokenConnectionState);
				    }
   
        private boolean sentHelloB= true; 
				  // True means previous HELLO was sent by us, not received by us.
				  // It is used to prevent HELLO message storms.

	    	public void onEntryV() throws IOException
		  	  /* Informs TCPCopier about this new connection,
		  	    which means a possible TCPServer to try.
		  	    It also records peer information in the Persistent storage
		  	    for faster connecting after app restart.
		  	    */
		  	  {
	    			IPAndPort remoteIPAndPort= theUnicaster.getKeyK();
		    		theTCPCopier.queuePeerConnectionV(remoteIPAndPort);
		    		IPAndPort.addPeerInfoV(thePersistent, remoteIPAndPort);
		  	  	super.onEntryV();
		  	  	}

			  public boolean onInputsB() throws IOException
			  	/* This method sends HELLO messages 
			  	  in response to extra received HELLO messages.
			  	  To prevent HELLO storms, response is made to only
			  	  every other received HELLO.
			  	  It also handles reception of the GOODBYE message by disconnecting. 
			  	  */
			  	{
			  			boolean signalB= true; // Assume signal will be produced.
			  		goReturn: {
				  		if ( super.onInputsB() ) // Try processing in sub-state machine.
				  			break goReturn; // Return with signal true.
				  		if (tryReceivingHelloB(this)) { // Try to process an extra HELLO.
					        appLogger.warning( "Extra HELLO received." );
					  			if  // If we received a HELLO and 
					  			  ( sentHelloB^= true ) // we didn't send one last time,
					  				sendHelloV(this); // send a HELLO this time.
					  			break goReturn; // Return with signal true.
					  			}
				  		if ( tryInputB("GOODBYE") ) { // Did peer disconnect itself?
		    			  	requestAncestorSubStateV( // Yes, so we do the same.
		    			  			theDisconnectedState);
					  			break goReturn; // Return with signal true.
					  			}
				  		signalB= false; // Everything failed.  Set no signal.
			  		} // goReturn: 
			  			return signalB;
					  }

		    public void onExitV() throws IOException
		      /* Informs the peer that we are disconnecting by 
		        sending 3 GOODBYE messages.
		        */
		      { 
		        for (int i=0; i<3; i++) { // Send 3 GOODBYE packets.
              theNetcasterOutputStream.writingTerminatedStringV( "GOODBYE" );
              theNetcasterOutputStream.sendingPacketV(); // Forcing send.
              }
		        super.onExitV();
		        }
		    
	  		} // class ConnectedState
    
    private class DisconnectedState extends StateList

      /* This state is entered when a voluntary disconnect occurs,
        for example when a GOODBYE message is received.
        It will exit this state only when HELLO message is received.
        */

      {

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
            }

        } // class DisconnectedState
    
    private class BrokenConnectionState extends StateList

      /* This state is entered when a connection involuntarily fails
        because packets could not be exchanged by the two peers.
        It will go to the ConnectedState when a HELLO message is received.
        It will go to the InitiatingConnectState
        if the reconnect period passes.
        */

      {

        public void onEntryV() throws IOException
          // Initializes reconnect timer.
          {
            theTimerInput.scheduleV(Config.reconnectTimeOutMsL);
            }

        public void onInputsToReturnFalseV() throws IOException
          {
            if (tryReceivingHelloB(this))
              {
                sendHelloV(this); // Send a response HELLO.
                requestAncestorSubStateV(theConnectedState);
                }
            else if (theTimerInput.getInputArrivedB()) // Time to try again? 
              requestAncestorSubStateV(theInitiatingReconnectState);
            }

        public void onExitV() throws IOException
          // Cancels timer.
          { 
            theTimerInput.cancelingV();
            super.onExitV();
            }

        } // class BrokenConnectionState

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
  	    
  	    ///pos This method could also select a leader based on NodeIdentity
  	    instead of IP address.  Presently NodeIdentity is read but discarded.
  	    */
	  	{
  		  boolean gotKeyB= subStateList.tryInputB("HELLO");
  		  if (gotKeyB) { // Decoding argument if input is "HELLO".
					String localIpString= theNetcasterInputStream.readAString();
					theNetcasterInputStream.readAString(); // Discard NodeIdentity. 
					String remoteIpString= 
							theUnicaster.getKeyK().getInetAddress().getHostAddress();
					theUnicaster.leadingDefaultBooleanLike.setValueB( // Decide who leads.
							localIpString.compareTo(remoteIpString) > 0 
							);
					///dbg leadingDefaultBooleanLike= !leadingDefaultBooleanLike; // Reverse roles for debug test. 
	        appLogger.info( 
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
		    theNetcasterOutputStream.writingTerminatedStringV( "HELLO" );
		    theNetcasterOutputStream.writingTerminatedStringV( 
						theUnicaster.getKeyK().getInetAddress().getHostAddress() 
						);  // Writing IP address of remote peer.
		    theNetcasterOutputStream.writingTerminatedStringV( 
						thePersistent.getDefaultingToBlankString("NodeIdentity")); 
		    theNetcasterOutputStream.sendingPacketV(); // Forcing send.
	  		}

		} // class LinkedMachineState
