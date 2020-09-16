package allClasses;

import static allClasses.SystemSettings.NL;
import static allClasses.AppLog.theAppLog;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class TextStreams1 extends SimplerListWithMap<String,TextStream1> {

  // Constructor-injected dependencies.
  private Persistent thePersistent;
  private AppGUIFactory theAppGUIFactory;
  private UnicasterManager theUnicasterManager;

  LinkedHashMap<Integer,Object> antiRepeatLinkedHashMap= // For preventing message storms.
      new LinkedHashMap<Integer,Object>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer,Object> eldest) {
            return size() > 8; // Limit map size to 8 entries.
            }
      };

  public TextStreams1( // Constructor.
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
      because some of the services on which this class relies
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
      
      createRemoteTextStreamsFromFoldersV();
      }

  private void createLocalTextStreamV()
    {
      String theRootIdString= thePersistent.getEmptyOrString(Config.userIdString);
      createAndAddTextStream(theRootIdString);
      }

  private void createRemoteTextStreamsFromFoldersV()
    {
      File peersFile= FileOps.makePathRelativeToAppFolderFile(
        Config.textStream1FolderString
        );
      String[] peerStrings= // Read names of peer directories from Peers directory.
          peersFile.list();
      String localRootIdString= thePersistent.getEmptyOrString(Config.userIdString);
      for (String scanRootIdString : peerStrings) // For every Peer folder 
        toPeerDone: { // Try creating a TextStream for this peer.
          if (localRootIdString.equals(scanRootIdString)) // Skip ourselves. 
            break toPeerDone;
          createAndAddTextStream(scanRootIdString);
          } // toPeerDone:
      }

  protected boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode)
    /* This method tries processing a MapEpiNode.
      It must be a single element map with a key of "StreamText".
      If it is not then it ignores the data and returns false.
      If it is then it returns true after having the value 
      processed for TextStream content.
      Processing is done on the Event Dispatch Thread (EDT), switching if needed.
      The message is passed to the appropriate TextStream based on
      PeerIdenty in theMapEpiNode, creating the TextStream if needed.
      */
    { 
      MapEpiNode payloadMapEpiNode= theMapEpiNode.getMapEpiNode("StreamText");
      boolean decodedB= (payloadMapEpiNode != null); // It a StreamText message?
      if (decodedB) {
        EDTUtilities.runOrInvokeAndWaitV( // Switch to EDT thread if needed. 
            new Runnable() {
              @Override  
              public void run() {
                synchronized(this) { //// Needed?
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
        Integer hashInteger= new Integer( theMapEpiNode.getString("time").hashCode());
        if (antiRepeatLinkedHashMap.containsKey(hashInteger)) // Already in map?
          break toReturn; // Yes, so it was received before, so we ignore it.
        antiRepeatLinkedHashMap.put(hashInteger,null); // Put in map to prevent repeat.
        sendToTextStreamV(theMapEpiNode);
        sendToPeersV(theMapEpiNode);
        } // toReturn:
      }

  private void sendToTextStreamV(MapEpiNode theMapEpiNode)
    /* This method sends theMapEpiNode to the appropriate child TextStream.
     */
    {
      String peerIdentityString= theMapEpiNode.getString(Config.userIdString);
      TextStream1 theTextStream=  // Getting the appropriate TextStream.
          getOrBuildAddAndTextStream(peerIdentityString);
      theTextStream.tryProcessingMapEpiNodeB(theMapEpiNode);
      }

  private TextStream1 getOrBuildAddAndTextStream(String peerIdentityString)
    {
      TextStream1 theTextStream= childHashMap.get(peerIdentityString); // Try lookup.
      if (null == theTextStream)
        theTextStream= createAndAddTextStream(peerIdentityString);
      return theTextStream;
      }

  private TextStream1 createAndAddTextStream(String peerIdentityString)
    {
      TextStream1 theTextStream= theAppGUIFactory.makeTextSteam1(peerIdentityString);
      addingV(peerIdentityString, theTextStream); // Add to list and HashMap.
      return theTextStream;
      }

  private void sendToPeersV(MapEpiNode theMapEpiNode)
    /* This sends to all connected peers theMapEpiNode.
      It is assumed that "TextStream" has already been added as the enclosing key 
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
        MapEpiNode scanMapEpiNode= scanPeersCursor.getSelectedMapEpiNode();
        //// if (! scanPeersCursor.testB("isConnected")) // This peer is not connected 
        if (! scanMapEpiNode.testB("isConnected")) // This peer is not connected
          continue peerLoop; // so loop to try next peer.
        //// String peerIPString= scanPeersCursor.getFieldString("IP");
        String peerIPString= scanMapEpiNode.getString("IP");
        //// String peerPortString= scanPeersCursor.getFieldString("Port");
        String peerPortString= scanMapEpiNode.getString("Port");
        IPAndPort theIPAndPort= new IPAndPort(peerIPString, peerPortString);
        Unicaster scanUnicaster= // Try getting associated Unicaster.
            theUnicasterManager.tryToGetUnicaster(theIPAndPort);
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