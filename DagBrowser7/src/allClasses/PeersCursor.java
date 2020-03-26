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

  public void findOrAddPeerV(MapEpiNode targetMapEpiNode)
    /* This method positions this iterator on the peer data associated with 
      the peer with identity elements from targetMapEpiNode.
      If there is none, it creates one based on that information
      and positions the iterator to it.
      */
    { 
      String ipString= // Extract IP.
          targetMapEpiNode.getString("IP");
      String portString= // Extract port.
          targetMapEpiNode.getString("Port");
      String theIdentityString= // Extract peer ID.
          targetMapEpiNode.getString("PeerIdentity");

      findOrAddPeerV(ipString, portString, theIdentityString);
      }

  public void findOrAddPeerV(IPAndPort theIPAndPort, String theIdentityString)
    /* This method finds the peer data associated with the peer 
      with the method arguments.
      If there is none, it creates one based on that information
      and positions the iterator to it.
      */
    { 
      String ipString= theIPAndPort.getIPString(); // Extract IP.
      String portString= String.valueOf(theIPAndPort.getPortI()); // Extract port.

      findOrAddPeerV(ipString, portString, theIdentityString);
      }

  public void findOrAddPeerV(String ipString, String portString, String theIdentityString)
    /* This method finds the peer data associated with the peer 
      with the method arguments.
      If there is none, it creates one based on that information
      and positions the iterator to it.
      */
    { 
      findPeerV(ipString, portString, theIdentityString);
      if (getEntryKeyString().isEmpty()) { // Create new element if needed.
        createEntryInPersistentCursor(); // Create new element.
        }
      { // Store or update the fields in the found or created element.
        putFieldV( "IP", ipString );
        putFieldV( "Port", portString );
        if (theIdentityString!=null)
          putFieldV( "PeerIdentity", theIdentityString );
        }
      }

  public void findPeerV(String ipString, String portString, String theIdentityString)
    /* This method searches for the entry that matches the arguments.  
      If there is such an entry, it returns with this PeersCursor on that element,
      otherwise it returns with this PeersCursor cursor on no element, 
      i.e., on the empty element.
      */
    { 
      moveToNoKeyString(); // Move before first element.
      while // Search entire list or until the desired element is found. 
        (! this.nextKeyString().isEmpty()) // Get next element or exit if there is none.
        {
          if (! testFieldIsB("IP",ipString)) continue;
          if (! testFieldIsB("Port", portString)) continue;
          if (
            (theIdentityString != null) // ID present. 
            && ! testFieldIsB("PeerIdentity", theIdentityString)) 
              continue;
          break; // All search parameters matched, exit loop positioned on matching entry.
          }
      } 
  
  }
