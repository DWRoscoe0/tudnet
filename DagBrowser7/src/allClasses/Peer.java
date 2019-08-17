package allClasses;

public class Peer {

  public static void setToPeersV(
      PersistentCursor thePersistentCursor)
      ///org Change to makePeersPersistentCursor(..)
    {
      thePersistentCursor.setListV("peers"); // Point cursor to peer list.
      }

  }