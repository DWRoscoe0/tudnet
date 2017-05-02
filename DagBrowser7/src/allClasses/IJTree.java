package allClasses;

import java.awt.Color;
import java.awt.Component;
//% import java.awt.event.FocusEvent;
//% import java.awt.event.FocusListener;

//% import javax.swing.JLabel;
//% import javax.swing.JList;
import javax.swing.JTree;
//% import javax.swing.TreeCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

//% import allClasses.TitledListViewer.TitledTreeCellRendererOfDataNodes;


public class IJTree extends JTree
  //% implements FocusListener
  
  // This class was created mainly to change the rendering of  tree cells.
   
  {       
    //% private static final long serialVersionUID = 1L;
    //% private DefaultTreeCellRenderer theDefaultTreeCellRenderer;
    //% private Color defaultBackgroundSelectionColor;

		private TreeCellRenderer theTitledTreeCellRenderer=
	  		new IJTreeCellRendererOfDataNodes(); // for custom cell rendering.
    private Color cachedBackgroundColor; // For faster color rendering.

    public IJTree( DataTreeModel inDataTreeModel ) // Constructor.
      { 
        super(inDataTreeModel); 

        { // customize the tree cell rendering.
	        cachedBackgroundColor= getBackground();
          //% theDefaultTreeCellRenderer=
		      //%   new DefaultTreeCellRenderer() 
		      //%   {
		      //%     private static final long serialVersionUID = 1L;
		      //%     };
          //% defaultBackgroundSelectionColor=   // save present color.  ////// ??
          //% 	  theDefaultTreeCellRenderer.getBackgroundSelectionColor();
          // theDefaultTreeCellRenderer.setBackgroundSelectionColor(Color.RED);
          //% setCellRenderer(theDefaultTreeCellRenderer);
          setCellRenderer( theTitledTreeCellRenderer );
          
          } // customize the tree cell rendering.

        //% this.addFocusListener(this);  // make this be FocusListener.
        }
          
    // FocusListener methods, for any component focus changes, and related methods.

      /*  //%
      public void focusGained(FocusEvent TheFocusEvent)
        { 
          theDefaultTreeCellRenderer.setBackgroundSelectionColor(
          		UIColor.activeColor
          		);
          repaint();
          }

      public void focusLost(FocusEvent TheFocusEvent)
        { 
          theDefaultTreeCellRenderer.setBackgroundSelectionColor(
            defaultBackgroundSelectionColor
            );
          repaint();
          }
      */  //%

    /*  //%
    public String XconvertValueToText  /////////////////
      ( Object value, boolean selected,
        boolean expanded, boolean leaf, int row,
        boolean hasFocus
        ) 
      /* Causes the text of a JTree node to be displayed as 
        only the final name in the associated path instead of the entire path. 
        */
    /*  //%
      {
    		return ((DataNode)value).getLineSummaryString();
        }
    */  //%

    public class IJTreeCellRendererOfDataNodes
      extends DefaultTreeCellRenderer
      implements TreeCellRenderer
      {
    	  public IJTreeCellRendererOfDataNodes() // Constructor.
      	  {
        		setOpaque(true);  // To turn off normal background transparency.
      	  	}
    	  
        public Component getTreeCellRendererComponent
          ( JTree theJTreeOfDataNodes,
            Object theDataNodeObject,
            boolean isSelectedB,
            boolean expandedB,
            boolean isLeafB,
            int rowI,
            boolean hasFocusB
            )
          /* Returns a Component for displaying a DataNode in a JTree. */
          {
        		Component resultTreeCellRendererComponent= // Get renderer...
        				super.getTreeCellRendererComponent(  // from superclass .
	              	theJTreeOfDataNodes,
	                theDataNodeObject,
	                isSelectedB,
	                expandedB,
	                isLeafB,
	                rowI,
	                hasFocusB
	                );
            UIColor.setColorsV( // Override its colors in renderer.
            	resultTreeCellRendererComponent,
              cachedBackgroundColor,
              (DataNode)theDataNodeObject,
              isSelectedB,
              hasFocusB
              );
            return resultTreeCellRendererComponent;
            }

      } // class IJTreeCellRendererOfDataNodes    

  }
