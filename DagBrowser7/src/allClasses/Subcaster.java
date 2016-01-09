package allClasses;

import static allClasses.Globals.appLogger;

public class Subcaster

	extends DataNodeWithKey< String >

  implements Runnable 

	/* ?? This is being included in Unicasters, but it doesn't do anything, yet.
	  Eventually it will handle nested protocols for Unicaster.
	  */

	{
	  public final LockAndSignal theLockAndSignal; // For inputs to this thread.
	  
	  private final NetOutputStream theNetOutputStream;
	  private final NetInputStream theNetInputStream;

	  public Subcaster(  // Constructor. 
	      LockAndSignal theLockAndSignal,
	      NetInputStream theNetInputStream,
	      NetOutputStream theNetOutputStream,
	      DataTreeModel theDataTreeModel,
	      String keyString
	      )
	    {
	      super( // Superclass's constructor injections.
		        theDataTreeModel,
		      	"Subcaster",
	      	  keyString
		        );

	      // This class's injections.
	      this.theLockAndSignal= theLockAndSignal;
	      this.theNetInputStream= theNetInputStream;
	      this.theNetOutputStream= theNetOutputStream;
	      }

    public void run()  // Main Unicaster thread.
    	{
    		initializingV();
        while (true) // Repeating until thread termination is requested.
	        {
	      		if // Exiting if requested.
	            ( Thread.currentThread().isInterrupted() ) 
	            break;
	      		theLockAndSignal.doWaitE(); // Waiting for any input.
	        	}
    		finalizingV();
    		}

    protected void initializingV()
	    {
    		appLogger.info("initializingV() at start."); // Needed if thread self-terminates.
		    addB( 	theNetOutputStream.getCounterNamedInteger() );
		    addB( 	theNetInputStream.getCounterNamedInteger() );
	    	}

    protected void finalizingV()
	    {
    		appLogger.info("finalizingV() for exit."); // Needed if thread self-terminates.
	    	}

		}
