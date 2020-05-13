package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import java.io.BufferedReader;
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

public class TextStream

  extends DataNode

  /* This class extends DataNode to represent text streams.
    */
  
  {
  
    // Variables.
  
      // Injected variables:
      private String valueString;
      private UnicasterManager theUnicasterManager;
      private Persistent thePersistent;
      private ConnectionManager theConnectionManager;

      // Other variables:
      private PlainDocument thePlainDocument= null;

    // Constructors.

      TextStream( 
          String inString, 
          UnicasterManager theUnicasterManager,
          Persistent thePersistent,
          ConnectionManager theConnectionManager
          )
        // Constructs a TextStream with a name inString.
        { 
          theAppLog.debug("TextStream.TextStream(.) called.");
          valueString= inString;
          this.theUnicasterManager= theUnicasterManager;
          this.thePersistent= thePersistent;
          this.theConnectionManager= theConnectionManager;
          
          loadDocumentV("textStreamFile.txt"); // Load document from disk text.
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

      public String getContentString()
        /* This method produces the value which is used by
          TitledTextViewer to display the contents of a file.
          */
        {
          return valueString;
          }
      
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
              theUnicasterManager,
              thePersistent,
              theConnectionManager,
              thePlainDocument
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

      protected void finalizeDataNodesV()
        /* This override method finalizes all the children and then the base class. */
        {
          storeDocumentV( "textStreamFile.txt");
          super.finalizeDataNodesV(); // Finalize base class
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

    }
