package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class LinkMeasurement 
	
	extends MutableList
	
	/* This class contains a [hierarchical] state machine 
	  that measures and displays several performance parameters 
	  of a Unicaster peer-to-peer link, including:
	  * the Round-Trip-Time;
	  * the count of packets sent and received, locally and remotely; and
	  * the packet loss ratios.  

	  This code is not thread-safe.  It is meant to be called only from:
	  * a Unicaster thread, and
	  * the thread belonging to a timer that it creates and uses.

	  This class make use of an internal non-static class LinkMeasurementState.
	  This was done as an expedient way for State code to access DataNode code.
	  ////// Eventually it might be possible to combine a 
	    State with a DataNode in a single class.  
	    The tricky part is sharing the DataNode/sub-state list. 
	 	*/

	{	
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong retransmitDelayMsNamedLong;
		private Timer theTimer; 
		
	  // The main state machine which is a nested non-static class.
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
		  	  theLinkMeasurementState.initializingV(); // Initializing manually 
		  	    // because we are not using State.initAndAddV(..).
	        }

	    public synchronized boolean processMeasurementMessageB(
	    		String keyString
	    		) 
	  		throws IOException
	  		/* This method passes possible inputs to theLinkMeasurementState.*/ 
	  		{
	    		return theLinkMeasurementState.processMeasurementMessageB(keyString); 
	    	  }
			
		  public synchronized void finalizingV() throws IOException
		    // This method processes any pending loose ends before shutdown.
			  {
		  	  theLinkMeasurementState.finalizingV(); // Finalizing state-machine.
	        }

	  private long sentSequenceNumberTimeNsL;

		private long lastSequenceNumberSentL;

		private boolean acknowledgementReceivedB= false;

		private class LinkMeasurementState extends AndState 

			// This is is the root State for LinkMeasurement.

			{
			  private TimerInput statisticsTimerInput; // Used for timing
			    // both pauses and time-outs. 
				private IOException delayedIOException= null; /* For re-throwing 
					an exception which occurred in one thread which couldn't handle it, 
			    in another thread that can.
			    */

				// Sub-state machine instances.
			  private RemoteMeasurementState theRemoteMeasurementState;
			  private LocalMeasurementState theLocalMeasurementState;

		    public void initializingV() throws IOException
		      /* This method initializes this state machine.  This includes:
		        * creating, initializing, and adding to the sub-state list
		          all of our sub-state-machines, and
		        * creating and starting our timer.
		        */
			    {
		    		super.initializingV();
			  	  initAndAddV( // Create and add orthogonal sub-state machine 1.
			  	  		theRemoteMeasurementState= new RemoteMeasurementState()
			  	  		);
			  	  initAndAddV( // Create and add orthogonal sub-state machine 2.
			  	  		theLocalMeasurementState= new LocalMeasurementState()
			  	  		);
			  	  statisticsTimerInput=  // Creating our timer. 
					  		new TimerInput(  //// Move to factory?
					  				theTimer,
					  				new Runnable() {
							        public void run() {
							        	  try { theLinkMeasurementState.stateHandlerB(); }
							        	  catch ( IOException theIOException) 
							        	    { delayedIOException= theIOException; }
							        	  }
							    		}
					  				);
		  	  	stateHandlerB(); // Calling the handler once to start timer.
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
			        	stateHandlerB();
			        return successB;
		        }

		    public synchronized void finalizingV() throws IOException
		      // This method processes any pending loose ends before shutdown.
		  	  {
		    	  super.finalizingV();
			  		stateHandlerB(); // This throws any saved IOException from timer.
		    		statisticsTimerInput.cancelingV(); // Stopping our timer.
		        }

			  public synchronized boolean stateHandlerB() throws IOException
			    /* The only difference between this handler and its superclass
			      is that this one re-throws any IOException that might have occurred
			      earlier when the machine was cycled by a timer thread.

			      This method is called by the Unicaster thread and 
			      the timer thread, both of which provide state-machine inputs.
			      */
			    { 
			  	  if  // Re-throw any previously saved exception from timer thread.
			  	    (delayedIOException != null) 
			  	  	throw delayedIOException;

			  	  return super.stateHandlerB(); // Calling superclass state handler.
			    	}
				
				private class LocalMeasurementState extends OrState 
		
				  /* This is the local concurrent sub-state 
				    for doing local measurements.  
				    */
		
				  {
		
						// The following States may be referenced and assigned to presentSubState.
					  // Create and add orthogonal sub-state machines.
						MeasurementPausedState theMeasurementPausedState;
						MeasurementHandshakesState theMeasurementHandshakesState;
		
				    public void initializingV() throws IOException 
					    {
				    		super.initializingV();
		
				    		// Create and add orthogonal sub-state machines.
					  	  initAndAddV(
					  	  		theMeasurementPausedState= new MeasurementPausedState()
					  	  		);
					  	  initAndAddV(
					  	  		theMeasurementHandshakesState= new MeasurementHandshakesState()
					  	  		);
		
								requestSubStateV( theMeasurementPausedState ); // Initialize sub-state.
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
		
				    private class MeasurementPausedState extends State 
					  
					  	{
					    	public void enterV() throws IOException
					    	  // Starts timer on state entry.
						  	  { 
					    	  	statisticsTimerInput.scheduleV(Config.handshakePause5000MsL);
					  				}

							  public boolean stateHandlerB()
							    /* This main state handler generates 
							      the pause between PS-PA handshakes.
							     	*/
							  	{ 
							  	  if (statisticsTimerInput.getInputArrivedB()) // Timer done. 
								  		requestStateV(theMeasurementHandshakesState);
										else  // Still waiting.
							    		setNoProgressInParentV();
							
							  	  return true;
							  		}

								public void exitV() throws IOException
								  // Cancels timer on state exit.
								  { 
										statisticsTimerInput.cancelingV();
										
										//// This will be moved back toSendingAndWaitingState later. 
				    			  retryTimeOutMsL=   // Initializing retry time-out.
				    			  		retransmitDelayMsNamedLong.getValueL();
										}
							
					  	}
		
						private class MeasurementHandshakesState extends State 
					  
					  	{
					    	public void enterV() throws IOException
						  	  { 
					  				}

							  public boolean stateHandlerB() throws IOException
							  	/* This state method handles the PS-PA handshakes, 
							  	  and retrying.
							  	  */
							  	{
							  	  	boolean runningB= true;
							  	  beforeExit: while(true) { 
							  	  beforeStartPausing: while(true) { 
		
							    		if // Doing state entry operations if this is state entry.
							    		  (! statisticsTimerInput.getInputScheduledB())
								    		{ // Sending PS and scheduling time-out, if not done yet.
							    			  statisticsTimerInput.scheduleV(retryTimeOutMsL);
								    			//appLogger.debug("handleSendingAndWaiting() scheduling "+retryTimeOutMsL);
									    		sendingSequenceNumberV();
									    		//% break beforeExit;
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
						    				    	continue beforeStartPausing;
								    					//% break beforeExit; // Going to send PS again.
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
							  	  requestStateV(theMeasurementPausedState);
								  	break beforeExit; 
						
						  	  } // beforeExit:
						  			return runningB;
						
						  	  }
							  
					  		}
					
					  protected void sendingSequenceNumberV() throws IOException
					    /* This method increments and writes 
					      the packet ID (sequence) number to the EpiOutputStream.
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
		
				private class RemoteMeasurementState extends State 
				
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

				} // class LinkMeasurementState

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

		//// This state and its subclasses AndState and OrState
		  do not [yet] provide 
		  * It does not provide behavioral inheritance, which is
		  	the most important benefit of hierarchical state machines.
		  * It does not handle synchronous inputs, aka events.
		    It handles only asynchronous inputs, variable values.
		  * //// Maybe merge AndState and OrState into State?
	  */

	protected State parentState= null;

  protected List<State> theListOfSubStates=
      new ArrayList<State>(); // Initially empty list.

  
  /* Methods used to build state objects. */
  
  public void initializingV() throws IOException
    /* This method initializes this state object, 
      It does actions needed when this state object is being bult.
      This is not the same as the entryV() method, which
      does actions needed when the associated state-machine state is entered.

      This version does nothing.
      It does not recursively initialize the sub-states because
      typically that is done by initAndAddV(..) as sub-states are added.
  		*/
    {
    	}

  public void initAndAddV(State theSubState) throws IOException
    /* This method is used as part of the State building process.  It:
      * adds theSubState this state's list of sub-states, and 
      * does an initialize of theSubState.
      It does them in this order because initialization needs to know 
      the parent state.
     	*/
  	{ 
  	  addV( theSubState ); // Add theSubState to list of sub-states.
  	    // This also sets the theSubState's parent state to be this state.

  		theSubState.initializingV(); // Initializing the added sub-state.
  	  }

  public void addV(State theSubState)
    /* This method adds/injects one sub-state to this state.
			It part of the State building process.  
      It adds theSubState to the state's sub-state list.
      It also sets the parent of the sub-state to be this state.
      */
  	{ 
  	  theListOfSubStates.add( theSubState ); // Add theSubState to
  	    // this state's list of sub-states.

  	  theSubState.setParentStateV( this ); // Store this state as
  	    // the sub-state's parent state.
  	  }

  public synchronized void finalizingV() throws IOException
    /* This method processes any pending loose ends before shutdown.
      In this case it finalizes each of its sub-states.
      This is not the same as the exitV() method, which
      does actions needed when the associated state-machine state is exited.
      */
	  {
	  	for (State subState : theListOfSubStates)
		  	{
	  			subState.finalizingV();
		  		}
      }

	public void setParentStateV( State parentState )
	  // Stores parentState as the parent state of this state.
		{ this.parentState= parentState; }

	
	/*  Methods which do actions for previously built State objects.  */
	
	public void enterV() throws IOException
	  /* This method is called when 
	    the state associated with this object is entered.
	    This version does nothing, but it should be overridden 
	    by subclasses that require entry actions.
	    This is not the same as initializingV(),
	    which does actions needed when the State object is being built.
	    */
	  { 
			}
	
	public boolean stateHandlerB() throws IOException
	  /* This is the default state handler. 
	    It does nothing because this is the superclass of all States.
	    All versions of this method should return 
	    * true to indicate that some computational input-processing progress 
	      was made,
	    * false otherwise.

	    This version does nothing and returns false because 
	    this is the superclass of all States.
	    */
	  { 
		  return false; 
		  }

	public void exitV() throws IOException
	  /* This method is called when a state is exited.
	    It does actions needed when a state is exited.
	    This version does nothing.
	    It should be overridden in subclasses that need exit actions.
	    */
	  { 
			}

	
	/*  Methods which return results from stateHandlerB().  
	  In addition to these methods, stateHandlerB() returns 
	  a boolean value which has its own meaning  
	  */

	protected void requestStateV(State nextState)
		/* This is called to change the state of a the state machine.
		  to the sibling state nextState.
		  It does this by calling the parent state's setNextSubStateV(..) method. 
		  Its affect is not immediate.
		  */
		{
			parentState.requestSubStateV(nextState);
			}
	
	protected void requestSubStateV(State nextState)
	  /* This method provides a link between setNextStateV(State)
	    and OrState.setNextSubStateV(State).
	    It is meaningful only in the OrState class,
	    and is meant to be overridden by the OrState class.
	    It has no meaning when inherited by other classes and throws an Error.
	    This method could be eliminated if setNextStateV(State)
	    casted parentState to an OrState.
	    */
	  {
			throw new Error();
			}

	protected void setNoProgressInParentV()
		{
			((OrState)parentState).setNoProgressV();
			}

	}  // State class 

class OrState extends State {

	/*  This class is the base class for all "or" state machine objects.
	  OrState machines have sub-states, but unlike AndStates,
	  only one sub-state can be active at a time.

	  There is no concurrency in an OrState machine, at least not at this level.
	  It sub-states are active one at a time.
		*/

  private State presentSubState= // State machine's state.
  		null; //// new State(); // Initial default no-op state.
        // null means no state is active.  non-null means that state is active.
  
  private State requestedSubState= null; // Becomes non-null when 
    // state-machine initializes and when machine's requests new state.
    //// ? Change to: null means deactivate state?  non-null means go to state.
  
	private boolean handlerRecordedProgressB= true; // setNoProgressV() clears.
	  // true means progress was made and recorded, false means waiting.

	private boolean handlerReturnedProgressB= true; // Returned by handlerB().
		// Waiting for input, false, is the exception.

	public boolean stateHandlerB() throws IOException
	  /* This handles the OrState by cycling it's machine.
	    It does this by calling the handler method 
	    associated with the present state-machine's sub-state.
      The sub-state might change with each of these calls.
      It keeps calling sub-state handlers until a handler returns false,
      indicating that it has processed all available inputs
      and is waiting for new input.
	    It returns true if any computational progress was made,
	    false otherwise.
	    
	    This method manipulates handlerRecordedProgressB in a way that combines
	    input from setNoProgress() and stateHandlerB() return values.
      */
	  { 
		  boolean anyProgressMadeB= false;
  		while (true) { // Cycle sub-states until done.
  	  		if  // Handling state change, if any, by doing exit and entry.
  	  		  (requestedSubState != null)
  	  			{ // Exit present state and enter requested one.
  	  			  if (presentSubState != null)
  	  			  	presentSubState.exitV(); 
	    	  		presentSubState= requestedSubState;
	    	  		//// Should check for sub-state validity vs. super-state here.
  	  			  if (presentSubState != null)
  	  			  	presentSubState.enterV(); 
			  			}
	  	  	{ // Calling handler and combining its results.
			  		handlerRecordedProgressB= true; // Assume progress.
			  		requestedSubState= null; // Preparing for transition request.
	  			  if (presentSubState != null) // Calling handler if state active.
	  			  	handlerReturnedProgressB= presentSubState.stateHandlerB();
	  	  		// handlerReturnedProgressB is needed so &= uses correct values.
	  	  		// Is this true if handlerRecordedProgressB is volatile?
	  	  		handlerRecordedProgressB &= handlerReturnedProgressB;
	  	  		}
	  	  	anyProgressMadeB |= handlerRecordedProgressB;
  	  	  if (!handlerRecordedProgressB) // Exiting loop if no progress made. 
  	  	  	break;
	  	  	}
			return anyProgressMadeB; // Returning overall progress result.
			}

	protected void setNoProgressV()
	  /* This is an alternate way for stateHandlerB() to indicate
	    that it made no progress and is waiting for new input.
	    The other way is for stateHandlerB() to return false.
	    */
	  {
	  	handlerRecordedProgressB= false;
			}
		
  protected void requestSubStateV(State nextState)
	  /* This method sets the state-machine state,
	    which is the same as this state's sub-state.
	    It overrides State.requestSubStateV(State nextState) to work.
	    
	    Note, though the requestedSubState variable changes immediately,
	    control is not transfered to the new sub-state until
	    the handler of the present sub-state exits.
	    */
	  {
  	  requestedSubState= nextState;
			}

	}  // OrState 

class AndState extends State {

	/* This class is the base class for all "and" state machine objects.
	  AndState machines have sub-states, but unlike OrStates,
	  all sub-states are active at the same time.
	
	  There is concurrency in an AndState machine, at least at this level.
	  */

	public boolean stateHandlerB() throws IOException
	  /* This method handles this AndState by cycling all of its sub-machines
	    until none of them makes any computational progress.
      It keeps going until all sub-state handlers return false,
      indicating that they have processed all available inputs
      and are waiting for new input.
	    This method returns true if any computational progress was made, 
	    false otherwise.
	    
    	//// rewrite loops for faster exit for 
    	 	minimum sub-state stateHandlerB() calls.
	    */
	  { 
		  boolean anythigMadeProgressB= false;
			boolean cycleMadeProgressB;
  	  do  // Cycling until no sub-machine progresses.
	  	  { // Cycle all sub-state-machines once each.
		  		cycleMadeProgressB= // Will be true if any machine progressed. 
		  				false;
	  	  	for (State subState : theListOfSubStates)
		  	  	{
	  	  		  boolean handlerMadeProgressB= subState.stateHandlerB(); 
		  	  		cycleMadeProgressB|= handlerMadeProgressB; 
		  	  		}
  	  		anythigMadeProgressB|= cycleMadeProgressB; 
	  	  	} while (cycleMadeProgressB);
			return anythigMadeProgressB; 
			}
  
	}  // AndState 
