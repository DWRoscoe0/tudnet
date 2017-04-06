package allClasses;

import java.io.IOException;
import java.util.Timer;

public class MultiMachineState extends AndState 

	{
	  // Injected dependencies.
		private NetcasterInputStream theNetcasterInputStream;
		private NetcasterOutputStream theNetcasterOutputStream; 
		private NamedLong retransmitDelayMsNamedLong;
		private Timer theTimer; 

		// Other instance variables.
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
		  {
				super.initializeWithIOExceptionState();

    		theLinkMeasurementState= new LinkMeasurementState( 
    				theTimer, 
    				theNetcasterInputStream,
    				theNetcasterOutputStream, 
    				retransmitDelayMsNamedLong 
	      		);
	  	  theLinkMeasurementState.initializeWithIOExceptionV();
	  	  addB( theLinkMeasurementState ); // This includes stream counts.
	  	  theLinkMeasurementState.stateHandlerB(); // To start theTimer.
		  	}
		
    protected void finalizingV() throws IOException
	    // This is the opposite of initilizingV().
	    {
    		theLinkMeasurementState.finalizeV();
	    	}

    public synchronized boolean handleSynchronousInputB( String keyString ) 
  		throws IOException
  		/* This method passes the input to the sub-state.
				*/
  		{
    		return theLinkMeasurementState.handleSynchronousInputB(keyString);
    	  }

		}
