package allClasses;

import java.io.File;
//// import java.util.ListIterator;

//// public class TextStreams extends NamedList {
public class TextStreams extends SimplerListWithMap<String,TextStream> {

  private Persistent thePersistent;
  private AppGUIFactory theAppGUIFactory;
  public TextStreams( // Constructor.
      String nameString,
      
      AppGUIFactory theAppGUIFactory,
      Persistent thePersistent
      )
    { 
      super(nameString,emptyListOfDataNodes());
      this.thePersistent= thePersistent;
      this.theAppGUIFactory= theAppGUIFactory;
      
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
      If it does then it returns true after having the value 
      processed by the appropriate child.
      Presently it does this by trying to have each child process that value,
      by its method of the same name, until one of them returns true,
      or the end of the child list is reached.
      //// Later it will pass it to the child with the matching PeerIdentity.
      */
    { 
      MapEpiNode payloadMapEpiNode= theMapEpiNode.getMapEpiNode("StreamText");
      boolean decodedB= (payloadMapEpiNode != null); // It a StreamText message?
      if (decodedB) {
        //// super.tryProcessingMapEpiNodeB(theMapEpiNode); // Just distribute.
        super.tryProcessingMapEpiNodeB(payloadMapEpiNode); // Just distribute.
        }
      return decodedB;
      }
    
  }
