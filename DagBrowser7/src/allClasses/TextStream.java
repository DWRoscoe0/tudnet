package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.tree.TreePath;

public class TextStream

  extends DataNode

  /* This class extends DataNode to represent text streams.
    */
  
  {
  
    // Variables.
  
      // Injected variables:
      private UnicasterManager theUnicasterManager;
      private Persistent thePersistent;
      private ConnectionManager theConnectionManager;

      // Other variables:
      private PlainDocument thePlainDocument= null; // Where the stream is stored.
      LinkedHashMap<Integer,Object> antiRepeatLinkedHashMap= // Used to prevent storms.
          new LinkedHashMap<Integer,Object>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer,Object> eldest) {
                return size() > 8; // Limit map size to 8 entries.
                }
      };

    // Constructors.

      TextStream( 
          UnicasterManager theUnicasterManager,
          Persistent thePersistent,
          ConnectionManager theConnectionManager
          )
        // Constructs a TextStream with a name inString.
        { 
          theAppLog.debug("TextStream.TextStream(.) called.");
          this.theUnicasterManager= theUnicasterManager;
          this.thePersistent= thePersistent;
          this.theConnectionManager= theConnectionManager;
          
          loadDocumentV("textStreamFile.txt"); // Load document from disk text.

          this.theConnectionManager.setEpiNodeListener( // Listen to ConnectionManager for
              this); // receiving text from  remote systems.
              ///org Should this be in an DataNode .initializeV() method?
          }

    // theFile pass-through methods.

    // A subset of delegated DataTreeModel methods.

      public boolean isLeaf( )
        {
          return true;
          }

      // Methods which return Strings about the node.

      public String getNameString( )
        {
          return "Text-Stream";
          }
          
    // other interface DataNode methods.

      public JComponent getDataJComponent( 
          TreePath inTreePath, 
          DataTreeModel inDataTreeModel 
          )
        {
          theAppLog.debug("TextStream.getDataJComponent(.) called.");
          JComponent resultJComponent= 
            new TextStreamViewer( 
              inTreePath, 
              inDataTreeModel,
              thePlainDocument,
              this
              );
          return resultJComponent;  // return the final result.
          }
          
    // other methods.

      public String toString( ) { return getNameString( ); }
        /* it appears that JList uses toString() but
          JTree uses something else (getName()?).
          */
      
      private void loadDocumentV( String fileString )
        /* This method loads the stream document Area with
          the contents of the external text file whose name is fileString.
          */
        {
          theAppLog.info("TextStream.loadStreamV(..) begins.");
          BufferedReader theBufferedReader= null; 
          try {
              thePlainDocument= new PlainDocument();
              FileInputStream theFileInputStream = 
                  new FileInputStream(
                      AppSettings.makeRelativeToAppFolderFile(fileString));
              theBufferedReader = 
                new BufferedReader(new InputStreamReader(theFileInputStream));
              String lineString;
              while ((lineString = theBufferedReader.readLine()) != null) {
                thePlainDocument.insertString( // Append line to document.
                    thePlainDocument.getLength(),lineString + "\n",null);
                }
              }
            catch (BadLocationException theBadLocationException) { 
              theAppLog.exception("TextStream.loadStreamV(..) ",theBadLocationException);
              }
            catch (FileNotFoundException theFileNotFoundException) { 
              theAppLog.info("TextStream.loadStreamV(..) file not found");
              }
            catch (IOException theIOException) { 
              theAppLog.exception("TextStream.loadStreamV(..)", theIOException);
              }
            finally {
              try {
                if ( theBufferedReader != null ) theBufferedReader.close();
                }
              catch ( IOException theIOException ) { 
                theAppLog.exception("TextStream.loadStreamV(..)", theIOException);
                }
              }
          theAppLog.info("TextStream.loadStreamV(..) ends.");
          }

      protected int finalizeDataNodesI()
        /* This override method finalizes all the children and then the base class. */
        {
          storeDocumentV( "textStreamFile.txt");
          return super.finalizeDataNodesI(); // Finalize base class
          }

      private void storeDocumentV( String fileString )
        /* This method stores the stream data that is in main memory to 
          the external text file whose name is fileString.
          */
        {
          theAppLog.info("TextStream.storeDocumentV(..) begins.");
          FileWriter theFileWriter= null;
          thePlainDocument.readLock();
          try {
              theFileWriter= new FileWriter(
                AppSettings.makeRelativeToAppFolderFile(fileString));  
              writeAllLineElementsV(theFileWriter);
              }
            catch (BadLocationException theBadLocationException) { 
              theAppLog.exception( "TextStream.storeDocumentV(..)",
                  theBadLocationException);
              }
            catch (IOException theIOException) { 
              theAppLog.exception("TextStream.storeDocumentV(..)",theIOException);
              }
            finally {
              try {
                if ( theFileWriter != null ) theFileWriter.close();
                }
              catch ( IOException theIOException ) { 
                theAppLog.exception("TextStream.storeDocumentV(..)", theIOException);
                }
              thePlainDocument.readUnlock();
              }
          theAppLog.info("TextStream.storeDocumentV(..) ends.");
          }

      private void writeAllLineElementsV(FileWriter theFileWriter)
        throws BadLocationException, IOException 
      {
        Element rootElement= thePlainDocument.getDefaultRootElement();
        for // For all line elements in document...
          ( int elementI=0; elementI<rootElement.getElementCount(); elementI++ ) 
          { // Write the line to file.
            Element lineElement= rootElement.getElement(elementI);
            int startOffset= lineElement.getStartOffset();
            int endOffset= lineElement.getEndOffset();
            theFileWriter.write(thePlainDocument.getText(
                startOffset,endOffset-startOffset-1
                )); // Output one line of text.
            theFileWriter.write(NL); // Write line terminator.
            }
        }

      
      // Code imported from TextStream, to be integrated and made to work.
      
      public boolean listenerToProcessIncomingMapEpiNodeB(MapEpiNode messageMapEpiNode)
        /* This is the Listener method called by the ConnectionManager
          to try decoding a TextStream message MapEpiNode.
          It returns true if the decode was successful, false otherwise.
          Note that if the payload message EpiNode is processed,
          it is done while switched to the Event Dispatch Thread (EDT).
          */
      {
        EpiNode payloadEpiNode= messageMapEpiNode.getEpiNode("StreamText");
        boolean decodedB= (payloadEpiNode != null); // It a StreamText message?
        if (decodedB) {
          EDTUtilities.runOrInvokeAndWaitV( // Do following on EDT thread. 
              new Runnable() {
                @Override  
                public void run() {
                  synchronized(this) {
                    MapEpiNode payloadMapEpiNode= payloadEpiNode.getMapEpiNode();
                    if (payloadMapEpiNode != null) {
                      processStreamMapEpiNodeV(payloadMapEpiNode);
                      }
                    }
                  }
                } 
              );
          }
        return decodedB;
        }

      public void processStreamStringV(String theString)
        /* This method builds a MapEpiNode containing theString,
          the PeerIdentity, and the present time, 
          and passes it along for processing and possible distribution.
         */
        {
          MapEpiNode theMapEpiNode= new MapEpiNode();
          theMapEpiNode.putV("message", theString);
          String nodeIdentyString= thePersistent.getTmptyOrString("PeerIdentity");
          theMapEpiNode.putV("PeerIdentity", nodeIdentyString);
          theMapEpiNode.putV("time", ""+System.currentTimeMillis());
          processStreamMapEpiNodeV(theMapEpiNode);
          }

      public void processStreamMapEpiNodeV(MapEpiNode theMapEpiNode)
        {
            String theString= theMapEpiNode.getString("message");
          toReturn: {
            theAppLog.debug(
                "TextStreamViewer.processStringStringV(.) String="
                + theString);
            Integer hashInteger= new Integer( ///opt Probably more complicated than needed.
                theString.hashCode()+theMapEpiNode.getString("time").hashCode());
            if (antiRepeatLinkedHashMap.containsKey(hashInteger)) // Already in map?
              break toReturn; // Yes, so received before and we are ignoring it.
            antiRepeatLinkedHashMap.put(hashInteger,null); // Put in map to prevent repeat.
            try {
              thePlainDocument.insertString( // Append message to document as a line.
                thePlainDocument.getLength(),theString + "\n",null);
            } catch (BadLocationException theBadLocationException) { 
              theAppLog.exception(
                  "TextStreamViewer.processStringStringV(..) ",theBadLocationException);
              }

            MapEpiNode messageMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
                "StreamText", theMapEpiNode);  // Complete MapEpiNode message.
            distributeMessageV(messageMapEpiNode); // Distribute message as EpiNode.
          } // toReturn: 
          }
      
      private void distributeMessageV(MapEpiNode messageMapEpiNode)
        /* This method notifies all connected peers about the message messageMapEpiNode,
          which should be an immutable value.
          */
        {
            theAppLog.debug( "TextStreamViewer.broadcastStreamMessageV() called.");
            PeersCursor scanPeersCursor= // Used for iteration. 
                PeersCursor.makeOnNoEntryPeersCursor( thePersistent );
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
