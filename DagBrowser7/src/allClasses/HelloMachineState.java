package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Timer;

public class HelloMachineState

	extends OrState
	
	/* This class has not been tested and is not being used.
	  After its creation it was decided that its sub-states
	  should be moved [incrementally] to Unicaster and tested there
	  before being made into its own class, if ever.  //?  

	  This class contains a [hierarchical] state machine 
	  that processes the HELLO handshake that is supposed to happen
	  at the beginning of a Unicaster connection.
	  */

	{	
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong retransmitDelayMsNamedLong;
		@SuppressWarnings("unused")  ////
		private Timer theTimer; 
		private Unicaster theUnicaster;
		
		// Sub-state-machine instances.
		private BeforeHelloExchangedState theBeforeHelloExchangedState;
		private AfterHelloExchangedState theAfterHelloExchangedState;
		private GoodbyesSentState theGoodbyesSentState;
		
		HelloMachineState(  // Constructor.
				)
			throws IOException
	  	{
			  }
			
	  public synchronized StateList initializeWithIOExceptionHelloMachineState(
					Timer theTimer, 
				  NetcasterInputStream theNetcasterInputStream,
					NetcasterOutputStream theNetcasterOutputStream,
					NamedLong retransmitDelayMsNamedLong,
					Unicaster theUnicaster
		  		)
				throws IOException
		  {
	  		super.initializeWithIOExceptionStateList();

  	  	// Injected dependencies.
			  this.theTimer= theTimer;
			  this.theNetcasterInputStream= theNetcasterInputStream;
			  this.theNetcasterOutputStream= theNetcasterOutputStream;
			  this.retransmitDelayMsNamedLong= retransmitDelayMsNamedLong;
				this.theUnicaster= theUnicaster;

	  		// Adding measurement count.

    		// Create and add to DAG the sub-states of this state machine.
    		initAndAddStateListV(
    				theBeforeHelloExchangedState= new BeforeHelloExchangedState());
    		initAndAddStateListV(
    				theAfterHelloExchangedState= new AfterHelloExchangedState());
    		initAndAddStateListV(
    				theGoodbyesSentState= new GoodbyesSentState());
    		setFirstOrSubStateV( theBeforeHelloExchangedState ); // Initial state.

	  	  helloTimerInput= // Creating our timer and linking to this state. 
			  		new TimerInput(  ///? Move to factory or parent?
			  				theTimer,
			  				this
			  				);

	  	  return this;
			  }
	
	  public synchronized void finalizeV() throws IOException
	    // This method processes any pending loose ends before shutdown.
		  {
	  	  super.finalizeV();
	  		///elim onInputsB(); // This throws any saved IOException from timer.
	  		helloTimerInput.cancelingV(); // To stop our timer.
	      }

  	public void onEntryV() throws IOException
		  { 
			  retryTimeOutMsL=   // Initializing retry time-out.
			  		retransmitDelayMsNamedLong.getValueL();
				}

	  public boolean onInputsB() throws IOException 
	    /* This input handler method is mainly concerned with
	      disconnecting its Unicaster from the one running on the peer node.
	      It processes the GOODBYE message by initiating thread termination.
	      It responding to the thread termination signal by
	      sending GOODBYEs to the peer.
	      In both cases, it transitions the sub-state machine
	      to the the GoodbyesSentState.
	     */
		  {
	  		boolean progressB= true; // Assume progress will be made.
	  		beforeReturn: {
		  		if ( super.onInputsB() ) // Try processing in sub-state machine.
		  			break beforeReturn; // Return with progress.
		  		if ( tryInputB("GOODBYE") ) { // Did peer tell us it's disconnecting?
	          Thread.currentThread().interrupt(); // Yes, initiate our disconnect.
	        	requestSubStateListV( theGoodbyesSentState );
		  			break beforeReturn; // Return with progress.
		  			}
		  		if // Process local disconnect request, if present.
		  		  ( Thread.currentThread().isInterrupted() ) 
			  		{ // Process local disconnect request.
		        	Thread.currentThread().interrupt(); // Reestablish interrupted.
		        	progressB= requestSubStateListB( theGoodbyesSentState );
		        	if (progressB) // Send GOODBYEs if state change was accepted.
				  			for (int i=0; i<3; i++) { // Send 3 GOODBYE packets.
				  		    theNetcasterOutputStream.writingTerminatedStringV( "GOODBYE" );
				  				theNetcasterOutputStream.sendingPacketV(); // Forcing send.
				  				}
			  			break beforeReturn; // Return with the progress calculated above.
			  			}
		  		progressB= false; // If we got this far, everything failed.  Override.
		  		} /// beforeReturn: 
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

		private class BeforeHelloExchangedState extends StateList 

	  	/* This class exchanges HELLO messages with the remote peer
	  	  to decide which peer will lead and which will follow.
	  	  It retries using exponential back-off 
	  	  until the acknowledgement is received.
			  */

	  	{

	    	public void onEntryV() throws IOException
		  	  // Sends a HELLO and initializes retry timer.
		  	  {
		    		newSendHelloV(this);
					  helloTimerInput.scheduleV(retryTimeOutMsL);
						}
		
			  public void onInputsV() throws IOException
			  	/* This method handles HELLO handshakes acknowledgement, 
			  	  initiating a retry using twice the time-out,
			  	  until a HELLO is received.
			  	  */
			  	{
			  		if // Try to process first HELLO.
			  		  (newTryProcessingReceivedHelloB(this))
			  			requestSiblingStateListV( // Success.  Request after-hello state.
			  					theAfterHelloExchangedState
					  			);
		      	else if (helloTimerInput.getInputArrivedB()) // Failure.  Time-out? 
			    		{ // Time-out occurred  Setup for retry.
			      		{ // Adjust retry time-delay.
			      		  retryTimeOutMsL*=2;  // Doubling time-out limit.
				    			if // but don't let it be above maximum.
			      				( retryTimeOutMsL > Config.maxTimeOut5000MsL )
			    			  	retryTimeOutMsL= Config.maxTimeOut5000MsL;
			      			}
		    			  requestSiblingStateListV(this); // Now retry using this state again.
		  			  	}
		  	  	}
	
		  		} // class BeforeHelloExchangedState
		
		private class AfterHelloExchangedState extends StateList

	  	/* This state handles the reception of extra HELLO messages,
	  	  which are HELLO messages received after the first one.
	  	  Extra HELLO messages means that the remote peer
	  	  did not receive the HELLO or HELLOs sent by us earlier.
	  	  */

	  	{
				private boolean sentHelloB= true; 
				  // True means previous HELLO was sent, not received.

			  public void onInputsV() throws IOException
			  	/* This method sends HELLO messages 
			  	  in response to extra unnecessary received HELLO messages.
			  	  To prevent HELLO storms, response is made to only
			  	  every other received HELLO. 
			  	  */
			  	{
			  		if // Try to process an extra HELLO.
			  		  (newTryProcessingReceivedHelloB(this))
				  		{
				        appLogger.warning( "Extra HELLO received." );
				  			if  // If we received a HELLO and 
				  			  ( sentHelloB^= true ) // we didn't send one last time,
				  				newSendHelloV(this); // send a HELLO this time.
				  			}
					  }

	  		} // class AfterHelloExchangedState
		
		private class GoodbyesSentState extends StateList

	  	/* This state is a termination state, entered after
	  	  GOODBYE has been sent and the Unicaster is disconnected.
	  	  It needs no methods because it needs to do nothing.
	  	  */

	  	{
	  		} // class GoodbyesSentState

  	private boolean newTryProcessingReceivedHelloB(
  			  StateList subStateList) 
  			throws IOException
  	  /* This method tries to process the Hello message and its arguments.
  	    Because it is called not by this state, but by sub-states,
  	    it is specified as the parameter subStateList.
  	    If the next input String is "HELLO" then it processes,
  	    which means parsing the IP address which follows and determining
  	    which peer, the local or remote, will be leader,
  	    by comparing the IP addresses, and setting 
  	    the value for leadingDefaultBooleanLike.
				Note, the particular ordering of IP address Strings doesn't matter.  
				What matters is that the ordering is consistent.
  	    This method does not send a reply "HELLO".
  	    Sending is assumed to be done elsewhere.
  	    This method returns true if HELLO was processed, false otherwise.
  	    */
	  	{
  		  boolean isKeyB= subStateList.tryInputB("HELLO");
  		  if (isKeyB) { // Decoding argument if input is "HELLO".
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
  		  return isKeyB;
	  		}

  	private void newSendHelloV(StateList subStateList)
  			throws IOException
  	  /* This method sends a HELLO message to the remote peer
  	    from state subStateList, and logs that it has done so.
  	    */
	  	{
		    appLogger.debug( 
		    		"HelloMachineState.newSendHelloV(..) sending HELLO from"
		  			+ subStateList.getFormattedStatePathString()
		  			);
		    theNetcasterOutputStream.writingTerminatedStringV( "HELLO" );
		    theNetcasterOutputStream.writingTerminatedStringV( 
						theUnicaster.getKeyK().getInetAddress().getHostAddress() 
						);  // Writing other peer's IP address.
		    theNetcasterOutputStream.sendingPacketV(); // Forcing send.
	  		}

		} // class HelloMachineState
