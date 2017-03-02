package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
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

	  This class makes use of an internal non-static class LinkMeasurementState.
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
				/*   //%
				super( // Constructing MutableList superclass with injections.
  		    theDataTreeModel,
	        "LinkMeasurements",
          new DataNode[]{} // Initially empty array of children.
      		);
				*/   //%
  	  	initializingV(
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
		    /* This method is called by
		      the Unicaster thread's initializingV() method.
		      It creates most of the variable values needed,
		      including ones added to the DAG for display.
		      */
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
		  	  addB( theLinkMeasurementState );
				  }

	    public synchronized boolean handleInputB(
	    		String keyString
	    		) 
	  		throws IOException
	  		/* This method is called to process a possible PS or PA message input.
  		  It returns true if one of those messages is processed,
  		  meaning keyString was one of them and it was consumed.
  		  It returns false otherwise.

  		  It does its processing by calling methods in the sub-machines.
  		  It cycles the main machine if input was processed.
  		  
  		  This was previously called processMeasurementMessageB(keyString).

  		  //// If state machines are used more extensively to process
  		    messages of this type, it might make sense to:
  		    * Cache the State that processes the keyString in a HashMap
  		      and use the HashMap to dispatch the message.
  		    * Add discrete event processing to State machines and
  		      make these keyString messages a subclass of those events.
  		  */
	  		{
	    		return theLinkMeasurementState.handleInputB(keyString);
	    	  }
			
		  public synchronized void finalizingV() throws IOException
		    // This method processes any pending loose ends before shutdown.
			  {
		  	  theLinkMeasurementState.finalizingV(); // Finalizing state-machine.
	        }

	  private long sentSequenceNumberTimeNsL;

		private long lastSequenceNumberSentL;

		private class LinkMeasurementState extends AndState 

			/* This class is the root State for, and nested within, 
			  the LinkMeasurement class. 
			 	*/

			{
			  private TimerInput theTimerInput; // Used for state machine 
			    // pauses and time-outs. 
				private IOException delayedIOException= null; /* For re-throwing 
					an exception which occurred in one thread which couldn't handle it, 
			    in another thread that can.
			    */

				// Sub-state machine instances.
				@SuppressWarnings("unused")
				private RemoteMeasurementState theRemoteMeasurementState;
				@SuppressWarnings("unused")
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
			  	  		(theRemoteMeasurementState= new RemoteMeasurementState())
			  	  		);
			  	  initAndAddV( // Create and add orthogonal sub-state machine 2.
			  	  		theLocalMeasurementState= new LocalMeasurementState()
			  	  		);
			  	  theTimerInput=  // Creating our timer. 
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

		    public synchronized void finalizingV() throws IOException
		      // This method processes any pending loose ends before shutdown.
		  	  {
		    	  super.finalizingV();
			  		stateHandlerB(); // This throws any saved IOException from timer.
		    		theTimerInput.cancelingV(); // Stopping our timer.
		        }

			  public synchronized boolean stateHandlerB() throws IOException
			    /* The only difference between this handler and its superclass
			      is that this one first checks for and re-throws 
			      any IOException that might have occurred earlier 
			      when the machine was cycled by a timer thread.
			      //// This kluge might not be needed if Timer is replaced by
			       java.util.concurrent.ScheduledThreadPoolExecutor.

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
		
				  /* This is the local concurrent/orthogonal sub-state 
				    for doing local measurements.  
						It partner is orthogonal sub-state RemoteMeasurementState. 
				    */
		
				  {
		
						// sub-states/machines.
						private MeasurementPausedState theMeasurementPausedState;
						private MeasurementInitializationState
						  theMeasurementInitializationState;
						private MeasurementHandshakesState theMeasurementHandshakesState;
		
				    public void initializingV() throws IOException 
					    {
				    		super.initializingV();
		
				    		// Create and add orthogonal sub-state machines.
					  	  initAndAddV(
					  	  		theMeasurementPausedState= new MeasurementPausedState()
					  	  		);
					  	  initAndAddV(
					  	  		theMeasurementInitializationState= 
					  	  		  new MeasurementInitializationState()
					  	  		);
					  	  initAndAddV(
					  	  		theMeasurementHandshakesState= new MeasurementHandshakesState()
					  	  		);
		
								requestSubStateV( theMeasurementPausedState ); // Initial state.
					    	}
		
				    private void processPacketAcknowledgementV() 
								throws IOException
						  /* This input method processes the "PA" sequence number 
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

								See processSequenceNumberB(..) about "PS", for more information.
								*/
					  	{
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
						
						// States.
		
				    private class MeasurementPausedState extends State
				    
				      /* This class does the pause between measurement handshakes.
				        It finishes by requesting the MeasurementHandshakesState.
				       	*/
					  
					  	{
					    	public void enterV() throws IOException
					    	  /* Starts timer for the pause interval before 
					    	    the next handshake.
					    	   	*/
						  	  {
					    	  	theTimerInput.scheduleV(Config.handshakePause5000MsL);
					  				}

							  public void stateHandlerV()
							    /* Waits for the end of the pause interval.
							     	*/
							  	{ 
							  	  if (theTimerInput.getInputArrivedB()) // Timer done. 
							  	  	requestStateV(theMeasurementInitializationState);
							  		}

								public void exitV() throws IOException
								  // Cancels timer and initializes the handshake time-out.
								  { 
										theTimerInput.cancelingV();
										}
							
					  	} // class MeasurementPausedState

						private class MeasurementInitializationState extends State 
					  	/* This state does nothing but initializes for the handshake,
					  	  then moves on to the MeasurementHandshakesState.
					  	  */

					  	{
							  public void stateHandlerV() throws IOException
							  	{
										//// This will be moved back to he
							  	  //// MeasurementHandshakesState later
							  	  //// when that state is split into two levels.
				    			  retryTimeOutMsL=   // Initializing retry time-out.
				    			  		retransmitDelayMsNamedLong.getValueL();

							  		requestStateV(theMeasurementHandshakesState);
						  	  	}

					  		} // class MeasurementInitializationState
						
						private class MeasurementHandshakesState extends State 
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
				    			  theTimerInput.scheduleV(retryTimeOutMsL);
						    		sendingSequenceNumberV();
					  				}

							  public void stateHandlerV() throws IOException
							  	/* This method handles handshakes acknowledgement, 
							  	  initiating a retry using twice the time-out,
							  	  until the acknowledgement is received,
							  	  or giving up if the time-out limit is reached.
							  	  */
							  	{
									  if (tryInputB("PA")) // Test and process acknowledgement.
										  { processPacketAcknowledgementV();
											  requestStateV(theMeasurementPausedState);
										  	}
						      	else if (theTimerInput.getInputArrivedB()) // Time-out. 
							    		{ if ( retryTimeOutMsL <= Config.maxTimeOut5000MsL )
						    				  { retryTimeOutMsL*=2;  // Doubling time-out limit.
						    				  	requestStateV(this); // Retrying by repeating state.
							  					  }
						    			  else // Giving up after maximum time-out reached.
										  	  requestStateV(theMeasurementPausedState);
							    		      // Do again after a pause.
							    			}
						  	  	}
							  
					  		} // class MeasurementHandshakesState 

						public void exitV() throws IOException
						  // Cancels asknowledgement timer.
						  { 
								theTimerInput.cancelingV();
								}
					
					  protected void sendingSequenceNumberV() throws IOException
					    /* This method, at the beginning of the handshake,
					      increments and writes the packet ID (sequence) number 
					      to the EpiOutputStream.  
					      
					      //// It doesn't flush().
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
					    which helps the remote peer make measurements.
					    It does this by responding with "PA" messages to 
					    "PS" messages sent from the remote peer.
					    It also records some information that is displayed locally.
					    This state doesn't have any sub-states.
					    It exists mainly to document its role along with
					    its orthogonal partner sub-state LocalMeasurementState.
					    */

				  public void stateHandlerV() throws IOException
				  	/* This method handles handshakes acknowledgement, 
				  	  initiating a retry using twice the time-out,
				  	  until the acknowledgement is received,
				  	  or giving up if the time-out limit is reached.
				  	  */
				  	{
						  if (tryInputB("PS" )) // Test and process PS message.
							  processPacketSequenceNumberV(); // Process PS message body.
						  // Staying in same state.
							}
		
						private void processPacketSequenceNumberV() 
								throws IOException
						  /* This method processes the "PS" message from the remote peer.
						    The "PS" is assumed to have been processed already.
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

						    Every packet has a sequence number, but 
						    not every packet needs to contain its sequence number.
						    The reception of a "PS" message with its sequence number 
						    means that the remote has sent at least that number of packets.
						    The difference of the highest sequence number received and
						    the number of packets received is the number of packets lost.
						    A new difference and a loss ratio average can be calculated
						    each time a new sequence number is received.  In fact,
						    that is how reception of a sequence number can be interpreted.
						
								//// Sequence numbers and other numbers eventually 
								  need to be converted to use modulo (wrap-around) arithmetic.
							  */
							{
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
						
							} // class RemoteMeasurementState

				} // class LinkMeasurementState

		} // class LinkMeasurements
