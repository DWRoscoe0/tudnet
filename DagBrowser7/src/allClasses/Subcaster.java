package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;


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
        NamedLong retransmitDelayMsNamedLong
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
			      retransmitDelayMsNamedLong
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
							if ( EpiThread.exitingB() ) ///org rename to exitRequestedB()? 
								break; // Exiting if requested. 
							///tmp pingReplyProtocolV();
			    		theLockAndSignal.waitingForInterruptOrNotificationE(); // Waiting for any input.
					    } // while(true)
					theSubcasterOutputStream.close(); // Closing output stream.
					}
				catch( IOException e ) {
					Globals.logAndRethrowAsRuntimeExceptionV( 
							"run() IOException", e 
							);
			    }
				
				appLogger.info("run() exiting."); // Needed if thread self-terminates.
    		finalizingV();
    		}

    public void initializeV()
      /* Adds the packet counters for the input and output streams and
        does Streamcaster initializing.
        */
	    {
    		appLogger.info("initializeV() at start."); // Needed if thread self-terminates.
		    super.initializeV();
		    addAtEndB( 	theSubcasterOutputStream.getCounterNamedLong() );
		    addAtEndB( 	theSubcasterInputStream.getCounterNamedLong() );
	    	}

    protected void finalizingV()
	    {
    		appLogger.info("finalizingV() for exit."); // Needed if thread self-terminates.
	    	}

		}
