package allClasses;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static allClasses.Globals.*;  // appLogger;

public class IPAndPort 

  //extends InetSocketAddress // Temporary extension while replacing class. 

  /* This class is a mutable replacement for InetSocketAddress.
   It's main purpose is to make Unicaster lookup by 
   remote address from a DatagramPacket faster by reducing
   the number of new-operators performed.

   /// opt Unfortunately it doesn't eliminate them because
   DatagramPacket.getAddress() returns a [new] InetAddress, and
   there doesn't appear to be a way to return the IP address another way.
	   ///fix A possible solution is to put 
	     an identifier at beginning of all packets?
	     Unfortunately this would need to be negotiated if it to be kept short.

   */

  {
		private InetAddress netcasterInetAddress;
		private int netcasterPortI;

		
		// Constructors.
		
		public IPAndPort( InetAddress netcasterInetAddress, int netcasterPortI )
			{
				this.netcasterInetAddress= netcasterInetAddress;
				this.netcasterPortI= netcasterPortI;
				}
		public IPAndPort( String IPString , String portString )
		  /* This constructor creates an IPAndPort from Strings. */
			{
				try { // Doing this here is a bit of a kludge.
						this.netcasterInetAddress= InetAddress.getByName(IPString);
						this.netcasterPortI= Integer.parseUnsignedInt(portString);
					} catch ( UnknownHostException | NumberFormatException e ) { 
				    appLogger.warning( // Log warning.
		      			"IPAndPort.IPAndPort("+IPString+", "+portString+")"+e );
						this.netcasterInetAddress= InetAddress.getLoopbackAddress();
						this.netcasterPortI= Integer.parseUnsignedInt(portString);
					}
				}

		
		// Getters.
		public InetAddress getInetAddress() 
	  	{ return netcasterInetAddress; }
		public int getPortI() 
			{ return netcasterPortI; }

		// Setters.
		public void setInetAddressV( InetAddress netcasterInetAddress) 
		  { this.netcasterInetAddress= netcasterInetAddress; }
		public void setPortI( int netcasterPortI )
		  { this.netcasterPortI= netcasterPortI; }


		// Object method overrides.
	  public boolean equals(Object otherObject) 
      // This is the standard equals() method.  
      {
        boolean resultB = false;  // assume objects are not equal.
        Comparer: {  // Comparer.
          if ( otherObject == null )  // Other object is null.
            break Comparer;  // Exiting with false.
          if ( ! ( otherObject instanceof IPAndPort ) )  // Unequal classes.
            break Comparer;  // Exiting with false.
          IPAndPort otherIPAndPort=  // Creating easy field-access variable.
            (IPAndPort)otherObject; 
          if  // Unequal ports.
            ( this.netcasterPortI != otherIPAndPort.netcasterPortI )
            break Comparer;  // Exiting with false.
          if ( ! Nulls.equals( // Unequal InetAddresses.
          			this.netcasterInetAddress, otherIPAndPort.netcasterInetAddress
          			))
            break Comparer;  // Exiting with false.
          resultB= true;  // All parts are equal, so override result.
          }  // Comparer.
        return resultB;
        }

	  public int hashCode() 
		  {
        return // Returning sum of the two component hashes. 
        		Nulls.hashCode(netcasterInetAddress) + netcasterPortI;
        }

		public String toString() 
		  {
				return netcasterInetAddress + ":" + netcasterPortI;
			  }


		// Persistent storage methods.

    public static void addPeerInfoV(
    		Persistent thePersistent, IPAndPort theIPAndPort)
      /* This is like addPeerInfoV(Persistent, ipString, portString) but
        identifies the peer using theIPAndPort instead.
        */
	    {
    		/// appLogger.debug( "IPAndPort.addPeerInfoV() called for "+theIPAndPort );
      	addPeerInfoV(
      			thePersistent, 
      			theIPAndPort.getIPString(),
      			String.valueOf(theIPAndPort.netcasterPortI)
      			);
      	}

    public static void addPeerInfoV(
    		Persistent thePersistent, String ipString, String portString)
    	/* Add to thePersistent data the peer identified by 
    	 	ipString and portString.
    	 	
    	 	///org do without PersistentCursor?
	      */
	    {	
    		/// appLogger.debug( "IPAndPort.addPeerInfoV(" 
    		/// 		+ipString+", "+portString+") called." );

    		String peerKeyString= ipString+"-"+portString; // Calculate peer key.

    		PersistentCursor thePersistentCursor= new PersistentCursor(
    				thePersistent);
 				//// thePersistentCursor.setListAndEntryV( "peers", peerKeyString );
    		Peer.setToPeersV(thePersistentCursor); // First define list. 
        thePersistentCursor.setEntryKeyV( peerKeyString ); // Next define position in list.

    	  // Store or update the fields.
 				thePersistentCursor.putFieldV( "IP", ipString );
    		thePersistentCursor.putFieldV( "Port", portString );
    		} 

    public String getIPString()
      /* This method returns only the numeric portion of
        the IP address part, without any hostname or separating slash.
       */
      {
        String rawString= netcasterInetAddress.toString();
        int slashOffsetI= rawString.lastIndexOf('/');
        return rawString.substring(slashOffsetI+1); 
        }
    
		} // class IPAndPort
