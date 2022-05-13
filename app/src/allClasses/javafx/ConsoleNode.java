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

import static allClasses.AppLog.theAppLog;


public class ConsoleNode

  extends BorderPane // This the a JavaFX UI Node subclass.

  implements DocumentListener

  {

    /* This class is a JavaFX UI Node subclass, 
     * based on the JavaFX BorderPane.
     * This class is used for interacting with
     * DataNodes that are subclasses of ConsoleNode.
     * This class provides a console-like user interface
     * within the JavaFX GUI.
     * 
     * 
     * The following problems were discovered when used with VolumeChecker.
     * ///ano Detect when the caret of the TextArea scrolls out of view, 
     *     and ask the user whether that action was the result 
     *     of the user's command, or the action was an anomaly.
     *     If it was an anomaly then scroll back to the correct position.
     *     and automatically correct future scrolling anomalies
     *     without asking  the user.
     */

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
    /* This method creates a TreeStuff and an associated ConsoleNode
     * that is useful for interacting with theConsoleBase DataNode.
     */
    { 
      TreeStuff theTreeStuff= TreeStuff.makeTreeStuff(
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
      theTreeStuff.initializeGUINodeV(theConsoleNode);
      return theTreeStuff;
      }

    public ConsoleNode( // Constructor.
        ConsoleBase theConsoleBase, 
        String initialContentString, 
        TreeStuff theTreeStuff
        )
      /* This constructor constructs a ConsoleNode object.
        subjectDataNode is the DataNode with which
        the user will be interacting.
        The initial text to be displayed is initialContentString.
        theTreeStuff is the associated TreeStuff.
        */
      {
        this.theTreeStuff= theTreeStuff;
        this.theConsoleBase= theConsoleBase;

        Label titleLabel= new Label( theConsoleBase.toString());
        setTop(titleLabel); // Add title to main Pane.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);
        theTextArea= // Construct TextArea with initial text.
          new TextArea(initialContentString);
        /// theAppLog.debug(
        ///   "ConsoleNode.ConsoleNode(.) TextArea(\""+initialContentString+"\")");
        theTextArea.setWrapText(true); // Make all visible.
        Platform.runLater( () -> {
            theTextArea.requestFocus();
            theTextArea.positionCaret(theTextArea.getLength());
            ///mys ///fix  Kake cursor visible again.  Does unrequested scroll.
            } );
        theTextArea.addEventFilter( // Prepare TextArea KeyEvents processing.
          KeyEvent.ANY, (theKeyEvent) -> handleKeyV(theKeyEvent) );
        theConsoleBase.addDocumentListener(this); // Prepare base Document
          // processing.
        setCenter(theTextArea);
        }

    private void handleKeyV(KeyEvent theKeyEvent)
      /* This method is used as a KeyEvent filter in 
       * the JavaFX event capture phase.
       * It processes and consumes ALL KeyEvents passed to it.
       * It requires that a JavaFX Node have keyboard focus,
       * but prevents the Node actually processing the KeyEvent.
       * 
       * This method passes keys from KEY_TYPED events to 
       * theConsoleBase DataNode associated with this JavaFX UI Node.
       * 
       * This method consumes, but otherwise ignores, 
       * the other 2 KeyEvent types: KEY_PRESSED and KEY_RELEASED.
       * This is done because apparently TextArea
       * inserts "\n" after the caret in response to (Enter) key presses.
       */
      {
        if // Pass only KEY_TYPED events to theConsoleBase. 
          (KeyEvent.KEY_TYPED == theKeyEvent.getEventType()) 
          {
            String keyString= theKeyEvent.getCharacter();
            theConsoleBase.processInputKeyV(keyString);
            }

        theKeyEvent.consume(); // Consume to prevent further Event processing.
        }


    /* DocumentListener methods, to keep theTextArea content equal to
     *  ConsoleBase.thePlainDocument content.
     * Presently only document inserts are handled by these methods.
     * The cursor is always placed at the end of theTextArea change.
     */

    public void changedUpdate(DocumentEvent theDocumentEvent) {} // Not used.

    public void insertUpdate(DocumentEvent theDocumentEvent)
      /* This method processes Document insert events by 
       * duplicating the document changes in the theTextArea.
       */
      {
        final Document theDocument= theDocumentEvent.getDocument();
        final int offsetI= theDocumentEvent.getOffset();
        final int lengthI= theDocumentEvent.getLength();
        final String theString= fromDocumentGetString(
            theDocument, offsetI, lengthI);
        /// theAppLog.debug(
        ///   "ConsoleNode.insertUpdate(.) theString='"+theString+"'");
        Platform.runLater( () -> { // (do this because TextArea is part of GUI)
            /*  ///dbg
            logTextAreaTailStateV("before..insertText(.) ");
            theAppLog.debug(
              "ConsoleNode.insertUpdate(.) before insert TextArea is"
              + " " + theTextArea.getLength() + " chars:\n'" 
              + AppLog.glyphifyString(
                  theTextArea.getText(0, theTextArea.getLength())) 
              + "'"
              );
            theAppLog.debug("ConsoleNode.insertUpdate(.) "
              + "at offset " + offsetI
              + " " + lengthI + " chars:\n'" 
              + AppLog.glyphifyString(theString) + "'");
            */  ///dbg
            theTextArea.insertText(offsetI, theString);
            ///dbg logTextAreaTailStateV("after...insertText(.) ");
            theTextArea.positionCaret(offsetI + theString.length());
            /*  ///dbg
            logTextAreaTailStateV("after.positionCaret() ");
            theAppLog.debug(
              "ConsoleNode.insertUpdate(.) after insert TextArea is"
              + " " + theTextArea.getLength() + " chars:\n'" 
              + AppLog.glyphifyString(
                  theTextArea.getText(0, theTextArea.getLength())) 
              + "'"
              );
            */  ///dbg
            } );
        }

    /*  ///dbg
    private void logTextAreaTailStateV(String contextString)
      {
        int caretI= theTextArea.getCaretPosition();
        int lengthI= theTextArea.getLength();
        theAppLog.debug(
            "ConsoleNode.logTextAreaStateV(.) " + contextString 
            + ", caret and length: " + " " + caretI + " " + lengthI
            + ", tail: '"
              + AppLog.glyphifyString(theTextArea.getText(caretI, lengthI)) 
              + "'"
            );
        }
    */  ///dbg

    private String fromDocumentGetString(
        Document theDocument, int offsetI, int lengthI)
      /* This method tries to return a String consisting of the characters
       * in theDocument from offset offsetI to offsetI+lengthI.
       * If an exception happens then it returns 
       * a String describing the exception.
       */
      {
        String theString;
        try {
            theString= theDocument.getText(offsetI, lengthI);
          } catch (BadLocationException theBadLocationException) {
            theString= theBadLocationException.toString();
            theAppLog.error("ConsoleNode.fromDocumentGetString(.) "+theString);
          }
        return theString;
      }

    /*  ///fix Watch for re-occurrence of following problem in removeUpdate(). 
      The method which follows, removeUpdate(.), produced: 
        java.lang.StringIndexOutOfBoundsException: 
              String index out of range: -127
      It happened when I pressed left-arrow and right-arrow to exit viewer
      and re-enter.  It resulted in an exception.  Console showed:
        ........DefaultExceptionHandler.uncaughtException(..): Uncaught Exception in thread JavaFX Application Threadjava.lang.StringIndexOutOfBoundsException: 
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
      I tried reproducing it and failed. 
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
        Platform.runLater( () -> { // (do this because TextArea is part of GUI)
            /// theAppLog.debug(
            ///   "ConsoleNode.removeUpdate(.) QUEUED theString='"+theString+"'");
            theTextArea.deleteText(offsetI, offsetI+lengthI);
            theTextArea.positionCaret(offsetI);
            } );
        }

    }
