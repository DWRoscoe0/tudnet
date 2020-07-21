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

import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.tree.TreePath;

public class TextStream2

  extends KeyedStateList< String > 

  /* This class represents a single text streams.
    */
  
  {
  
    // Variables.
  
      // Injected variables:
      private Persistent thePersistent;
      private TextStreams2 theTextStreams2;

      // Other variables:
      private PlainDocument thePlainDocument= null; // Internal document store.
      private File streamFile; // File name of external document store. 
      private String theRootIdString;
      
    // Constructors.

      TextStream2( 
          String peerIdentityString,
          Persistent thePersistent,
          TextStreams2 theTextStreams2
          )
        { 
          super( // Superclass KeyedStateList<String> constructor injections.
              "TextStream", // Type name but not entire name.
              peerIdentityString // key
              );
          theAppLog.debug("TextStream2.TextStream(.) called.");
          this.thePersistent= thePersistent;
          Nulls.fastFailNullCheckT(theTextStreams2);
          this.theTextStreams2= theTextStreams2;
          this.theRootIdString= getKeyK(); 
          
          streamFile= FileOps.makePathRelativeToAppFolderFile(
              Config.textStream2FolderString,getKeyK(),"textStreamFile.txt"
              ); // Create file path name.
          loadDocumentV(streamFile); // Load document from file.
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
          theAppLog.debug("TextStream2.getDataJComponent(.) called.");
          JComponent resultJComponent= 
            new TextStream2Viewer( 
              inTreePath, 
              inDataTreeModel,
              thePlainDocument,
              this,
              theRootIdString,
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
          theAppLog.info("TextStream2.loadStreamV(..) begins.");
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
              theAppLog.exception("TextStream2.loadStreamV(..) ",theBadLocationException);
              }
            catch (FileNotFoundException theFileNotFoundException) { 
              theAppLog.info("TextStream2.loadStreamV(..) "
                + theFileNotFoundException);
              }
            catch (IOException theIOException) { 
              theAppLog.exception("TextStream2.loadStreamV(..)", theIOException);
              }
            finally {
              try {
                if ( theBufferedReader != null ) theBufferedReader.close();
                }
              catch ( IOException theIOException ) { 
                theAppLog.exception("TextStream2.loadStreamV(..)", theIOException);
                }
              }
          theAppLog.info("TextStream2.loadStreamV(..) ends.");
          }

      public void requestNextTextV()
        /* Notifies text sources that we are interested in receiving
         * the next append-able text. 
         */
        {
          theAppLog.debug("TextStream2.requestNextTextV() called.");
          if (! isOurStreamB())
            {
              MapEpiNode theMapEpiNode= new MapEpiNode();
              String nodeIdentyString= 
                  thePersistent.getEmptyOrString(Config.rootIdString);
              theMapEpiNode.putV(Config.rootIdString, nodeIdentyString);
              theMapEpiNode.putV(
                  "StartOffset", ""+thePlainDocument.getLength());
              theTextStreams2.sendToPeersV(theMapEpiNode);  ////
              }

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
          theAppLog.info("TextStream2.storeDocumentV(..) begins.");
          FileWriter theFileWriter= null;
          thePlainDocument.readLock();
          try {
              FileOps.makeDirectoryAndAncestorsWithLoggingV(
                theFile.getParentFile()); // Create directory if needed.
              theFileWriter= new FileWriter( theFile ); // Open file.
              writeAllLineElementsV(theFileWriter);
              }
            catch (BadLocationException theBadLocationException) { 
              theAppLog.exception( "TextStream2.storeDocumentV(..)",
                  theBadLocationException);
              }
            catch (IOException theIOException) { 
              theAppLog.exception("TextStream2.storeDocumentV(..)",theIOException);
              }
            finally {
              try {
                if ( theFileWriter != null ) theFileWriter.close();
                }
              catch ( IOException theIOException ) { 
                theAppLog.exception("TextStream2.storeDocumentV(..)", theIOException);
                }
              thePlainDocument.readUnlock();
              }
          thePlainDocument= null; // Indicate document is no longer being viewed.
          theAppLog.info("TextStream2.storeDocumentV(..) ends.");
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

      public void processEnteredStringV(String theString)
        /* This method processes a new Stream String entered by the user
          into an associated TextStreamViewer.
          It is appended to the stream document.
         */
        {
          try {
              thePlainDocument.insertString( // Append message to document as a line.
                thePlainDocument.getLength(),theString + "\n",null);
            } catch (BadLocationException theBadLocationException) { 
              theAppLog.exception(
                "TextStream2.processEnteredStringV(..) ",theBadLocationException);
            }
          processSendAbleChangeV();
          }

      private void processSendAbleChangeV()
        /* This method tries to satisfy any stream subscription requests
         * that are satisfiable.
         */
        {}
      
      public void processNewStreamStringV(String theString) ////
        /* This method processes a new Stream String entered by the user
          into an associated TextStreamViewer.
          It does this by building a MapEpiNode containing theString,
          the RootId, and the present time, 
          and passing it to the TextStreams coordinator for processing.
         */
        {
          MapEpiNode theMapEpiNode= new MapEpiNode();
          theMapEpiNode.putV("text", theString);
          String nodeIdentyString= thePersistent.getEmptyOrString(Config.rootIdString);
          theMapEpiNode.putV(Config.rootIdString, nodeIdentyString);
          theMapEpiNode.putV("time", ""+System.currentTimeMillis());
          theTextStreams2.processNewTextV(theMapEpiNode);
          }

      public boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode) 
        /* This method tries to process TextStream message theMapEpiNode.
          It switches to the Event Dispatch Thread (EDT) if needed,
          so this method can be called from the EDT or another thread.
          */
        {
          String theString= theMapEpiNode.getString("text");
          theAppLog.debug(
              "TextStream2.tryProcessingMapEpiNodeB(.) String="
              + theString);
          try {
            thePlainDocument.insertString( // Append message to document as a line.
              thePlainDocument.getLength(),theString + "\n",null);
          } catch (BadLocationException theBadLocationException) { 
            theAppLog.exception(
                "TextStream2.tryProcessingMapEpiNodeB(..) ",theBadLocationException);
          }
          return true;
          }

      public boolean isOurStreamB()
        /* Returns true is this is our stream, editable,
          and there not a read-only stream.
          */
        {
          String localRootIdString= 
                thePersistent.getEmptyOrString(Config.rootIdString);
          boolean resultB=  
              (localRootIdString.equals(theRootIdString)); 
          return resultB;
          }
      
      }  
