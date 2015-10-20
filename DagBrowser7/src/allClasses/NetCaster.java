package allClasses;

//import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import allClasses.LockAndSignal.Input;

//import static allClasses.Globals.*;  // appLogger;

public class NetCaster 

	extends MutableList

	// This class is the superclass of Unicaster and Multicaster.

	{
		protected InetSocketAddress remoteInetSocketAddress;  // Address of peer.
		public final InputQueue<SockPacket> // Send output.
	    sendQueueOfSockPackets;  // SockPackets to be sent.
    
    // Detail-containing child sub-objects.
	    protected NamedMutable addressNamedMutable;
	    protected NamedMutable portNamedMutable;
	    protected NamedInteger packetsSentNamedInteger;
	    protected NamedInteger packetsReceivedNamedInteger;
		  
    LockAndSignal threadLockAndSignal=  // LockAndSignal for this thread.
			// LockAndSignal for inputs to this thread.  It is used in
      // the construction of the following queue. 
        new LockAndSignal(false); 

    int packetIDI; // Sequence number for sent packets.
    
    protected PacketQueue receiveQueueOfSockPackets=
      new PacketQueue( threadLockAndSignal );
        // For SockPackets from receiver thread.

    NetOutputStream theNetOutputStream;
		NetInputStream theNetInputStream;

	  public NetCaster(  // Constructor. 
	      DataTreeModel theDataTreeModel,
	      InetSocketAddress remoteInetSocketAddress,
	      InputQueue<SockPacket> sendQueueOfSockPackets,
	      String namePrefixString
	      )
	    {
	      super( // Constructing MutableList superclass.
		        theDataTreeModel,
		        namePrefixString + 
    	          remoteInetSocketAddress.getAddress() +
    	          ":" + remoteInetSocketAddress.getPort(),
	          new DataNode[]{} // Initially empty of children.
	      		);
	
	      this.remoteInetSocketAddress= remoteInetSocketAddress;
	      this.sendQueueOfSockPackets= sendQueueOfSockPackets;

        packetIDI= 0; // Setting starting packet sequence number.

    	  theNetInputStream= new NetInputStream(
  	    		receiveQueueOfSockPackets, 
  	    		remoteInetSocketAddress.getAddress(),
            remoteInetSocketAddress.getPort()
  	    		);
    	  theNetOutputStream= new NetOutputStream(
    	  		sendQueueOfSockPackets, 
    	  		remoteInetSocketAddress.getAddress(),
            remoteInetSocketAddress.getPort()
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
		    
		    addB( 	portNamedMutable= new NamedMutable( 
			      		theDataTreeModel, "Port", "" + remoteInetSocketAddress.getPort()
			      		)
		    			);
		
		    addB( 	packetsSentNamedInteger= new NamedInteger( 
			      		theDataTreeModel, "Packets-Sent", 0 
			      		)
		    			);
		
		    addB( 	packetsReceivedNamedInteger= new NamedInteger( 
			      		theDataTreeModel, "Packets-Received", 0 
			      		)
		    			);
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
	        		threadLockAndSignal.intervalRemainingMsL( startMsL, lengthMsL ); 
	        if // Exiting if time before, or more likely after, time interval.
	          ( remainingMsL == 0 )
	          { theInput= Input.TIME; break process; }
	    		if ( testingMessageB( ) ) // Exiting if a notification input is ready.
		        { theInput= Input.NOTIFICATION; break process; }
    	  	theInput= // Doing general wait. 
    	  			threadLockAndSignal.doWaitWithTimeOutE( remainingMsL );
    			}

    	  return theInput;
	      }

    protected boolean testingMessageB( String aString ) throws IOException
      /* This method tests whether the packet, if any,
        at the head of the receiveQueueOfSockPackets,
        contains aString.
        It returns true if there is a packet and
        it is aString, false otherwise.
        The packet, if any, remains in the queue.
        */
      { 
        boolean resultB= false;  // Assuming aString is not present.
        decodingPacket: {
          String packetString= // Getting string from packet if possible. 
          	peekingMessageString( );
          if ( packetString == null ) // Exiting if no packet or no string.
            break decodingPacket;  // Exiting with false.
          if   // Exiting if the desired String is not in packet String.
            ( ! packetString.contains( aString ) )
            break decodingPacket;  // Exiting with false.
          resultB= true;  // Changing result because Strings are equal.
          } // decodingPacket:
        return resultB;  // Returning the result.
        }

    private boolean testingMessageB( ) throws IOException
      /* This method tests whether a packet, if any,
        at the head of the receiveQueueOfSockPackets, is available.
        It returns true if there is a packet available, false otherwise.
        */
      { 
        return ( 
        		//receiveQueueOfSockPackets.peek() 
        		getOrTestString( null, false )
        		!= 
        		null 
        		);
        }

    private String peekingMessageString( ) throws IOException
      /* This method returns the String in the next received packet
        in the queue, if there is one.  
        If there's no packet then it returns null.
        */
      { 
	      return getOrTestString( null, false );
	      }

    protected String getOrTestString( String desiredString, boolean consumeB) 
    		throws IOException
      /* This is a possibly temporary method,
        through which all packet reading will pass
        at least during the transition from packet io to stream io.

        This method tries to get or test for desiredString in the input.
        consumeB means consume any acceptable string, 
        otherwise do a test only.
        
        If no bytes are available then it returns null.
        Otherwise it reads an entire string, 
        after blocking if necessary to read new packets.
        If desiredString==null then it returns the read string.
        If desiredString!=null and the read string contains desiredString
        then it returns the read string, null otherwise. 
        If consumedB is true and a a non-null string is being returned,
        then the read string is consumed and can not be read
        from the stream again.
        */
      {
    	  String readString= "";
	    	String returnString= null;
        parsing: {
  				theNetInputStream.mark(0); // Marking now  in case we reset() later.
					if // Exiting if no bytes available. 
					  ( 0 >= theNetInputStream.available() )
						break parsing;
  				while (true) { // Reading and accumulating all bytes in string.
  					int byteI= theNetInputStream.read();
  					readString+= (char)byteI;
  					if ( '.' == byteI ) break; // Exiting if terminator seen.
  				  }
         	} // parsing: 
        testing: {
          if ( readString == "" ) // Exiting if no packet or no string.
            break testing; // Exiting with null.
          if ( desiredString == null ) // Exiting if any string is acceptable.
	          { returnString= readString; // Using read string as result.
	            break testing;  // Exiting with string.
	            }
          if   // Exiting if the desired String is the one read.
            ( readString.contains( desiredString ) )
	          { returnString= readString; // Using read string as result.
		          break testing;  // Exiting with string.
		          }
          } // testing:
        consuming: {
          if ( ! consumeB ) // Exiting if consuming not requested.
            {
		  				theNetInputStream.reset(); // Backup stream to start.
	          	break consuming; // Exiting 
	          	}
          if ( returnString == null) // Exiting if no string to consume. 
          	break consuming;
          packetsReceivedNamedInteger.addValueL( 1 );  // Counting the packet.
          } // consuming:
      	return returnString;
      	}

    protected boolean tryingToConsumeOneMessageB() throws IOException
      /* This method consumes one message.
        It returns true if a message was consumed,
        false if there was none to consume.
        */
      {
	      return ( 
	      		getOrTestString( null, true)
	      		!= 
	      		null 
	      		);
	      }

    // Send packet code.  This might be enhanced with streaming.

      protected void OLDsendingMessageV( String aString ) throws IOException
        /* This method sends a packet containing aString to the peer.
          It does NOT use NetOutputStream.  It accesses packets directly.
          It prepends a packet ID number.
          */
        {
      	  String payloadString= ((packetIDI++) + ":" + aString + ".");
          //appLogger.debug( "sendingMessageV(): " + payloadString );
          byte[] buf = payloadString.getBytes();
          DatagramPacket packet = new DatagramPacket(
            buf, 
            buf.length, 
            remoteInetSocketAddress.getAddress(),
            remoteInetSocketAddress.getPort()
            );
          SockPacket aSockPacket= new SockPacket(packet);
          sendQueueOfSockPackets.add( // Queuing packet for sending.
              aSockPacket
              );

          packetsSentNamedInteger.addValueL( 1 );
          }

      protected void sendingMessageV( String aString ) throws IOException//??
        /* This method sends a packet containing aString to the peer.
          It uses NetOutputStream instead of accessing packets directly.
          It prepends a packet ID number.
          It does it using a NetOutputStream.
          */
        {
          String payloadString= ((packetIDI++) + ":" + aString) + ".";
          //appLogger.debug( "sendingMessageV(): " + payloadString );
          byte[] buf = payloadString.getBytes();
          
          theNetOutputStream.write(buf); // Writing it to memory.
          //theNetOutputStream.write('.'); // Writing terminator.
          theNetOutputStream.flush(); // Sending it in packet.

          packetsSentNamedInteger.addValueL( 1 );
          }

		}
