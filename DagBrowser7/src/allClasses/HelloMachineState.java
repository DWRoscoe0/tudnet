package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Timer;

public class HelloMachineState

	extends AndState
	
	/* This class contains a [hierarchical] state machine 
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
		private ProcessingFirstHelloState
		  theProcessingFirstHelloState;
		//// @SuppressWarnings("unused")
		private ProcessingLaterHellosState
		  theProcessingLaterHellosState;
		
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
			  		new TimerInput(  //// Move to factory or parent?
			  				theTimer,
			  				this
			  				);

	  	  return this;
			  }
	
	  public synchronized void finalizeV() throws IOException
	    // This method processes any pending loose ends before shutdown.
		  {
	  	  super.finalizeV();
	  		stateHandlerB(); // This throws any saved IOException from timer.
	  		helloTimerInput.cancelingV(); // To stop our timer.
	      }
		
  	public void enterV() throws IOException
		  { 
			  retryTimeOutMsL=   // Initializing retry time-out.
			  		retransmitDelayMsNamedLong.getValueL();
				}

	  public void stateHandlerV() throws IOException {}  ////// temp. NOP to prevent action.
  	// The default OrState.stateHandlerV() is used.

		public void exitV() throws IOException
		  // Cancels acknowledgement timer.
		  { 
				helloTimerInput.cancelingV();
				}

		// Other variables.
	  private TimerInput helloTimerInput;
		private long retryTimeOutMsL;

		private class ProcessingFirstHelloState extends StateList 

	  	/* This class exchanges HELLO messages with the remote peer
	  	  to decide which peer will lead and which will follow.
	  	  It retries using exponential back-off 
	  	  until the acknowledgement is received.
			  */

	  	{

	    	public void enterV() throws IOException
		  	  // Sends a HELLO and initializes retry timer.
		  	  {
		    		sendHelloV();
					  helloTimerInput.scheduleV(retryTimeOutMsL);
						}
		
			  public void stateHandlerV() throws IOException
			  	/* This method handles handshakes acknowledgement, 
			  	  initiating a retry using twice the time-out,
			  	  until the acknowledgement is received.
			  	  */
			  	{
			  		if (tryProcessingReceivedHelloB()) // Try to process first HELLO.
			  			requestStateListV( // Success.  Go handle any later HELLOs.
					  			theProcessingLaterHellosState
					  			);
		      	else if (helloTimerInput.getInputArrivedB()) // Failure.  Time-out? 
			    		{ // Time-out occurred  Setup for retry.
			      		{ // Adjust retry time-delay.
			      		  retryTimeOutMsL*=2;  // Doubling time-out limit.
				    			if // but don't let it be above maximum.
			      				( retryTimeOutMsL > Config.maxTimeOut5000MsL )
			    			  	retryTimeOutMsL= Config.maxTimeOut5000MsL;
			      			}
		    			  requestStateListV(this); // Now retry by requesting this state again.
		  			  	}
		  	  	}
	
		  		} // class ProcessingFirstHelloState
		
		private class ProcessingLaterHellosState extends StateList

	  	/* This state handles the reception of extra HELLO messages,
	  	  which are HELLO messages received after the first one.
	  	  Extra HELLO messages means that the remote peer
	  	  did not receive the HELLO or HELLOs sent by us,
	  	  or a HELLO message from the remote peer was delayed.
	  	  */

	  	{
				private boolean respondB= false; // Flag to prevent HELLO storms.
				
			  public void stateHandlerV() throws IOException
			  	/* This method sends HELLO messages 
			  	  in response to received HELLO messages.
			  	  To prevent HELLO storms, response is made to only
			  	  every other received HELLO. 
			  	  */
			  	{
			  		if (tryProcessingReceivedHelloB()) // Try to process first HELLO.
			  			if  // If we received a HELLO 
			  			  ( respondB^= true ) // and we didn't respond last time
			  				sendHelloV(); // send a response HELLO this time.
					  }

	  		} // class ProcessingLaterHellosState 
		
  	private void sendHelloV()
  			throws IOException
  	  /* This method sends a HELLO message to the remote peer
  	    and logs that it has done so.
  	    */
	  	{
		    appLogger.info( "HelloMachineState sending HELLO." );
		    theNetcasterOutputStream.writingTerminatedStringV( "HELLO" );
		    theNetcasterOutputStream.writingTerminatedStringV(  // Writing other peer's IP address. 
						theUnicaster.getKeyK().getInetAddress().getHostAddress() 
						);
		    theNetcasterOutputStream.sendingPacketV(); // Forcing send.
	  		}
  	
  	private boolean tryProcessingReceivedHelloB() 
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
  	    This is assumed to have been done elsewhere.
  	    This method returns true if HELLO was processed, false otherwise.
  	    This method may be called multiple times, 
  	    but it should produce the same result each time.
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

		} // class LinkMeasurementState
