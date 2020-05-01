package allClasses;

// import static allClasses.Globals.appLogger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
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
        private Border raisedEtchedBorder= // Common style used elsewhere.
            BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
      
        private JLabel titleJLabel;  // Label with the title.

        private IJTextArea streamIJTextArea; // For viewing the stream text.

        private IJTextArea inputIJTextArea; // For entering next text to append.

    // Constructors and constructor-related methods.

      public TextStreamViewer(  // Constructor.
          TreePath theTreePath, 
          DataTreeModel theDataTreeModel 
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
          add(streamIJTextArea,BorderLayout.CENTER); // Adding to center.
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
                  inputIJTextArea.append("\n"); // Add JTextArea line terminator.
                  { // Move all text from input area to stream area.
                    streamIJTextArea.append(inputIJTextArea.getText());
                    inputIJTextArea.setText("");
                    }
                  theKeyEvent.consume(); // Prevent further processing.
                  }
              }
      
              @Override
              public void keyTyped(KeyEvent e) {
              }
      
              @Override
              public void keyReleased(KeyEvent e) {
              }
            });
          add(inputIJTextArea,BorderLayout.SOUTH); // Adding it at bottom of JPanel.
          }
        
    // rendering methods.  to be added ??

    // TreeAware interface code for TreeHelper access.

      public TreeHelper theTreeHelper;

      public TreeHelper getTreeHelper() { return theTreeHelper; }

      class MyTreeHelper  // TreeHelper customization subclass.

        extends TreeHelper 

        {

          MyTreeHelper(  // Constructor.
              JComponent inOwningJComponent, 
              MetaRoot theMetaRoot,
              TreePath inTreePath
              )
            {
              super(inOwningJComponent, theMetaRoot, inTreePath);
              }

          public void initializeHelperV( 
              TreePathListener coordinatingTreePathListener,
              FocusListener coordinatingFocusListener,
              DataTreeModel theDataTreeModel
              )
            {
              super.initializeHelperV(
                  coordinatingTreePathListener,
                  coordinatingFocusListener,
                  theDataTreeModel
                  );
              loadStreamV( "textStreamFile.txt");
              }
          
          private void loadStreamV( String fileString )
            /* This method loads the streamJTextArea from 
              the contents of the external text file whose name is fileString.  
              Note, this opens a random access file,
              though random access is not needed, yet.
              */
            {
              RandomAccessInputStream theRandomAccessInputStream= null;
              RandomAccessFile theRandomAccessFile= null;

              try { 
                  theRandomAccessFile= new RandomAccessFile(
                      AppSettings.makeRelativeToAppFolderFile( fileString ),"r");
                  theRandomAccessInputStream= 
                      new RandomFileInputStream(theRandomAccessFile);
                  byte[] bufferAB= new byte[1024];
                  int lengthI;
                  while (true) {
                    lengthI= theRandomAccessInputStream.read(bufferAB);
                    if (lengthI <= 0) // Transfer completed.
                      break; // Record success and exit loop.
                    String readString= new String(bufferAB);
                    streamIJTextArea.append(readString);
                    }
                  } 
                catch (FileNotFoundException theFileNotFoundException) { 
                  theAppLog.warning("Persistent.loadEpiNodeDataV(..)"+theFileNotFoundException);
                  }
                catch (Exception theException) { 
                  theAppLog.exception("Persistent.loadEpiNodeDataV(..)", theException);
                  }
                finally { 
                  try { 
                    if ( theRandomAccessInputStream != null ) theRandomAccessInputStream.close(); 
                    if ( theRandomAccessFile != null ) theRandomAccessFile.close(); 
                    }
                  catch (Exception theException) { 
                    theAppLog.exception("Persistent.loadEpiNodeDataV(..)", theException);
                    }
                  }
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
