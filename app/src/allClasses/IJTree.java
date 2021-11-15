package allClasses;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;


public class IJTree extends JTree
  
  // This class was created mainly to change the rendering of  tree cells.
   
  {       

		private TreeCellRenderer theTitledTreeCellRenderer=
	  		new IJTreeCellRendererOfDataNodes(); // for custom cell rendering.
    private Color cachedBackgroundColor; // For faster color rendering.

    public IJTree( DataTreeModel inDataTreeModel ) // Constructor.
      { 
        super(inDataTreeModel); 

        { // customize the tree cell rendering.
	        cachedBackgroundColor= getBackground();
          setCellRenderer( theTitledTreeCellRenderer );
          } // customize the tree cell rendering.
        }

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
