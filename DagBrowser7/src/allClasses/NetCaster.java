package allClasses;

//import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import allClasses.LockAndSignal.Input;

public class NetCaster 
	
	extends MutableList

	// This class is the superclass of Unicaster and Multicaster.
	
	{
		protected InetSocketAddress remoteInetSocketAddress;  // Address of peer.
		public final InputQueue<SockPacket> // Send output.
	    sendQueueOfSockPackets;  /// SockPackets for ConnectionManager to send?
    
    // Detail-containing child sub-objects.
	    protected NamedMutable addressNamedMutable;
	    protected NamedMutable portNamedMutable;
	    protected NamedInteger packetsSentNamedInteger;
	    protected NamedInteger packetsReceivedNamedInteger;
		  
    LockAndSignal threadLockAndSignal=  // LockAndSignal for this thread.
			// LockAndSignal for inputs to this thread.  It is used in
      // the construction of the following queue. 
        new LockAndSignal(false); 

    ///private String cachedString= "";  // Used for parsing packet data.
      // This is a copy of the next string in the NetInputStream.

    int packetIDI; // Sequence number for sent packets.
    
    protected PacketQueue receiveQueueOfSockPackets=
    // Queue for SockPackets from unconnected receiver thread.
    /// References to this for input are being replaces by 
    /// InputStream references.
      new PacketQueue( threadLockAndSignal );
        // For SockPackets from ConnectionManager.

    NetOutputStream theNetOutputStream;
		NetInputStream theNetInputStream;

	  public NetCaster(  // Constructor. 
	      DataTreeModel theDataTreeModel,
	      InetSocketAddress remoteInetSocketAddress,
	  		InputQueue<SockPacket> sendQueueOfSockPackets,
	      String namePrefixString
	      )
	    {
	      super( // Constructing MutableList.  
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
      /* ??? Being converted to give TIME priority.
       * 
       * This is a special test-and-wait method which will return immediately 
        with Input.NOTIFICATION if a received packet 
        is available for processing,
        otherwise it will do a LockAndSignal.doWaitWithIntervalE(..).
        So it might block, or it might not.
        NOTIFICATION has priority over TIME,
        even if the time limit has already passed.
        This is not the normal way the LockAndSignal wait methods work.
       */
	    {
    		LockAndSignal.Input theInput;

    		process: {
	        final long remainingMsL= 
	        		threadLockAndSignal.intervalRemainingMsL( startMsL, lengthMsL ); 
	        if // Exiting if time before, but more likely after, time interval.
	          ( remainingMsL == 0 )
	          { theInput= Input.TIME; break process; } // Exiting loop.
	    		if ( testingMessageB( ) ) ///
		        { theInput= Input.NOTIFICATION; break process; }
    	  	theInput= // Doing general wait. 
		    	///	  threadLockAndSignal.doWaitWithIntervalE(
		    	///  	    startMsL,
		    	///     		lengthMsL
		    	///     		);
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
        at the head of the receiveQueueOfSockPackets,
        is available.
        It returns true if there is a packet available, false otherwise.
        */
      { /// Marker
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
        As a side-effect it stores the string, or null, in cachedString.  
        */
      { /// Marker
    	  /* ???
        calculatingString: {
  				if ( cachedString != "" ) // Exiting if String is cached. 
  				  break calculatingString;
          SockPacket receivedSockPacket= // Testing queue for a packet.
            receiveQueueOfSockPackets.peek();
          if (receivedSockPacket == null) // Exiting if no packet.
            break calculatingString;
          DatagramPacket theDatagramPacket= // Getting DatagramPacket.
            receivedSockPacket.getDatagramPacket();
          cachedString= // Calculating and caching String from packet.
            PacketStuff.gettingPacketString( theDatagramPacket );
          } // calculatingString: 
	    	return cachedString; // Returning whatever is now in cache.
	    	??? */

	      return getOrTestString( null, false );
	      }

    protected String getOrTestString( String desiredString, boolean consumeB) 
    		throws IOException
      /* ??? This method is being converted to use NetInputStream.

        This is a new, possibly temporary method,
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
        If consumedB is true and a a non-null string is returned,
        then the read string is consumed and can not be read
        from the stream again.

        As a side-effect it stores the read string, or null, in cachedString.  
        */
      {
	    	///String oldCachedString= cachedString; // debug.
    	  String cachedString= ""; /// Rename to readString.
	    	String returnString= null;
        parsing: {
  				theNetInputStream.mark(0); // Marking now  in case we reset() later.
  				///if ( cachedString != "" ) // Exiting if String already cached. 
  				///  break parsing;
					if // Exiting if no bytes available. 
					  ( 0 >= theNetInputStream.available() )
						break parsing;
  				while (true) { // Reading all bytes in string.
  					int byteI= theNetInputStream.read();
  					cachedString+= (char)byteI;
  					if ( '.' == byteI ) break; // Exiting if terminator seen.
  				  }
         	} // parsing: 
        testing: {
          if ( cachedString == "" ) // Exiting if no packet or no string.
            break testing; // Exiting with null.
          if ( desiredString == null ) // Exiting if any string is acceptable.
	          { returnString= cachedString; // Using read string as result.
	            break testing;  // Exiting with string.
	            }
          if   // Exiting if the desired String is the one read.
            ( cachedString.contains( desiredString ) )
	          { returnString= cachedString; // Using read string as result.
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
        	// Stream has already been advanced.
          //appLogger.debug( "consuming from InputStream: "+cachedString );
        	///cachedString= ""; // Emptying the string cache.
          packetsReceivedNamedInteger.addValueL( 1 );  // Counting the packet.
          } // consuming:
	      ///appLogger.debug( 
        ///  	"getOrTestString(..) \n"
        ///+"  old="+oldCachedString
        ///+"  new="+cachedString
        ///);
      	return returnString;
      	}

    /* ???
    protected String OLDgetOrTestString( /// 
    		String desiredString, boolean consumeB
    		)
      /* This is a new, possibly temporary method,
        through which all packet reading will pass
        at least during the transition from packet io to stream io.

        This method tries to get or test for desiredString in the input.
        consumeB means consume any acceptable packet, otherwise test only.
        Returns reference to desired string, or null if desired string
        was not seen.
        If desiredStringB==null then any input string is acceptable.  
      	As a side-effect it stores the string, or null, in cachedString.  
       */
    /* ???
      {
    	  String returnString= null;
        parsing: {
  				if ( cachedString != "" ) // Exiting if String is cached. 
  				  break parsing;
          SockPacket receivedSockPacket= // Testing queue for a packet.
            receiveQueueOfSockPackets.peek();
          if (receivedSockPacket == null) // Exiting if no packet.
            break parsing;
          DatagramPacket theDatagramPacket= // Getting DatagramPacket.
            receivedSockPacket.getDatagramPacket();
          cachedString= // Calculating and caching String from packet.
            PacketStuff.gettingPacketString( theDatagramPacket );
          } // parsing: 
        testing: {
          if ( cachedString == "" ) // Exiting if no packet or no string.
            break testing; // Exiting with null.
          if ( desiredString == null ) // Exiting if any string is acceptable.
	          { returnString= cachedString; // Using cached string as result.
	            break testing;  // Exiting with string.
	            }
          if   // Exiting if the desired String is in packet String.
            ( cachedString.contains( desiredString ) )
	          { returnString= desiredString; // Using desired string as result.
	            break testing;  // Exiting with string.
	            }
          } // testing:
        consuming: {
          if ( ! consumeB ) // Exiting if consuming not requested.
          	break consuming;
          //appLogger.debug( "consuming packet with: "+cachedString );
          if ( returnString == null) // Exiting if nothing to consume. 
          	break consuming;
        	receiveQueueOfSockPackets.poll(); // Removing head of queue.
        	cachedString= ""; // Emptying the string cache.
          packetsReceivedNamedInteger.addValueL( 1 );  // Counting the packet.
          } // consuming:
      	return returnString;
      	}
    ??? */

    protected boolean tryingToConsumeOneMessageB() throws IOException /// Packet.
      /* This method consumes one packet, if any,
        at the head of the queue.
        It returns true if a packet was consumed,
        false if there was none to consume.
        */
      { /// Marker
	      /* ??
	      boolean processingPacketB= 
	      	( peekingMessageString( ) != null );
	      if ( processingPacketB ) // Consuming the packet if there is one.
	        {
	        	receiveQueueOfSockPackets.poll(); // Removing head of queue.
	        	cachedString= ""; // Empty the string cache/flag.
	          packetsReceivedNamedInteger.addValueL( 1 );  // Count the packet.
	          }
	  	  return processingPacketB;
	  	  ??? */
    	
	      return ( 
	      		getOrTestString( null, true)
	      		!= 
	      		null 
	      		);
	      }

    // Send packet code.  This might be enhanced with streaming.

      protected void sendingMessageV( String aString ) throws IOException
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

      protected void NEWsendingMessageV( String aString ) throws IOException///
        /* This method sends a packet containing aString to the peer.
          It uses NetOutputStream instead of accessing packets directly.
          It prepends a packet ID number.
          It does it using a NetOutputStream.
          */
        {
      		//appLogger.debug( "sending: "+aString );
      		
          String payloadString= ((packetIDI++) + ":" + aString);
          //appLogger.info( "sendingMessageV(): " + payloadString );
          byte[] buf = payloadString.getBytes();
          
          theNetOutputStream.write(buf); // Writing it to memory.
          theNetOutputStream.write('.'); // Writing terminator.
          theNetOutputStream.flush(); // Sending it in packet.

          packetsSentNamedInteger.addValueL( 1 );
          }

		}
