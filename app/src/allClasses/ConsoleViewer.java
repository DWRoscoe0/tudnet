package allClasses;


import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Document;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.tree.TreePath;
import static allClasses.AppLog.theAppLog;


public class ConsoleViewer

  extends JPanel
 
  implements 
    TreeAware
    // TreeModelListener
  
  /* 
   * This class is based on the old InstallerBuilder viewer,
   * which was based on TextStream2Viewer.
   * This is a JComponent for using a TextArea for
   * implementing a console-like user interface. 
   */
    
  {
    // variables.
    
      // static variables.

      // instance variables.
  
        // Constructor-injected variables.
        //// private Persistent thePersistent;
        
        private Border raisedEtchedBorder= // Common style used elsewhere.
            BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        private JLabel titleJLabel;  // Label with the title.
        private IJTextArea ioIJTextArea; // For entering next text to be appended.

    // Constructors and constructor-related methods.

      public ConsoleViewer(  // Constructor.
          TreePath theTreePath, 
          DataTreeModel theDataTreeModel,
          Persistent thePersistent //// leave for now.
          )
        /* Constructs a ConsoleViewer viewer.
          theTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that Node.
          theTreeModel provides context.
          */
        {
          super();   // Constructing the superclass JPanel.
          
          //// this.thePersistent= thePersistent;

          theAppLog.debug("ConsoleViewer.ConsoleViewer(.) begins.");
          if ( theTreePath == null )  // prevent null TreePath.
            theTreePath = new TreePath( 
                NamedLeaf.makeNamedLeaf( "ERROR TreePath" ));
            //////

          theTreeHelper= // Create and store customized TreeHelper. 
              new MyTreeHelper(this, theDataTreeModel.getMetaRoot(), theTreePath);

          setLayout( new BorderLayout() );

          addJLabelV();
          addIOIJTextAreaV(); // Add the input TextArea to window.
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
      
      private void addIOIJTextAreaV()
        {
          ioIJTextArea= new IJTextArea();
          ioIJTextArea.getCaret().setVisible(true); // Make input cursor visible.
          ioIJTextArea.setBorder(raisedEtchedBorder);
          ioIJTextArea.setRows(2);
          ioIJTextArea.setEditable(true);
          ioIJTextArea.setLineWrap(true);
          ioIJTextArea.setWrapStyleWord(true);
          ioIJTextArea.addKeyListener(new KeyAdapter(){
              @Override
              public void keyPressed(KeyEvent theKeyEvent) {
                processKeyPressedV(theKeyEvent);
                }
              });
          add(ioIJTextArea,BorderLayout.CENTER); // Adding it at bottom of JPanel.
          }

      private void processKeyPressedV(KeyEvent theKeyEvent)
        //// Thread safety might be a problem.  
        //// Only insertString(.) is thread safe.
        {
          //// if(theKeyEvent.getKeyCode() == KeyEvent.VK_ENTER){
          theAppLog.debug( "ConsoleViewer.processKeyPressedV(.) called.");
          //// theKeyEvent.consume(); // Prevent further processing.
          ioIJTextArea.append("\nA key was pressed.\n");
          }

      private void putCursorAtEndDocumentV()
        {
          Document d = ioIJTextArea.getDocument();
          ioIJTextArea.select(d.getLength(), d.getLength());
          }

    // rendering methods.  to be added ??

    // TreeAware interface code for TreeHelper access.

      public TreeHelper theTreeHelper;

      public TreeHelper getTreeHelper() { return theTreeHelper; }

      class MyTreeHelper  // TreeHelper customization subclass.

        extends TreeHelper 

        {
          ConsoleViewer theConsoleViewer; 

          MyTreeHelper(  // Constructor.
              ConsoleViewer theConsoleViewer, 
              MetaRoot theMetaRoot,
              TreePath inTreePath
              )
            {
              super(theConsoleViewer, theMetaRoot, inTreePath);

              this.theConsoleViewer= theConsoleViewer; // Save a copy. 
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
              
              putCursorAtEndDocumentV();
              }
         
          } // MyTreeHelper

    }
