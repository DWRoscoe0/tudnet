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
		private Timer theTimer; ///elim.  use function parameter only. 
		private Unicaster theUnicaster;
		private StateList[] theLinkedStateLists;

		// Sub-state-machine instances.
		private ConnectingState theConnectingState;
		private ConnectedState theConnectedState;
		private UnconnectedState theUnconnectedState;
		
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

	  		// Adding measurement count.

    		// Create and add to DAG the sub-states of this state machine.
    		initAndAddStateListV(theConnectingState= new ConnectingState());
    		addStateListV(
    				(theConnectedState= new ConnectedState())
    				  .initializeWithIOExceptionStateList(this.theLinkedStateLists)
    				);
    		initAndAddStateListV(theUnconnectedState= new UnconnectedState());
    		setFirstOrSubStateV( theConnectingState ); // Set initial state.

	  	  helloTimerInput= // Creating our timer and linking to this state. 
			  		new TimerInput(  ///? Move to factory or parent?
			  				this.theTimer,
			  				this
			  				);

	  	  /*  ///dbg
	  	  { ///dbg ///tmp adjust logging for debugging.
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
	  		///elim? onInputsB(); // This rethrows any saved IOException from timer.
	  		helloTimerInput.cancelingV(); // To stop our timer.
	      }

  	public void onEntryV() throws IOException
		  { 
  			//elim propagateIntoSubtreeV( AppLog.LogLevel.TRACE );
  			///dbg appLogger.warning( "LinkedMachineState.onEntryV() state being used." );
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
	      to the the UnconnectedState.
	     */
		  {
	  		boolean progressB= true; // Assume progress will be made.
	  		beforeReturn: {
		  		if ( super.onInputsB() ) // Try processing in sub-state machine.
		  			break beforeReturn; // Return with progress.
		  		if // Process local disconnect request, if present.
		  		  ( Thread.currentThread().isInterrupted() ) 
			  		{ // Process local disconnect request.
		        	Thread.currentThread().interrupt(); // Reestablish interrupted.
		        	progressB= requestSubStateListB( theUnconnectedState );
		        	if (progressB) // Send GOODBYEs if state change was accepted.
				  			for (int i=0; i<3; i++) { // Send 3 GOODBYE packets.
				  		    theNetcasterOutputStream.writingTerminatedStringV( "GOODBYE" );
				  				theNetcasterOutputStream.sendingPacketV(); // Forcing send.
				  				}
			  			break beforeReturn; // Return with the progress calculated above.
			  			}
		  		progressB= false; // If we got this far, everything failed.  Override.
		  		} // beforeReturn: 
		  	return progressB;
		  	}

		public void onExitV() throws IOException
		  // Cancels acknowledgement timer.
		  { 
				helloTimerInput.cancelingV();
				super.onExitV();
				}

		// Other variables.
	  private TimerInput helloTimerInput;
		private long retryTimeOutMsL;
		
		public boolean isConnectedB()
		  /* This method returns true if this Unicaster is connected to its peer,
		    false otherwise.
		   	*/
			{
				return (getpresentSubStateList() == theConnectedState) ;
				}

		private class ConnectingState extends StateList 

	  	/* This class exchanges HELLO messages with the remote peer
	  	  to decide which peer will lead and which will follow.
	  	  It retries using exponential back-off 
	  	  until the acknowledgement is received.
			  */

	  	{

	    	public void onEntryV() throws IOException
		  	  // Sends a HELLO and initializes retry timer.
		  	  {
		    		sendHelloV(this);
					  helloTimerInput.scheduleV(retryTimeOutMsL);
						}
		
			  public void onInputsForLeafStatesV() throws IOException
			  	/* This method handles HELLO handshakes acknowledgement, 
			  	  initiating a retry using twice the time-out,
			  	  until a HELLO is received.
			  	  */
			  	{
			  		if // Try to process first HELLO.
			  		  (tryReceivingHelloB(this))
			  			requestSiblingStateListV( // Success.  Request after-hello state.
			  					theConnectedState
					  			);
		      	else if (helloTimerInput.getInputArrivedB()) // Failure.  Time-out? 
			    		{ // Time-out occurred  Setup for retry.
			      		{ // Adjust retry time-delay.
			      		  retryTimeOutMsL*=2;  // Doubling time-out limit.
				    			if // but don't let it be above maximum.
			      				( retryTimeOutMsL > Config.maxTimeOut5000MsL )
			    			  	retryTimeOutMsL= Config.maxTimeOut5000MsL;
			      			}
		    			  requestSiblingStateListV(this); // Now retry, reusing this state.
		  			  	}
		  	  	}
	
		  		} // class ConnectingState
		
		private class ConnectedState extends AndState

	  	/* This state handles the reception of extra HELLO messages,
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
								///dbg theStateList.propagateIntoSubtreeB( LogLevel.INFO ); ///dbg /// tmp
								}
						
						return this;
						}
			
				private boolean sentHelloB= true; 
				  // True means previous HELLO was sent, not received.

	    	public void onEntryV() throws IOException
		  	  /* Informs TCPClient about new connection,
		  	    which means a possible TCPServer.
		  	    */
		  	  {
	    			IPAndPort remoteIPAndPort= theUnicaster.getKeyK();
		    		theTCPClient.reportPeerConnectionV(remoteIPAndPort);

		  	  	super.onEntryV();
		  	  	}

			  public boolean onInputsB() throws IOException
			  	/* This method sends HELLO messages 
			  	  in response to extra unnecessary received HELLO messages.
			  	  To prevent HELLO storms, response is made to only
			  	  every other received HELLO.
			  	  It also handles the GOODBYE message by disconnecting. 
			  	  */
			  	{
			  		boolean progressB= true; // Assume progress will be made.
			  		beforeReturn: {
				  		if ( super.onInputsB() ) // Try processing in sub-state machine.
				  			break beforeReturn; // Return with progress.
				  		if (tryReceivingHelloB(this)) { // Try to process an extra HELLO.
					        appLogger.warning( "Extra HELLO received." );
					  			if  // If we received a HELLO and 
					  			  ( sentHelloB^= true ) // we didn't send one last time,
					  				sendHelloV(this); // send a HELLO this time.
					  			break beforeReturn; // Return with progress.
					  			}
				  		if ( tryInputB("GOODBYE") ) { // Did peer disconnect?
		    			  	requestSiblingStateListV( theUnconnectedState ); // Us also.
					  			break beforeReturn; // Return with progress.
					  			}
				  		progressB= false; // Everything failed.  Set no progress.
			  			} // beforeReturn: 
			  		return progressB;
					  }

	  		} // class ConnectedState
		
		private class UnconnectedState extends StateList

	  	/* This state is a termination state, entered after
	  	  GOODBYE has been sent and the Unicaster is disconnected.
	  	  It needs no methods because it needs to do nothing.
	  	  */

	  	{

			  public void onInputsForLeafStatesV() throws IOException
			  	/* This method does nothing except test for HELLO messages.
			  	  If it receives one then it transitions to the ConnectedState. 
			  	  */
			  	{
			  		if (tryReceivingHelloB(this))
				  		{
			  				sendHelloV(this); // send a response HELLO.
				  			requestSiblingStateListV( // Switch to ConnectedState.
				  					theConnectedState
				  					);
				  			}
					  }
	
		  	} // class UnconnectedState

  	private boolean tryReceivingHelloB(StateList subStateList) 
  			throws IOException
  	  /* This method tries to process the Hello message and its arguments.
  	    This method is part of this state, but is not called its code.
  	    It is called by various sub-states. 
  	    specified with the parameter subStateList.
  	    If the next input to the sub-state is "HELLO" then it processes it,
  	    which means parsing the IP address which follows and determining
  	    which peer, the local or remote, will be leader,
  	    by comparing the IP addresses, and setting 
  	    the value for leadingDefaultBooleanLike.
				Note, the particular ordering of IP address Strings doesn't matter.  
				What matters is that the ordering is consistent.
  	    This method does not send a reply "HELLO".
  	    Sending is assumed to be done elsewhere.
  	    This method returns true if HELLO was received and processed, 
  	    false otherwise.
  	    */
	  	{
  		  boolean gotKeyB= subStateList.tryInputB("HELLO");
  		  if (gotKeyB) { // Decoding argument if input is "HELLO".
					String localIpString= theNetcasterInputStream.readAString();
					String remoteIpString= 
							theUnicaster.getKeyK().getInetAddress().getHostAddress();
					theUnicaster.leadingDefaultBooleanLike.setValueB( // Decide who leads.
							localIpString.compareTo(remoteIpString) > 0 
							);
					//leadingDefaultBooleanLike= !leadingDefaultBooleanLike; // Reverse roles for debug test. 
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
  	    */
	  	{
		    //// appLogger.debug( ///dbg 
				//// 		"LinkedMachineState.sendHelloV(..) sending HELLO from"
				//// 			+ subStateList.getFormattedStatePathString()
				//// 		);
		    theNetcasterOutputStream.writingTerminatedStringV( "HELLO" );
		    theNetcasterOutputStream.writingTerminatedStringV( 
						theUnicaster.getKeyK().getInetAddress().getHostAddress() 
						);  // Writing other peer's IP address.
		    theNetcasterOutputStream.sendingPacketV(); // Forcing send.
	  		}

		} // class LinkedMachineState
