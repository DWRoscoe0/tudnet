package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;

import allClasses.LockAndSignal.Input;

public class Streamcaster< K >

	extends DataNodeWithKey< K >

  /* This is the base class is for streaming with UDP packets, but
    it doesn't know anything about packet IP addresses or ports.
    That is handled by subclass NetCaster.
    */

	{
  
	  // Constants.
	  protected final long PeriodMillisL=  // Period between sends or receives.
	    4000;   // 4 seconds.
	  protected final long HalfPeriodMillisL= // Half of period.
	    PeriodMillisL / 2;  

    protected final LockAndSignal netcasterLockAndSignal;
			// LockAndSignal for inputs to this thread.  It is used in
      // the construction of the following queue. 

	  protected final NetOutputStream theNetOutputStream;
		protected final NetInputStream theNetInputStream;

		protected long pingSentNanosL; // Time the last ping was sent.
		int packetIDI; // Sequence number for sent packets.

    // Detail-containing child sub-objects.
      protected NamedInteger RoundTripTimeNamedInteger;
        // This is an important value.  It can be used to determine
        // how long to wait for a message acknowledgement before
        // re-sending a message.

	  public Streamcaster(  // Constructor.
	      DataTreeModel theDataTreeModel,
	      String typeString,
	  		K theKeyK,
	  		LockAndSignal netcasterLockAndSignal,
	      NetInputStream theNetInputStream,
	      NetOutputStream theNetOutputStream
	  		)
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableList superclass.
		        theDataTreeModel,
		        typeString, // Type name but not entire name.
		        theKeyK
	      		);

	      this.theNetInputStream= theNetInputStream;
	      this.theNetOutputStream= theNetOutputStream;
	      this.netcasterLockAndSignal= netcasterLockAndSignal;
	      }

    protected Input testWaitInIntervalE( long startMsL, long lengthMsL) 
    		throws IOException
      /* This is a special test-and-wait method with 
        different input priorities than the LockAndSignal wait methods.
        The priorities used here are:
		      TIME
		      NOTIFICATION
		      INTERRUPTION
       */
	    {
    		LockAndSignal.Input theInput;

    		process: {
	        final long remainingMsL= 
	          netcasterLockAndSignal.intervalRemainingMsL( startMsL, lengthMsL ); 
	        if // Exiting if time before, or more likely after, time interval.
	          ( remainingMsL == 0 )
	          { theInput= Input.TIME; break process; }
	    		//if ( testingMessageB( ) ) // Exiting if a notification input is ready.
	        if // Exiting if a notification input is ready.
	          ( theNetInputStream.available() > 0 )
		        { theInput= Input.NOTIFICATION; break process; }
    	  	theInput= // Doing general wait. 
    	  	  netcasterLockAndSignal.doWaitWithTimeOutE( remainingMsL );
    			}

    	  return theInput;
	      }

    protected boolean tryingToCaptureTriggeredExitB( ) throws IOException
      /* This method tests whether exit has been triggered, meaning either:
        * The current thread's isInterrupted() is true, or
        * The next packet, if any, at the head of the receiverToNetCasterPacketQueue,
		    	contains "SHUTTING-DOWN", indicating the remote node is shutting down.
		    	If true then the packet is consumed and
		    	the current thread's isInterrupted() status is set true.
		    This method returns true if exit is triggered, false otherwise.
		    */
      { 
        if // Trying to get and convert SHUTTING-DOWN packet to interrupt status. 
          ( tryingToGetStringB( "SHUTTING-DOWN" ) )
	        {
	          appLogger.info( "SHUTTING-DOWN packet received.");
	          Thread.currentThread().interrupt(); // Interrupting this thread.
	          }
	      return Thread.currentThread().isInterrupted();
        }

    protected boolean tryingToGetStringB( String aString ) throws IOException
      /* This method tries to get a particular String aString.
        It consumes the String and returns true if the desired string is there, 
        otherwise it does not consume the message and returns false.
        */
      {
  			boolean gotStringB= false;
    		theNetInputStream.mark(0); // Marking stream position.
    		String inString= tryingToGetString();
    	  gotStringB=  // Testing for desired string.
    	  		aString.equals( inString );
    	  if ( ! gotStringB ) // Resetting position if String is not correct.
    	  	theNetInputStream.reset();
    	  return gotStringB;
      	}

    protected String tryingToGetString( ) throws IOException
    /* This method tries to get any String.
      It returns a String if there is one available, null otherwise.
      */
    {
			String inString= null;
			if // Overriding if desired string is able to be read. 
			  ( theNetInputStream.available() > 0 )
				{
	    	  inString= readAString();
	    	  }
  	  return inString;
    	}

		protected String readAString()
  		throws IOException
  		/* This method reads and returns one String ending in the first
  		  delimiterChar='.' from theNetInputStream.  It blocks if needed.
  		  The String returned includes the delimiter.
  		 */
			{
			  final char delimiterChar= '.';
				String readString= "";
				while (true) { // Reading and accumulating all bytes in string.
					if ( theNetInputStream.available() <= 0 ) // debug.
						{
							readString+="!NOT-AVAILABLE!";
		          appLogger.error( "readAString(): returning " + readString );
							break;
	  					}
					int byteI= theNetInputStream.read();
					if ( delimiterChar == byteI ) break; // Exiting if terminator seen.
					readString+= (char)byteI;
					}
				return readString;
				}

    // Send packet code.

      protected void sendingMessageV( String aString ) throws IOException//??
        /* This method sends a packet containing aString to the peer.
          It uses NetOutputStream instead of accessing packets directly.
          It prepends a packet ID number.
          It does it using a NetOutputStream.
          */
        {
          String payloadString=
          		"N."
          		+((packetIDI++)+ "." 
          		+ aString) + ".";
          //appLogger.debug( "sendingMessageV(): " + payloadString );
          byte[] buf = payloadString.getBytes();
          
          theNetOutputStream.write(buf); // Writing it to memory.
          theNetOutputStream.flush(); // Sending it in packet.
          }

	}
