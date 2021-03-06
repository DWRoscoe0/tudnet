package allClasses;

// import static allClasses.Globals.appLogger;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

//import static allClasses.Globals.*;  // appLogger;

public class TitledTextViewer

  extends JPanel
 
  implements 
    TreeAware, 
    TreeModelListener
  
  /* This class provides a simple DagNodeViewer that
   * displays and browses Text using a JTextArea.
   * It was created from TextViewer, which was created quickly from ListViewer.
   * For a while it contained a lot of unused and useless code,
   * but it has been trimmed down.
   */
    
  {
    // variables.
    
      // static variables.
    
        private static final long serialVersionUID = 1L;

      // instance variables.

        private DataTreeModel theDataTreeModel;
        
        private JLabel titleJLabel;  // Label with the title.

        private IJTextArea theIJTextArea;  // Component for the text.

    // Constructors and constructor-related methods.

      public TitledTextViewer(  // Constructor for viewing String text.
          TreePath theTreePath, 
          DataTreeModel theDataTreeModel, 
          String theString 
          )
        /* Constructs a TitledTextViewer.
          theTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that Node.
          The contents is theString.
          theTreeModel provides context.
          */
        { // TitledTextViewer(.)
          this.theDataTreeModel= theDataTreeModel;

          theIJTextArea= new IJTextArea(   // Construct JTextArea.
            /// "--------------DEBUG--------------  \n"+
            theString  // String to view.
            );
          CommonInitialization( theTreePath, theDataTreeModel );
          } // TitledTextViewer(.)


      private void CommonInitialization( 
          TreePath theTreePath, 
          TreeModel theTreeModel
          )
        /* This grouping method creates and initializes the JTextArea.  */
        { // CommonInitialization( )
          if ( theTreePath == null )  // prevent null TreePath.
            theTreePath = new TreePath( 
                NamedLeaf.makeNamedLeaf( "ERROR TreePath" )
                );

          theTreeHelper=  // construct helper class instance.
            new MyTreeHelper( 
              this, theDataTreeModel.getMetaRoot(), theTreePath 
              );

          setLayout( new BorderLayout() );
          //setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

          titleJLabel= new JLabel(
            //"TEST-TITLE"
            theTreeHelper.getWholeDataNode().getNameString( )
            );
          //titleJLabel.setBackground( Color.RED );
          titleJLabel.setOpaque( true );
          Font labelFont= titleJLabel.getFont();
          titleJLabel.setFont( 
              labelFont.deriveFont( labelFont.getSize() * 1.5f) );
          //titleJLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
          titleJLabel.setHorizontalAlignment( SwingConstants.CENTER );
          Border raisedetched= 
              BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
          titleJLabel.setBorder(raisedetched);
          add(titleJLabel,BorderLayout.NORTH); // Adding it to main JPanel.

          theIJTextArea.getCaret().setVisible(true); // Make viewer cursor visible.
          theIJTextArea.setLineWrap(true); // Make all visible.
          theIJTextArea.setWrapStyleWord(true); // Make it pretty.
          JScrollPane theJScrollPane= // Place the JTextArea in a scroll pane.
              new JScrollPane(theIJTextArea);
          add(theJScrollPane,BorderLayout.CENTER); // Add it to main JPanel.
          } // CommonInitialization( )

    // rendering methods.  to be added ??

    /* TreeModelListener interface methods.
      The only method which is handled is treeNodesChanged(..),
      which is used to indicate that the JTextArea needs to be updated,
      and only if the tree changes is associated with the displayed DataNode.
       */

      public void treeNodesInserted(TreeModelEvent theTreeModelEvent)
        { } // Ignoring.

      public void treeNodesRemoved(TreeModelEvent theTreeModelEvent)
        { } // Ignoring.

      public void treeNodesChanged(TreeModelEvent theTreeModelEvent) 
        /* Translates theTreeModelEvent reporting a DataNode change into 
          replacing the JTextArea text.
          */
        {
          //appLogger.debug("TitledTextViewer.treeNodesChanged(..)");
          if ( // Ignoring event if event parent path isn't our parent path. 
                !theTreeHelper.getWholeTreePath().getParentPath().equals(
                    theTreeModelEvent.getTreePath()
                    )
                )
            ; // Ignoring.
          else // Updating TextArea if it shows any (the) event child DataNode.
            for 
              ( Object childObject: theTreeModelEvent.getChildren() )
              { // Updating TextArea if it shows this event child DataNode.
                if ( childObject == theTreeHelper.getWholeDataNode() )
                  theIJTextArea.replaceRange(
                      ((DataNode)childObject).getContentString(),
                      0,
                      theIJTextArea.getText().length()
                      );
                }
          }

      public void treeStructureChanged(TreeModelEvent theTreeModelEvent)
        { } // Ignoring.
    
      // End of TreeModelListener interface methods.

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

        } // MyTreeHelper

    }
