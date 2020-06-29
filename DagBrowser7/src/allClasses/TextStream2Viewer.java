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

public class TextStream2Viewer

  extends JPanel
 
  implements 
    TreeAware
    // TreeModelListener
  
  /* This class if a JComponent for viewing a TextStream.
    If the TextStream is our own then it includes an input area
    into which the user may type text to be appended to the stream.
    */
    
  {
    // variables.
    
      // static variables.

      // instance variables.
  
        // Constructor-injected variables.
        private PlainDocument thePlainDocument;
        private TextStream2 theTextStream2;

        private Border raisedEtchedBorder= // Common style used elsewhere.
            BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

        private JLabel titleJLabel;  // Label with the title.

        private IJTextArea outputIJTextArea; // For viewing the stream text.

        private IJTextArea inputIJTextArea; // For entering next text to be appended.

    // Constructors and constructor-related methods.

      public TextStream2Viewer(  // Constructor.
          TreePath theTreePath, 
          DataTreeModel theDataTreeModel,
          PlainDocument thePlainDocument,
          TextStream2 theTextStream,
          String theRootIdString,
          Persistent thePersistent
          )
        /* Constructs a TextStream2Viewer.
          theTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that Node.
          The contents is theString.
          theTreeModel provides context.
          */
        {
          super();   // Constructing the superclass JPanel.
          
          this.thePlainDocument=  thePlainDocument;
          this.theTextStream2= theTextStream;

          theAppLog.debug("TextStream2Viewer.TextStream2Viewer(.) begins.");
          if ( theTreePath == null )  // prevent null TreePath.
            theTreePath = new TreePath( NamedLeaf.makeNamedLeaf( "ERROR TreePath" ));

          theTreeHelper= // Create and store customized TreeHelper. 
              new MyTreeHelper(this, theDataTreeModel.getMetaRoot(), theTreePath);

          setLayout( new BorderLayout() );

          addJLabelV();
          addStreamIJTextAreaV();
          String localRootIdString= 
              thePersistent.getEmptyOrString(Config.rootIdString);
          if // If the TextStream 
            (localRootIdString.equals(theRootIdString)) // is our own 
            addInputIJTextAreaV(); // then add the input TextArea.
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
          outputIJTextArea= new IJTextArea();
          outputIJTextArea.getCaret().setVisible(true); // Make viewer cursor visible.
          outputIJTextArea.setBorder(raisedEtchedBorder);
          outputIJTextArea.setEditable(false);
          outputIJTextArea.setLineWrap(true);
          outputIJTextArea.setWrapStyleWord(true);
          outputIJTextArea.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
              outputIJTextArea.getCaret().setVisible(true); // Make  cursor visible again.
              }
            public void focusLost(FocusEvent e) {}
            });
          JScrollPane streamJScrollPane= // Place the JTextArea in a scroll pane.
              new JScrollPane(outputIJTextArea);
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
                        "TextStream2Viewer.TextStream2Viewer.keyPressed(.) ENTER pressed.");
                    inputIJTextArea.setText(""); // Clear input area for next line.
                    theTextStream2.processNewStreamStringV(messageString);
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
          Document d = outputIJTextArea.getDocument();
          outputIJTextArea.select(d.getLength(), d.getLength());
          }

    // rendering methods.  to be added ??

    // TreeAware interface code for TreeHelper access.

      public TreeHelper theTreeHelper;

      public TreeHelper getTreeHelper() { return theTreeHelper; }

      class MyTreeHelper  // TreeHelper customization subclass.

        extends TreeHelper 

        {
          TextStream2Viewer theTextStream2Viewer; 

          MyTreeHelper(  // Constructor.
              TextStream2Viewer theTextStream2Viewer, 
              MetaRoot theMetaRoot,
              TreePath inTreePath
              )
            {
              super(theTextStream2Viewer, theMetaRoot, inTreePath);

              this.theTextStream2Viewer= theTextStream2Viewer; // Save a copy. 
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
              
              outputIJTextArea.setDocument(thePlainDocument); 
              putCursorAtEndOfStreamDocumentV();
              }
         
          } // MyTreeHelper

    }
