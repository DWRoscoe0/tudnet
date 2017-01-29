package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class LinkMeasurement 
	
	extends MutableList
	
	/* This class is, or contains, or will contain,
	  a [hierarchical] state machine that measures and displays 
	  several performance parameters of a peer-to-peer link.
	  * Round-Trip-Time.
	  * Count of packets sent and received, locally and remotely.
	  * Packet loss ratios.  
	  
	  This code is not thread-safe.
	  It is meant to be called only from the Unicaster thread,
	  except for a timer which it uses.
	  
	  This class make use of internal non-static classes
	  to separate DataNode code from State code while
	  allowing the latter to access the former.
	 	*/
	{	
	  
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong retransmitDelayMsNamedLong;
		private Timer theTimer; 

	  private TimerInput statisticsTimerInput; // Used for timing
	    // both pauses and time-outs. 
		
	  // What will be the main state machine.
	  private LinkMeasurementState theLinkMeasurementState;
	
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
		private IOException delayedIOException= null; /* For re-throwing 
			an exception which occurred in one thread which couldn't handle it 
	    in another thread that can.
	    */
	
		LinkMeasurement(  // Constructor.
		    DataTreeModel theDataTreeModel,
				Timer theTimer, 
			  NetcasterInputStream theNetcasterInputStream,
				NetcasterOutputStream theNetcasterOutputStream,
				NamedLong retransmitDelayMsNamedLong
				)
	  	{
	  	  super( // Constructing MutableList superclass with injections.
	  		    theDataTreeModel,
		        "LinkMeasurements",
	          new DataNode[]{} // Initially empty array of children.
	      		);
	
	    // Injected dependencies.
			  this.theNetcasterInputStream= theNetcasterInputStream;
			  this.theNetcasterOutputStream= theNetcasterOutputStream;
			  this.retransmitDelayMsNamedLong= retransmitDelayMsNamedLong;
			  this.theTimer= theTimer;
			  }
	
	
		// Input code activated or used by inputs.
			
		  public synchronized void initializingV() throws IOException
			  {

	  	  	// Adding measurement count.
		  	  addB( measurementHandshakesNamedLong= new NamedLong(
		      		theDataTreeModel, "Measurement-Handshakes", 0 
		      		)
		  	  	);
	
		  		addB( retransmitDelayMsNamedLong );
	
	        // Adding the new round trip time trackers.
		      addB( smoothedRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong(
		      		theDataTreeModel, "Smoothed-Round-Trip-Time (ms)", 
		      		Config.initialRoundTripTime100MsAsNsL
		      		)
		  	  	);
		  	  addB( smoothedMinRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong(
		      		theDataTreeModel, "Smoothed-Minimum-Round-Trip-Time (ms)", 
		      		Config.initialRoundTripTime100MsAsNsL
		      		)
		  	  	);
		  	  addB( smoothedMaxRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong(
		      		theDataTreeModel, "Smoothed-Maximum-Round-Trip-Time (ms)", 
		      		Config.initialRoundTripTime100MsAsNsL
		      		)
		  	  	);
		      addB( rawRoundTripTimeNsAsMsNamedLong= new NsAsMsNamedLong( 
		      		theDataTreeModel, "Raw-Round-Trip-Time (ms)", 
		      		Config.initialRoundTripTime100MsAsNsL
		      		)
		  			);
	
	        // Adding incoming packet statistics children and related trackers.
		  	  newIncomingPacketsSentDefaultLongLike= new DefaultLongLike(0);
		  	  addB( oldIncomingPacketsSentNamedLong= new NamedLong(
		      		theDataTreeModel, "Incoming-Packets-Sent", 0 
		      		)
		  			);
			    addB( newIncomingPacketsReceivedNamedLong=
			    		theNetcasterInputStream.getCounterNamedLong()
			    		);
		  	  oldIncomingPacketsReceivedDefaultLongLike= new DefaultLongLike(0);
		  	  addB( incomingPacketLossNamedFloat= new NamedFloat( 
		      		theDataTreeModel, "Incoming-Packet-Loss", 0.0F  
		      		)
		  			);
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
			    		theDataTreeModel, "Outgoing-Packets-Sent-Echoed", 0 
			    		)
			    	); 
			    oldOutgoingPacketsSentDefaultLongLike= new DefaultLongLike(0);
			    addB( newOutgoingPacketsReceivedNamedLong= new NamedLong( 
				      theDataTreeModel, "Outgoing-Packets-Received", 0 
				      )
			    	);
		  	  addB( outgoingPacketLossNamedFloat= new NamedFloat( 
		      		theDataTreeModel, "Outgoing-Packet-Loss", 0.0f 
		      		)
		  			);
		  	  oldOutgoingPacketsReceivedDefaultLongLike= new DefaultLongLike(0);
		  	  outgoingPacketLossLossAverager= new LossAverager(
		  	  		oldOutgoingPacketsSentDefaultLongLike,
		  	  		oldOutgoingPacketsReceivedDefaultLongLike,
		  	  		outgoingPacketLossNamedFloat
		  	  		);

				  // Create and initialize the top-level state machine.
				  theLinkMeasurementState= new LinkMeasurementState();
		  	  theLinkMeasurementState.initializingV(null); // Initializing manually 
		  	    // because it's not being added to a sub-state list.
	        }

	    public synchronized boolean processMeasurementMessageB(
	    		String keyString
	    		) 
	  		throws IOException
	  		// This method just passes the input through to theLinkMeasurementState. 
	  		{
	    		return theLinkMeasurementState.processMeasurementMessageB(keyString); 
	    	  }
			
		  public synchronized void finalizingV() throws IOException
		    // This method processes any pending loose ends before shutdown.
			  {
		  		theLinkMeasurementState.cycleMainMachineV(); 
		  		  // Throwing any saved IOException from timer.
	
		  		statisticsTimerInput.cancelingV(); // Stopping timer.
	        }

	  private long sentSequenceNumberTimeNsL;

		private long lastSequenceNumberSentL;

		private boolean acknowledgementReceivedB= false;

		class LinkMeasurementState extends AndState 

			/* This is is the root State for LinkMeasurement.
			  Code should be moved from LinkMeasurement to here.
			  */

			{
			  // Sub-state machine instances.
			  private RemoteMeasurementState theRemoteMeasurementState;
			  private LocalMeasurementState theLocalMeasurementState;

		    public void initializingV(State parentState) throws IOException 
			    {
		    		super.initializingV(parentState);
			  	  initAndAddV( // Create and add orthogonal sub-state machine 1.
			  	  		theRemoteMeasurementState= new RemoteMeasurementState()
			  	  		);
			  	  initAndAddV( // Create and add orthogonal sub-state machine 2.
			  	  		theLocalMeasurementState= new LocalMeasurementState()
			  	  		);
			  	  statisticsTimerInput= 
					  		new TimerInput(  //// Move to factory.
					  				theTimer,
					  				new Runnable() {
							        public void run() {
							        	  try { theLinkMeasurementState.cycleMainMachineV(); }
							        	  catch ( IOException theIOException) 
							        	    { delayedIOException= theIOException; }
							        	  }
							    		}
					  				);
		  	  	theLinkMeasurementState.cycleMainMachineV(); // Now that 
		  	  	  // everything is ready, start the input-producing timer.
			    	}

		    public synchronized boolean processMeasurementMessageB(
		    		String keyString
		    		) 
		  		throws IOException
		  		/* This method is called to process a possible PS or PA message input.
		  		  It returns true if one of those messages is processed,
		  		  meaning keyString was one of them and it was consumed.
		  		  It returns false otherwise.

		  		  It does its processing by calling methods in the sub-machines.
		  		  It cycles the main machine if input was processed.
		  		  */
		      {
		        	boolean successB= true; // Assuming message is one of the two.
		        beforeExit: {
		        	if  // "PS"
			          ( theRemoteMeasurementState.processPacketSequenceNumberB(
			          		keyString
			          		) 
			          	)
			        	break beforeExit;
			        if  // "PA"
			          ( theLocalMeasurementState.processPacketAcknowledgementB(
			          		keyString
			          		) 
			          	)
			        	break beforeExit;
			        successB= false;  // Indicating message was neither of the two.
		        } // beforeExit:
			        if (successB)  // Post-processing if input successful.
			        	cycleMainMachineV();
			        return successB;
		        }

			  public synchronized void cycleMainMachineV() throws IOException
			    /* This method cycles the main state machine.
			      Its purpose is to make certain that 
			      machine inputs have been fully processed.
			      To do this the sub-machines are recursively cycled.
			      It also re-throws any IOException that might have occurred
			      earlier when the machine was cycled by a timer thread.
			      
			      This method is called by the Unicaster thread and 
			      the timer thread, both of which provide state-machine inputs.
			      
			      //// It needs to do this only for the local machine.
			      */
			    { 
			  	  if  // Re-throw saved exception from other thread, if any.
			  	    (delayedIOException != null) 
			  	  	throw delayedIOException;

			  	  stateHandlerB(); // Calling state handler to cycle machine.
			    	}

				} // class LinkMeasurementState
		
		class LocalMeasurementState extends OrState 

		  /* This is the local concurrent sub-state 
		    for doing local measurements.  
		    */

		  {

				// The following States may be referenced and assigned to subState.
			  // Create and add orthogonal sub-state machines.
				PausingState thePausingState;
				SendingAndWaitingState theSendingAndWaitingState;

		    public void initializingV(State parentState) throws IOException 
			    {
		    		super.initializingV(parentState);

		    		// Create and add orthogonal sub-state machines.
			  	  initAndAddV(
			  	  		thePausingState= new PausingState()
			  	  		);
			  	  initAndAddV(
			  	  		theSendingAndWaitingState= new SendingAndWaitingState()
			  	  		);

						setNextStateV( thePausingState ); // Initialize machine sub-state.
			    	}

		    private boolean processPacketAcknowledgementB(String keyString) 
						throws IOException
				  /* This input method tries to process the "PA" sequence number 
				    feedback message, which the remote peer sends
				    in response to receiving an "PS" sequence number message.
				    The "PA" is followed by:
				    * a copy of the sent packet sequence number received by the remote peer,
				    * the remote peers received packet count.
				    From these two values it calculates 
				    the packet loss ratio in the remote peer receiver.
				    By having "PA" include both values, its calculation is RTT-immune.
			
						See processSequenceNumberB(..) about "PS" for more information.
			  	  */
			  	{
			  	  boolean isKeyB= keyString.equals( "PA" ); 
					  if (isKeyB) {
					  	long ackReceivedTimeNsL= System.nanoTime();
				  		int sequenceNumberI=  // Reading echo of sequence #.
				  				theNetcasterInputStream.readANumberI();
				  		int packetsReceivedI=  // Reading packets received.
				  				theNetcasterInputStream.readANumberI();
			    		acknowledgementReceivedB= true; 
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
					  return isKeyB;
			  		}
				
				// States.

			  // State handler methods.

				class PausingState extends State 
			  
			  	{
					  public boolean stateHandlerB()
					    // This state method generates the pause between PS-PA handshakes.
					  	{ 
					  	  boolean runningB= true;
					  	  beforeExit: {
					
					  	  	if // Scheduling pause timer if not scheduled yet.
					  	  		(! statisticsTimerInput.getInputScheduledB()) 
					    	  	{ statisticsTimerInput.scheduleV( Config.handshakePause5000MsL );
					    			  //appLogger.debug("handlingPauseB() scheduling pause end input.");
							    		break beforeExit;
							    	  }
					
					    		if // Handling pause complete if it is.
						    		(statisticsTimerInput.getInputArrivedB()) 
						    		{ // Handling pause complete by changing state. 
					    				statisticsTimerInput.cancelingV();
					    			  retryTimeOutMsL=   // Initializing retry time-out.
					    			  		retransmitDelayMsNamedLong.getValueL();
					      	  	setNextStateV(theSendingAndWaitingState);
							    		break beforeExit;
					    			  }
					
					    		{ runningB= false; // Indicate state machine is waiting.
					  				//appLogger.debug("handlingPauseB() pause end input scheduled.");
						    		break beforeExit;
						    	  }
					
					  	  } // beforeExit:
					  		  return runningB; 
					  		}
					
			  	}

				class SendingAndWaitingState extends State 
			  
			  	{

					  public boolean stateHandlerB() throws IOException
					  	// This state method handles the PS-PA handshakes, and retrying.
					  	{
					  	  	boolean runningB= true;
					  	  beforeExit: { 
					  	  beforeStartPausing: { 

					    		if // Doing state entry operations if this is state entry.
					    		  (! statisticsTimerInput.getInputScheduledB())
						    		{ // Sending PS and scheduling time-out, if not done yet.
					    			  statisticsTimerInput.scheduleV(retryTimeOutMsL);
						    			//appLogger.debug("handleSendingAndWaiting() scheduling "+retryTimeOutMsL);
							    		sendingSequenceNumberV();
							    		break beforeExit;
								    	}
					      	if  // Processing acknowledgement PA if received.
					      	  (acknowledgementReceivedB) 
					        	{ // Handling received PA.
					        		//appLogger.debug("handleSendingAndWaiting() PA acknowledgement and resets.");
					        		acknowledgementReceivedB= false;  // Resetting PS input,
					        		statisticsTimerInput.cancelingV(); // Resetting timer state.
							    		break beforeStartPausing; // Going to Pausing state.
								    	}
					    		if // Handling PA reply timer time-out if it happened.
					    			(statisticsTimerInput.getInputArrivedB()) 
						    		{ // Handling PA reply timer time-out.   
							    		//appLogger.debug("handleSendingAndWaiting() time-out occurred.");
					    				statisticsTimerInput.cancelingV(); // Resetting timer.
					    			  if  // Handling maximum time-out interval not reached yet.
					    			  	( retryTimeOutMsL <= Config.maxTimeOut5000MsL )
					    				  { retryTimeOutMsL*=2;  // Doubling time limit for retrying.
						    					break beforeExit; // Going to send PS again.
						  					  }
					    			  //appLogger.debug("handleSendingAndWaiting() last time-out.");
					    			  {  // Handling maximum exponential backoff time-out reached.
						    			  break beforeStartPausing; // Pausing.
						    			  }
						    			}
					    		{ runningB= false; // Indicate state machine is waiting for input.
						    	  //appLogger.debug("handleSendingAndWaiting() time-out scheduled.");
						    		break beforeExit;
							    	}
				
				  	  } // beforeStartPausing:
				  	  	setNextStateV(thePausingState);
						  	break beforeExit; 
				
				  	  } // beforeExit:
				  			return runningB;
				
				  	  }
					  
			  		}
			
			  protected void sendingSequenceNumberV() throws IOException
			    /* This method increments and writes the packet ID (sequence) number
			      to the EpiOutputStream.
			      It doesn't flush().
			      */
			    {
			    	lastSequenceNumberSentL= 
			    			theNetcasterOutputStream.getCounterNamedLong().getValueL(); 
			      appLogger.debug( "sendingSequenceNumberV() " + lastSequenceNumberSentL);
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
				        		smoothedMinRoundTripTimeNsAsMsNamedLong.getValueString()+","+
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

		class RemoteMeasurementState extends State 
		
			{

			  /* This is a concurrent/orthogonal sub-state 
			    which helps the remote peer makes measurements.
			    It does this by responding with "PA" messages to 
			    "PS" messages sent from the remote peer.
			    It also records some information that is displayed locally.
			    This state doesn't have any sub-states.
			    */

				private boolean processPacketSequenceNumberB(String keyString) 
						throws IOException
				  /* This method tries to process the "PS" message from the remote peer.
				    It keyString is a "PS" then it is processed along with
				    the packet sequence number that follows "PS",
				    which comes from the remote peer EpiOutputStreamO packet count.
				    From this and the local EPIInputStreamI packet count it calculates 
				    a new value of the local peer's incoming packet loss ratio.
				    This ratio is accurate when calculated because
				    the values it uses in the calculation are synchronized.

				    It also sends an "PA" message back to the remote peer with 
				    the same numbers so the remote peer can calculate the same ratio, 
				    which for the remote peer is called the outgoing packet loss ratio.
				    By sending and using both numbers the remote peer's calculation is
				    not affected by variations in Round-Trip-Time (RTT).

				    Every packet has a sequence number, but 
				    not every packet needs to contain its sequence number.
				    The reception of a "PS" message with its sequence number means that
				    the remote has sent at least that number of packets.
				    The difference of the highest sequence number received and
				    the number of packets received is the number of packets lost.
				    A new difference and a loss ratio average can be calculated
				    each time a new sequence number is received.  In fact,
				    that is how reception of a sequence number can be interpreted.
				
						//// Sequence numbers and other numbers eventually need to be converted 
						  to use modulo (wrap-around) arithmetic.
					  */
					{
					  boolean isKeyB= keyString.equals( "PS" ); 
					  if (isKeyB) {
						  int sequenceNumberI= theNetcasterInputStream.readANumberI(); // Reading # from packet.
						  newIncomingPacketsSentDefaultLongLike.setValueL( // Recording.
									sequenceNumberI + 1
									); // Adding 1 to convert sequence # to remote sent packet count.
							incomingPacketLossAverager.recordPacketsReceivedOrLostV(
									newIncomingPacketsSentDefaultLongLike,
									newIncomingPacketsReceivedNamedLong
							  );
							theNetcasterOutputStream.writingTerminatedStringV( "PA" );
							theNetcasterOutputStream.writingTerminatedLongV( // The remote sequence number.
				  				//newIncomingPacketsSentDefaultLongLike.getValueL()
				  				sequenceNumberI
				  				);
							long receivedPacketCountL= 
							  newIncomingPacketsReceivedNamedLong.getValueL(); 
							theNetcasterOutputStream.writingTerminatedLongV( // The local received packet count.
									receivedPacketCountL 
				  				);
							theNetcasterOutputStream.sendingPacketV(); // Sending now for minimum RTT.
				      appLogger.debug( "processPacketSequenceNumberB(..) PS:"
				  		  +sequenceNumberI+","
				      	+receivedPacketCountL
				  		  );
							}
					  return isKeyB;
						}
				
					} // class RemoteMeasurementState

		} // class LinkMeasurements


class State {
	
	/*  This class is the base class for all state objects.
	  
	  States are hierarchical, meaning that
	  states may have child states, or sub-states.
	  Sub-states may themselves be state machines 
	  with their own sub-states.
	  
	  When writing code it is important to distinguish between
	  * the current State, designated by "this", and
	  * its current sub-state, or child state, 
	    designated by "this.subState".

	  */

	protected State parentState= null;

  protected List<State> theListOfSubStates=
      new ArrayList<State>(); // Initially empty list.

  protected State subState;

  public void initAndAddV(State theSubState) throws IOException
    /* This method does an initialize of theSubState and then
      adds it to the sub-state list of this state. 
     	*/
  	{ 
  		theSubState.initializingV( this );
  	  addV( theSubState ); // Add theSubState to
  	    // this state's list of sub-states.
  	  }

  public void initializingV(State parentState) throws IOException
    /* This method initializes this state.
  		It only sets the parent of this state to be parentState,
  		but it can be overridden.
  		*/
    {
    	//% setParentStateV( parentState );
    	}

  public void addV(State theSubState)
    /* This method adds/injects one sub-state to this state.
      It adds theSubState to the state's sub-state list.
      It also sets the parent of the sub-state to be this state.
      */
  	{ 
  	  theListOfSubStates.add( theSubState ); // Add theSubState to
  	    // this state's list of sub-states.

  	  theSubState.setParentStateV( this ); // Store this state as
  	    // the sub-state's parent state.
  	  }

	public void setParentStateV( State parentState )
	  // Stores parentState as the parent state of this state.
		{ this.parentState= parentState; }
	
	public boolean stateHandlerB() throws IOException
	  /* This is the default state handler. 
	    It does nothing because this is the superclass of all States.
	    It also returns false to indicate:
	    * no computation processing was possible on previous inputs,
	    * and a test for new inputs found none.
	    This method should be called repeatedly until it returns false,
	    though it need not necessarily be called again immediately. 
	    */
	  { return false; }

	}  // State class 

class OrState extends State {

	/*  This class is the base class for all "or" super-state objects.
	  OrStates have sub-states, but unlike AndStates,
	  only one sub-state can be active at a time.

	  There is no concurrency in an OrState machine,
	  at least not at the immediate level of its sub-states.
		*/

  protected void setNextStateV(State nextState)
    // Changes the state-machine State.
    {
  		subState= nextState;
			}

	public boolean stateHandlerB() throws IOException
	  /* This handles this OrState by cycling it's machine.
	    It does this by calling the handler method 
	    associated with the state-machine's sub-state.
      The sub-state might change with each of these calls.
      It keeps calling sub-state handlers until a handler returns false,
      indicating that it has processed all available inputs
      and is waiting for new input.
	    It returns true if any computational progress was made,
	    false otherwise.
      */
	  { 
		  boolean anyProgressMadeB= false;
  		boolean substateProgressMadeB;
  	  do { // Handle sub-states until done.
	  	  	substateProgressMadeB= subState.stateHandlerB();
	  	  	anyProgressMadeB|= substateProgressMadeB;
	  	  	}
  	  	while (substateProgressMadeB);
			return anyProgressMadeB; 
			}

	}  // OrState 

class AndState extends State {

	/* This class is the base class for all "and" super-state objects.
	  AndStates have sub-states like OrStates, but unlike OrStates,
	  all AndState sub-states are active all the time.

	  AndStates are used when logical concurrency is needed.
	  Implementation might or might use actual concurrency.
	  The sub-states are said to be orthogonal or concurrent.
		*/

	public boolean stateHandlerB() throws IOException
	  /* This method handles this AndState by cycling all of it sub-machines
	    until none of them makes any computational progress.
      It keeps going until all sub-state handlers return false,
      indicating that it has processed all available inputs
      and is waiting for new input.
	    It returns false to indicate that there is nothing else to do
      */
	  //// rewrite for minimum stateHandlerB() calls.
	  { 
		  boolean anythigMadeProgressB= false;
			boolean cycleMadeProgressB;
  	  do  // Cycling until no sub-machine progresses.
	  	  { // Cycle all sub-state-machines once each.
		  		cycleMadeProgressB= // Will be true if any machine progressed. 
		  				false;
	  	  	for (State aState : theListOfSubStates)
		  	  	{
	  	  		  boolean handlerMadeProgressB= aState.stateHandlerB(); 
		  	  		cycleMadeProgressB|= handlerMadeProgressB; 
		  	  		}
  	  		anythigMadeProgressB|= cycleMadeProgressB; 
	  	  	} while (cycleMadeProgressB);
			return anythigMadeProgressB; 
			}

	}  // AndState 
