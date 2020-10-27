package allClasses;

import static allClasses.AppLog.theAppLog;

public class PeersCursor extends PersistentCursor {
  
  /* This class is a piterator for peer data in the Persistent data structure.

    The use of this class, as well as PersistentCursor, is being deprecated.
    / Field access elimination (mostly done)
    = Stop using as iterator (not done yet)
    The plan is to use more conventional map iterator techniques.

    Each type of app data should have its own distinguishing type.
      Presently they are:
      * Persistent: the data structure
      * IPAndPort: 2 fields: IP address and port #
      * String: RootId ///org makes its own type?

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
      thePeersCursor.setListFirstKeyString(
          "UnicasterIndexes"); // Point cursor to Unicasters.
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
          targetMapEpiNode.getString(Config.userIdString);

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
      checkOwnerIdB(theIdentityString); // debug
      
      findPeerV(ipString, portString, theIdentityString);
      if (getEntryKeyString().isEmpty()) { // Create new element if needed.
        createEntryInPersistentCursor(); // Create new element.
        }
      { // Store or update the fields in the found or created element.
        childMapEpiNode.putV( "IP", ipString );
        childMapEpiNode.putV( "Port", portString );
        if (theIdentityString!=null)
          childMapEpiNode.putV( Config.userIdString, theIdentityString );
          else
          theAppLog.debug(
            "PeersCursor.findOrAddPeerV(ipString,portString,theIdentityString) "
            + "theIdentityString==null, IP="+ipString+", Port="+portString);
        }
      }

  private boolean checkOwnerIdB(String ownerIdString)
    {
      boolean resultB= Nulls.equals(
          ownerIdString,
          thePersistent.getEmptyOrString(Config.userIdString)
          );
      if (resultB)
          theAppLog.debug(
            "PeersCursor.checkOwnerIdB(.) This is US!"); 
      return resultB;
      }

  public void findPeerV(String ipString, String portString, String peerIdentityString)
    /* This method searches for the first entry that matches the arguments.
      If peerIdentityString is null then that parameter is not checked.  
      If there is such an entry, it returns with this PeersCursor on that element,
      otherwise it returns with this PeersCursor cursor on no element, 
      i.e., on the empty element.
      */
    { 
      moveToNoKeyString(); // Move before first element.
      while // Search entire list or until the desired element is found. 
        (! this.nextKeyString().isEmpty()) // Get next element or exit if there is none.
        {
          MapEpiNode theMapEpiNode= getSelectedMapEpiNode();
          if (! theMapEpiNode.testKeyForValueB("IP",ipString)) continue;
          if (! theMapEpiNode.testKeyForValueB("Port", portString)) continue;
          if (
            (peerIdentityString != null) // ID present. 
            && ! theMapEpiNode.testKeyForValueB(Config.userIdString, peerIdentityString))
              continue;
          break; // All search parameters matched, exit loop positioned on matching entry.
          }
      } 
  
  }
