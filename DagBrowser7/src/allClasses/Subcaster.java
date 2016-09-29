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
	      DataTreeModel theDataTreeModel,
	      String keyString,
	      Shutdowner theShutdowner,
        boolean leadingB
	      )
	    {
	      super( // Superclass's constructor injections.
		        theDataTreeModel,
		      	"Subcaster",
		        theShutdowner,
		        leadingB,
	      	  keyString,
	      	  streamcasterLockAndSignal,
	      	  theSubcasterInputStream,
			      theSubcasterOutputStream
		        );

	      // This class's injections.
	      this.theSubcasterInputStream= theSubcasterInputStream;
	      this.theSubcasterOutputStream= theSubcasterOutputStream;
	      }

    public void run()  // Main Unicaster thread.
    	{
    	  try {
	    		initializingV();
	
					while (true) // Repeating until thread termination is requested.
					  {
							if ( EpiThread.exitingB() ) break; // Exiting if requested. 
							pingReplyProtocolV();
			    		streamcasterLockAndSignal.doWaitE(); // Waiting for any input.
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

    protected void initializingV() throws IOException
      /* Adds the packet counters for the input and output streams and
        does Streamcaster initializing.
        */
	    {
    		appLogger.info("initializingV() at start."); // Needed if thread self-terminates.
		    addB( 	theSubcasterOutputStream.getCounterNamedInteger() );
		    addB( 	theSubcasterInputStream.getCounterNamedInteger() );
		    super.initializingV();
	    	}

    protected void finalizingV()
	    {
    		appLogger.info("finalizingV() for exit."); // Needed if thread self-terminates.
	    	}

		}
