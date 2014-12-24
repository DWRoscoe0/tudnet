package allClasses;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JPanel;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import static allClasses.Globals.*;  // appLogger;

public class TitledTextViewer

  ///extends IJTextArea
  extends JPanel
 
  implements 
    TreeAware
  
  /* This class was created from TextViewer,
    which was created quickly from ListViewer,
    to provide a simple DagNodeViewer that 
    displays and browses Text using a JTextArea.
    For a while it contained a lot of unused and useless code,
    but it has been trimmed down.
    */
    
  {
    // variables.
    
      // static variables.
    
        private static final long serialVersionUID = 1L;

      // instance variables.

        private DataTreeModel theDataTreeModel;
        
        private JLabel titleJLabel;  // Label with the title.

        private IJTextArea theIJTextArea;  // Component for the text.

    // Constructors and related methods.

      public TitledTextViewer(  // Constructor.
          TreePath TreePathIn, 
          DataTreeModel theDataTreeModel, 
          String InString 
          )
        /* Constructs a TitledTextViewer.
          TreePathIn is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that Node.
          The contents is InString.
          InTreeModel provides context.
          */
        { // TitledTextViewer(.)
          this.theDataTreeModel= theDataTreeModel;

          theIJTextArea= new IJTextArea(   // Construct JTextArea.
            InString  // String to view.
            );
          CommonInitialization( TreePathIn );
          } // TitledTextViewer(.)

      public TitledTextViewer( 
          TreePath TreePathIn, TreeModel InTreeModel, IFile InIFile 
          )
        /* Constructs a TitledTextViewer.
          TreePathIn is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that object.
          The contents is InIFile.
          InTreeModel provides context.
          */
        { // TitledTextViewer(.)
          theIJTextArea= new IJTextArea(    // Construct JTextArea.
            InIFile.GetFile()   // File to view.
            );
          CommonInitialization( TreePathIn );
          } // TitledTextViewer(.)

      private void CommonInitialization( TreePath TreePathIn )
        /* This grouping method creates and initializes the JTextArea.  */
        { // CommonInitialization( )
          if ( TreePathIn == null )  // prevent null TreePath.
            TreePathIn = new TreePath( new NamedLeaf( "ERROR TreePath" ));

          aTreeHelper=  // construct helper class instance.
            new TreeHelper( 
              this, theDataTreeModel.getMetaRoot(), TreePathIn 
              );

          setLayout( new BorderLayout() );
          //setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

          titleJLabel= new JLabel(
            //"TEST-TITLE"
            aTreeHelper.getWholeDataNode().GetNameString( )
            );
          //titleJLabel.setBackground( Color.RED );
          titleJLabel.setOpaque( true );
          Font labelFont= titleJLabel.getFont();
          titleJLabel.setFont( labelFont.deriveFont( labelFont.getSize() * 1.5f) );
          //titleJLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
          titleJLabel.setHorizontalAlignment( SwingConstants.CENTER );
          Border raisedetched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
          titleJLabel.setBorder(raisedetched);
          add(titleJLabel,BorderLayout.NORTH); // Adding it to main JPanel.

          theIJTextArea.getCaret().setVisible(true);  // Make cursor visible.
          add(theIJTextArea,BorderLayout.CENTER); // Adding it to main JPanel.

          { // Add listeners.
            addKeyListener(aTreeHelper);  // Make TreeHelper the KeyListeer.
            } // Add listeners.
          } // CommonInitialization( )

    // input (setter) methods.  this includes Listeners.
    
      // ??? temporary.
        public void partTreeChangedV( 
            TreePathEvent inTreePathEvent )
          {
            appLogger.error( "TitledListViewer.partTreeChangedV(..) called");
            }
        
      /* ListSelectionListener method, deleted. */
  
      // KeyListener methods,  (moved to TreeHelper).

    // command methods (moved to TreeHelper).
      
    // state updating methods (deleted).

    // interface TreeAware code for TreeHelper access.

			public TreeHelper aTreeHelper;  // helper class ???

			public TreeHelper getTreeHelper() { return aTreeHelper; }

    // rendering methods.  to be added.
      
    }
