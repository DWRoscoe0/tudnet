package allClasses;

public class PeersCursor extends PersistentCursor {
  
  /* This class is a piterator for peer data in the Persistent data structure.

    Each type of data should have its own distinguishing type.
      Presently they are:
      * Persistent: the data structure
      * IPAndPort: 2 fields: IP address and port #
      * String: PeerIdentity ///org makes its own type?

   */

  public static PeersCursor makePeersCursor( Persistent thePersistent )
    { 
      PeersCursor thePeersCursor= new PeersCursor(thePersistent);
      thePeersCursor.setListFirstKeyString("peers"); // Point cursor to peer list.
      return thePeersCursor;
      }

  private PeersCursor( Persistent thePersistent ) // constructor
  {
    super( thePersistent ); 
    }

  public PeersCursor addInfoUsingPeersCursor(
      IPAndPort theIPAndPort, String thePeerIdentityString)
    /* This method adds the provided information to the current peer.  
      If there is none, create one based on that information.
      */
    { 
      String ipString= theIPAndPort.getIPString();
      String portString= String.valueOf(theIPAndPort.getPortI());
      
      String peerKeyString= ipString+"-"+portString; // Calculate peer key.
      setEntryKeyV( peerKeyString ); // Next define position in peer list.

      // Store or update the fields.
      putFieldV( "IP", ipString );
      putFieldV( "Port", portString );
      if (thePeerIdentityString!=null)
        putFieldV( "PeerIdentity", thePeerIdentityString );

      return this;
      } 
  
  }
