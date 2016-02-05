package allClasses;

import java.net.DatagramPacket;
//import java.net.InetAddress;

public abstract class PacketManager< 
    K, // Key.
    E extends KeyedPacket<K> // Packets it manages. 
		>

  {
	  abstract E produceKeyedPacketE(DatagramPacket theDatagramPacket, K theK);
	    // Method which does new-operation, because new E() is not allowed. 
	  
		protected static final int DEFAULT_BUFFER_SIZE = 1024;

		// Injected variables.
    private final K theK;

		public PacketManager( K theK ) // Constructor.
			{
			  this.theK= theK;
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
	    // Produces an KeyedPacket with a default size empty buffer.
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
	       It might or might not be empty, depending on context.
	       */
	    { 
			  DatagramPacket theDatagramPacket= new DatagramPacket(
			  		bufferBytes, 0, sizeI
			  		);
			  E theKeyedPacketE= // Calling overridden abstract method to execute new. 
			  		produceKeyedPacketE( theDatagramPacket, theK );
			  return theKeyedPacketE;
	  	  }


	  // Methods for converting packets to String.
	  
		public static String gettingPacketString( DatagramPacket theDatagramPacket )
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
			    	resultString= // Calculating String from packet.
			      		theDatagramPacket.getAddress()+":"+theDatagramPacket.getPort();
			      } // calculatingString: 
			  	return resultString; // Returning present and final value.
			    }

  	}
