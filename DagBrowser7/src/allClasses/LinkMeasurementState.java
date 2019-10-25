package allClasses;

import java.io.IOException;
import java.util.Timer;

import static allClasses.AppLog.LogLevel.TRACE;
import static allClasses.AppLog.theAppLog;

public class LinkMeasurementState 

	extends AndState
	
	/* This class contains a [hierarchical] state machine 
	  that measures and displays several performance parameters 
	  of a Unicaster peer-to-peer link, including:
	  * the Round-Trip-Time;
	  * the count of packets sent and received, locally and remotely; and
	  * the packet loss ratios.  

	  This code is not thread-safe.  It is meant to be called only from:
	  * a Unicaster thread, and
	  * the thread belonging to a timer that this code uses.
	 	*/

	{	
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong initialRetryTimeOutMsNamedLong;
		private Timer theTimer; 
		
		// Sub-state machine instances.
		@SuppressWarnings("unused")
		private RemoteMeasurementState theRemoteMeasurementState;
		private LocalMeasurementState theLocalMeasurementState;

		LinkMeasurementState(  // Constructor.
				Timer theTimer, 
			  NetcasterInputStream theNetcasterInputStream,
				NetcasterOutputStream theNetcasterOutputStream,
				NamedLong initialRetryTimeOutMsNamedLong
				)
			throws IOException
	  	{
  	  	// Injected dependencies.
			  this.theNetcasterInputStream= theNetcasterInputStream;
			  this.theNetcasterOutputStream= theNetcasterOutputStream;
			  this.initialRetryTimeOutMsNamedLong= initialRetryTimeOutMsNamedLong;
			  this.theTimer= theTimer;
			  }
			
	  public synchronized StateList initializeWithIOExceptionStateList() 
				throws IOException
	    /* This method is the initializer for LinkMeasurementState.  It:
	      * initializes many variable values.
	      * add some of those variables to the DAG.
	      * creates sub-states and adds them to the DAG.
	      */
		  {
	  		super.initializeWithIOExceptionStateList();
	  		
	  	  measurementTimerInput= // Creating our timer and linking to this state. 
			  		new TimerInput(  ///? Move to factory or parent?
			  				theTimer,
			  				this
			  				);

    		// Create and add to DAG the sub-states of this and-state-machine.
	  	  initAndAddStateListV( theRemoteMeasurementState= 
	  	  		new RemoteMeasurementState() );
	  	  initAndAddStateListV( theLocalMeasurementState= 
	  	  		new LocalMeasurementState() );

	  		// Adding measurement count.
	  	  addAtEndB( measurementHandshakesNamedLong= new NamedLong(
	      		"Measurement-Handshakes", 0 ) );

	  		addAtEndB( initialRetryTimeOutMsNamedLong );

        // Adding the new round trip time trackers.
	      addAtEndB( smoothedRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong(
	      		"Smoothed-Round-Trip-Time (ms)", 
	      		Config.initialRoundTripTime100MsAsNsL ) );
	  	  addAtEndB( smoothedMinRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong(
	      		"Smoothed-Minimum-Round-Trip-Time (ms)", 
	      		Config.initialRoundTripTime100MsAsNsL ) );
	  	  addAtEndB( smoothedMaxRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong(
	      		"Smoothed-Maximum-Round-Trip-Time (ms)", 
	      		Config.initialRoundTripTime100MsAsNsL ) );
	      addAtEndB( rawRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong( 
	      		"Raw-Round-Trip-Time (ms)", 
	      		Config.initialRoundTripTime100MsAsNsL ) );

        // Adding incoming packet statistics children and related trackers.
	  	  newIncomingPacketsSentDefaultLongLike= new DefaultLongLike(0);
	  	  addAtEndB( oldIncomingPacketsSentNamedLong= new NamedLong(
	      		"Incoming-Packets-Sent", 0 ) );
		    addAtEndB( newIncomingPacketsReceivedNamedLong=
		    		theNetcasterInputStream.getCounterNamedLong() );
	  	  oldIncomingPacketsReceivedDefaultLongLike= new DefaultLongLike(0);
	  	  addAtEndB( incomingPacketLossNamedFloat= new NamedFloat(  
	  	  		"Incoming-Packet-Loss", 0.0F ) );
	  	  incomingPacketLossAverager= new LossAverager(
	  	  				oldIncomingPacketsSentNamedLong,
	  	  				oldIncomingPacketsReceivedDefaultLongLike,
	  	  				incomingPacketLossNamedFloat
	  	  				);

	  	  // Adding outgoing packet statistics children and related trackers.
    		newOutgoingPacketsSentNamedLong=
    				theNetcasterOutputStream.getCounterNamedLong(); 
		    addAtEndB( newOutgoingPacketsSentNamedLong );
		    addAtEndB( newOutgoingPacketsSentEchoedNamedLong= new NamedLong(
		    		"Outgoing-Packets-Sent-Echoed", 0 ) ); 
		    oldOutgoingPacketsSentDefaultLongLike= new DefaultLongLike(0);
		    addAtEndB( newOutgoingPacketsReceivedNamedLong= new NamedLong( 
			      "Outgoing-Packets-Received", 0 ) );
	  	  addAtEndB( outgoingPacketLossNamedFloat= new NamedFloat( 
	  	  		"Outgoing-Packet-Loss", 0.0f ) );
	  	  oldOutgoingPacketsReceivedDefaultLongLike= new DefaultLongLike(0);
	  	  outgoingPacketLossLossAverager= new LossAverager(
	  	  		oldOutgoingPacketsSentDefaultLongLike,
	  	  		oldOutgoingPacketsReceivedDefaultLongLike,
	  	  		outgoingPacketLossNamedFloat
	  	  		);

			  return this;
			  }
    
    public void setTargetDisconnectStateV(StateList theBrokenConnectionState) ///org 
      {

        theLocalMeasurementState.theBrokenConnectionState= theBrokenConnectionState; 

        }
	
	  public synchronized void finalizeV() throws IOException
	    // This method processes any pending loose ends before shutdown.
		  {
	  	  super.finalizeV();
	  		onInputsB(); // This throws any saved IOException from timer.
	  		measurementTimerInput.cancelingV(); // To stop our timer.
	      }
		
	  private class LocalMeasurementState extends OrState 
	
		  /* This is the local concurrent/orthogonal sub-state 
		    for doing local measurements of the Unicaster link.  
				It partner is orthogonal sub-state RemoteMeasurementState. 
		    */
	
		  {
	
				// sub-states/machines.
				private MeasurementPausedState 
				  theMeasurementPausedState;
				//// private MeasurementInitializationState
				////   theMeasurementInitializationState;
				private MeasurementHandshakingState
				  theMeasurementHandshakingState;
	
		    private StateList theBrokenConnectionState; // State for when the link breaks. 

		    public StateList initializeWithIOExceptionStateList() throws IOException 
			    {
		    		super.initializeWithIOExceptionStateList();
	
		    		// Create and add orthogonal sub-state machines.
			  	  initAndAddStateListV( theMeasurementPausedState= 
			  	  		new MeasurementPausedState() );
			  	  //// initAndAddStateListV( theMeasurementInitializationState= 
			  	  //// 		new MeasurementInitializationState() );
			  	  initAndAddStateListV( theMeasurementHandshakingState= 
			  	  		new MeasurementHandshakingState() );
	
						//?requestSubStateListV( theMeasurementPausedState ); // Initial state.
			  	  setFirstOrSubStateV( theMeasurementPausedState ); // Initial state.
						return this;
			    	}
	
        
        private boolean tryProcessingOldPacketAcknowledgementB() 
            throws IOException
          {
            int streamPositionI= theNetcasterInputStream.getPositionI();
            boolean successB= tryInputB("PA");
            try {
              if (successB)
                { // Read and ignore the 2 numbers that follow.
                  int sequenceNumberI= theNetcasterInputStream.readANumberI();
                  theNetcasterInputStream.readANumberI();
                  theAppLog.debug( "MeasurementHandshakingState "
                    + "tryProcessingOldPacketAcknowledgementB() ignoring old PA "
                    + sequenceNumberI);
                  }
                }
              catch ( BadReceivedDataException theBadReceivedDataException ) {
                successB= false;
                theAppLog.exception( "MeasurementHandshakingState."
                    + "tryProcessingOldPacketAcknowledgementB() ",
                    theBadReceivedDataException);
                }
            //// appLogger.debug( "MeasurementHandshakingState."
            ////    + "tryProcessingOldPacketAcknowledgementB() successB="+successB);
            if (!successB) // Rewind input stream if input was not acceptable. 
              theNetcasterInputStream.setPositionV(streamPositionI);
            return successB;
            }

		    
		    private boolean tryProcessingExpectedPacketAcknowledgementB() 
						throws IOException
				  /* This input method finishes processing the "PA" sequence number 
				    feedback message, which the remote peer sends
				    in response to receiving an "PS" sequence number message.
				    The "PA", which is assumed to have already been processed,
				    is followed by:
				    * a copy of the sent packet sequence number 
				      received by the remote peer,
				    * the remote peers received packet count.
				    From these two values this method calculates 
				    the packet loss ratio in the remote peer receiver.
				    By having "PA" include both values, 
				    its calculation is RTT-immune.
				    
				    It returns true if it succeeds, false otherwise.
	
						See processSequenceNumberB(..) about "PS", for more information.
						*/
			  	{
		          int streamPositionI= theNetcasterInputStream.getPositionI();
              //// theNetcasterInputStream.bufferLoggerV(
      		     ////     "MeasurementHandshakingState.tryProcessingPacketAcknowledgementB()"
      		     ////     + " before try PA", streamPositionI);
		    	    boolean successB= false;
		    	    boolean gotPAB= false;
		    	  toReturn: {
  		    	  try {
  		    	    gotPAB= tryInputB("PA"); 
  		    	    if (!gotPAB) // If acknowledgement token PA not gotten
  		    	      break toReturn; // exit.
  		    	    //// theNetcasterInputStream.bufferLoggerV(
      		    	////     "MeasurementHandshakingState.tryProcessingPacketAcknowledgementB()"
      		    	////    + " got PA", streamPositionI);
  		    	    long ackReceivedTimeNsL= System.nanoTime();
  				  		int sequenceNumberI=  // Reading echo of sequence #.
  				  				theNetcasterInputStream.readANumberI();
  	            if // Reject out-of-sequence sequence number.  ////
  	              ( sequenceNumberI != lastSequenceNumberSentL )
    	            {
                    //// appLogger.debug( 
        	           ////   "MeasurementHandshakingState.tryProcessingPacketAcknowledgementB() "
          	         ////     + "wrong sequence # "
          	         ////   +sequenceNumberI+" != "+lastSequenceNumberSentL);
                    break toReturn;
    	              }
  				  		int packetsReceivedI=  // Reading packets received.
  				  				theNetcasterInputStream.readANumberI();
  			    		measurementHandshakesNamedLong.addDeltaL(1);
  			        
  			        newOutgoingPacketsSentEchoedNamedLong.setValueL(
  					    		sequenceNumberI + 1); // Convert sequence # to sent packet count.
  				  		newOutgoingPacketsReceivedNamedLong.setValueL(packetsReceivedI);
  				  		outgoingPacketLossLossAverager.recordPacketsReceivedOrLostV(
  				  				newOutgoingPacketsSentEchoedNamedLong,
  				  				newOutgoingPacketsReceivedNamedLong
  								  );
  				  	  calculateRoundTripTimesV(
  				  	  		sequenceNumberI, ackReceivedTimeNsL, packetsReceivedI);
  				  	  successB= true; // Everything succeeded.
  				  	  }
  		    	  catch ( BadReceivedDataException theBadReceivedDataException ) {
                successB= false; ///? needed?
                theAppLog.exception( 
                    "MeasurementHandshakingState.tryProcessingPacketAcknowledgementB() ",
                    theBadReceivedDataException);
  		    	  	}
		    	    } // toReturn:
		    	  if (!successB) // Rewind inputs if any input was not acceptable.
  		    	  {
  		    	    theNetcasterInputStream.setPositionV(streamPositionI);
                if (gotPAB) // If acknowledgement token PA gotten
                  setOfferedInputV("PA"); // restore it as offered input.
                }
		    	  return successB;
		    	  }
				
				// States.
	
		    private class MeasurementPausedState extends StateList
		    
		      /* This class does the pause between measurement handshakes.
		        It finishes by requesting the MeasurementHandshakingState.
		       	*/
			  
			  	{
			    	public void onEntryV() throws IOException
			    	  /* Starts timer for the pause interval before 
			    	    the next handshake.
			    	   	*/
				  	  {
			    			///dbg appLogger.debug( "MeasurementPausedState.onEntryV() ");
			    	  	measurementTimerInput.scheduleV(Config.measurementPauseMsL);
			  				}
	
					  public void onInputsToReturnFalseV()
					    // Waits for the end of the pause interval.
					  	{ 
					  	  if (measurementTimerInput.testInputArrivedB()) // Timer done. 
					  	  	//// requestAncestorSubStateV(theMeasurementInitializationState);
	                requestAncestorSubStateV(theMeasurementHandshakingState);
					  		}
	
						public void onExitV() throws IOException
						  // Cancels timer and initializes the handshake time-out.
						  { 
								measurementTimerInput.cancelingV();
								super.onExitV();
								}
					
			  	} // class MeasurementPausedState
	
		    /*  ////
				private class MeasurementInitializationState extends StateList 
			  	/* This state does nothing but initializes for the handshake,
			  	  then instantly moves on to the MeasurementHandshakingState.
					  
					  ///? This initialization could be moved to either:
					    * enterV() of MeasurementHandshakingState if
					      that state was split into two levels.
					    * exitV() of MeasurementPausedState.
			  	  */
        /*  ////
			  	{
			    	public void onEntryV() throws IOException
			    	  /* Starts timer for the pause interval before 
			    	    the next handshake.
			    	   	*/
        /*  ////
				  	  {
			    			///dbg appLogger.debug( "MeasurementInitializationState.onEntryV() ");
			    	  	//// measurementTimerInput.scheduleV(Config.measurementPauseMsL);
			    	  	  ///opt has no effect?  Delete.
			  				}
			    	
					  public void onInputsToReturnFalseV() throws IOException
					  	{
		    			  //// exponentialRetryTimeOutMsL=   // Initializing retry time-out.
		    			  ////   		initialRetryTimeOutMsNamedLong.getValueL();
	
					  		requestAncestorSubStateV(theMeasurementHandshakingState);
				  	  	}
	
			  		} // class MeasurementInitializationState
        */  ////
				
				private class MeasurementHandshakingState extends StateList 
			  	/* This state handles the PS-PA handshakes, 
			  	  including sending the first packet, 
			  	  processing the acknowledgement, and
			  	  retrying using a time-doubling exponential back-off.
						If acknowledgement succeeds it requests the MeasurementPausedState.
						If the maximum time-out is exceeded it requests the BrokenConnectionState.
			  	  */
	
			  	{
			    	public void onEntryV() throws IOException
			    	  // Initiates the handshake and starts acknowledgement timer.
				  	  { 
                exponentialRetryTimeOutMsL=   // Initializing retry time-out.
                    initialRetryTimeOutMsNamedLong.getValueL();
                //// appLogger.debug( 
                 ////     "MeasurementHandshakingState.onEntryV() exponentialRetryTimeOutMsL="
                 ////     +exponentialRetryTimeOutMsL);
		    			  measurementTimerInput.scheduleV(exponentialRetryTimeOutMsL);
				    		sendingSequenceNumberV();
			  				}
	
					  public void onInputsToReturnFalseV() throws IOException
					  	/* This method handles handshakes acknowledgement.
					  	  initiating a retry using twice the time-out,
					  	  until the acknowledgement is received,
					  	  or giving up if the time-out limit is reached.
					  	  
					  	  ///opt Simplify by using TimerInput.rescheduleB(.) instead.
					  	  */
					  	{
							  //// if (tryInputB("PA")) // Try processing acknowledgement received.
								if ( tryProcessingExpectedPacketAcknowledgementB() ) {
									requestAncestorSubStateV(theMeasurementPausedState);
								  }
								// /*  ////
								else if ( tryProcessingOldPacketAcknowledgementB() ) {
	                //// appLogger.debug( "MeasurementHandshakingState() old PA received");
	                ; // Ignoring it.
								  }
								// */  ////
		            else if // Try handling time-out?
		              (measurementTimerInput.testInputArrivedB())
                  //// (testAndLogIfTrueB(measurementTimerInput.testInputArrivedB(),
                   ////   "exponential PA retry receive time-out,"))
				      		//// (measurementTimerInput.getInputArrivedB()) // Time-out happened? 
					    		{ // Process time-out.
		                theAppLog.info("MeasurementHandshakingState "
		                    + "exponential PA receive time-out for PS "
		                    + lastSequenceNumberSentL);
								    if ( exponentialRetryTimeOutMsL <= Config.maxTimeOutMsL )
				    				  { exponentialRetryTimeOutMsL*=2;  // Doubling time-out limit.
  			                measurementTimerInput.scheduleV(exponentialRetryTimeOutMsL);
  			                sendingSequenceNumberV();
				    				  	//// requestAncestorSubStateV(this); // Retrying by repeating state.
  	                   } 
				    			  else // Giving up after maximum time-out reached.
				    			  { // Trigger breaking of connection.
                      theAppLog.info("MeasurementHandshakingState time-out.");
				    			    requestAncestorSubStateV(theBrokenConnectionState);
					    		      // Break the connection.
				    			    }
  					    		}
				  	  	}
					  
			  		} // class MeasurementHandshakingState 
	
				public void onExitV() throws IOException
				  // Cancels acknowledgement timer.
				  { 
						measurementTimerInput.cancelingV();
						super.onExitV();
						}
			
			  protected void sendingSequenceNumberV() throws IOException
			    /* This method, at the beginning of the handshake,
			      increments and writes the packet ID (sequence) number 
			      to the EpiOutputStream.  
			      
			      ///? It doesn't flush().
			      */
			    {
			    	lastSequenceNumberSentL= 
			    			theNetcasterOutputStream.getCounterNamedLong().getValueL(); 
			      ///dbg appLogger.info( 
				    ///dbg 		"sendingSequenceNumberV() " + lastSequenceNumberSentL
				    ///dbg 	);
			      theNetcasterOutputStream.writingTerminatedStringV( "PS" );
			      theNetcasterOutputStream.writingTerminatedLongV( 
			      		lastSequenceNumberSentL 
			      		);
			      theNetcasterOutputStream.sendingPacketV();
			  		sentSequenceNumberTimeNsL= System.nanoTime();
			      }
			  
				// Other support code.
				
				private void calculateRoundTripTimesV(
						int sequenceNumberI, long ackReceivedTimeNsL, int packetsReceivedI
						)
				  /* This method calculates round-trip-time.
				    It is called when a PA message is received.
				    */
					{
						String rttString= "";
						if ( sequenceNumberI != lastSequenceNumberSentL )
							rttString+= "unchanged, wrong number."; // Ignore if out of sequence.
							else
							{ 
								long rawRoundTripTimeNsL= 
										ackReceivedTimeNsL - sentSequenceNumberTimeNsL; 
					  	  processRoundTripTimeV(rawRoundTripTimeNsL);
				  			rttString+= ""+
			        		rawRoundTripTimeNsAsMsNamedLong.getValueString()+","+
			        		smoothedMinRoundTripTimeNsAsMsNamedLong.getValueString()+
			        		","+
			        		smoothedMaxRoundTripTimeNsAsMsNamedLong.getValueString()
					  			;
								}
					  if (theAppLog.logB(TRACE)) theAppLog.logV(
				  		TRACE,
				  		"calculateRoundTripTimesV(...) PA:"
							  +sequenceNumberI+","
					    	+packetsReceivedI+";RTT="
					    	+rttString
						  );
				  	}
				
				private void processRoundTripTimeV(long rawRoundTripTimeNsL)
				  /* This method updates various variables which
				    are dependent on round trip times.
				    This include retransmit delay time-out values.
				    
				    Integer arithmetic is used for speed.
				    */
					{
				  	rawRoundTripTimeNsAsMsNamedLong.setValueL(
				    		rawRoundTripTimeNsL
				    		);
				
				  	smoothedRoundTripTimeNsAsMsNamedLong.setValueL(
				  			( (7 * smoothedRoundTripTimeNsAsMsNamedLong.getValueL()) + 
				  				rawRoundTripTimeNsL
				  				)
				  			/ 
				  			8
				  			);
				
				  	{ /*  Smoothed maximum and minimum RTT are calculated here.
					      More extreme values are stored immediately.
					      A less extreme value is exponentially smoothed in.
					      The smoothing factor is presently 1/8.
					      */
					  	{ // Updating minimum round trip time.
					  	  long minL= 
					  	  		smoothedMinRoundTripTimeNsAsMsNamedLong.getValueL();
					  	  if ( minL > rawRoundTripTimeNsL )
					  	  	minL= rawRoundTripTimeNsL;
					  	  	else
					  	  	minL= minL + ( ( rawRoundTripTimeNsL - minL ) + 7 ) / 8;
					  	  smoothedMinRoundTripTimeNsAsMsNamedLong.setValueL(minL);
					  		}
					  	{ // Updating maximum round trip time.
					  	  long maxL= 
					  	  		smoothedMaxRoundTripTimeNsAsMsNamedLong.getValueL();
					  	  if ( maxL < rawRoundTripTimeNsL )
					  	  	maxL= rawRoundTripTimeNsL;
					  	  	else
					  	  	maxL= maxL - ( ( maxL - rawRoundTripTimeNsL ) + 7 ) / 8;
				  	  	smoothedMaxRoundTripTimeNsAsMsNamedLong.setValueL(maxL);
					  		}
				  		}
				
				  	initialRetryTimeOutMsNamedLong.setValueL(
								(smoothedRoundTripTimeNsAsMsNamedLong.getValueL()/1000000) * 2
								); // Use double present maximum round-trip-time as initial time-out.
						}
		
		  	} // class LocalMeasurementState
	
		private class RemoteMeasurementState extends StateList 
		
			{
	
			  /* This is a concurrent/orthogonal sub-state 
			    which helps the remote peer make measurements.
			    It does this by responding with "PA" messages to 
			    "PS" messages sent from the remote peer.
			    It also records some information that is displayed locally.
			    This state doesn't have any sub-states of its own.
			    It exists mainly to document its role along with
			    its orthogonal partner sub-state named LocalMeasurementState.
			    */
	
			  public void onInputsToReturnFalseV() throws IOException
			  	/* This method processes the "PS" message if it is received.
			  	  It does this forever.  This state is never inactive.
			  	  */
			  	{
					  if (tryInputB("PS" )) // Test and process PS message.
						  processPacketSequenceNumberB(); // Process PS message body.

					  // Staying in same state, whether message is processed or not.
						}
	
		    private boolean processPacketSequenceNumberB() 
						throws IOException
				  /* This method finishes processing the "PS" message.
				    The "PS" header is assumed to have been processed already.
				    It is followed by the packet sequence number which comes from 
				    the remote peer EpiOutputStreamO packet count.
				    From this and the local EPIInputStreamI packet count it 
				    calculates a new value of the 
				    local peer's incoming packet loss ratio.
				    This ratio is accurate when calculated because
				    the values it uses in the calculation are synchronized.
	
				    This method also sends an "PA" message back with the same 
				    numbers so the remote peer can calculate the same ratio, which 
				    for the remote peer is called the outgoing packet loss ratio.
				    By sending and using both numbers the remote peer's calculation 
				    is not affected by variations in Round-Trip-Time (RTT).

						If it succeeds then it returns true, otherwise false.

				    Every packet has a sequence number, but 
				    not every packet needs to contain its sequence number.
				    The reception of a "PS" message with its sequence number 
				    means that the remote has sent at least that number of packets.
				    The difference of the highest sequence number received and
				    the number of packets received is the number of packets lost.
				    A new difference and a loss ratio average can be calculated
				    each time a new sequence number is received.  In fact,
				    that is how reception of a sequence number can be interpreted.
				
						///? Sequence numbers and other numbers eventually 
						  need to be converted to use modulo (wrap-around) arithmetic.
					  */
			  	{
		    	  boolean successB= true;
		    	  try {
						  int sequenceNumberI=  // Reading # from packet.
						  		theNetcasterInputStream.readANumberI();
						  newIncomingPacketsSentDefaultLongLike.setValueL( // Recording.
									sequenceNumberI + 1
									); // Adding 1 converts sequence # to remote packet count.
							incomingPacketLossAverager.recordPacketsReceivedOrLostV(
									newIncomingPacketsSentDefaultLongLike,
									newIncomingPacketsReceivedNamedLong
							  );
							theNetcasterOutputStream.writingTerminatedStringV( "PA" );
							theNetcasterOutputStream.writingTerminatedLongV(
				  				sequenceNumberI // The remote sequence number.
				  				);
							long receivedPacketCountL= 
							  newIncomingPacketsReceivedNamedLong.getValueL(); 
							theNetcasterOutputStream.writingTerminatedLongV(
									receivedPacketCountL  // The local received packet count.
				  				);
							theNetcasterOutputStream.sendingPacketV();
							  // Sending now for minimum RTT.
						  if (theAppLog.logB(TRACE)) theAppLog.logV(
					  		TRACE,
					  		"processPacketSequenceNumberB(..) PS:"
					  		  +sequenceNumberI+","
					      	+receivedPacketCountL
				  		  );
	    	  		}
		    	  catch ( BadReceivedDataException theBadReceivedDataException ) {
		    	  	successB= false; 
	    	  		}
		    	  return successB;
						}
			
			} // class RemoteMeasurementState

		
	  // Variables for managing incoming packets and 
	  // their sequence numbers.  They all start at 0.
	  private DefaultLongLike newIncomingPacketsSentDefaultLongLike;
	  	// This is set to the value of the sequence number argument of the
		  // most recently received "PS" message plus 1.  
		  // When sent this argument was the remote end's 
		  // EpiOutputStream packet counter.
	  	// This value usually increases, but can decrease 
	    // if an "PS" is carried by an out-of-order packets.
	  private NamedLong oldIncomingPacketsSentNamedLong;
	  	// A difference between this and newIncomingPacketsSentDefaultLongLike 
	    // indicates a new received sequence number needs to be processed.
	  private NamedLong newIncomingPacketsReceivedNamedLong;
	  	// This is a copy of the local EpiInputStream packet counter.
	  	// It represents the latest count of locally received packets.
	  	// This value can only increase.
		private DefaultLongLike oldIncomingPacketsReceivedDefaultLongLike;
			// A difference between this and newIncomingPacketsReceivedNamedLong 
			// indicates a new packet has been received and needs to be processed.
		private NamedFloat incomingPacketLossNamedFloat;
	    // Floating representation of the fraction of incoming packets lost.
	  LossAverager incomingPacketLossAverager;
		
	  // Variables for managing outgoing packets and their acknowledgement.
	  private NamedLong newOutgoingPacketsSentNamedLong;
	  	// This is a copy of the local EpiOutputStreamO packet counter.
	  	// This value can only increase.
	  private NamedLong newOutgoingPacketsSentEchoedNamedLong;
	  	// This is the local EpiOutputStreamO packet counter after 
	    // being returned from remote.  It might lag the real counter by RTT.
	  	// This value can only increase.
	  private DefaultLongLike oldOutgoingPacketsSentDefaultLongLike;
	  	// A difference between this and newOutgoingPacketsSentNamedLong 
	    // indicates new packets have been sent and need to be processed.
	  private NamedLong newOutgoingPacketsReceivedNamedLong;
	  	// This value comes from the most recently received "PA" message.  
		  // When sent this argument was the remote end's 
		  // EpiInputStream packet counter, 
	    // counting the number of packets received.
	  	// This value normally increases, but can decrease because of 
	    // out-of-order "PA" packets. 
	  private DefaultLongLike oldOutgoingPacketsReceivedDefaultLongLike;
	  	// A difference between this and newOutgoingPacketsReceivedNamedLong 
	    // indicates a new packet has been acknowledged and 
			// needs to be processed.
		private NamedFloat outgoingPacketLossNamedFloat;
	  	// Floating representation of the fraction of outgoing packets lost.
	  private LossAverager outgoingPacketLossLossAverager;
	
		// Variables for managing round trip time.
	  protected NsAsMsNamedLong rawRoundTripTimeNsAsMsNamedLong;
	    // This is an important value.  It can be used to determine
	    // how long to wait for a message acknowledgement before
	    // re-sending a message.  Initial value of 1/10 second.
	  private NsAsMsNamedLong smoothedRoundTripTimeNsAsMsNamedLong;
	  private NsAsMsNamedLong smoothedMinRoundTripTimeNsAsMsNamedLong; // Minimum.
	  private NsAsMsNamedLong smoothedMaxRoundTripTimeNsAsMsNamedLong; // Maximum.
	  
		// Other variables.
		private long exponentialRetryTimeOutMsL; 
		  // This is used as an time-out value
		  // in exponential back off for re-sending PSs.

	  private long sentSequenceNumberTimeNsL;
	    // This is the time last PS message was sent. 

		private long lastSequenceNumberSentL;
		  // This is the sequence number in the last PS message sent.

	  private TimerInput measurementTimerInput;
	    // This is the timer used by this state machine for pauses and time-outs. 

	  private NamedLong measurementHandshakesNamedLong;
	    // This counts home many measurement handshakes have occurred,
	    // which is the number of PA messages received.

		} // class LinkMeasurementState
