package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.InetSocketAddress;

import allClasses.LockAndSignal.Input;

//import static allClasses.Globals.*;  // appLogger;

public class NetCaster 

	extends MutableList

	// This class is the superclass of Unicaster and Multicaster.

	{
		protected InetSocketAddress remoteInetSocketAddress;  // Address of peer.
    private LockAndSignal netcasterLockAndSignal;
			// LockAndSignal for inputs to this thread.  It is used in
      // the construction of the following queue. 
    
    // Some detail-containing child sub-objects.  Others are in streams.
	    protected NamedMutable addressNamedMutable;
	    protected NamedMutable portNamedMutable;

    protected NetOutputStream theNetOutputStream;
		protected NetInputStream theNetInputStream;

    int packetIDI; // Sequence number for sent packets.

	  public NetCaster(  // Constructor. 
	      LockAndSignal netcasterLockAndSignal,
	      NetInputStream theNetInputStream,
	      NetOutputStream theNetOutputStream,
	      DataTreeModel theDataTreeModel,
	      InetSocketAddress remoteInetSocketAddress,
	      String namePrefixString
	      )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableList superclass.
		        theDataTreeModel,
		        namePrefixString + 
    	          remoteInetSocketAddress.getAddress() +
    	          ":" + remoteInetSocketAddress.getPort(),
	          new DataNode[]{} // Initially empty of children.
	      		);

	      // This class's injections.
	      this.netcasterLockAndSignal= netcasterLockAndSignal;
	      this.remoteInetSocketAddress= remoteInetSocketAddress;
	      this.theNetInputStream= theNetInputStream;
	      this.theNetOutputStream= theNetOutputStream;

        packetIDI= 0; // Setting starting packet sequence number.
		    portNamedMutable= new NamedMutable( 
			    theDataTreeModel, "Port", "" + remoteInetSocketAddress.getPort()
			  	);
	      }

    protected void initializeV()
	    throws IOException
	    {
    		addB( 	addressNamedMutable= new NamedMutable( 
		        theDataTreeModel, 
		        "IP-Address", 
		        "" + remoteInetSocketAddress.getAddress()
		      	)
					);
		    addB( 	portNamedMutable );
		    addB( 	theNetOutputStream.getCounterNamedInteger() );
		    addB( 	theNetInputStream.getCounterNamedInteger());
	    	}
    
		InetSocketAddress getInetSocketAddress()
			{ return remoteInetSocketAddress; }

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

    protected boolean testingMessageB( String aString ) throws IOException
      /* This method tests whether the next message String in 
        the next received packet in the queue, if there is one,  is aString.
        It returns true if so, false otherwise.
        The message is not consumed, so can be read later.
        */
      { 
        boolean resultB= false;  // Assuming aString is not present.
        decodingPacket: {
          String packetString= // Getting string from packet if possible. 
          	peekingMessageString( );
          if ( packetString == null ) // Exiting if no packet or no string.
            break decodingPacket;  // Exiting with false.
          if   // Exiting if the desired String is not in packet String.
          	( ! packetString.equals( aString ) )
            break decodingPacket;  // Exiting with false.
          resultB= true;  // Changing result because Strings are equal.
          } // decodingPacket:
        return resultB;  // Returning the result.
        }

    private String peekingMessageString( ) throws IOException
      /* This method returns the next message String in 
        the next received packet in the queue, if there is one.  
        If there's no message then it returns null.
        The message is not consumed, so can be read later.
        */
      { 
    		String inString= null;
	  		if ( theNetInputStream.available() > 0) // Reading string if available.
		  		{
			  		theNetInputStream.mark(0); // Marking stream position.
			  	  inString= readAString();
		  	  	theNetInputStream.reset(); // Resetting so String is not consumed.
			  		}
	  	  return inString;
	  		}

		String readAString()
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
