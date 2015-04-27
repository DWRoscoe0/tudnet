package allClasses;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;


public class IJTree 
  extends JTree
  implements FocusListener
  
  /* This class was created mainly to render tree cells
   * differently when the JTree has focus.
   * This is to make the holder of focus more identifiable. 
   */
   
  {       
    private static final long serialVersionUID = 1L;
    private DefaultTreeCellRenderer theDefaultTreeCellRenderer;
    private Color defaultBackgroundSelectionColor;

    public IJTree( DataTreeModel inDataTreeModel )
      // Constructor.
      { 
        super(inDataTreeModel); 

        { // customize the tree cell rendering.
          theDefaultTreeCellRenderer=
            new DefaultTreeCellRenderer() 
              {
                private static final long serialVersionUID = 1L;
                };
          defaultBackgroundSelectionColor=   // save present color.
        		  theDefaultTreeCellRenderer.getBackgroundSelectionColor();
          // theDefaultTreeCellRenderer.setBackgroundSelectionColor(Color.RED);
          setCellRenderer(theDefaultTreeCellRenderer);
          } // customize the tree cell rendering.

        this.addFocusListener(this);  // make this be FocusListener.
        }
          
    // FocusListener methods, for any component focus changes, and related methods.

      public void focusGained(FocusEvent TheFocusEvent)
        { 
          theDefaultTreeCellRenderer.setBackgroundSelectionColor(Color.GREEN);
          repaint();
          }

      public void focusLost(FocusEvent TheFocusEvent)
        { 
          theDefaultTreeCellRenderer.setBackgroundSelectionColor(
            defaultBackgroundSelectionColor
            );
          repaint();
          }

          
    public String convertValueToText
      ( Object value, boolean selected,
        boolean expanded, boolean leaf, int row,
        boolean hasFocus
        ) 
      /* Causes the text of a JTree node to be displayed as 
        only the final name in the associated path instead of the entire path. 
        */
      {
        //return ((DataNode)value).getNameString() + "-???";
    		return ((DataNode)value).getLineSummaryString();
        }
        
        
    }
