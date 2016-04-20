package allClasses;

import static allClasses.Globals.appLogger;

import java.net.DatagramPacket;
import java.net.InetAddress;
//import java.net.InetAddress;

public abstract class PacketManager< 
    K, // Key.
    E extends KeyedPacket<K> // Packets it manages. 
		>

  /* The class manages KeyedPackets.
    Presently it only allocates them and their buffers.
    It might eventually manage a pool of recycled packets and buffers.

    This class can be used in two different ways, depending on
    whether a non-null K key is provided.

    * If a non-null K key is provided then it stores the key in 
      the packets it produces.  In this case the PacketManager 
      is being used exclusively by a particular Streamcaster
      which is associated with that key.

    * If a non-null K key is NOT provided then 
      it stores a null for the key in the packets it produces.  
      In this case the PacketManager is probably being used by 
      a packet receiver thread or threads to produce empty packets.
      A key will be stored in each packet after it has been filled with
      received data and the Streamcaster to which the packet is destined
      has been determined.
      
    ////Add and use a pool ability to allow recycle and reuse packets.
    */
  {
	  abstract E produceKeyedPacketE(DatagramPacket theDatagramPacket );
	    /* This method must be provided by a subclass.
	      Its purpose is to do a new-operation, because 
	      new E() is not allowed in this generic class. 
	     */

		protected static final int DEFAULT_BUFFER_SIZE = 1024;

		// Injected variables.
    protected final K theKeyK;

		public PacketManager( K theK ) // Constructor.
			{
			  this.theKeyK= theK;
				}

		
		// Buffer array producers.
		
	  public byte[] produceDefaultSizeBufferBytes()
	    // Produces a byte buffer array of the default size.
	    { return produceBufferBytes( DEFAULT_BUFFER_SIZE ); }

	  public byte[] produceBufferBytes( int sizeI )
	  	// Produces a byte buffer array of size sizeI.
	    { return new byte[ sizeI ]; }


	  // KeyedPacket producers.

	  public E produceKeyedPacket()
	    // Produces a KeyedPacket with a default size empty buffer.
	    { 
	  	  byte[] bufferBytes= produceDefaultSizeBufferBytes();
			  E theKeyedPacketE= produceKeyedPacketE( 
			  		bufferBytes, 
			  		bufferBytes.length
			  		);
			  return theKeyedPacketE;
	  	  }

	  public E produceKeyedPacketE(
	  		byte[] bufferBytes, 
	  		int sizeI
	  		)
	    /* Produces a KeyedPacket with a buffer bufferBytes 
	       with only the first sizeI bytes significant.
	       The buffer might or might not be empty, depending on context.
	       */
	    { 
			  DatagramPacket theDatagramPacket= new DatagramPacket(
			  		bufferBytes, 0, sizeI
			  		);
			  E theKeyedPacketE= // Calling overridden abstract method to execute new. 
			  		produceKeyedPacketE( theDatagramPacket );
			  return theKeyedPacketE;
	  	  }


	  // Methods for packet logging, mostly for debugging.

		public static void logUnconnectedReceiverPacketV( 
				DatagramPacket theDatagramPacket 
				)
			{
			  ///* 
			  appLogger.debug(
	      		PacketManager.gettingDirectedPacketString(
	      				theDatagramPacket, false
	      				)
	      		);
	      //*/
	    	}

		public static void logSenderPacketV( 
				DatagramPacket theDatagramPacket 
				)
			{
			  // /*
	    	if ( ! theDatagramPacket.getAddress().isMulticastAddress() )
				  appLogger.debug(
		      		PacketManager.gettingDirectedPacketString(
		      				theDatagramPacket, true
		      				)
		      		);
		    // */
	    	}

		public static void logMulticastReceiverPacketV( 
				DatagramPacket theDatagramPacket 
				)
			{
			  /* 
			  appLogger.debug(
	      		PacketManager.gettingDirectedPacketString(
	      				theDatagramPacket, false
	      				)
	      		);
	      */
	    	}

	  // Methods for converting packets to Strings for display.

		public static String gettingDirectedPacketString( 
				DatagramPacket theDatagramPacket, boolean sentB 
				)
		  /* This method returns a String representation of theDatagramPacket
		    along with it's direction: 
		    * sentB==true means packet was being sent.
		    * sentB==false means packet was being received.
		    */
		  {
		    String resultString= sentB ? "  to " : "from ";
	    	resultString+= gettingPacketAddressString(theDatagramPacket);
	    	resultString+= sentB ? " went " : " came ";
	    	resultString+= new String(
		          theDatagramPacket.getData()
		          ,theDatagramPacket.getOffset()
		          ,theDatagramPacket.getLength()
		          );
		  	return resultString; // Returning present and final value.
		    }

		public static String gettingPacketString( 
				DatagramPacket theDatagramPacket 
				)
		  /* This method returns a String representation of theDatagramPacket.
		    If theDatagramPacket is null then null is returned.
		    */
		  {
		    String resultString= null; // Setting default null value.
		    calculatingString: {
		    	if ( theDatagramPacket == null) // Exiting if there is no packet.
		    		break calculatingString;// Exiting to use default value.
		    	resultString= // Calculating String from packet.
		    			gettingPacketAddressString(theDatagramPacket)
  	          +";" 
		      		+ new String(
			          theDatagramPacket.getData()
			          ,theDatagramPacket.getOffset()
			          ,theDatagramPacket.getLength()
			          );
		      } // calculatingString: 
		  	return resultString; // Returning present and final value.
		    }
		 
			private static String gettingPacketAddressString( 
					DatagramPacket theDatagramPacket 
					)
			  /* This method returns a String representation of 
			    theDatagramPacket IP and port.
			    If theDatagramPacket is null then null is returned.
			    */
			  {
			    String resultString= null; // Setting default null value.
			    calculatingString: {
			    	if ( theDatagramPacket == null) // Exiting if there is no packet.
			    		break calculatingString;// Exiting to use default value.
			    	resultString= "";
			    	resultString+= gettingString(theDatagramPacket.getAddress());
			    	resultString+= ":";
			    	resultString+= theDatagramPacket.getPort();
			      } // calculatingString: 
			  	return resultString; // Returning present and final value.
			    }
			 
			private static String gettingString( 
					InetAddress theInetAddress 
					)
			  /* This method returns a String representation of theInetAddress.
			    This address representation is fixed for columnar alignment.
			    */
			  {
					String resultString= "";
					if ( theInetAddress == null )
						resultString+= "!null-InetAddress!";
					else
						{
							byte[] addressBytes= theInetAddress.getAddress();
					    for (int indexI= 0; indexI < addressBytes.length; indexI++)
						    {
						    	if (indexI != 0) resultString+= ".";
						    	resultString+= String.format(
						    			"%03d", addressBytes[indexI] & 0xFF
						    			);
						    	}
							}
			  	return resultString; // Returning present and final value.
			    }

  	}
