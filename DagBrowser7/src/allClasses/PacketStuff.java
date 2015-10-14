package allClasses;

//import static allClasses.Globals.appLogger;

import java.net.DatagramPacket;

public class PacketStuff 
  /* This class contains methods to perform operations on DatagramPackets,
    because DatagramPacket can not be extended because it is a final class.
    */
  {
	 
		public static SockPacket makeSockPacket( )
		  // Create a SockPacket with a standard sized DatagramPacket buffer.
			{
				byte[] bufferBytes= new byte[ 512 ];
			  DatagramPacket theDatagramPacket=
			    new DatagramPacket( bufferBytes, bufferBytes.length );
			  SockPacket theSockPacket=  // Construct SockPacket.
			    new SockPacket( theDatagramPacket );
			  return theSockPacket;
				}
	 
		public static String gettingPacketString( DatagramPacket theDatagramPacket )
		  /* This method returns a String representation of theDatagramPacket.
		    If theDatagramPacket is null then null is returned.
		    */
		  {
		    String packetString= null; // Setting default null value.
		    calculatingString: {
		    	if ( theDatagramPacket == null) // Exiting if there is no packet.
		    		break calculatingString;// Exiting to use default value.
		    	packetString= // Calculating String from packet.
		      		theDatagramPacket.getAddress()+":"+theDatagramPacket.getPort()
  	          +";" + new String(
			          theDatagramPacket.getData()
			          ,theDatagramPacket.getOffset()
			          ,theDatagramPacket.getLength()
			          );
		      } // calculatingString: 
		  	return packetString; // Returning present and final value.
		    }

		}