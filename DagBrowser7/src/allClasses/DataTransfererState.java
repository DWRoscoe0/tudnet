package allClasses;

import java.io.IOException;
import java.util.Timer;

public class DataTransfererState

  extends AndState
  
  /* This class contains a [hierarchical] state machine 
    that transfer data to and from a Unicaster peer.
    This code is not thread-safe.  It is meant to be called only from:
    * a Unicaster thread, and
    * the thread belonging to a timer that this code uses.
    
    This code is under devvelopment.
    */

  { 
    // Injected dependencies.
    @SuppressWarnings("unused")
    private NetcasterInputStream theNetcasterInputStream;
    @SuppressWarnings("unused")
    private NetcasterOutputStream theNetcasterOutputStream; 
    private NamedLong initialRetryTimeOutMsNamedLong;
    private Timer theTimer; 
    
    // Sub-state machine instances.
    @SuppressWarnings("unused")
    private DataReceiverState theDataReceiverState;
    @SuppressWarnings("unused")
    private DataSenderState theDataSenderState;

    DataTransfererState(  // Constructor.
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

        measurementTimerInput= // Creating our timer input and linking to this state. 
            new TimerInput(  ///? Move to factory or parent?
                theTimer,
                this
                );

        // Create and add to DAG the sub-states of this and-state-machine.
        initAndAddStateListV( theDataReceiverState= 
            new DataReceiverState() );
        initAndAddStateListV( theDataSenderState= 
            new DataSenderState() );

        // Adding measurement count.
        addAtEndB( new NamedLong(
            "Measurement-Handshakes", 0 ) );

        addAtEndB( initialRetryTimeOutMsNamedLong );

        return this;
        }
  
    public synchronized void finalizeV() throws IOException
      // This method processes any pending loose ends before shutdown.
      {
        super.finalizeV();
        onInputsB(); // This throws any saved IOException from timer.
        measurementTimerInput.cancelingV(); // To stop our timer.
        }
    
    private class DataSenderState extends OrState 
  
      /* This is the local concurrent/orthogonal sub-state 
        for doing local measurements of the Unicaster link.  
        It partner is orthogonal sub-state DataReceiverState. 
        */
  
      {

        public StateList initializeWithIOExceptionStateList() throws IOException 
          {
            super.initializeWithIOExceptionStateList();
  
            return this;
            }
  
        public void onExitV() throws IOException
          // Cancels acknowledgement timer.
          { 
            measurementTimerInput.cancelingV();
            super.onExitV();
            }
        
        // Other support code.

    
        } // class LocalMeasurementState
  
    private class DataReceiverState extends StateList 
    
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
  
        private boolean processPacketSequenceNumberB() //// 
            throws IOException
          /* This method finishes processing the "PS" message.
            The "PS" header is assumed to have been processed already.
            It is followed by ...
                        */
          {
            boolean successB= true;
            return successB;
            }
      
      } // class RemoteMeasurementState

    
    // Variables for managing round trip time.
    protected NsAsMsNamedLong rawRoundTripTimeNsAsMsNamedLong;
      private TimerInput measurementTimerInput;
      // This is the TimerInput used by this state machine for pauses and time-outs. 

    } // class LinkMeasurementState
