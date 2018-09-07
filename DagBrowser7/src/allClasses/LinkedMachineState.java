package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Timer;

public class LinkedMachineState

	extends OrState
	
	/* This class has not been tested and is not being used.
	  After its creation it was decided that its sub-states
	  should be moved [incrementally] to Unicaster and tested there
	  before being made into its own class, if ever.  //?  

	  This class contains a [hierarchical] state machine 
	  that processes the HELLO handshake that is supposed to happen
	  at the beginning of a Unicaster connection,
	  and the GOODBYE message that ends a Unicaster connection.
	  */

	{	
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong retransmitDelayMsNamedLong;
		private TCPCopier.TCPClient theTCPClient;
		private Timer theTimer; ///opt.  use function parameter only. 
		private Unicaster theUnicaster;
		private StateList[] theLinkedStateLists;
		private Persistent thePersistent; 

		// Sub-state-machine instances.
		private TryingToConnectState theTryingToConnectState;
		private ConnectedState theConnectedState;
		private UnconnectedWaitingState theUnconnectedWaitingState;

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
					TCPCopier.TCPClient theTCPClient,
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
				this.theTCPClient= theTCPClient;
				this.theUnicaster= theUnicaster;
				this.theLinkedStateLists= theLinkedStateLists;
				this.thePersistent= thePersistent; 

	  		// Adding measurement count.

    		// Create and add to DAG the sub-states of this state machine.
    		initAndAddStateListV(theTryingToConnectState= 
    				new TryingToConnectState());
    		addStateListV(
    				(theConnectedState= new ConnectedState())
    				  .initializeWithIOExceptionStateList(this.theLinkedStateLists)
    				);
    		initAndAddStateListV(
    				theUnconnectedWaitingState= new UnconnectedWaitingState());
    		setFirstOrSubStateV( theTryingToConnectState ); // Set initial state.

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
	    // This method processes any pending loose ends before shutdown.
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
	      to the the UnconnectedWaitingState.
	     */
		  {
	  		boolean progressB= true; // Assume progress will be made.
	  		goReturn: {
		  		if ( super.onInputsB() ) // Try processing in sub-state machine.
		  			break goReturn; // Return with progress.
		  		if // Process local disconnect request, if present.
		  		  ( Thread.currentThread().isInterrupted() ) 
			  		{ // Process local disconnect request.
		        	Thread.currentThread().interrupt(); // Reestablish interrupted.
		        	progressB= requestSubStateListB( theUnconnectedWaitingState );
		        	if (progressB) // Send GOODBYEs if state change was accepted.
				  			for (int i=0; i<3; i++) { // Send 3 GOODBYE packets.
				  		    theNetcasterOutputStream.writingTerminatedStringV( "GOODBYE" );
				  				theNetcasterOutputStream.sendingPacketV(); // Forcing send.
				  				}
			  			break goReturn; // Return with the progress calculated above.
			  			}
		  		progressB= false; // If we got this far, everything failed.  Override.
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

		private class TryingToConnectState extends StateList 

	  	/* This class exchanges HELLO messages with the remote peer
	  	  to decide which peer will lead and which will follow.
	  	  It retries using exponential back-off 
	  	  until the acknowledgement HELLO is received.
			  */

	  	{

	    	public void onEntryV() throws IOException
		  	  // Sends a HELLO and initializes retry timer.
		  	  {
		    		sendHelloV(this);
					  theTimerInput.scheduleV(retryTimeOutMsL);
						}
		
			  public void onInputsForLeafStatesV() throws IOException
			  	/* This method handles HELLO handshake acknowledgement, 
			  	  initiating a retry using twice the time-out,
			  	  until a HELLO is received.
			  	  */
			  	{
			  		if (tryReceivingHelloB(this)) // Try to process first HELLO.
			  			requestSiblingStateListV( // Success.  Request connected state.
			  					theConnectedState );
		      	else if (theTimerInput.getInputArrivedB()) // Try Time-out? 
			      	{
				    		sendHelloV(this); // Resent hello.
							  if // Reschedule time-out with exponential back-off. 
							    (theTimerInput.rescheduleB(Config.maxTimeOut5000MsL))
							    requestSiblingStateListV( // Give up if max delay reached.
			    			  		theUnconnectedWaitingState); //   Go to unconnected.
			    			}

			    		/*  ////
			    		{ // Time-out occurred.  Retry or give up.
			    		  retryTimeOutMsL*=2;  // Doubling time-out limit.
			    			if // but stop if above maximum.
		      				( retryTimeOutMsL > Config.maxTimeOut5000MsL )
				    			{
			    			  	retryTimeOutMsL= Config.maxTimeOut5000MsL; // Cap time-out.
				    			  requestSiblingStateListV( // Give up.  Go to unconnected.
				    			  		theUnconnectedWaitingState);
				    				}
			    				else
			    			  requestSiblingStateListV( // Retry by reentering this state.
			    			  		this);
			      		}
			      	*/  ////
		  	  	}
	
		  		} // class TryingToConnectState
		
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

	    	public void onEntryV() throws IOException
		  	  /* Informs TCPClient about this new connection,
		  	    which means a possible TCPServer to try.
		  	    It also records peer information in the Persistent storage
		  	    for faster connecting after app restart.
		  	    */
		  	  {
	    			IPAndPort remoteIPAndPort= theUnicaster.getKeyK();
		    		theTCPClient.reportPeerConnectionV(remoteIPAndPort);
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
				  		if ( tryInputB("GOODBYE") ) { // Did peer disconnect?
		    			  	requestSiblingStateListV( // Do the same here.
		    			  			theUnconnectedWaitingState);
					  			break goReturn; // Return with progress.
					  			}
				  		progressB= false; // Everything failed.  Set no progress.
			  		} // goReturn: 
			  			return progressB;
					  }

	  		} // class ConnectedState
		
		private class UnconnectedWaitingState extends StateList

	  	/* This state is entered when a disconnect occurs.
	  	  It will exit this state if a HELLO message is received.
	  	  */

	  	{

	    	public void onEntryV() throws IOException
		  	  // Sends a HELLO and initializes retry timer.
		  	  {
					  theTimerInput.scheduleV(Config.reconnectTimeOutMsL);
						}

			  public void onInputsForLeafStatesV() throws IOException
			  	/* This method does nothing except test for HELLO messages.
			  	  If it receives one then it sends a HELLO in response
			  	  and transitions to the ConnectedState. 
			  	  */
			  	{
			  		if (tryReceivingHelloB(this))
				  		{
			  				sendHelloV(this); // send a response HELLO.
				  			requestSiblingStateListV( // Switch to ConnectedState.
				  					theConnectedState
				  					);
				  			}
		      	else if (theTimerInput.getInputArrivedB()) // Try Time-out? 
			    		{ // Time-out occurred.  Retry initiating connection.
		    			  requestSiblingStateListV(theTryingToConnectState);
			      		}
					  }

				public void onExitV() throws IOException
				  // Cancels timer.
				  { 
						theTimerInput.cancelingV();
						super.onExitV();
						}

		  	} // class UnconnectedWaitingState

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
