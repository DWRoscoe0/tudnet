package allClasses;

public class PeersCursor extends PersistentCursor {
  
  /* This class is a piterator for peer data in the Persistent data structure.

    Each type of data should have its own distinguishing type.
      Presently they are:
      * Persistent: the data structure
      * IPAndPort: 2 fields: IP address and port #
      * String: PeerIdentity ///org makes its own type?

   */

  public static PeersCursor makeOnFirstEntryPeersCursor( Persistent thePersistent )
    /* This method creates a PeersCursor positioned to the first peer in the peer list.  */
    { 
      PeersCursor thePeersCursor= // Create PeersCursor before first entry. 
          makeOnNoEntryPeersCursor( thePersistent );
      thePeersCursor.nextKeyString(); // Move to first entry unless list is empty.
      return thePeersCursor;
      }

  public static PeersCursor makeOnNoEntryPeersCursor( Persistent thePersistent )
    /* This method creates a PeersCursor positioned so that
      no entry is selected, which can be considered to be 
      before the first peer in the peer list or after the last peer in the peer list.
      */
    { 
      PeersCursor thePeersCursor= new PeersCursor(thePersistent);
      thePeersCursor.setListFirstKeyString("peers"); // Point cursor to peer list.
      thePeersCursor.moveToNoKeyString(); // Point to no entry within the list.
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
      
      searchForEntryInPeersCursor(ipString, portString);
      if (getEntryKeyString().isEmpty()) { // Create new element if needed.
        createEntryInPersistentCursor(); // Create new element.
        }
      
      { // Store or update the fields in the found or created element.
        putFieldV( "IP", ipString );
        putFieldV( "Port", portString );
        if (thePeerIdentityString!=null)
          putFieldV( "PeerIdentity", thePeerIdentityString );
        }
      
      return this;
      }

  public PeersCursor addInfoUsingPeersCursor(
      IPAndPort theIPAndPort, boolean isConnectedB)
    /* This method adds the provided information to the current peer.  
      If there is none, create one based on that information.
      */
    { 
      String ipString= theIPAndPort.getIPString();
      String portString= String.valueOf(theIPAndPort.getPortI());
      
      searchForEntryInPeersCursor(ipString, portString);
      if (getEntryKeyString().isEmpty()) { // Create new element if needed.
        createEntryInPersistentCursor(); // Create new element.
        }
      
      { // Store or update the fields in the found or created element.
        putFieldV( "IP", ipString );
        putFieldV( "Port", portString );
        putFieldV( "wasConnected", ""+isConnectedB);
        }
      
      return this;
      }

  public PeersCursor searchForEntryInPeersCursor(
      String ipString, String portString)
    /* This method searches for the entry that matches the arguments.  
      If there is no such entry, it returns this PeersCursor on that element,
      otherwise it returns this PeersCursor cursor on no element.
      */
    { 
      moveToNoKeyString(); // Move before first element.
      while // Search entire list or until the desired element is found. 
        (! this.nextKeyString().isEmpty()) // Get next element or exit if there is none.
        {
          if ( (getFieldString("IP").equals(ipString)) &&
              (getFieldString("Port").equals(portString)) 
              )
            break; // Exit loop with match.
          }
      return this;
      } 
  
  }
