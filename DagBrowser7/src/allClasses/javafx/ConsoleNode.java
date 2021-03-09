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

// import static allClasses.Globals.appLogger;


public class ConsoleNode 
  extends BorderPane
  implements DocumentListener
  

  /* This class is used for displaying leaf Nodes that
   * can be displayed as blocks of text.
   * 
   * ///fix Though the name of this class includes "Titled",
   * it does not yet display a title.
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

        //// theAppLog.debug(
        ////   "ConsoleNode.ConsoleNode(.) TextArea('"+initialContentString+"'");
        theTextArea= // Construct TextArea with initial text.
          new TextArea(initialContentString);
        /// theTextArea.getCaret().setVisible(true); // Make viewer cursor visible.
        theTextArea.setWrapText(true); // Make all visible.
        /// theTextArea.setWrapStyleWord(true); // Make it pretty.
        //// theTextArea.requestFocus();
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

    public void changedUpdate(DocumentEvent e) {} // Not needed.

    public void insertUpdate(DocumentEvent e) 
      {
        try {
          Document theDocument= e.getDocument();
          int offsetI= e.getOffset();
          int lengthI= e.getLength();
          final String theString= theDocument.getText(offsetI, lengthI);
          //// theAppLog.debug(
          ////   "ConsoleNode.insertUpdate(.) theString='"+theString+"'");
          Platform.runLater( () -> {
              //// theAppLog.debug(
              ////   "ConsoleNode.insertUpdate(.) QUEUED theString='"+theString+"'");
              int endI= theTextArea.getLength();
              theTextArea.insertText(endI, theString);
              theTextArea.positionCaret(theTextArea.getLength());
              } );
        } catch (BadLocationException e1) {
          ////// TODO Auto-generated catch block
          e1.printStackTrace();
        }
      }

    public void removeUpdate(DocumentEvent e) {} // Not needed.


    private void handleKeyV(KeyEvent theKeyEvent)
      /* This event handler method passes the key in theKeyEvent
       * to theConsoleBase DataNode for processing.
       */
      {
        String keyString= theKeyEvent.getCharacter();
        //// theTextArea.appendText(
        ////     "The character '"+keyString+"' was typed.\n");

        theConsoleBase.processInputKeyV(keyString);
        theKeyEvent.consume(); // Prevent further processing.
        }

    }
