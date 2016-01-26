package allClasses;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class PacketManager

  {
		protected static final int DEFAULT_BUFFER_SIZE = 1024;

		// Injected variables.
		private final InetAddress unicasterInetAddress; 
		private final int unicasterPortI;

		public PacketManager(
				InetAddress unicasterInetAddress, 
				int unicasterPortI
				)
			{
			  this.unicasterInetAddress= unicasterInetAddress;
			  this.unicasterPortI= unicasterPortI;
				}

	  public byte[] produceBufferBytes()
	    { return new byte[DEFAULT_BUFFER_SIZE]; }

	  public NetcasterPacket produceNetcasterPacket(
	  		byte[] bufferBytes, 
	  		int sizeI
	  		)
	    /* This is used by:
	      * NetcasterPacketManager, used by NetOutStream.flush().
	      * UnconnectedReceiver.run() for received packets.
	      * PacketStuff, used by Multicaster.run().
	     */
	    { 
			  DatagramPacket theDatagramPacket= new DatagramPacket(
			  		bufferBytes, 0, sizeI, unicasterInetAddress, unicasterPortI
			  		);
		    NetcasterPacket theNetcasterPacket= 
		    		new NetcasterPacket(theDatagramPacket);
	  	  return theNetcasterPacket; 
	  	  }

  	}
