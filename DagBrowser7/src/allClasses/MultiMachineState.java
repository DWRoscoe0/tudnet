package allClasses;

import java.io.IOException;
import java.util.Timer;

public class MultiMachineState extends AndState

  /* This is the root of a hierarchical state machine for Unicaster.
    The Unicaster deals exclusively with this state machine.
    This state machine manages concurrent sub-state machines.
    They can be of any type.
    More sub-state machines can be added as needed.
    */

	{
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong retransmitDelayMsNamedLong;
		private Timer theTimer; 

		// Sub-state machines.
		private LinkMeasurementState theLinkMeasurementState;
		
		MultiMachineState (  // Constructor.
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
	
		public void initializeWithIOExceptionV() throws IOException
		  /* This initializer method creates the sub-state machines
		    and adds them to this machine.
		    Then it calls the handler method to start machine timers.
		    */
		  {
				super.initializeWithIOExceptionState();

    		theLinkMeasurementState= new LinkMeasurementState( 
    				theTimer, 
    				theNetcasterInputStream,
    				theNetcasterOutputStream, 
    				retransmitDelayMsNamedLong 
	      		);
	  	  theLinkMeasurementState.initializeWithIOExceptionV();
	  	  addStateV( theLinkMeasurementState );
		  	}
		
		}
