package allClasses;

import java.io.File;

public class TextStreams extends NamedList {

  private Persistent thePersistent;
  private AppGUIFactory theAppGUIFactory;
  
  public TextStreams( // Constructor.
      String nameString,
      AppGUIFactory theAppGUIFactory,
      Persistent thePersistent
      //// DataNode... inDataNodes  //// remove
      )
    { 
      //// super(nameString,inDataNodes);
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
    

  /*  ////
  private boolean tryProcessingByTextStreamB(MapEpiNode theMapEpiNode)
    // Returns true if TextStream was able to process, false otherwise.
    {
      return false;  //// theTextStream.tryProcessingMapEpiNodeB(theMapEpiNode);
      }
  */  ////

  }
