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

	/* ?? This is being included in Unicasters, but it doesn't do anything, yet.
	  Eventually it will handle nested protocols for Unicaster.
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
							if   // Exiting if requested.
					      ( EpiThread.exitingB() ) 
					      break;
							pingReplyProtocolV(); //////
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
