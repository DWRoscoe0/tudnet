package allClasses.javafx;


import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import allClasses.ConsoleBase;
import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Persistent;
/// import static allClasses.AppLog.theAppLog;

// import static allClasses.Globals.appLogger;


public class ConsoleNode 
  extends BorderPane
  implements DocumentListener
  

  /* This class is used for displaying leaf Nodes that
   * can be displayed as blocks of text.
   * 
   * ///fix Sometimes the caret of the TextArea is below the bottom, 
   *   so it is not visible.  Make it always be visible.
   */
  
  {

    @SuppressWarnings("unused") ///
    private TreeStuff theTreeStuff;
    private TextArea theTextArea;
    private ConsoleBase theConsoleBase; 

    public static TreeStuff makeTreeStuff(
                ConsoleBase theConsoleBase,
                DataNode selectedDataNode,
                String theString,
                Persistent thePersistent,
                DataRoot theDataRoot,
                EpiTreeItem theRootEpiTreeItem,
                Selections theSelections
                )
    { 
      TreeStuff theTreeStuff= TreeStuff.makeWithAutoCompleteTreeStuff(
          theConsoleBase,
          selectedDataNode,
          thePersistent,
          theDataRoot,
          theRootEpiTreeItem,
          theSelections
          );
      ConsoleNode theConsoleNode= new ConsoleNode( 
        theConsoleBase,
        theString,
        theTreeStuff
        );
      theTreeStuff.initializeV(theConsoleNode);
      return theTreeStuff;
      }
    
    public ConsoleNode(
        ConsoleBase theConsoleBase, 
        String initialContentString, 
        TreeStuff theTreeStuff
        )
      /* Constructs a ConsoleNode.
        subjectDataNode is the node of the Tree to be displayed.
        The last DataNode in the path is that Node.
        The content text to be displayed is theString.
        */
      {
        this.theTreeStuff= theTreeStuff;
        this.theConsoleBase= theConsoleBase;
        Label titleLabel= new Label( theConsoleBase.toString());
        setTop(titleLabel); // Add title to main Pane.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);

        /// theAppLog.debug(
        ///   "ConsoleNode.ConsoleNode(.) TextArea(\""+initialContentString+"\")");
        theTextArea= // Construct TextArea with initial text.
          new TextArea(initialContentString);
        /// theTextArea.getCaret().setVisible(true); // Make viewer cursor visible.
        theTextArea.setWrapText(true); // Make all visible.
        /// theTextArea.setWrapStyleWord(true); // Make it pretty.
        Platform.runLater( () -> {
            theTextArea.requestFocus();
            theTextArea.positionCaret(theTextArea.getLength());
            } );
        theTextArea.addEventFilter( // or addEventHandler(
          KeyEvent.KEY_TYPED, (theKeyEvent) -> handleKeyV(theKeyEvent) );
        theConsoleBase.addDocumentListener(this);
        setCenter(theTextArea);
        }


    /* DocumentListener methods, to make TextArea same as ConsoleBase Document.
     * Presently only inserts are handled.
     * The cursor is placed at the end of the insert.
     */

    public void changedUpdate(DocumentEvent theDocumentEvent) {} // Not needed.

    public void insertUpdate(DocumentEvent theDocumentEvent)
      /* This method processes Document insert events by 
       * duplicating the document change in the theTextArea.
       */
      {
        final Document theDocument= theDocumentEvent.getDocument();
        final int offsetI= theDocumentEvent.getOffset();
        final int lengthI= theDocumentEvent.getLength();
        final String theString= fromDocumentGetString(
            theDocument, offsetI, lengthI);
        /// theAppLog.debug(
        ///   "ConsoleNode.insertUpdate(.) theString='"+theString+"'");
        Platform.runLater( () -> {
            //// Document tmpDocument= theDocument; ////// debug
            /// theAppLog.debug(
            ///   "ConsoleNode.insertUpdate(.) QUEUED theString='"+theString+"'");
            theTextArea.insertText(offsetI, theString);
            theTextArea.positionCaret(offsetI + theString.length());
            } );
        }

    private String fromDocumentGetString(
        Document theDocument, int offsetI, int lengthI)
      {
        String theString;
        try {
            theString= theDocument.getText(offsetI, lengthI);
          } catch (BadLocationException e1) {
            theString= "[BadLocationException]";
            ////// TODO Auto-generated catch block
            e1.printStackTrace();
          }
        return theString;
      }

    /*  ///fix in removeUpdate(.) method which follows: 
          java.lang.StringIndexOutOfBoundsException: 
                String index out of range: -127
        It happened when I pressed left-arrow and right-arrow to exit viewer
        and re-enter, it resulted in an exception.  Console showed:
          !!!!!!!!!!!!!!!!!!!!!!!!!!!!DefaultExceptionHandler.uncaughtException(..): Uncaught Exception in thread JavaFX Application Threadjava.lang.StringIndexOutOfBoundsException: 
            String index out of range: -127
          !EXCEPTION: DefaultExceptionHandler.uncaughtException(..): Uncaught Exception in thread JavaFX Application Thread :
            java.lang.StringIndexOutOfBoundsException: String index out of range: -127
          !java.lang.StringIndexOutOfBoundsException: String index out of range: -127
            at java.lang.String.substring(String.java:1967)
            at javafx.scene.control.TextInputControl.updateContent(TextInputControl.java:571)
            at javafx.scene.control.TextInputControl.replaceText(TextInputControl.java:548)
            at javafx.scene.control.TextInputControl.deleteText(TextInputControl.java:496)
            at allClasses.javafx.ConsoleNode.lambda$3(ConsoleNode.java:160)
            at com.sun.javafx.application.PlatformImpl.lambda$null$5(PlatformImpl.java:295)
        * It happened in the Runnable in ConsoleNode.removeUpdate(.) calling
          TextArea.deleteText(.).
        ? I suspect this problem is related to 
          ? a synchronization problem
          ? a leaked Listener
        I tried reproducing it and failed, 
      */
    
    public void removeUpdate(DocumentEvent theDocumentEvent)
      /* This method processes Document remove events by 
       * duplicating the document change in the theTextArea.
       */
      {
        final int offsetI= theDocumentEvent.getOffset();
        final int lengthI= theDocumentEvent.getLength();
        /// theAppLog.debug(
        ///   "ConsoleNode.removeUpdate(.) theString='"+theString+"'");
        Platform.runLater( () -> {
            /// theAppLog.debug(
            ///   "ConsoleNode.removeUpdate(.) QUEUED theString='"+theString+"'");
            theTextArea.deleteText(offsetI, offsetI+lengthI);
            theTextArea.positionCaret(offsetI);
            } );
        }

    private void handleKeyV(KeyEvent theKeyEvent)
      /* This event handler method passes the key in theKeyEvent
       * to theConsoleBase DataNode for processing.
       */
      {
        String keyString= theKeyEvent.getCharacter();

        theConsoleBase.processInputKeyV(keyString);

        theKeyEvent.consume(); // Prevent further processing.
        }

    }
