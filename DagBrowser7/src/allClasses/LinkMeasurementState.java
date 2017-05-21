package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.Timer;

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
		private NamedLong retransmitDelayMsNamedLong;
		private Timer theTimer; 
		
		// Sub-state machine instances.
		@SuppressWarnings("unused")
		private RemoteMeasurementState theRemoteMeasurementState;
		@SuppressWarnings("unused")
		private LocalMeasurementState theLocalMeasurementState;

		LinkMeasurementState(  // Constructor.
				Timer theTimer, 
			  NetcasterInputStream theNetcasterInputStream,
				NetcasterOutputStream theNetcasterOutputStream,
				NamedLong retransmitDelayMsNamedLong
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

    		// Create and add to DAG the sub-states of this state machine.
	  	  initAndAddStateListV( theRemoteMeasurementState= 
	  	  		new RemoteMeasurementState() );
	  	  initAndAddStateListV( theLocalMeasurementState= 
	  	  		new LocalMeasurementState() );

	  		// Adding measurement count.
	  	  addB( measurementHandshakesNamedLong= new NamedLong(
	      		"Measurement-Handshakes", 0 ) );

	  		addB( retransmitDelayMsNamedLong );

        // Adding the new round trip time trackers.
	      addB( smoothedRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong(
	      		"Smoothed-Round-Trip-Time (ms)", 
	      		Config.initialRoundTripTime100MsAsNsL ) );
	  	  addB( smoothedMinRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong(
	      		"Smoothed-Minimum-Round-Trip-Time (ms)", 
	      		Config.initialRoundTripTime100MsAsNsL ) );
	  	  addB( smoothedMaxRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong(
	      		"Smoothed-Maximum-Round-Trip-Time (ms)", 
	      		Config.initialRoundTripTime100MsAsNsL ) );
	      addB( rawRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong( 
	      		"Raw-Round-Trip-Time (ms)", 
	      		Config.initialRoundTripTime100MsAsNsL ) );

        // Adding incoming packet statistics children and related trackers.
	  	  newIncomingPacketsSentDefaultLongLike= new DefaultLongLike(0);
	  	  addB( oldIncomingPacketsSentNamedLong= new NamedLong(
	      		"Incoming-Packets-Sent", 0 ) );
		    addB( newIncomingPacketsReceivedNamedLong=
		    		theNetcasterInputStream.getCounterNamedLong() );
	  	  oldIncomingPacketsReceivedDefaultLongLike= new DefaultLongLike(0);
	  	  addB( incomingPacketLossNamedFloat= new NamedFloat(  
	  	  		"Incoming-Packet-Loss", 0.0F ) );
	  	  incomingPacketLossAverager= new LossAverager(
	  	  				oldIncomingPacketsSentNamedLong,
	  	  				oldIncomingPacketsReceivedDefaultLongLike,
	  	  				incomingPacketLossNamedFloat
	  	  				);

	  	  // Adding outgoing packet statistics children and related trackers.
    		newOutgoingPacketsSentNamedLong=
    				theNetcasterOutputStream.getCounterNamedLong(); 
		    addB( newOutgoingPacketsSentNamedLong );
		    addB( newOutgoingPacketsSentEchoedNamedLong= new NamedLong(
		    		"Outgoing-Packets-Sent-Echoed", 0 ) ); 
		    oldOutgoingPacketsSentDefaultLongLike= new DefaultLongLike(0);
		    addB( newOutgoingPacketsReceivedNamedLong= new NamedLong( 
			      "Outgoing-Packets-Received", 0 ) );
	  	  addB( outgoingPacketLossNamedFloat= new NamedFloat( 
	  	  		"Outgoing-Packet-Loss", 0.0f ) );
	  	  oldOutgoingPacketsReceivedDefaultLongLike= new DefaultLongLike(0);
	  	  outgoingPacketLossLossAverager= new LossAverager(
	  	  		oldOutgoingPacketsSentDefaultLongLike,
	  	  		oldOutgoingPacketsReceivedDefaultLongLike,
	  	  		outgoingPacketLossNamedFloat
	  	  		);

			  return this;
			  }
	
	  public synchronized void finalizeV() throws IOException
	    // This method processes any pending loose ends before shutdown.
		  {
	  	  super.finalizeV();
	  		overrideStateHandlerB(); // This throws any saved IOException from timer.
	  		measurementTimerInput.cancelingV(); // To stop our timer.
	      }
		
	  private class LocalMeasurementState extends OrState 
	
		  /* This is the local concurrent/orthogonal sub-state 
		    for doing local measurements.  
				It partner is orthogonal sub-state RemoteMeasurementState. 
		    */
	
		  {
	
				// sub-states/machines.
				private MeasurementPausedState 
				  theMeasurementPausedState;
				private MeasurementInitializationState
				  theMeasurementInitializationState;
				private MeasurementHandshakingState
				  theMeasurementHandshakingState;
	
		    public StateList initializeWithIOExceptionStateList() throws IOException 
			    {
		    		super.initializeWithIOExceptionStateList();
	
		    		// Create and add orthogonal sub-state machines.
			  	  initAndAddStateListV( theMeasurementPausedState= 
			  	  		new MeasurementPausedState() );
			  	  initAndAddStateListV( theMeasurementInitializationState= 
			  	  		new MeasurementInitializationState() );
			  	  initAndAddStateListV( theMeasurementHandshakingState= 
			  	  		new MeasurementHandshakingState() );
	
						requestSubStateListV( theMeasurementPausedState ); // Initial state.
						return this;
			    	}
	
		    private boolean processPacketAcknowledgementB() 
						throws IOException
				  /* This input method finishes processing the "PA" sequence number 
				    feedback message, which the remote peer sends
				    in response to receiving an "PS" sequence number message.
				    The "PA", which is assumed to have already been processed,
				    is followed by:
				    * a copy of the sent packet sequence number received by the remote peer,
				    * the remote peers received packet count.
				    From these two values this method calculates 
				    the packet loss ratio in the remote peer receiver.
				    By having "PA" include both values, 
				    its calculation is RTT-immune.
				    
				    It returns true if it succeeds, false otherwise.
	
						See processSequenceNumberB(..) about "PS", for more information.
						*/
			  	{
		    	  boolean successB= true;
		    	  try {
					  	long ackReceivedTimeNsL= System.nanoTime();
				  		int sequenceNumberI=  // Reading echo of sequence #.
				  				theNetcasterInputStream.readANumberI();
				  		int packetsReceivedI=  // Reading packets received.
				  				theNetcasterInputStream.readANumberI();
			    		measurementHandshakesNamedLong.addDeltaL(1);
			        
			        newOutgoingPacketsSentEchoedNamedLong.setValueL(
					    		sequenceNumberI + 1 // Convert sequence # to sent packet count.
					    		);
				  		newOutgoingPacketsReceivedNamedLong.setValueL(packetsReceivedI);
				  		outgoingPacketLossLossAverager.recordPacketsReceivedOrLostV(
				  				newOutgoingPacketsSentEchoedNamedLong,
				  				newOutgoingPacketsReceivedNamedLong
								  );
				  	  calculateRoundTripTimesV(
				  	  		sequenceNumberI, ackReceivedTimeNsL, packetsReceivedI
				  	  		);
		    	  	}
		    	  catch ( BadReceivedDataException theBadReceivedDataException ) {
		    	  	successB= false; 
		    	  	}
		    	  return successB;
		    	  }
				
				// States.
	
		    private class MeasurementPausedState extends StateList
		    
		      /* This class does the pause between measurement handshakes.
		        It finishes by requesting the MeasurementHandshakingState.
		       	*/
			  
			  	{
			    	public void enterV() throws IOException
			    	  /* Starts timer for the pause interval before 
			    	    the next handshake.
			    	   	*/
				  	  {
			    	  	measurementTimerInput.scheduleV(Config.handshakePause5000MsL);
			  				}
	
					  public void overrideStateHandlerV()
					    /* Waits for the end of the pause interval.
					     	*/
					  	{ 
					  	  if (measurementTimerInput.getInputArrivedB()) // Timer done. 
					  	  	requestStateListV(theMeasurementInitializationState);
					  		}
	
						public void exitV() throws IOException
						  // Cancels timer and initializes the handshake time-out.
						  { 
								measurementTimerInput.cancelingV();
								super.exitV();
								}
					
			  	} // class MeasurementPausedState
	
				private class MeasurementInitializationState extends StateList 
			  	/* This state does nothing but initializes for the handshake,
			  	  then instantly moves on to the MeasurementHandshakingState.
					  
					  ///? This initialization could be moved to either:
					    * enterV() of MeasurementHandshakingState if
					      that state was split into two levels.
					    * exitV() of MeasurementPausedState.
			  	  */
			  	{
					  public void overrideStateHandlerV() throws IOException
					  	{
		    			  retryTimeOutMsL=   // Initializing retry time-out.
		    			  		retransmitDelayMsNamedLong.getValueL();
	
					  		requestStateListV(theMeasurementHandshakingState);
				  	  	}
	
			  		} // class MeasurementInitializationState
				
				private class MeasurementHandshakingState extends StateList 
			  	/* This state handles the PS-PA handshakes, 
			  	  including sending the first packet, 
			  	  processing the acknowledgement, and
			  	  retrying using a time-doubling exponential back-off.
						It ends by requesting the MeasurementPausedState.
			  	  */
	
			  	{
			    	public void enterV() throws IOException
			    	  // Initiates the handshake and starts acknowledgement timer.
				  	  { 
		    			  measurementTimerInput.scheduleV(retryTimeOutMsL);
				    		sendingSequenceNumberV();
			  				}
	
					  public void overrideStateHandlerV() throws IOException
					  	/* This method handles handshakes acknowledgement, 
					  	  initiating a retry using twice the time-out,
					  	  until the acknowledgement is received,
					  	  or giving up if the time-out limit is reached.
					  	  */
					  	{
							  if (tryInputB("PA")) // Try processing acknowledgement received.
								  { if ( processPacketAcknowledgementB() )
									  	requestStateListV(theMeasurementPausedState);
								  	}
				      	else  if // Try processing time-out.
				      		(measurementTimerInput.getInputArrivedB()) 
					    		{ if ( retryTimeOutMsL <= Config.maxTimeOut5000MsL )
				    				  { retryTimeOutMsL*=2;  // Doubling time-out limit.
				    				  	requestStateListV(this); // Retrying by repeating state.
					  					  }
				    			  else // Giving up after maximum time-out reached.
								  	  requestStateListV(theMeasurementPausedState);
					    		      // Do again after a pause.
					    			}
				  	  	}
					  
			  		} // class MeasurementHandshakingState 
	
				public void exitV() throws IOException
				  // Cancels acknowledgement timer.
				  { 
						measurementTimerInput.cancelingV();
						super.exitV();
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
			      appLogger.debug( 
			      		"sendingSequenceNumberV() " + lastSequenceNumberSentL
			      		);
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
							rttString+= "unchanged, wrong number.";
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
				    appLogger.debug( "calculateRoundTripTimesV(...) PA:"
						  +sequenceNumberI+","
				    	+packetsReceivedI+";RTT="
				    	+rttString
						  );
				  	}
				
				private void processRoundTripTimeV(long rawRoundTripTimeNsL)
				  /* This method updates various variables which
				    are dependent on round trip times.
				    
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
				
				  	retransmitDelayMsNamedLong.setValueL(
								(smoothedMaxRoundTripTimeNsAsMsNamedLong.getValueL()/1000000) * 2
								); // Use double present maximum round-trip-time.
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
	
			  public void overrideStateHandlerV() throws IOException
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
				      appLogger.debug( "processPacketSequenceNumberB(..) PS:"
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
		
		// Variables for counting measurement handshakes.
	  private NamedLong measurementHandshakesNamedLong;

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
	    // counting the number of packets recevied.
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
		private long retryTimeOutMsL;

	  private long sentSequenceNumberTimeNsL;

		private long lastSequenceNumberSentL;

	  private TimerInput measurementTimerInput; // Used for state machine 
    // pauses and time-outs. 

		} // class LinkMeasurementState
