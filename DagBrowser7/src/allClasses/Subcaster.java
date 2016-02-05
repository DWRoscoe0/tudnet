package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;


public class Subcaster

	extends Streamcaster< String >

  implements Runnable 

	/* ?? This is being included in Unicasters, but it doesn't do anything, yet.
	  Eventually it will handle nested protocols for Unicaster.
	  */

	{
  
    // Injected dependencies.
	  private final NetcasterOutputStream theNetcasterOutputStream;
	  private final NetInputStream theNetInputStream;
	  private final Shutdowner theShutdowner;

    // Other instance variables.
    private boolean arbitratedYieldingB; // Used to arbitrate race conditions.

	  public Subcaster(  // Constructor. 
	      LockAndSignal netcasterLockAndSignal,
	      NetInputStream theNetInputStream,
	      NetcasterOutputStream theNetcasterOutputStream,
	      DataTreeModel theDataTreeModel,
	      String keyString,
	      Shutdowner theShutdowner
	      )
	    {
	      super( // Superclass's constructor injections.
		        theDataTreeModel,
		      	"Subcaster",
	      	  keyString,
	      	  netcasterLockAndSignal,
			      theNetInputStream,
			      theNetcasterOutputStream
		        );

	      // This class's injections.
	      this.theNetInputStream= theNetInputStream;
	      this.theNetcasterOutputStream= theNetcasterOutputStream;
	      this.theShutdowner= theShutdowner;
	      }

    public void run()  // Main Unicaster thread.
    	{
    	  try { /// Needs work.
	    		initializingV();
	
			  	int stateI= // Initialize ping-reply protocol state from yield flag. 
			  			arbitratedYieldingB ? 0 : 1 ;
					while (true) // Repeating until thread termination is requested.
					  {
							if   // Exiting if requested.
					      ( Thread.currentThread().isInterrupted() ) 
					      break;
						  switch ( stateI ) { // Decoding alternating state.
					  	  case 0:
					        //appLogger.info(getName()+":\n  CALLING tryingPingSendV() ===============.");
					        ///tryingPingSendV();
					        stateI= 1;
					        break;
					  	  case 1:
					        //appLogger.info(getName()+":\n  CALLING tryingPingReceiveV() ===============.");
					        ///tryingPingReceiveV();
					        stateI= 0;
					        break;
					  	  }
					    } // while(true)
					if  // Informing remote end whether we are doing Shutdown.
					  ( theShutdowner.isShuttingDownB() ) 
						{
				      appLogger.info( "SHUTTING-DOWN message sent.");
							}
					theNetcasterOutputStream.close(); // Closing output stream.
					}
				catch( IOException e ) {
					Globals.logAndRethrowAsRuntimeExceptionV( 
							"run() IOException", e 
							);
			    }
				
				appLogger.info("run() exiting."); // Needed if thread self-terminates.
    		finalizingV();
    		}

    protected void initializingV()
	    {
    		appLogger.info("initializingV() at start."); // Needed if thread self-terminates.
		    addB( 	theNetcasterOutputStream.getCounterNamedInteger() );
		    addB( 	theNetInputStream.getCounterNamedInteger() );
	    	}

    protected void finalizingV()
	    {
    		appLogger.info("finalizingV() for exit."); // Needed if thread self-terminates.
	    	}

		}
