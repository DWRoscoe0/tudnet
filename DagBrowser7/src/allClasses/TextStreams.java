package allClasses;

import static allClasses.SystemSettings.NL;
import static allClasses.AppLog.theAppLog;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class TextStreams extends SimplerListWithMap<String,TextStream> {

  // Constructor-injected dependencies.
  private Persistent thePersistent;
  private AppGUIFactory theAppGUIFactory;
  private UnicasterManager theUnicasterManager;

  LinkedHashMap<Integer,Object> antiRepeatLinkedHashMap= // Used to prevent storms.
      new LinkedHashMap<Integer,Object>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer,Object> eldest) {
            return size() > 8; // Limit map size to 8 entries.
            }
      };

  public TextStreams( // Constructor.
      String nameString,
      
      AppGUIFactory theAppGUIFactory,
      Persistent thePersistent,
      UnicasterManager theUnicasterManager
      )
    { 
      super(nameString,emptyListOfDataNodes());

      this.thePersistent= thePersistent;
      this.theAppGUIFactory= theAppGUIFactory;
      this.theUnicasterManager= theUnicasterManager;
      }

  public void startServiceV() 
    /* This is the first service of this class.
      It specifically not called during construction-initialization
      because some of the services on which it this class relies
      are not ready to provide service until after construction.
      */
    {
      createTextStreamsV();
      }
    
  private void createTextStreamsV()
    /* Creates a TextStream DataNode for this peer and
      one for every TextStream file that already exists,
      and adds them all to this list.
      However, it does not load the Document objects from the files.
      That is done only when a stream is viewed.
      */
    {
      createLocalTextStreamV();
      
      createRemoteTextStreamsV();
      }

  private void createLocalTextStreamV()
    {
      String localPeerIdentityString= thePersistent.getTmptyOrString("PeerIdentity");
      TextStream theTextStream= theAppGUIFactory.makeTextSteam(localPeerIdentityString);
      addAtEndB( theTextStream );
      }

  private void createRemoteTextStreamsV()
    {
      File peersFile= AppSettings.makePathRelativeToAppFolderFile(
        "Peers"
        );
      String[] peerStrings= // Read names of peer directories from Peers directory.
          peersFile.list();
      String localPeerIdentityString= thePersistent.getTmptyOrString("PeerIdentity");
      for (String peerIdentityString : peerStrings) // For every Peer folder 
        toPeerDone: { // Try creating a TextStream for this peer.
          if (localPeerIdentityString.equals(peerIdentityString)) // Skip ourselves. 
            break toPeerDone;
          TextStream theTextStream= theAppGUIFactory.makeTextSteam(peerIdentityString);
          addAtEndB( theTextStream );
          } // toPeerDone:
      }

  protected boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode)
    /* This method tries processing a MapEpiNode.
      It must be a single element map with a key of "StreamText".
      If it is not then it ignores the data and returns false.
      If it is then it returns true after having the value 
      processed for TextStream content.
      Processing is done on the Event Dispatch Thread (EDT), switching if needed.

      Presently it does this by trying to have each child process that value,
      by its method of the same name, until one of them returns true,
      or the end of the child list is reached.
      //// This is being changed to be partially processed here.
       * Later it will pass it to the child with the matching PeerIdentity.
      */
    { 
      MapEpiNode payloadMapEpiNode= theMapEpiNode.getMapEpiNode("StreamText");
      boolean decodedB= (payloadMapEpiNode != null); // It a StreamText message?
      if (decodedB) {
        EDTUtilities.runOrInvokeAndWaitV( // Switch to EDT thread if needed. 
            new Runnable() {
              @Override  
              public void run() {
                synchronized(this) {
                  processIfNewV(payloadMapEpiNode);
                  }
                }
              } 
            );
        }
      return decodedB;
      }

  public void processIfNewV(MapEpiNode theMapEpiNode)
    /* This method examines theMapEpiNode.  
      If the TextStream message it represents has been seen before,
      then the method returns with no further action.
      If it has not been seen before then it
      * sends the message to the appropriate child TextStream, and
      * distributes the message to other connected peers.
     */
    {
      toReturn: {
        Integer hashInteger= new Integer( ///opt Probably more complicated than needed.
            theMapEpiNode.getString("time").hashCode());
        if (antiRepeatLinkedHashMap.containsKey(hashInteger)) // Already in map?
          break toReturn; // Yes, so received before and we are ignoring it.
        antiRepeatLinkedHashMap.put(hashInteger,null); // Put in map to prevent repeat.
        super.tryProcessingMapEpiNodeB(theMapEpiNode); // Distribute to all children.
        sendToPeersV(theMapEpiNode);
        } // toReturn:
      }

  public void sendToPeersV(MapEpiNode theMapEpiNode)
    /* This sends to all connected peers the message messageMapEpiNode.
      It is assumed that "TextStream" has already been added.
      */
    {
        theAppLog.debug( "TextStreamViewer.broadcastStreamMessageV() called.");
        PeersCursor scanPeersCursor= // Used for iteration. 
            PeersCursor.makeOnNoEntryPeersCursor( thePersistent );
        MapEpiNode messageMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
            "StreamText", theMapEpiNode);  // Complete MapEpiNode message.
      peerLoop: while (true) { // Process all peers in my peer list. 
        if (scanPeersCursor.nextKeyString().isEmpty() ) // Try getting next scan peer. 
          break peerLoop; // There are no more peers, so exit loop.
        theAppLog.appendToFileV("(stream?)"); // Log that peer is being considered.
        if (! scanPeersCursor.testB("isConnected")) // This peer is not connected 
          continue peerLoop; // so loop to try next peer.
        String peerIPString= scanPeersCursor.getFieldString("IP");
        String peerPortString= scanPeersCursor.getFieldString("Port");
        IPAndPort theIPAndPort= new IPAndPort(peerIPString, peerPortString);
        Unicaster scanUnicaster= // Try getting associated Unicaster.
            theUnicasterManager.tryingToGetUnicaster(theIPAndPort);
        if (scanUnicaster == null) { // Unicaster of scan peer doesn't exist
          theAppLog.error(
              "TextStreamViewer.broadcastStreamMessageV() non-existent Unicaster.");
          continue peerLoop; // so loop to try next peer.
          }
        theAppLog.appendToFileV("(YES!)"); // Log that we're sending data.
        scanUnicaster.putV( // Queue full message EpiNode to Unicaster of scan peer.
            messageMapEpiNode);
      } // peerLoop: 
        theAppLog.appendToFileV("(end of peers)"+NL); // Mark end of list with new line.
      }
  
  }
