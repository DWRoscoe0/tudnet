package allClasses;

// import static allClasses.Globals.appLogger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.TreePath;
import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

public class TextStreamViewer

  extends JPanel
 
  implements 
    TreeAware
    // TreeModelListener
  
  /* This class, based on TextViewer, will eventually be a TextStreamViewer.
   * 
   * TextViewer was a simple DagNodeViewer that
   * displays and browses Text using a JTextArea.
   * It was based on TitledTextViewer.
   * It was created from TextViewer, which was created quickly from ListViewer.
   * For a while it contained a lot of unused and useless code,
   * but it has been trimmed down.
   */
    
  {
    // variables.
    
      // static variables.

      // instance variables.
  
        // Constructor-injected variables.
        private UnicasterManager theUnicasterManager;
        private Persistent thePersistent;
        private ConnectionManager theConnectionManager;

        private Border raisedEtchedBorder= // Common style used elsewhere.
            BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

        private PlainDocument thePlainDocument;

        private JLabel titleJLabel;  // Label with the title.

        private IJTextArea streamIJTextArea; // For viewing the stream text.

        private IJTextArea inputIJTextArea; // For entering next text to append.

    // Constructors and constructor-related methods.

      public TextStreamViewer(  // Constructor.
          TreePath theTreePath, 
          DataTreeModel theDataTreeModel,
          UnicasterManager theUnicasterManager,
          Persistent thePersistent,
          ConnectionManager theConnectionManager,
          PlainDocument thePlainDocument
          )
        /* Constructs a TextStreamViewer.
          theTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that Node.
          The contents is theString.
          theTreeModel provides context.
          */
        {
          super();   // Constructing the superclass JPanel.
          
          this.theUnicasterManager= theUnicasterManager;
          this.thePersistent= thePersistent;
          this.theConnectionManager= theConnectionManager;
          this.thePlainDocument=  thePlainDocument;

          theAppLog.debug("TextStreamViewer.TextStreamViewer(.) begins.");
          if ( theTreePath == null )  // prevent null TreePath.
            theTreePath = new TreePath( NamedLeaf.makeNamedLeaf( "ERROR TreePath" ));

          theTreeHelper= // Create and store customized TreeHelper. 
              new MyTreeHelper(this, theDataTreeModel.getMetaRoot(), theTreePath);

          setLayout( new BorderLayout() );

          addJLabelV();
          addStreamIJTextAreaV();
          addInputIJTextAreaV();
          }

      private void addJLabelV()
        {
          titleJLabel= new JLabel(theTreeHelper.getWholeDataNode().getNameString( ));
          titleJLabel.setOpaque( true );
          Font labelFont= titleJLabel.getFont();
          titleJLabel.setFont(labelFont.deriveFont( labelFont.getSize() * 1.5f) );
          titleJLabel.setHorizontalAlignment( SwingConstants.CENTER );
          titleJLabel.setBorder(raisedEtchedBorder);
          add(titleJLabel,BorderLayout.NORTH); // Adding it to top of main JPanel.
          }

      private void addStreamIJTextAreaV()
        {
          streamIJTextArea= new IJTextArea("");
          streamIJTextArea.getCaret().setVisible(true); // Make viewer cursor visible.
          streamIJTextArea.setBorder(raisedEtchedBorder);
          streamIJTextArea.setEditable(false);
          streamIJTextArea.setLineWrap(true);
          streamIJTextArea.setWrapStyleWord(true);
          streamIJTextArea.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
              streamIJTextArea.getCaret().setVisible(true); // Make  cursor visible again.
              }
            public void focusLost(FocusEvent e) {}
            });
          JScrollPane streamJScrollPane= // Place the JTextArea in a scroll pane.
              new JScrollPane(streamIJTextArea);
          add(streamJScrollPane,BorderLayout.CENTER); // Adding to center.
          }
      
      private void addInputIJTextAreaV()
        {
          inputIJTextArea= new IJTextArea("");
          inputIJTextArea.getCaret().setVisible(true); // Make input cursor visible.
          inputIJTextArea.setBorder(raisedEtchedBorder);
          inputIJTextArea.setRows(2);
          inputIJTextArea.setEditable(true);
          inputIJTextArea.setLineWrap(true);
          inputIJTextArea.setWrapStyleWord(true);
          inputIJTextArea.addKeyListener(new KeyListener(){
              @Override
              public void keyPressed(KeyEvent theKeyEvent){
                if(theKeyEvent.getKeyCode() == KeyEvent.VK_ENTER){
                  { // Move all text from input area to stream area.
                    String messageString= inputIJTextArea.getText();
                    theAppLog.debug(
                        "TextStreamViewer.TextStreamViewer.keyPressed(.) ENTER pressed.");
                    inputIJTextArea.setText(""); // Clear input area for next line.
                    processStreamPayloadV(messageString);
                    }
                  theKeyEvent.consume(); // Prevent further processing.
                  }
                }
              @Override
              public void keyTyped(KeyEvent e) {}
              @Override
              public void keyReleased(KeyEvent e) {}
              });
          add(inputIJTextArea,BorderLayout.SOUTH); // Adding it at bottom of JPanel.
          }

      public boolean processIncomingMapEpiNodeB(MapEpiNode theMapEpiNode)
        /* This is the Listener method called by the ConnectionManager
          to try decoding a TextStream message MapEpiNode.
          It returns true if the decode was successful, false otherwise.
          */
      {
        EpiNode payloadEpiNode= theMapEpiNode.getEpiNode("StreamText");
        boolean decodedB= (payloadEpiNode != null); // It a StreamText message?
        if (decodedB) {
          //// String valueString= payloadEpiNode.toString();
          EDTUtilities.runOrInvokeAndWaitV( // Do following on EDT thread. 
              new Runnable() {
                @Override  
                public void run() {
                  synchronized(this) {
                    //// processStreamPayloadV(valueString);
                    processStreamPayloadV(payloadEpiNode);
                    }
                  }
                } 
              );
          }
        return decodedB;
        }

      LinkedHashMap<String,Object> antiRepeatLinkedHashMap= // Used to prevent storms.
          new LinkedHashMap<String,Object>();

      //// Text stream message area.  Cancelled!
      //// Because of queuing of values, later mutations can cause errors.
    ////ScalarEpiNode payloadScalarEpiNode= new ScalarEpiNode(null); 
    ////// A message String will replace the null above.
    ////MapEpiNode messageMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
    ////  "StreamText", payloadScalarEpiNode);  // Complete MapEpiNode message.
      
      private void processStreamPayloadV(EpiNode payloadEpiNode)
        {
          String payloadString= payloadEpiNode.toString();
          processStreamPayloadV(payloadString);
          }
        
      private void processStreamPayloadV(String payloadString)
        {
          toReturn: {
            theAppLog.debug(
                "TextStreamViewer.TextStreamViewer.processLineV(.) payload="  //////
                + payloadString);
            if (antiRepeatLinkedHashMap.containsKey(payloadString)) // Already in map?
              break toReturn; // Yes, so received before and we are ignoring it.
            antiRepeatLinkedHashMap.put(payloadString,null); // Put in map to prevent repeat.
            //// streamIJTextArea.append(messageString); // Append to stream window.
            //// streamIJTextArea.append("\n"); // Add JTextArea line terminator.
            try {
              thePlainDocument.insertString( // Append message to document as a line.
                thePlainDocument.getLength(),payloadString + "\n",null);
            } catch (BadLocationException theBadLocationException) { 
              theAppLog.exception(
                  "TextStreamViewer.processLineV(..) ",theBadLocationException);
              }

            putCursorAtEndOfStreamDocumentV();
            
            ScalarEpiNode payloadScalarEpiNode= new ScalarEpiNode(payloadString); 
            MapEpiNode messageMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode(
                "StreamText", payloadScalarEpiNode);  // Complete MapEpiNode message.
            ////payloadScalarEpiNode.storeStringV( // Store string in message for sending.
            ////    payloadString);
            distrubuteMessageV(messageMapEpiNode); // Distribute message as EpiNode.
          } // toReturn: 
          }
      
      private void distrubuteMessageV(MapEpiNode messageMapEpiNode)
        /* This method notifies all connected peers about the message messageMapEpiNode,
          which should be an immutable value.
          */
        {
            theAppLog.debug( "TextStreamViewer.broadcastStreamMessageV() called.");
            //// EpiNode messageEpiNode= new ScalarEpiNode(messageString);
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
            //// scanUnicaster.putV( // Queue text stream message to Unicaster of scan peer
             ////   MapEpiNode.makeSingleEntryMapEpiNode( // wrapped in its own State map.
             ////     "StreamText", messageEpiNode)
             ////   );
            scanUnicaster.putV( // Queue full message EpiNode to Unicaster of scan peer.
                messageMapEpiNode);
          } // peerLoop: 
            theAppLog.appendToFileV("(end of peers)"+NL); // Mark end of list with new line.
          }

      private void putCursorAtEndOfStreamDocumentV()
        {
          Document d = streamIJTextArea.getDocument();
          streamIJTextArea.select(d.getLength(), d.getLength());
          }

    // rendering methods.  to be added ??

    // TreeAware interface code for TreeHelper access.

      public TreeHelper theTreeHelper;

      public TreeHelper getTreeHelper() { return theTreeHelper; }

      class MyTreeHelper  // TreeHelper customization subclass.

        extends TreeHelper 

        {
          TextStreamViewer theTextStreamViewer; 

          MyTreeHelper(  // Constructor.
              TextStreamViewer theTextStreamViewer, 
              MetaRoot theMetaRoot,
              TreePath inTreePath
              )
            {
              super(theTextStreamViewer, theMetaRoot, inTreePath);

              this.theTextStreamViewer= theTextStreamViewer; // Save a copy. 
              }

          public void initializeHelperV( 
              TreePathListener coordinatingTreePathListener,
              FocusListener coordinatingFocusListener,
              DataTreeModel theDataTreeModel
              )
            {
              super.initializeHelperV( // Call superclass constructor.
                  coordinatingTreePathListener,
                  coordinatingFocusListener,
                  theDataTreeModel
                  );
              
              streamIJTextArea.setDocument(thePlainDocument); 
              putCursorAtEndOfStreamDocumentV();
              theConnectionManager.setEpiNodeListener( // Listen to ConnectionManager for
                  theTextStreamViewer); // receiving text from  remote systems. 
              }
         
          } // MyTreeHelper

    }
