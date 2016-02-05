package allClasses;

import java.net.DatagramPacket;

public class SubcasterPacketManager

  extends PacketManager<String,SubcasterPacket>

  {
		// Injected variables.
	    // None.

		public SubcasterPacketManager(
				String theString
				)
			{
			  super(
						theString
						);
				}

    // Superclass abstract methods.
		
		SubcasterPacket produceKeyedPacketE( 
				DatagramPacket theDatagramPacket, String theString
				)
			{ return null; } ///


		// Methods for producing NetcasterPackets.

	  public NetcasterPacket makeSize512NetcasterPacket( )
		  // Create a NetcasterPacket with a standard sized DatagramPacket buffer.
			{
				byte[] bufferBytes= new byte[ 512 ];
			  DatagramPacket theDatagramPacket=
			    new DatagramPacket( bufferBytes, bufferBytes.length );
			  NetcasterPacket theNetcasterPacket=  // Construct NetcasterPacket.
			    new NetcasterPacket( theDatagramPacket, (IPAndPort)null ); ///
			  return theNetcasterPacket;
				}


	  // Methods for converting to String.
	  
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

