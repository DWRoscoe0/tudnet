package allClasses;

import java.io.IOException;
import java.util.Timer;

public class MultiMachineState extends AndState

  /* This is the root of a hierarchical state machine for Unicaster.
    The Unicaster deals exclusively with this state machine.
    This state machine manages concurrent sub-state machines.
    Those sub-machines can be of any type.
    More sub-machines can be added as needed.
    */

	{
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong retransmitDelayMsNamedLong;
		private Timer theTimer; 
		private Unicaster theUniaster;

		/* Sub-state-machines.
		  Though theHelloMachineState receives input first, 
		  it normally receives little, 
		  and only during connection establishment,
		  so for efficiency it is placed last.
		  */
		@SuppressWarnings("unused")
		private LinkMeasurementState theLinkMeasurementState;
		@SuppressWarnings("unused")
		private HelloMachineState theHelloMachineState;
		
		MultiMachineState (  // Constructor.
				Timer theTimer, 
			  NetcasterInputStream theNetcasterInputStream,
				NetcasterOutputStream theNetcasterOutputStream,
				NamedLong retransmitDelayMsNamedLong,
				Unicaster theUniaster
				)
			throws IOException
	  	{
  	  	// Injected dependencies.
			  this.theNetcasterInputStream= theNetcasterInputStream;
			  this.theNetcasterOutputStream= theNetcasterOutputStream;
			  this.retransmitDelayMsNamedLong= retransmitDelayMsNamedLong;
			  this.theTimer= theTimer;
			  this.theUniaster= theUniaster;
			  }
	
		public void initializeWithIOExceptionV() throws IOException
		  /* This initializer method creates the sub-state machines
		    and adds them to this machine.
		    Then it calls the handler method to start machine timers.
		    */
		  {
				super.initializeWithIOExceptionStateList();

				// Construct, initialize, and add sub-states to this machine.
	  	  addStateListV( 
	  	  		( theLinkMeasurementState= new LinkMeasurementState( 
		    				theTimer, 
		    				theNetcasterInputStream,
		    				theNetcasterOutputStream, 
		    				retransmitDelayMsNamedLong 
			      		) 
	  	  			).initializeWithIOExceptionStateList() 
	  	  		);

	  	  addStateListV( 
	  	  		( theHelloMachineState= new HelloMachineState( 
		    				theTimer, 
		    				theNetcasterInputStream,
		    				theNetcasterOutputStream, 
		    				retransmitDelayMsNamedLong,
		    				theUniaster )
	  	  			).initializeWithIOExceptionStateList() 
	  	  		);
		  	}
		
		}
