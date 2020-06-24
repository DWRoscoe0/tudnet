package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import java.io.File;

public class TextStreams2 extends SimplerListWithMap<String,TextStream2> {
  
  /* This class implements a list of peer TextStreams 
    based on data replication instead of TextStreams2 broadcast.  
    
    ///fix Eventually the protocols will include time-out and retrying,
      but for now those things are not included.
      Retrying will be the job of the person doing the testing
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

      boolean falseB= false;
      if (falseB) updatePeersAboutStreamsV(); //// Reference, but don't call.
      }

  private void updatePeersAboutStreamsV() 
    {
    sendToPeersV(null);
    }

  private void sendToPeersV(MapEpiNode theMapEpiNode)  ////
    /* This sends to all connected peers theMapEpiNode.
      It wraps it in a single-entry MapEpiNode with the key "TextStreams2" first.
      */
    {
        theAppLog.debug( "TextStreams2.sendToPeersV() called.");
        PeersCursor scanPeersCursor= // Used for iteration. 
            PeersCursor.makeOnNoEntryPeersCursor( thePersistent );
        MapEpiNode messageMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
            "TextStreams2", theMapEpiNode);  // Complete MapEpiNode message.
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
              "TextStreams2.sendToPeersV() non-existent Unicaster.");
          continue peerLoop; // so loop to try next peer.
          }
        theAppLog.appendToFileV("(YES!)"); // Log that we're sending data.
        scanUnicaster.putV( // Queue full message EpiNode to Unicaster of scan peer.
            messageMapEpiNode);
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
      String thePeerIdentityString= thePersistent.getEmptyOrString("PeerIdentity");
      createAndAddTextStream(thePeerIdentityString);
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
      String localPeerIdentityString= thePersistent.getEmptyOrString("PeerIdentity");
      for (String scanPeerIdentityString : peerStrings) // For every Peer folder 
        toPeerDone: { // Try creating a TextStream for this peer.
          if (localPeerIdentityString.equals(scanPeerIdentityString)) // Skip ourselves. 
            break toPeerDone;
          createAndAddTextStream(scanPeerIdentityString);
          } // toPeerDone:
      }

  protected boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode) //// remove?
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
                synchronized(this) {
                  processReplicationMessageV(payloadMapEpiNode);
                  }
                }
              } 
            );
        }
      return decodedB;
      }

  public void processReplicationMessageV(MapEpiNode theMapEpiNode)
    /* This method examines theMapEpiNode, and tries to process it. 
     */
    {
      //// do something.
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
      //// do nothing
      }

  private TextStream2 createAndAddTextStream(String peerIdentityString)
    {
      TextStream2 theTextStream= theAppGUIFactory.makeTextSteam2(peerIdentityString);
      addingV(peerIdentityString, theTextStream); // Add to list and HashMap.
      return theTextStream;
      }

  
  }
