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

public class TextStream1

  extends KeyedStateList< String > 

  /* This class represents a single text streams.
    */
  
  {
  
    // Variables.
  
      // Injected variables:
      private Persistent thePersistent;
      private TextStreams1 theTextStreams;

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

      TextStream1( 
          String peerIdentityString,
          Persistent thePersistent,
          TextStreams1 theTextStreams
          )
        // Constructs a TextStream with a name inString.
        { 
        // Superclass's injections.
          super( // Constructing KeyedStateList< String > superclass.
              "TextStream", // Type name but not entire name.
              peerIdentityString // key
              );
          theAppLog.debug("TextStream.TextStream(.) called.");
          this.thePersistent= thePersistent;
          Nulls.fastFailNullCheckT(theTextStreams);
          this.theTextStreams= theTextStreams;
          this.thePeerIdentityString= getKeyK(); 
          
          /*  ////
          streamFile= AppSettings.makePathRelativeToAppFolderFile(
                  "Peers" 
                  + File.separator + getKeyK()
                  + File.separator + "textStreamFile.txt"
                  );
          */  ////
          streamFile= FileOps.makePathRelativeToAppFolderFile(
              Config.textStream1FolderString,getKeyK(),"textStreamFile.txt"
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
            new TextStream1Viewer( 
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
              FileOps.makeDirectoryAndAncestorsWithLoggingV(
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

      public void processNewStreamStringV(String theString)
        /* This method processes a new Stream String entered by the user
          into an associated TextStreamViewer.
          It does this by building a MapEpiNode containing theString,
          the PeerIdentity, and the present time, 
          and passing it to the TextStreams coordinator for processing.
         */
        {
          MapEpiNode theMapEpiNode= new MapEpiNode();
          theMapEpiNode.putV("message", theString);
          String nodeIdentyString= thePersistent.getEmptyOrString("PeerIdentity");
          theMapEpiNode.putV("PeerIdentity", nodeIdentyString);
          theMapEpiNode.putV("time", ""+System.currentTimeMillis());
          theTextStreams.processIfNewV(theMapEpiNode);
          }

      public boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode) 
        /* This method tries to process TextStream message theMapEpiNode.
          It switches to the Event Dispatch Thread (EDT) if needed,
          so this method can be called from the EDT or another thread.
          */
        {
          String theString= theMapEpiNode.getString("message");
          theAppLog.debug(
              "TextStreamViewer.processStringStringV(.) String="
              + theString);
          try {
            thePlainDocument.insertString( // Append message to document as a line.
              thePlainDocument.getLength(),theString + "\n",null);
          } catch (BadLocationException theBadLocationException) { 
            theAppLog.exception(
                "TextStreamViewer.processStringStringV(..) ",theBadLocationException);
          }
          return true;
          }

    }
