
package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import java.io.File;

public class TextStreams2 extends SimplerListWithMap<String,TextStream2> {
  
  /* This class implements a list of peer TextStreams 
    based on data replication instead of TextStreams2 broadcast.  

    ///new : New code for subscription-based text streaming being created or adapted.

    ///fix : Eventually the protocols will include time-out and retrying,
      but for now those things are not included.
      Retrying will be the job of the developer doing the testing
      in those few cases where it is needed.
    */

  // Constructor-injected dependencies.
  private Persistent thePersistent;
  private AppGUIFactory theAppGUIFactory;
  private UnicasterManager theUnicasterManager; //// fully activate this.
  
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
      //// Update documentation.
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
        //// String peerIPString= scanPeersCursor.getFieldString("IP");
        String peerIPString= peerMapEpiNode.getString("IP");
        //// String peerPortString= scanPeersCursor.getFieldString("Port");
        String peerPortString= peerMapEpiNode.getString("Port");
        IPAndPort theIPAndPort= new IPAndPort(peerIPString, peerPortString);
        Unicaster scanUnicaster= // Try getting associated Unicaster.
            theUnicasterManager.tryingToGetUnicaster(theIPAndPort);
        if (scanUnicaster == null) { // Unicaster of scan peer doesn't exist
          theAppLog.error(
              "TextStreams2.updatePeerAboutStreams2V() non-existent Unicaster.");
          break endPeer; // so end this peer to try next peer.
          }
        theAppLog.appendToFileV("(PEER-STREAM2!)"); // Log that we're sending.
        updateStreamsToUnicasterV(scanUnicaster);
        //// scanUnicaster.putV( // Queue full message EpiNode to Unicaster of scan peer.
        ////     messageMapEpiNode);
      } // peerDone:
  }

  private void updateStreamsToUnicasterV(Unicaster theUnicaster)
    /* Sends all TextStream2 IDs to theUnicaster. */
    {
      theAppLog.appendToFileV("(Streams2:)"); // Log beginning of data.
      for  // For all Streams2
        (TextStream2 scanTextStream2 : childHashMap.values())
        { // Send the stream ID to theUnicaster.
          MapEpiNode theMapEpiNode= new MapEpiNode();
          String nodeIdentyString= scanTextStream2.getKeyK();
          theMapEpiNode.putV(Config.rootIdString, nodeIdentyString);
          MapEpiNode messageMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
            "TextStreams2", theMapEpiNode);  // Complete MapEpiNode message.
          //// sendToPeersV(theMapEpiNode);
          theAppLog.appendToFileV("(STREAM2!)"); // Log sending Stream2 data.
          theUnicaster.putV( // Queue full message EpiNode to Unicaster.
             messageMapEpiNode);
          }
      theAppLog.appendToFileV("(STREAMS2!)"); // Log end of data.
    }

  @SuppressWarnings("unused")
  private void OLDupdatePeersAboutStreamsV() ////
    /* Because now all peers are assumed to be subscribed to 
      the entire TextStreams2 peer list, peers update each other
      whenever anything changes.
      
      ///fix? This normally does nothing because it is called
        before any connections have been made.
        */
    {
      for (TextStream2 scanTextStream2 : childHashMap.values()) // For all Streams2
        { // Send its ID to all peers.
          MapEpiNode theMapEpiNode= new MapEpiNode();
          String nodeIdentyString= scanTextStream2.getKeyK();
          theMapEpiNode.putV(Config.rootIdString, nodeIdentyString);
          sendToPeersV(theMapEpiNode);
          }
    }
  
  public void sendToPeersV(MapEpiNode theMapEpiNode)
    /* This sends to all connected peers theMapEpiNode.
      It wraps it in a single-entry MapEpiNode with 
      the key "TextStreams2" first.
      */
    {
        theAppLog.debug( "TextStreams2.sendToPeersV() called with"
          + " theMapEpiNode=" + NL + "  " + theMapEpiNode);
        PeersCursor scanPeersCursor= // Used for iteration. 
            PeersCursor.makeOnNoEntryPeersCursor( thePersistent );
        MapEpiNode messageMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
            "TextStreams2", theMapEpiNode);  // Complete MapEpiNode message.
      peerLoop: while (true) { // Process all peers in my peer list. 
        if (scanPeersCursor.nextKeyString().isEmpty() ) // Try getting next scan peer. 
          break peerLoop; // There are no more peers, so exit loop.
        theAppLog.appendToFileV("(stream2?)"); // Log that peer is being considered.
      endPeer: {
        MapEpiNode scanMapEpiNode= scanPeersCursor.getSelectedMapEpiNode();
        if (! scanMapEpiNode.testB("isConnected")) // This peer is not connected
          break endPeer; // so end this peer to try next peer.
        //// String peerIPString= scanPeersCursor.getFieldString("IP");
        String peerIPString= scanMapEpiNode.getString("IP");
        //// String peerPortString= scanPeersCursor.getFieldString("Port");
        String peerPortString= scanMapEpiNode.getString("Port");
        IPAndPort theIPAndPort= new IPAndPort(peerIPString, peerPortString);
        Unicaster scanUnicaster= // Try getting associated Unicaster.
            theUnicasterManager.tryingToGetUnicaster(theIPAndPort);
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
      String theRootIdString= thePersistent.getEmptyOrString(Config.rootIdString);
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
      String localRootIdString= thePersistent.getEmptyOrString(Config.rootIdString);
      for (String scanRootIdString : peerStrings) // For every Peer folder 
        toPeerDone: { // Try creating a TextStream for this peer.
          if (localRootIdString.equals(scanRootIdString)) // Skip ourselves. 
            break toPeerDone;
          createAndAddTextStream(scanRootIdString);
          } // toPeerDone:
      }

  protected boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode) //// being adapted.
    //// The original or something like it belongs in TextStream2 for actual text.
    /* This method tries processing a MapEpiNode.
      It must be a single element map with a key of "TextStreams2"
      which should contain an update to the TextStream list.
      If it is not then it ignores the data and returns false.
      If it is then it returns true after having the value 
      processed for TextStream peer list content.
      Processing is done on the Event Dispatch Thread (EDT), switching if needed.
      The message is passed to the appropriate TextStream based on
      PeerIdenty in theMapEpiNode, creating the TextStream if needed.
      */
    { 
      MapEpiNode payloadMapEpiNode= theMapEpiNode.getMapEpiNode("TextStreams2");
      boolean isTextStreamMessageB= (null != payloadMapEpiNode); 
      if (isTextStreamMessageB) {
        String peerIdentityString= 
            payloadMapEpiNode.getString(Config.rootIdString);
        TextStream2 theTextStream2= childHashMap.get(peerIdentityString);
        if (null == theTextStream2) { // If this TextStream2 does not exist
          theTextStream2= // create it and
              createAndAddTextStream(peerIdentityString);
          sendToPeersV(payloadMapEpiNode); // inform other peers about it.
          }
        }
      return isTextStreamMessageB;
      }

  public void processNewTextV(MapEpiNode theMapEpiNode)
    /* This method processes the text message within it.
      * sends the message to the appropriate child TextStream
      It does not distribute the message to other connected peers, yet.
     */
    {
      sendToTextStreamV(theMapEpiNode);
      // sendToPeersV(theMapEpiNode);
      }

  private void sendToTextStreamV(MapEpiNode theMapEpiNode)
    /* This method sends theMapEpiNode to the appropriate child TextStream.
     */
    {
      String peerIdentityString= theMapEpiNode.getString(Config.rootIdString);
      TextStream2 theTextStream=  // Getting the appropriate TextStream.
          getOrBuildAddAndTextStream(peerIdentityString);
      theTextStream.tryProcessingMapEpiNodeB(theMapEpiNode);
      }

  private TextStream2 getOrBuildAddAndTextStream(String peerIdentityString)
    {
      TextStream2 theTextStream= childHashMap.get(peerIdentityString); // Try lookup.
      if (null == theTextStream)
        theTextStream= createAndAddTextStream(peerIdentityString);
      return theTextStream;
      }

  private TextStream2 createAndAddTextStream(String peerIdentityString)
    {
      TextStream2 theTextStream= theAppGUIFactory.makeTextSteam2(peerIdentityString);
      addingV(peerIdentityString, theTextStream); // Add to list and HashMap.
      return theTextStream;
      }

  
  }
