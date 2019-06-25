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
    
	  */

	{	
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong retransmitDelayMsNamedLong;
		private TCPCopier theTCPCopier;
		private Timer theTimer; ///opt.  use function parameter only. 
		private Unicaster theUnicaster;
		private StateList[] theLinkedStateLists;
		private Persistent thePersistent; 

		// Sub-state-machine instances.
    private InitiatingConnectionState theInitiatingConnectionState;
		private CompletingConnectionState theCompletingConnectionState;
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
					StateList[] theLinkedStateLists
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
				this.theLinkedStateLists= theLinkedStateLists;
				this.thePersistent= thePersistent; 

	  		// Adding measurement count.

    		// Create and add to DAG the sub-states of this state machine.
        initAndAddStateListV(theInitiatingConnectionState= 
            new InitiatingConnectionState());
        initAndAddStateListV(theCompletingConnectionState= 
            new CompletingConnectionState());
    		addStateListV(
    				(theConnectedState= new ConnectedState())
    				  .initializeWithIOExceptionStateList(this.theLinkedStateLists)
    				);
    		initAndAddStateListV(
            theDisconnectedState= new DisconnectedState());
    		initAndAddStateListV(
    		    theBrokenConnectionState= new BrokenConnectionState());
        
    		setFirstOrSubStateV( theInitiatingConnectionState );
    		
	  	  theTimerInput= // Creating our timer and linking to this state. 
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

  	public void onEntryV() throws IOException
		  { 
			  retryTimeOutMsL=   // Initializing retry time-out.
			  		retransmitDelayMsNamedLong.getValueL();

			  super.onEntryV();
				}

	  public boolean onInputsB() throws IOException 
	    /* This input handler method is mainly concerned with
	      disconnecting its Unicaster from the one running on the peer node.
	      It responds to the thread termination interrupt by
	      sending GOODBYEs to the peer.
	      In both cases, it transitions the sub-state machine
	      to the the DisconnectedState.
	     */
		  {
	  		boolean progressB= true; // Assume progress will be made.
	  		goReturn: {
		  		if ( super.onInputsB() ) // Try processing in sub-state machine.
		  			break goReturn; // Return because progress was made.
		  		if // Process local disconnect request, if present.
		  		  ( Thread.currentThread().isInterrupted() ) 
			  		{ // Process local disconnect request.
		        	Thread.currentThread().interrupt(); // Reestablish interrupted.
		        	progressB= requestSubStateListB( theDisconnectedState );
		        	if (progressB) // Send GOODBYEs if state change was accepted.
				  			for (int i=0; i<3; i++) { // Send 3 GOODBYE packets.
				  		    theNetcasterOutputStream.writingTerminatedStringV( "GOODBYE" );
				  				theNetcasterOutputStream.sendingPacketV(); // Forcing send.
				  				}
			  			break goReturn; // Return with the progress calculated above.
			  			}
		  		progressB= false; // Everything failed to progress, so return same.
		  		} // goReturn: 
		  	return progressB;
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

    private class InitiatingConnectionState extends StateList 

      /* This state is used to initiate a new connection.
        It sends a HELLO message to the remote peer
        and then transitions to the CompletingConnectionState.
        */

      {
    
        public void onInputsForLeafStatesV() throws IOException
          {
            sendHelloV(this);
            requestSiblingStateListV(theCompletingConnectionState);
            }
  
        } // class InitiatingConnectionState

    private class CompletingConnectionState extends StateList 

      /* Being in this state means that the other peer
        is believed to be on-line and ready for a connection.
        This class sends a HELLO message to the remote peer
        and tries to receive one from the remote peer.
        It retransmits HELLO if a HELLO is not received,
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
    
        public void onInputsForLeafStatesV() throws IOException
          /* This method completes the HELLO handshake, 
            retransmitting the initial HELLO if needed,
            using twice the previous time-out,
            until a HELLO is received, or eventually giving up.
            */
          {
            if (tryReceivingHelloB(this)) // Try to process first HELLO.
              requestSiblingStateListV( // Success.  Request connected state.
                  theConnectedState );
            else if (theTimerInput.getInputArrivedB()) // Try Time-out? 
              {
                sendHelloV(this); // Resent hello.
                if // Reschedule time-out with exponential back-off. 
                  (theTimerInput.rescheduleB(Config.maxTimeOutMsL))
                  requestSiblingStateListV( // Give up if max delay reached by
                      theBrokenConnectionState); // going to unconnected.
                }
            }
  
          } // class CompletingConnectionState
		
		private class ConnectedState extends AndState

	  	/* This state means we are connected to the peer node.
	  	  This state handles the reception of any extra HELLO messages,
	  	  which are HELLO messages received after the first one.
	  	  Extra HELLO messages means that the remote peer
	  	  did not receive the HELLO or HELLOs sent by us earlier.
	  	  */

	  	{
				public StateList initializeWithIOExceptionStateList(
						StateList[] theLinkedStateLists)
			    throws IOException
			    /* This method initializes the sub-states.  */
					{
						super.initializeWithIOExceptionStateList();
						
						for // Add each child state from array.
						  ( StateList theStateList : theLinkedStateLists ) 
							{ 
								addStateListV(theStateList); // Add it as sub-state.
								}
						
						return this;
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
			  	  It also handles the GOODBYE message by disconnecting. 
			  	  */
			  	{
			  			boolean progressB= true; // Assume progress will be made.
			  		goReturn: {
				  		if ( super.onInputsB() ) // Try processing in sub-state machine.
				  			break goReturn; // Return with progress.
				  		if (tryReceivingHelloB(this)) { // Try to process an extra HELLO.
					        appLogger.warning( "Extra HELLO received." );
					  			if  // If we received a HELLO and 
					  			  ( sentHelloB^= true ) // we didn't send one last time,
					  				sendHelloV(this); // send a HELLO this time.
					  			break goReturn; // Return with progress.
					  			}
				  		if ( tryInputB("GOODBYE") ) { // Did peer disconnect itself?
		    			  	requestSiblingStateListV( // Do the same here.
		    			  			theDisconnectedState);
					  			break goReturn; // Return with progress.
					  			}
				  		progressB= false; // Everything failed.  Set no progress.
			  		} // goReturn: 
			  			return progressB;
					  }

	  		} // class ConnectedState
    
    private class DisconnectedState extends StateList

      /* This state is entered when a controlled disconnect occurs,
        for example when a GOODBYE message is received.
        It will exit this state only when HELLO message is received.
        */

      {

        public void onInputsForLeafStatesV() throws IOException
          /* This method does nothing except test for HELLO messages.
            If it receives one then it sends a HELLO in response
            and transitions to the ConnectedState. 
            */
          {
            if (tryReceivingHelloB(this))
              {
                sendHelloV(this); // Send a response HELLO.
                requestSiblingStateListV( // Switch to ConnectedState.
                    theConnectedState
                    );
                }
            }

        } // class DisconnectedState
    
    private class BrokenConnectionState extends StateList

      /* This state is entered when a connection fails
        because packets could not be exchanged by the two peers.
        It will go to the ConnectedState when a HELLO message is received.
        It will go to the InitiatingConnectionState
        if the reconnect period passes.
        */

      {

        public void onEntryV() throws IOException
          // Initializes reconnect timer.
          {
            theTimerInput.scheduleV(Config.reconnectTimeOutMsL);
            }

        public void onInputsForLeafStatesV() throws IOException
          {
            if (tryReceivingHelloB(this))
              {
                sendHelloV(this); // Send a response HELLO.
                requestSiblingStateListV(theConnectedState);
                }
            else if (theTimerInput.getInputArrivedB()) // Time to try again? 
              //// requestSiblingStateListV(theInitiatingConnectionState);
              {
                sendHelloV(this); // Send a greeting HELLO.
                requestSiblingStateListV(theBrokenConnectionState);
                  // But stay in this state.  A kludge.
                }
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
  	  /* This method tries to read and process the Hello message,
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
