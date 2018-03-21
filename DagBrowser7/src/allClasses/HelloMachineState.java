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
		private Timer theTimer; 
		private Unicaster theUnicaster;
		
		// Sub-state-machine instances.
		@SuppressWarnings("unused")
		private BeforeHelloExchangeState
		  theBeforeHelloExchangeState;
		private AfterHelloExchangeState
		  theAfterHelloExchangeState;
		
		HelloMachineState(  // Constructor.
				Timer theTimer, 
			  NetcasterInputStream theNetcasterInputStream,
				NetcasterOutputStream theNetcasterOutputStream,
				NamedLong retransmitDelayMsNamedLong,
				Unicaster theUnicaster
				)
			throws IOException
	  	{
  	  	// Injected dependencies.
			  this.theNetcasterInputStream= theNetcasterInputStream;
			  this.theNetcasterOutputStream= theNetcasterOutputStream;
			  this.retransmitDelayMsNamedLong= retransmitDelayMsNamedLong;
			  this.theTimer= theTimer;
			  }
			
	  public synchronized StateList initializeWithIOExceptionStateList() 
				throws IOException
		  {
	  		super.initializeWithIOExceptionStateList();

	  		// Adding measurement count.

    		// Create and add to DAG the sub-states of this state machine.
	
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
	  		onInputsB(); // This throws any saved IOException from timer.
	  		helloTimerInput.cancelingV(); // To stop our timer.
	      }

  	public void onEntryV() throws IOException
		  { 
			  retryTimeOutMsL=   // Initializing retry time-out.
			  		retransmitDelayMsNamedLong.getValueL();
				}

	  public void onInputsV() throws IOException {}  ///tmp NOP to prevent action.
  	// The default OrState.overrideStateHandlerV() will be used.

		public void onExitV() throws IOException
		  // Cancels acknowledgement timer.
		  { 
				helloTimerInput.cancelingV();
				super.onExitV();
				}

		// Other variables.
	  private TimerInput helloTimerInput;
		private long retryTimeOutMsL;

		private class BeforeHelloExchangeState extends StateList 

	  	/* This class exchanges HELLO messages with the remote peer
	  	  to decide which peer will lead and which will follow.
	  	  It retries using exponential back-off 
	  	  until the acknowledgement is received.
			  */

	  	{

	    	public void onEntryV() throws IOException
		  	  // Sends a HELLO and initializes retry timer.
		  	  {
		    		newSendHelloV();
					  helloTimerInput.scheduleV(retryTimeOutMsL);
						}
		
			  public void onInputsV() throws IOException
			  	/* This method handles handshakes acknowledgement, 
			  	  initiating a retry using twice the time-out,
			  	  until a HELLO is received.
			  	  */
			  	{
			  		if (newTryProcessingReceivedHelloB()) // Try to process first HELLO.
			  			requestStateListV( // Success.  Go handle any later HELLOs.
					  			theAfterHelloExchangeState
					  			);
		      	else if (helloTimerInput.getInputArrivedB()) // Failure.  Time-out? 
			    		{ // Time-out occurred  Setup for retry.
			      		{ // Adjust retry time-delay.
			      		  retryTimeOutMsL*=2;  // Doubling time-out limit.
				    			if // but don't let it be above maximum.
			      				( retryTimeOutMsL > Config.maxTimeOut5000MsL )
			    			  	retryTimeOutMsL= Config.maxTimeOut5000MsL;
			      			}
		    			  requestStateListV(this); // Now retry using this state again.
		  			  	}
		  	  	}
	
		  		} // class BeforeHelloExchangeState
		
		private class AfterHelloExchangeState extends StateList

	  	/* This state handles the reception of extra HELLO messages,
	  	  which are HELLO messages received after the first one.
	  	  Extra HELLO messages means that the remote peer
	  	  did not receive the HELLO or HELLOs sent by us earlier.
	  	  */

	  	{
				private boolean ignoreB= false; // True means previous HELLO ignored.

			  public void onInputsV() throws IOException
			  	/* This method sends HELLO messages 
			  	  in response to received HELLO messages.
			  	  To prevent HELLO storms, response is made to only
			  	  every other received HELLO. 
			  	  */
			  	{
			  		if (newTryProcessingReceivedHelloB()) // Try to process another HELLO.
				  		{
				        appLogger.warning( "Extra HELLO received." );
				  			if  // If we received a HELLO 
				  			  ( ignoreB^= true ) // and we ignored it last time
				  				newSendHelloV(); // send a response HELLO this time.
				  			}
					  }

	  		} // class AfterHelloExchangeState 

  	private boolean newTryProcessingReceivedHelloB() 
  			throws IOException
  	  /* This method tries to process the Hello message and its arguments.
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
  		  boolean isKeyB= tryInputB("HELLO");
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

  	private void newSendHelloV()
  			throws IOException
  	  /* This method sends a HELLO message to the remote peer
  	    and logs that it has done so.
  	    */
	  	{
		    appLogger.info( "HelloMachineState sending HELLO." );
		    theNetcasterOutputStream.writingTerminatedStringV( "HELLO" );
		    theNetcasterOutputStream.writingTerminatedStringV( 
						theUnicaster.getKeyK().getInetAddress().getHostAddress() 
						);  // Writing other peer's IP address.
		    theNetcasterOutputStream.sendingPacketV(); // Forcing send.
	  		}

		} // class HelloMachineState
