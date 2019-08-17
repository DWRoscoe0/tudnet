package allClasses;

public class PeersCursor extends PersistentCursor {

  public PeersCursor( Persistent thePersistent ) // constructor
  {
    super( thePersistent ); 
    }

  public static PeersCursor makePeersCursor( Persistent thePersistent )
    { 
      PeersCursor thePeersCursor= new PeersCursor(thePersistent);
      //// PeersCursor.setToPeersV(thePeersCursor); 
      thePeersCursor.setListV("peers"); // Point cursor to peer list.
      return thePeersCursor;
      }

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
          String.valueOf(theIPAndPort.getPortI())
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
      ///     +ipString+", "+portString+") called." );

      String peerKeyString= ipString+"-"+portString; // Calculate peer key.

      PersistentCursor thePersistentCursor= new PersistentCursor(
          thePersistent);
      //// thePersistentCursor.setListAndEntryV( "peers", peerKeyString );
      thePersistentCursor.setEntryKeyV( peerKeyString ); // Next define position in list.

      // Store or update the fields.
      thePersistentCursor.putFieldV( "IP", ipString );
      thePersistentCursor.putFieldV( "Port", portString );
      } 

  }
