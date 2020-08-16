package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TextStreams2 extends SimplerListWithMap<String,TextStream2> {
  
  /* This class implements a list of peer TextStreams 
    based on data replication instead of TextStreams2 broadcast.
    ///org However it is probably going to be changed into
    a more general UserId list as part of a general PubSub system.

    ///new : New code for subscription-based text streaming being created or adapted.

    ///fix : Eventually the protocols will include time-out and retrying,
      but for now those things are not included.
      Retrying will be the job of the developer doing the testing
      in those few cases where it is needed.
    */

  // Constructor-injected dependencies.
  private Persistent thePersistent;
  private AppGUIFactory theAppGUIFactory;
  private UnicasterManager theUnicasterManager;
  
  public TextStreams2( // Constructor.
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
      It is specifically not called during construction-initialization
      because some of the services on which this class relies
      are not ready to provide service until after construction.
      */
    {
      createTextStreamsV();

      boolean falseB= true;
      if (falseB) updatePeersAboutStreamsV(); //// Reference, but don't call.
      }

  private void updatePeersAboutStreamsV()
    /* This is like updatePeersAboutStreamsV() but with
      the inner and outer loops reversed.  
       */
    {
        theAppLog.debug("updatePeersAboutStreamsV() begins.");
        PeersCursor scanPeersCursor= // Used for iteration. 
            PeersCursor.makeOnNoEntryPeersCursor( thePersistent );
      peerLoop: while (true) { // Process all peers in my peer list. 
        if (scanPeersCursor.nextKeyString().isEmpty() ) // Try getting next scan peer. 
          break peerLoop; // There are no more peers, so exit loop.
        theAppLog.appendToFileV("(peer?)"); // Log that peer is being considered.
        MapEpiNode peerMapEpiNode= scanPeersCursor.getSelectedMapEpiNode();
        updatePeerAboutStreams2V(peerMapEpiNode);
      } // peerLoop: 
        theAppLog.debug("updatePeersAboutStreamsV() ends.");
    }

  public void notifyNewConnectionAboutTextStreamsV(
      MapEpiNode subjectPeerMapEpiNode)
    {
      theAppLog.debug("notifyNewConnectionAboutTextStreamsV() begins.");
      updatePeerAboutStreams2V(subjectPeerMapEpiNode);
      theAppLog.debug("notifyNewConnectionAboutTextStreamsV() ends.");
      }
  

  private void updatePeerAboutStreams2V(MapEpiNode peerMapEpiNode)
    {
      endPeer: {
        theAppLog.appendToFileV("(peer-Stream2?)"); // Log that consideration.
        if (! peerMapEpiNode.testB("isConnected")) // This peer is not connected
          break endPeer; // so end this peer to try next peer.
        String peerIPString= peerMapEpiNode.getString("IP");
        String peerPortString= peerMapEpiNode.getString("Port");
        IPAndPort theIPAndPort= new IPAndPort(peerIPString, peerPortString);
        Unicaster scanUnicaster= // Try getting associated Unicaster.
            theUnicasterManager.tryToGetUnicaster(theIPAndPort);
        if (scanUnicaster == null) { // Unicaster of scan peer doesn't exist
          theAppLog.error(
              "TextStreams2.updatePeerAboutStreams2V() non-existent Unicaster.");
          break endPeer; // so end this peer to try next peer.
          }
        theAppLog.appendToFileV("(PEER-STREAM2!)"); // Log that we're sending.
        updateStreamsToUnicasterV(scanUnicaster);
      } // peerDone:
  }

  private void updateStreamsToUnicasterV(Unicaster theUnicaster)
    /* Sends all TextStream2 IDs to theUnicaster. 
     * The purpose of this is to let connected peers know about
     * the existence of other streams about which it might not yet know.
     */
    {
      theAppLog.appendToFileV("(Streams2:)"); // Log beginning of data.
      for  // For all Streams2
        (TextStream2 scanTextStream2 : childHashMap.values())
        { // Send the stream ID to theUnicaster.
          MapEpiNode theMapEpiNode= new MapEpiNode();
          String nodeIdentyString= scanTextStream2.getKeyK();
          //// theMapEpiNode.putV(Config.userIdString, nodeIdentyString);
          theMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
              nodeIdentyString, theMapEpiNode);
          MapEpiNode messageMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
            "Subs", theMapEpiNode);  // Complete MapEpiNode message.
          // We now have a triple-nested map to send.
          theAppLog.appendToFileV("(STREAM2!)"); // Log sending Stream2 data.
          theUnicaster.putV( // Queue full message EpiNode to Unicaster.
             messageMapEpiNode);
          }
      theAppLog.appendToFileV("(STREAMS2!)"); // Log end of data.
    }
  
  public void sendToPeersV(MapEpiNode theMapEpiNode)
    /* This sends to all connected peers theMapEpiNode.
      It wraps it in a single-entry MapEpiNode with 
      the key "Subs" first.

      ///chg Wrap in "Subs" instead.
      */
    {
        theAppLog.debug( "TextStreams2.sendToPeersV() called with"
          + " theMapEpiNode=" + NL + "  " + theMapEpiNode);
        PeersCursor scanPeersCursor= // Used for iteration. 
            PeersCursor.makeOnNoEntryPeersCursor( thePersistent );
        MapEpiNode messageMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
            "Subs", theMapEpiNode);  // Complete MapEpiNode message.
      peerLoop: while (true) { // Process all peers in my peer list. 
        if (scanPeersCursor.nextKeyString().isEmpty() ) // Try getting next scan peer. 
          break peerLoop; // There are no more peers, so exit loop.
        theAppLog.appendToFileV("(stream2?)"); // Log that peer is being considered.
      endPeer: {
        MapEpiNode scanMapEpiNode= scanPeersCursor.getSelectedMapEpiNode();
        if (! scanMapEpiNode.testB("isConnected")) // This peer is not connected
          break endPeer; // so end this peer to try next peer.
        String peerIPString= scanMapEpiNode.getString("IP");
        String peerPortString= scanMapEpiNode.getString("Port");
        IPAndPort theIPAndPort= new IPAndPort(peerIPString, peerPortString);
        Unicaster scanUnicaster= // Try getting associated Unicaster.
            theUnicasterManager.tryToGetUnicaster(theIPAndPort);
        if (scanUnicaster == null) { // Unicaster of scan peer doesn't exist
          theAppLog.error(
              "TextStreams2.sendToPeersV() non-existent Unicaster.");
          break endPeer; // so end this peer to try next peer.
          }
        theAppLog.appendToFileV("(YES!)"); // Log that we're sending data.
        scanUnicaster.putV( // Queue full message EpiNode to Unicaster of scan peer.
            messageMapEpiNode);
      } // peerDone:
      } // peerLoop: 
        theAppLog.appendToFileV("(end of peers)"+NL); // Mark end of list with new line.
      }

  public void sendToSubscriberUnicasterV(
      MapEpiNode subscribeeUserIdMapEpiNode, String subscriberUserIdString)
    /* This method sends the subscribeeUserIdMapEpiNode to the Unicaster whose
     * UserId is subscriberUserIdString, unless subscriberUserIdString
     * identifies the local user, or the Unicaster does not exist.  
     * If it does send, it wraps the map node in a Subs map first.
     */
    {
      goReturn: {
        if (isLocalB(subscriberUserIdString)) // Exit if local node.
          break goReturn;
        Unicaster theUnicaster= // Try getting associated Unicaster.
            theUnicasterManager.tryToGetUnicaster(subscriberUserIdString);
        if (null == theUnicaster) { // Exit if Unicaster does not exist.
          theAppLog.debug("TextStreams2.sendToSubscriberUnicasterV() "
              + "non-existent Unicaster.");
          break goReturn;
          }
        MapEpiNode subsMapEpiNode= // Wrap subscriber UserIds map in Subs map. 
          MapEpiNode.makeSingleEntryMapEpiNode(
            "Subs", // This is the root key of all subscription messages.
            subscribeeUserIdMapEpiNode
            );
        theUnicaster.putV(subsMapEpiNode); // Send to peer through Unicaster.
      } // goReturn:
        return;
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
      
      createRemoteTextStreamsFromFoldersV();
      }

  private void createLocalTextStreamV()
    {
      String theRootIdString= 
          thePersistent.getEmptyOrString(Config.userIdString);
      createAndAddTextStream(theRootIdString);
      }

  private void createRemoteTextStreamsFromFoldersV()
    {
      File peersFile= FileOps.makePathRelativeToAppFolderFile(
        Config.textStream2FolderString
        );
      String[] peerStrings= // Read names of peer directories from Peers directory.
          peersFile.list();
      if ( peerStrings == null ) // If array is null replace with empty array.
        peerStrings= new String[ 0 ]; // Replace with empty array.
      String localRootIdString= thePersistent.getEmptyOrString(Config.userIdString);
      for (String scanRootIdString : peerStrings) // For every Peer folder 
        toPeerDone: { // Try creating a TextStream for this peer.
          if (localRootIdString.equals(scanRootIdString)) // Skip ourselves. 
            break toPeerDone;
          createAndAddTextStream(scanRootIdString);
          } // toPeerDone:
      }

  protected boolean tryProcessingMapEpiNodeB(  //// being adapted.
      MapEpiNode messageMapEpiNode,String senderUserIdString)
    /* This method tries processing a message MapEpiNode.
      This method is called by the ConnectionManager.
      senderUserIdString identifies the sender of the message.
      The message must be a single element map 
      with a key of "Subs" [was "TextStreams2", then "MessageUserIds"].
      It can contain various types of 
      TextStreams2 subscription-related information.
      If it is not a TextStream message then 
      this method ignores the data and returns false.
      If it is a TextStream message then it returns true 
      after processing the data.
      Some processing may be done on the Event Dispatch Thread (EDT). 
      */
    { 
      MapEpiNode subsUserIdsMapEpiNode= 
          messageMapEpiNode.getMapEpiNode("Subs");
      boolean isMessageUserIdsB= (null != subsUserIdsMapEpiNode); 
      if (isMessageUserIdsB) {
        processSubsUserIdsV(subsUserIdsMapEpiNode,senderUserIdString);
        }
      return isMessageUserIdsB;
      }

  public void processSubsUserIdsV(
      MapEpiNode userIdsMapEpiNode,String senderUserIdString)
    /* This method processes userIdsMapEpiNode
      which is assumed to be a map of 0 or more 
      subscribee UserIds and associated subscription data.
      senderUserIdString provides context and is assumed to be
      the UserId of the source of the request.
      */
    {
      Set<Map.Entry<EpiNode,EpiNode>> subscribeeSetOfMapEntrys= 
          userIdsMapEpiNode.getLinkedHashMap().entrySet();
      Iterator<Map.Entry<EpiNode,EpiNode>> subscribeeIterator= 
          subscribeeSetOfMapEntrys.iterator();
      while (subscribeeIterator.hasNext()) { // Iterate over all subscribees. 
        Map.Entry<EpiNode,EpiNode> subscribeeUserIdMapEntry= 
            subscribeeIterator.next(); // Get next map entry.
        processSubscribeeMapEntryV( // Process
            subscribeeUserIdMapEntry, // the subscribee map entry
            senderUserIdString // with this sender UserId as context.
            );
        }
    }

  public void processSubscribeeMapEntryV(
      Map.Entry<EpiNode,EpiNode> subscribeeUserIdMapEntry,
      String senderUserIdString)
    /* This method processes one subscribeeUserIdMapEntry 
     * received from another node,
     * using senderUserIdString as context as the source.
     * Processing includes:
     * * Creating a new stream if needed and informing other peers about it.
     * * Processing any new stream text data. 
     * * Processing any new stream offset data.
     * * Forwarding text to subscribers if it became possible with new text.
     * * Updating the associated stream display if it needs changing.
     *   This happens by calling a Document listener.
     */
    {
      goReturn: {
        String subscribeeUserIdString= 
            subscribeeUserIdMapEntry.getKey().toString();
        EpiNode subscribeeUserDataEpiNode= subscribeeUserIdMapEntry.getValue();
        MapEpiNode subscribeeUserDataMapEpiNode= // Try converting to map.
            subscribeeUserDataEpiNode.tryOrLogMapEpiNode();
        if (null == subscribeeUserDataMapEpiNode) // The value is not a map
          break goReturn; // so abandon further processing and exit.
        TextStream2 subscribeeIdTextStream2= 
            childHashMap.get(subscribeeUserIdString);
        if (null == subscribeeIdTextStream2) // Make TextStream2 if needed.
          subscribeeIdTextStream2= 
            makeAndAnnounceTextStream2(subscribeeUserIdString);
        subscribeeIdTextStream2.tryProcessingUserDataMapEpiNodeB(
          subscribeeUserDataMapEpiNode,senderUserIdString);
      } // goReturn:
      }

  public TextStream2 makeAndAnnounceTextStream2(String userIdString)
    /* This method create a new stream associated with userIdString,
     * adds it to the other streams, and informs other peers about it.
     * It should be called only when it has been determined
     * that a stream for the given user does not exist.
     */
    {
      TextStream2 theTextStream2= createAndAddTextStream(userIdString);
      { // Inform other peers about new stream.
        MapEpiNode newUserStreamMapEpiNode= new MapEpiNode();
        newUserStreamMapEpiNode.putV(Config.userIdString, userIdString);
        sendToPeersV(newUserStreamMapEpiNode);
        }
      return theTextStream2;
      }

  private TextStream2 createAndAddTextStream(String peerIdentityString)
    {
      TextStream2 theTextStream= theAppGUIFactory.makeTextSteam2(peerIdentityString);
      addingV(peerIdentityString, theTextStream); // Add to list and HashMap.
      return theTextStream;
      }

  public boolean isLocalB(String theUserIdString) 
    /* Returns true if the text stream identified by theUserIdString
     * is local, returns false otherwise.
     * * true means that the stream's text source is only
     *   the keyboard attached to this local device.
     *   This stream may send text but never send
     *   requests for text or text acknowledgments.
     * * false means that this stream may receive text from
     *   any other device that has it, and may send
     *   acknowledgment of, and requests for, text.
     */
    {
      String localUserIdString= // Get the UserId of the local node.
        thePersistent.getEmptyOrString(Config.userIdString);
      boolean isLocalB= // Compare UserIds.
        (localUserIdString.equals(theUserIdString)); 
      return isLocalB;
      }

  
  }
