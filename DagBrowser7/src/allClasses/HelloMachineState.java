package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Timer;

//% import allClasses.LinkMeasurementState.LocalMeasurementState.MeasurementHandshakesState;
//% import allClasses.LinkMeasurementState.LocalMeasurementState.MeasurementInitializationState;

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
		private AwaitingHellosExchangedState
		  theAwaitingHellosExchangedState;
		//// @SuppressWarnings("unused")
		private HandlingHellosReceivedLateState
		  theHandlingHellosReceivedLateState;
		
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
			
	  public synchronized State initializeWithIOExceptionState() 
				throws IOException
		  {
	  		super.initializeWithIOExceptionState();

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

	  public void stateHandlerV() throws IOException {}  ////// NOP to prevent action.
  	// The default OrState.stateHandlerV() is used.

		public void exitV() throws IOException
		  // Cancels acknowledgement timer.
		  { 
				helloTimerInput.cancelingV();
				}

		// Other variables.
	  private TimerInput helloTimerInput;
		private long retryTimeOutMsL;

		private class AwaitingHellosExchangedState extends State 
	  	{
	    	public void enterV() throws IOException
	  	  // Initiates the handshake and initialize retry timer.
	  	  { 
	        appLogger.info( "AwaitingHellosExchangedState sending HELLO." );
	        theNetcasterOutputStream.writingTerminatedStringV( "HELLO" );
	        theNetcasterOutputStream.writingTerminatedStringV(  // Writing other peer's IP address. 
		  				theUnicaster.getKeyK().getInetAddress().getHostAddress() 
		  				);
	        theNetcasterOutputStream.sendingPacketV(); // Forcing send.
				  helloTimerInput.scheduleV(retryTimeOutMsL);
					}
		
			  public void stateHandlerV() throws IOException
			  	/* This method handles handshakes acknowledgement, 
			  	  initiating a retry using twice the time-out,
			  	  until the acknowledgement is received,
			  	  or giving up if the time-out limit is reached.
			  	  */
			  	{
					  if (tryInputB("HELLO")) // Try to process HELLO.
					  	requestStateV( // Success.  Go handle any straggler HELLOs.
					  			theHandlingHellosReceivedLateState
					  			);
		      	else if (helloTimerInput.getInputArrivedB()) // Failure.  Time-out? 
			    		{ // Yes.  Setup for retry.
		      		  retryTimeOutMsL*=2;  // Doubling time-out limit.
			    			if // but don't let it stay above maximum.
		      				( retryTimeOutMsL > Config.maxTimeOut5000MsL )
		    			  	retryTimeOutMsL= Config.maxTimeOut5000MsL;
		    			  requestStateV(this); // Retrying by requesting this same state.
		  			  	}
		  	  	}
	
		  		} // class AwaitingHellosExchangedState
		
		private class HandlingHellosReceivedLateState extends State 
	  	/* This state handles the PS-PA handshakes, 
	  	  including sending the first packet, 
	  	  processing the acknowledgement, and
	  	  retrying using a time-doubling exponential back-off.
				It ends by requesting the MeasurementPausedState.
	  	  */

	  	{
			  
	  		} // class HandlingHellosReceivedLateState 
  	
		@SuppressWarnings("unused") //////
  	private boolean tryHelloB() 
  			throws IOException
  	  /* This method tries to process the Hello message and its arguments.
  	    If keyString is "HELLO" then it processes,
  	    which means parsing the IP address which follows and determining
  	    which peer, the local or remote, will be leader,
  	    by comparing the IP addresses, and setting 
  	    the value for leadingDefaultBooleanLike.
  	    It does not send a reply "HELLO".  
  	    This is assumed to have been done simultaneously elsewhere.
  	    It returns true if HELLO was processed, false otherwise.
  	    The first time this method is called is the only one that counts.
  	    Later calls might produce redundant retransmissions, 
  	    but should have no other effect.
  	   */
	  	{
  		  boolean isKeyB= tryInputB("HELLO");
  		  if (isKeyB) { // Decoding argument if key is "HELLO".
					String localIpString= theNetcasterInputStream.readAString();
					String remoteIpString= 
							theUnicaster.getKeyK().getInetAddress().getHostAddress();
					theUnicaster.leadingDefaultBooleanLike.setValueB( 
							localIpString.compareTo(remoteIpString) > 0 
							);
					//leadingDefaultBooleanLike= !leadingDefaultBooleanLike; // Reverse roles for debug test. 
					// Note, the particular ordering of IP address Strings
					// doesn't matter.  What matters is that the ordering is consistent.
					////////theSubcasterManager.setLeadingV( leadingDefaultBooleanLike );
	        appLogger.info( 
	        		"HELLO received.  Overriding role to be: "
	        		////////+ (leadingDefaultBooleanLike.getValueB() ? "LEADER" : "FOLLOWER")
	        		);
				  }
  		  return isKeyB;
	  		}

		} // class LinkMeasurementState
