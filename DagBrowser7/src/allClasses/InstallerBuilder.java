package allClasses;

// import static allClasses.Globals.appLogger;

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

public class InstallerBuilder

  extends JPanel
 
  implements 
    TreeAware
    // TreeModelListener
  
  /* This class is based on TextStream2Viewer.
   * It is a JComponent for using a TextArea for 
   * interacting with the user to build an installer volume,
   * most likely a USB thumb drive,
   * which contains all the files for installing this app.
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

      public InstallerBuilder(  // Constructor.
          TreePath theTreePath, 
          DataTreeModel theDataTreeModel,
          Persistent thePersistent //// leave for now.
          )
        /* Constructs an InstallerBuilder viewer.
          theTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that Node.
          theTreeModel provides context.
          */
        {
          super();   // Constructing the superclass JPanel.
          
          //// this.thePersistent= thePersistent;

          theAppLog.debug("InstallerBuilder.InstallerBuilder(.) begins.");
          if ( theTreePath == null )  // prevent null TreePath.
            theTreePath = new TreePath( 
                NamedLeaf.makeNamedLeaf( "ERROR TreePath" ));
            //////

          theTreeHelper= // Create and store customized TreeHelper. 
              new MyTreeHelper(this, theDataTreeModel.getMetaRoot(), theTreePath);

          setLayout( new BorderLayout() );

          addJLabelV();
          addIOIJTextAreaV(); // Add the input TextArea to window.
          cycleStateMachineV();
          }

      public enum State
        {
          INITIAL_GREETING,
          AWAIT_DEVICE_INSERTION
          }

      State theState= State.INITIAL_GREETING;
      State nextState= null;
      
      private void cycleStateMachineV()
        {
          while (true) {
            switch (theState) {
              case INITIAL_GREETING: initialGreetingV(); break;
              case AWAIT_DEVICE_INSERTION: awaitDeviceInsertionV(); break;
              }
            if (null == nextState) break;  // Exit loop and return.
            theState= nextState;
            nextState= null;
            }
        }
      
      private void initialGreetingV()
        {
          ioIJTextArea.append(
              "\nTo begin building a volume with installation files,"+
              "\nplease insert the device to use into a USB port."+
              "\nIf you have already inserted one then please "+
              "\nremove it and insert it again.");
          nextState= State.AWAIT_DEVICE_INSERTION;
          }

      private void awaitDeviceInsertionV()
        {
          ioIJTextArea.append(
              "\n\nThis is where we wait.");
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
          theAppLog.debug( "InstallerBuilder.processKeyPressedV(.) called.");
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
          InstallerBuilder theInstallerBuilder; 

          MyTreeHelper(  // Constructor.
              InstallerBuilder theInstallerBuilder, 
              MetaRoot theMetaRoot,
              TreePath inTreePath
              )
            {
              super(theInstallerBuilder, theMetaRoot, inTreePath);

              this.theInstallerBuilder= theInstallerBuilder; // Save a copy. 
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
