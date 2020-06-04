package allClasses;

// import static allClasses.Globals.appLogger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.TreePath;
import static allClasses.AppLog.theAppLog;

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
        private PlainDocument thePlainDocument;
        private TextStream theTextStream;

        private Border raisedEtchedBorder= // Common style used elsewhere.
            BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

        private JLabel titleJLabel;  // Label with the title.

        private IJTextArea streamIJTextArea; // For viewing the stream text.

        private IJTextArea inputIJTextArea; // For entering next text to append.

    // Constructors and constructor-related methods.

      public TextStreamViewer(  // Constructor.
          TreePath theTreePath, 
          DataTreeModel theDataTreeModel,
          PlainDocument thePlainDocument,
          TextStream theTextStream,
          String thePeerIdentityString,
          Persistent thePersistent
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
          
          this.thePlainDocument=  thePlainDocument;
          this.theTextStream= theTextStream;

          theAppLog.debug("TextStreamViewer.TextStreamViewer(.) begins.");
          if ( theTreePath == null )  // prevent null TreePath.
            theTreePath = new TreePath( NamedLeaf.makeNamedLeaf( "ERROR TreePath" ));

          theTreeHelper= // Create and store customized TreeHelper. 
              new MyTreeHelper(this, theDataTreeModel.getMetaRoot(), theTreePath);

          setLayout( new BorderLayout() );

          addJLabelV();
          addStreamIJTextAreaV();
          String localPeerIdentityString= thePersistent.getTmptyOrString("PeerIdentity");
          if (! localPeerIdentityString.equals(thePeerIdentityString)) // Skip ourselves. 
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
          streamIJTextArea= new IJTextArea();
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
          inputIJTextArea= new IJTextArea();
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
                    theTextStream.processNewStreamStringV(messageString);
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
              }
         
          } // MyTreeHelper

    }
