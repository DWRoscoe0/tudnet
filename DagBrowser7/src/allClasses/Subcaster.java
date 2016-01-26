package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;

import allClasses.LockAndSignal.Input;


public class Subcaster

	extends Streamcaster< String >

  implements Runnable 

	/* ?? This is being included in Unicasters, but it doesn't do anything, yet.
	  Eventually it will handle nested protocols for Unicaster.
	  */

	{
  
    // Injected dependencies.
	  private final NetOutputStream theNetOutputStream;
	  private final NetInputStream theNetInputStream;
	  private final Shutdowner theShutdowner;

    // Other instance variables.
    private boolean arbitratedYieldingB; // Used to arbitrate race conditions.

	  public Subcaster(  // Constructor. 
	      LockAndSignal netcasterLockAndSignal,
	      NetInputStream theNetInputStream,
	      NetOutputStream theNetOutputStream,
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
			      theNetOutputStream
		        );

	      // This class's injections.
	      this.theNetInputStream= theNetInputStream;
	      this.theNetOutputStream= theNetOutputStream;
	      this.theShutdowner= theShutdowner;
	      }

    public void run()  // Main Unicaster thread.
    	{
    	  try { // Needs work???
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
					        tryingPingSendV();
					        stateI= 1;
					        break;
					  	  case 1:
					        //appLogger.info(getName()+":\n  CALLING tryingPingReceiveV() ===============.");
					        tryingPingReceiveV();
					        stateI= 0;
					        break;
					  	  }
					    } // while(true)
					if  // Informing remote end whether we are doing Shutdown.
					  ( theShutdowner.isShuttingDownB() ) 
						{
				      appLogger.info( "SHUTTING-DOWN message sent.");
							}
					theNetOutputStream.close(); // Closing output stream.
					}
				catch( IOException e ) {
					Globals.logAndRethrowAsRuntimeExceptionV( 
							"run() IOException", e 
							);
			    }
				
				appLogger.info("run() exiting."); // Needed if thread self-terminates.
				/* ?? Subcaster do-op thread.
			  while (true) // Repeating until thread termination is requested.
				  {
	      		if // Exiting if requested.
	            ( Thread.currentThread().isInterrupted() ) 
	            break;
	      		netcasterLockAndSignal.doWaitE(); // Waiting for any input.
	        	}
	      ?? */
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

    private void tryingPingSendV() throws IOException
      /* This method does the sub-protocol of trying to 
        send a ping to the remote peer and receiving an echo response.
        If it doesn't receive an echo packet in response
        within one-half period, it tries again.
        It tries several times before giving up and 
        terminating the current thread.
        If it receives a ping instead of an echo then
        it might give up this sub-protocol.
        */
      {
    		LockAndSignal.Input theInput;  // Type of input that ends waits.
        int maxTriesI= 3;
        int triesI= 1;
        pingEchoRetryLoop: while (true) { // Sending ping and receiving echo.
          triesI+= 4; //??? to temporarily disable execution of following.
          if  // Checking and exiting if maximum attempts have been done.
            ( triesI > maxTriesI )  // Maximum attempts exceeded.
            { // Terminating thread.
              appLogger.info(
              		"Requesting termination after "
                	+"\n  "+maxTriesI+" ECHO time-outs."
                  );
              Thread.currentThread().interrupt(); // Starting termination.
              break pingEchoRetryLoop;
              }
          sendingMessageV("PING"); // Sending ping packet.
      		pingSentNanosL= System.nanoTime(); // Recording ping send time in ns
          long pingSentMsL= System.currentTimeMillis(); // and in ms.
          echoWaitLoop: while (true) { // Handling echo or other conditions.
            theInput=  // Awaiting next input within reply interval.
            		testWaitInIntervalE( pingSentMsL, HalfPeriodMillisL );
            if  // Exiting echo wait if time-out.
            	( theInput == LockAndSignal.Input.TIME )
              { appLogger.info( "Time-out waiting for ECHO: "+triesI );
                break echoWaitLoop;  // End wait to send new PING, maybe.
              	}
          	if // Handling SHUTTING-DOWN packet or interrupt by exiting.
    	    		( tryingToCaptureTriggeredExitB( ) )
  	    			break pingEchoRetryLoop; // Exit everything.
            theNetInputStream.mark(0); // Preparing to not consume message.
        		String inString= readAString(); // Reading message.
        		if ( inString.equals( "ECHO" ) ) // Handling echo, maybe.
              { // Handling echo and exiting.
                RoundTripTimeNamedInteger.setValueL(
                		(System.nanoTime() - pingSentNanosL)
                		); // Calculating RoundTripTime.
            		break pingEchoRetryLoop; // Exit everything.
                }
        		if ( inString.equals( "PING" ) ) // Handling ping conflict, maybe.
              { // Handling ping conflict.
                appLogger.info( "PING-PING conflict." );
              	if ( arbitratedYieldingB ) // Arbitrating ping-ping conflict.
                  { // Yielding ping processing to other peer.
                    appLogger.info( "PING ping yield: "+triesI );
                		theNetInputStream.reset(); // Putting message back.
                    break pingEchoRetryLoop; // Yielding by exiting main loop.
                    }
              		else
              		{ appLogger.info( "PING ping not yielding: "+triesI );
              			// Ignoring this PING.
                    break echoWaitLoop;  // End wait to send new PING, maybe.
              			}
              	  }
            //appLogger.debug( 
        		//		"tryingPingSendV(): unexpected: "
        		//	+ inString
        		//	+ " from "
        		//	+ PacketStuff.gettingPacketString( 
        		//			theNetInputStream.getSockPacket().getDatagramPacket()
        		//			)
        		//	);
        		// Ignoring the message that was gotten, whatever it was.
            } // echoWaitLoop:
          triesI++;
          } // pingEchoRetryLoop: 
        }

    private void tryingPingReceiveV() throws IOException
	    /* This method does the sub-protocol of trying to 
		    receive a PING message from the remote peer 
		    to which it replies by sending an ECHO response.
        If a PING is not immediately available then
        it waits up to PeriodMillisL for a PING to arrive,
        after which it gives up and returns.
        If a PING is received it responds immediately by sending an ECHO,
        after which it waits PeriodMillisL while ignoring all
        received messages except SHUTTING-DOWN.
        It will exit immediately if isInterrupted() becomes true
        or a SHUTTING-DOWN message is received.
        */
      {
        LockAndSignal.Input theInput;  // Type of input that ends wait.
        long pingWaitStartMsL= System.currentTimeMillis();
        pingWaitLoop: while (true) { // Processing until something causes exit.
      		if // Handling SHUTTING-DOWN packet or interrupt by exiting.
	    			( tryingToCaptureTriggeredExitB( ) )
	    			break pingWaitLoop;
      		// Note, can't readAString() here because it might not be available.
          if // Handling a received ping if present.
            ( tryingToGetStringB( "PING" ) )
            { // Handling received ping, then exit.
              sendingMessageV("ECHO"); // Sending echo packet as reply.
              long echoSentMsL= System.currentTimeMillis();
              while (true) { // Ignoring packets for a while, then exit.
                theInput=  // Awaiting input within the ignore interval.
                		testWaitInIntervalE( echoSentMsL, PeriodMillisL );
                if ( theInput == Input.TIME ) // Exiting if time limit reached.
                  break pingWaitLoop;  
            		if // Exiting everything if exit has been triggered.
            			( tryingToCaptureTriggeredExitB( ) )
            			break pingWaitLoop;
            		readAString(); // Reading message and ignoring it.
              	} // while (true)
              }
      		tryingToGetString(); // Reading and ignoring any message.

          theInput= testWaitInIntervalE( // Awaiting next input.
          		pingWaitStartMsL, PeriodMillisL + HalfPeriodMillisL
          		);
          if ( theInput == Input.TIME ) // Exiting outer loop if time-out.
	          {
	            appLogger.info( "Time-out waiting for PING." );
	            break pingWaitLoop;  // Exiting to abort wait.
	          	}
          } // pingWaitLoop
        }

		}
