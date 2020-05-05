package allClasses;

// import static allClasses.Globals.appLogger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
//// import java.util.Timer;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Document;
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
      
        private JLabel titleJLabel;  // Label with the title.

        private IJTextArea streamIJTextArea; // For viewing the stream text.

        private IJTextArea inputIJTextArea; // For entering next text to append.

    // Constructors and constructor-related methods.

      public TextStreamViewer(  // Constructor.
          TreePath theTreePath, 
          DataTreeModel theDataTreeModel,
          UnicasterManager theUnicasterManager,
          Persistent thePersistent,
          ConnectionManager theConnectionManager
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
                    processLineV(messageString);
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

      public boolean processMapEpiNodeB(MapEpiNode theMapEpiNode)
        /* This is the Listener method called by the ConnectionManager
          to try decoding a TextStream message MapEpiNode.
          It returns true if the decode was successful, false otherwise.
          */
      {
        String valueString= theMapEpiNode.getString("StreamText");
        boolean decodedB= (valueString != null); // Test for a StreamText message?
        if (decodedB)
          EDTUtilities.runOrInvokeAndWaitV( // Do following on EDT thread. 
              new Runnable() {
                @Override  
                public void run() {
                  synchronized(this) {
                    processLineV(valueString);
                    }
                  }
                } 
              );
          ; // process text.
        return decodedB;
        }

      LinkedHashMap<String,Object> theLinkedHashMap= 
          new LinkedHashMap<String,Object>();
      
      private void processLineV(String messageString)
        {
          toReturn: {
            theAppLog.debug(
                "TextStreamViewer.TextStreamViewer.processLineV(.) message="
                + messageString);
            if (theLinkedHashMap.containsKey(messageString)) // Already in map?
              break toReturn; // Yes, so received before and we are ignoring it.
            theLinkedHashMap.put(messageString,null); // Put in map to prevent repeat.
            streamIJTextArea.append(messageString); // Append to stream window.
            streamIJTextArea.append("\n"); // Add JTextArea line terminator.
            { // Put cursor at end of append.
              Document d = streamIJTextArea.getDocument();
              streamIJTextArea.select(d.getLength(), d.getLength());
              }
            broadcastStreamMessageV(messageString); // Inform peers of input text.
          } // toReturn:
          }

      private void broadcastStreamMessageV(String messageString)
        /* This method notifies all connected peers about
          the messageString.
          */
        {
          EpiNode messageEpiNode= new ScalarEpiNode(messageString);
            theAppLog.debug( "TextStreamViewer.broadcastStreamMessageV() called.");
            PeersCursor scanPeersCursor= // Used for iteration. 
                PeersCursor.makeOnNoEntryPeersCursor( thePersistent );
          peerLoop: while (true) { // Process all peers in my peer list. 
            if (scanPeersCursor.nextKeyString().isEmpty() ) // Try getting next scan peer. 
              break peerLoop; // There are no more peers, so exit loop.
            theAppLog.appendToFileV("(strean?)"); // Log that peer is being considered.
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
            scanUnicaster.putV( // Queue text stream message to Unicaster of scan peer
              MapEpiNode.makeSingleEntryMapEpiNode( // wrapped in its own State map.
                "StreamText", messageEpiNode)
              );
          } // peerLoop: 
            theAppLog.appendToFileV("(end of peers)"+NL); // Mark end of list with new line.
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
              
              loadStreamV( "textStreamFile.txt"); // Load text previously saved to disk.
              
              theConnectionManager.setEpiNodeListener( // Listen to ConnectionManager for
                  theTextStreamViewer); // receiving text from  remote systems. 
              }
          
          private void loadStreamV( String fileString )
            /* This method loads the streamJTextArea from 
              the contents of the external text file whose name is fileString.  
              */
            {
              theAppLog.info("TextStreamViewer.MyTreeHelper.loadStreamV(..) begins.");
              FileReader theFileInputStream= null;
              try {
                  theFileInputStream= new FileReader(
                    AppSettings.makeRelativeToAppFolderFile(fileString));  
                  streamIJTextArea.read(theFileInputStream,null); 
                  }
                catch (FileNotFoundException theFileNotFoundException) { 
                  theAppLog.info(
                      "TextStreamViewer.MyTreeHelper.loadStreamV(..) file not found");
                  }
                catch (IOException theIOException) { 
                  theAppLog.exception(
                      "TextStreamViewer.MyTreeHelper.loadStreamV(..)", theIOException);
                  }
                finally {
                  try {
                    if ( theFileInputStream != null ) theFileInputStream.close();
                    }
                  catch ( IOException theIOException ) { 
                    theAppLog.exception(
                        "TextStreamViewer.MyTreeHelper.loadStreamV(..)", theIOException);
                    }
                  }
              theAppLog.info("TextStreamViewer.MyTreeHelper.loadStreamV(..) ends.");
              }

          public void finalizeHelperV() 
            {
              storeStreamV( "textStreamFile.txt");
              super.finalizeHelperV();
              }
          
          private void storeStreamV( String fileString )
            /* This method stores the stream data that is in main memory to 
              the external text file whose name is fileString.
              */
            {
              theAppLog.info("TextStreamViewer.MyTreeHelper.storeStreamV(..) begins.");
              FileWriter theFileOutputStream= null;
              try {
                  theFileOutputStream= new FileWriter(
                    AppSettings.makeRelativeToAppFolderFile(fileString));  
                  streamIJTextArea.write(theFileOutputStream); 
                  }
                catch (IOException theIOException) { 
                  theAppLog.exception(
                      "TextStreamViewer.MyTreeHelper.storeStreamV(..)", theIOException);
                  }
                finally {
                  try {
                    if ( theFileOutputStream != null ) theFileOutputStream.close();
                    }
                  catch ( IOException theIOException ) { 
                    theAppLog.exception(
                        "TextStreamViewer.MyTreeHelper.storeStreamV(..)", theIOException);
                    }
                  }
              theAppLog.info("TextStreamViewer.MyTreeHelper.storeStreamV(..) ends.");
              }

          } // MyTreeHelper

    }
