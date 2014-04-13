package allClasses;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TextViewer 

  extends IJTextArea
 
  implements 
    FocusListener, 
    VHelper
  
  /* This class was created quickly from ListViewer 
    to provide a simple DagNodeViewer that 
    displays and browses Text using a JTextArea.
    It's got a lot of unused and useless code,
    but it does display text strings and text files.
    */
    
  { // TextViewer
  
    // variables.
    
      // static variables.
    
        private static final long serialVersionUID = 1L;

      // instance variables.

        private Color DefaultBackgroundColor;  // For focusLost(..).
        public ViewHelper aViewHelper;  // Mutual composition Helper class.

    // constructor and related methods.

      public TextViewer
        ( TreePath TreePathIn, TreeModel InTreeModel, String InString )
        /* Constructs a TextViewer.
          TreePathIn is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that Node.
          The contents is InString.
          InTreeModel provides context.
          */
        { // TextViewer(.)
          super(   // do the inherited constructor code.
            InString  // String to view.
            );  // do the inherited constructor code.
          CommonInitialization( TreePathIn );
          } // TextViewer(.)

      public TextViewer
        ( TreePath TreePathIn, TreeModel InTreeModel, IFile InIFile )
        /* Constructs a TextViewer.
          TreePathIn is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that object.
          The contents is InIFile.
          InTreeModel provides context.
          */
        { // TextViewer(.)
          super(   // do the inherited constructor code.
            InIFile.GetFile()   // File to view.
            );  // do the inherited constructor code.
          CommonInitialization( TreePathIn );
          } // TextViewer(.)

      private void CommonInitialization( TreePath TreePathIn )
        /* This grouping method creates and initializes the JTextArea.  */
        { // CommonInitialization( )
          if ( TreePathIn == null )  // prevent null TreePath.
            TreePathIn = new TreePath( new StringObject( "ERROR TreePath" ));
          aViewHelper=  // construct helper class instance.
            new ViewHelper( this, TreePathIn ); 
          { // Add listeners.
            addKeyListener(aViewHelper);  // Make ViewHelper the KeyListeer.
            addFocusListener(this);  // listen to repaint on focus events.
            DefaultBackgroundColor=   // save present background color.
                getBackground();
            } // Add listeners.
          getCaret().setVisible(true);  // Make text cursor visible.
          } // CommonInitialization( )

    // input (setter) methods.  this includes Listeners.
        
      /* ListSelectionListener method, deleted. */
  
      // KeyListener methods,  (moved to ViewHelper).
      
      // FocusListener methods, to fix JTable cell-invalidate/repaint bug.

        @Override
        public void focusGained(FocusEvent arg0) 
          {
            // System.out.println( "TextViewer.focusGained()" );
            setBackground( Color.GREEN );
            }
      
        @Override
        public void focusLost(FocusEvent arg0) 
          {
            // System.out.println( "TextViewer.focusLost()" );
            setBackground( DefaultBackgroundColor );
            }
      
    // command methods (moved to ViewHelper).
      
    // state updating methods (deleted).

    // ViewHelper pass-through methods.

      public TreePath getWholeTreePath()
        { 
          return aViewHelper.getWholeTreePath();
          }

      public TreePath getPartTreePath()
        { 
          return aViewHelper.getPartTreePath();
          }

      public void addTreeSelectionListener( TreeSelectionListener listener ) 
        {
          aViewHelper.addTreeSelectionListener( listener );
          }

    // rendering methods.  to be added.
      
    } // TextViewer
