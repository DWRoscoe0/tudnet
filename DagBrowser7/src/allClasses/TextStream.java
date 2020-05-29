package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import java.io.BufferedReader;
import java.io.File;
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

  extends KeyedStateList< String > 

  /* This class represents a single text streams.
    */
  
  {
  
    // Variables.
  
      // Injected variables:
      private UnicasterManager theUnicasterManager;
      private Persistent thePersistent;

      // Other variables:
      private PlainDocument thePlainDocument= null; // Where the stream is stored.
      LinkedHashMap<Integer,Object> antiRepeatLinkedHashMap= // Used to prevent storms.
          new LinkedHashMap<Integer,Object>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer,Object> eldest) {
                return size() > 8; // Limit map size to 8 entries.
                }
          };
      private File streamFile; 
      private String thePeerIdentityString;
      
    // Constructors.

      TextStream( 
          String peerIdentityString,
          UnicasterManager theUnicasterManager,
          Persistent thePersistent
          )
        // Constructs a TextStream with a name inString.
        { 
        // Superclass's injections.
          super( // Constructing KeyedStateList< String > superclass.
              "TextStream", // Type name but not entire name.
              peerIdentityString // key
              );
          theAppLog.debug("TextStream.TextStream(.) called.");
          this.theUnicasterManager= theUnicasterManager;
          this.thePersistent= thePersistent;
          
          streamFile= AppSettings.makePathRelativeToAppFolderFile(
                  "Peers" 
                  + File.separator + getKeyK()
                  + File.separator + "textStreamFile.txt"
                  );
          loadDocumentV(streamFile); // Load document from disk text.
          }

    // theFile pass-through methods.

    // A subset of delegated DataTreeModel methods.

      public boolean isLeaf( )
        {
          return true;
          }

      // Methods about returning Strings about the node.

      public boolean isDecoratingB()
        /* Disables DataNode String decoration because we 
          don't want that StateList feature.
          */
        {
          return false;
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
              this,
              thePeerIdentityString,
              thePersistent
              );
          return resultJComponent;  // return the final result.
          }
          
    // other methods.
      
      private void loadDocumentV( File theFile )
        /* This method loads the stream document Area with
          the contents of the external text file whose name is fileString.
          */
        {
          theAppLog.info("TextStream.loadStreamV(..) begins.");
          BufferedReader theBufferedReader= null; 
          try {
              thePlainDocument= new PlainDocument();
              FileInputStream theFileInputStream= new FileInputStream( theFile );
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
              theAppLog.info("TextStream.loadStreamV(..) "
                + theFileNotFoundException);
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
          storeDocumentV( streamFile );
          return super.finalizeDataNodesI(); // Finalize base class
          }

      private void storeDocumentV( File theFile )
        /* This method stores the stream data that is in main memory to 
          the external text file whose name is fileString.
          */
        {
          theAppLog.info("TextStream.storeDocumentV(..) begins.");
          FileWriter theFileWriter= null;
          thePlainDocument.readLock();
          try {
              AppSettings.makeDirectoryAndAncestorsWithLoggingV(
                theFile.getParentFile()); // Create directory if needed.
              theFileWriter= new FileWriter( theFile ); // Open file.
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
          thePlainDocument= null; // Indicate document is no longer being viewed.
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
      
      public boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode)
        /* This method tries to process TextStream message theMapEpiNode.
          It switches to the Event Dispatch Thread (EDT) if needed,
          so this method can be called from the EDT or another thread.
          */
      {
        EDTUtilities.runOrInvokeAndWaitV( // Do following on EDT thread. 
            new Runnable() {
              @Override  
              public void run() {
                synchronized(this) {
                  processStreamMapEpiNodeV(theMapEpiNode);
                  }
                }
              } 
            );
        return true;
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
