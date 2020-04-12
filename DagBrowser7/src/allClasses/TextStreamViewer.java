package allClasses;

// import static allClasses.Globals.appLogger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.tree.TreeModel;
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

        private DataTreeModel theDataTreeModel;
        
        private JLabel titleJLabel;  // Label with the title.

        private IJTextArea streamIJTextArea; // For viewing the stream text.

        private IJTextArea inputIJTextArea; // For entering next text to append.

    // Constructors and constructor-related methods.

      public TextStreamViewer(  // Constructor.
          TreePath theTreePath, 
          DataTreeModel theDataTreeModel, 
          String initialString 
          )
        /* Constructs a TextStreamViewer.
          theTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that Node.
          The contents is theString.
          theTreeModel provides context.
          */
        {
          theAppLog.debug("TextStreamViewer.TextStreamViewer(.) begins.");
          this.theDataTreeModel= theDataTreeModel;
          // streamIJTextArea= new IJTextArea(initialString);
          streamIJTextArea= new IJTextArea("Type text will be moved to here\n");
          inputIJTextArea= new IJTextArea("");
          commonInitializationV( theTreePath, theDataTreeModel );
          }


      private void commonInitializationV( 
          TreePath theTreePath, 
          TreeModel theTreeModel
          )
        /* This grouping method creates and initializes the JTextArea.  
          It was intended for use when there is more than one constructor.
          */
        {
          if ( theTreePath == null )  // prevent null TreePath.
            theTreePath = new TreePath( 
                NamedLeaf.makeNamedLeaf( "ERROR TreePath" )
                );
          aTreeHelper=  // construct helper class instance.
            new TreeHelper(this, theDataTreeModel.getMetaRoot(), theTreePath);

          setLayout( new BorderLayout() );

          doSubcomponentsV(); // Initialize and add the subcomponents.

          { // Add listeners.
            addKeyListener(aTreeHelper);  // Make TreeHelper the KeyListeer.
            // addTreeModelListener( this ) is done elsewhere.
            }
          }

      private void doSubcomponentsV() // Initializes and adds the subcomponents.
        {
          Border raisedEtchedBorder=
              BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
          
          titleJLabel= new JLabel(
            //"TEST-TITLE"
            aTreeHelper.getWholeDataNode().getNameString( )
            );
          titleJLabel.setOpaque( true );
          Font labelFont= titleJLabel.getFont();
          titleJLabel.setFont(labelFont.deriveFont( labelFont.getSize() * 1.5f) );
          //titleJLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
          titleJLabel.setHorizontalAlignment( SwingConstants.CENTER );
          titleJLabel.setBorder(raisedEtchedBorder);
          add(titleJLabel,BorderLayout.NORTH); // Adding it to top of main JPanel.
    
          streamIJTextArea.setBorder(raisedEtchedBorder);
          streamIJTextArea.setLineWrap(true);
          add(streamIJTextArea,BorderLayout.CENTER); // Adding to center.
          
          inputIJTextArea.getCaret().setVisible(true); // Make input cursor visible.
          inputIJTextArea.setBorder(raisedEtchedBorder);
          inputIJTextArea.setRows(2);
          inputIJTextArea.setEditable(true);
          inputIJTextArea.setLineWrap(true);
          inputIJTextArea.setWrapStyleWord(true);
          inputIJTextArea.addKeyListener(new KeyListener(){
            @Override
            public void keyPressed(KeyEvent e){
              if(e.getKeyCode() == KeyEvent.VK_ENTER){
                streamIJTextArea.append(inputIJTextArea.getText());
                streamIJTextArea.append("\n");
                inputIJTextArea.setText("");
                //// inputIJTextArea.setCaretPosition(0);
                e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });          add(inputIJTextArea,BorderLayout.SOUTH); // Adding it at bottom.
          }
      
    // rendering methods.  to be added ??

    // TreeAware interface code for TreeHelper access.

      public TreeHelper aTreeHelper;

      public TreeHelper getTreeHelper() { return aTreeHelper; }

    }
