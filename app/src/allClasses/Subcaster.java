package allClasses;


import static allClasses.AppLog.theAppLog;


public class Subcaster

  extends Streamcaster< 
    String,
    SubcasterPacket,
    SubcasterQueue,
    SubcasterPacketManager,
    SubcasterInputStream,
    SubcasterOutputStream
    >

  implements Runnable 

  /* 
    This class extends the UDP Streamcaster.
    The main thing it adds is knowledge of using a String as a name
    which Streamcaster uses as a key and to differentiate 
    this Subcaster from other Subcasters.
    A Subcaster is a Streamcaster that is nested within
    the data stream of a Unicaster.
    */

  {
  
    // Injected dependencies.
    private final SubcasterOutputStream theSubcasterOutputStream;
    private final SubcasterInputStream theSubcasterInputStream;

    // Other instance variables.  none.

    public Subcaster(  // Constructor. 
        LockAndSignal streamcasterLockAndSignal,
        SubcasterInputStream theSubcasterInputStream,
        SubcasterOutputStream theSubcasterOutputStream,
        String keyString,
        Shutdowner theShutdowner,
        DefaultBooleanLike leadingDefaultBooleanLike,
        NamedLong initialRetryTimeOutMsNamedLong
        )
      {
        super( // Superclass's constructor injections.
            "Subcaster",
            theShutdowner,
            leadingDefaultBooleanLike,
            keyString,
            streamcasterLockAndSignal,
            theSubcasterInputStream,
            theSubcasterOutputStream,
            initialRetryTimeOutMsNamedLong
            );

        // This class's injections.
        this.theSubcasterInputStream= theSubcasterInputStream;
        this.theSubcasterOutputStream= theSubcasterOutputStream;
        }

    public void run()  // Main Unicaster thread.
      {
        try {
          initializeV();
  
          while (true) // Repeating until thread termination is requested.
            {
              if ( EpiThread.testInterruptB() ) ///org rename to exitRequestedB()? 
                break; // Exiting if requested. 
              ///tmp pingReplyProtocolV();
              theLockAndSignal.waitingForInterruptOrNotificationE(); // Waiting for any input.
              } // while(true)
          Closeables.closeAndReportTimeUsedAndThrowExceptionsV(theSubcasterOutputStream); // Closing output stream.
          }
        catch( Exception e ) {
          Misc.logAndRethrowAsRuntimeExceptionV( 
              "run() IOException", e 
              );
          }
        
        theAppLog.info("run() exiting."); // Needed if thread self-terminates.
        finalizingV();
        }

    public void initializeV()
      /* Adds the packet counters for the input and output streams and
        does Streamcaster initializing.
        */
      {
        theAppLog.info("initializeV() at start."); // Needed if thread self-terminates.
        super.initializeV();
        addAtEndV(   theSubcasterOutputStream.getCounterNamedLong() );
        addAtEndV(   theSubcasterInputStream.getCounterNamedLong() );
        }

    protected void finalizingV()
      {
        theAppLog.info("finalizingV() for exit."); // Needed if thread self-terminates.
        }

    }
