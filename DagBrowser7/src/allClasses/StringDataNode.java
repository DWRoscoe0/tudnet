package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class StringDataNode  extends AbDataNode
  
  /* This is a utility class that appears to be a leaf with a name.  
   // ?? change to StringLeaf. 
   */
  
  { // class StringDataNode
  
    // private static final long serialVersionUID = 1L;

    private String theString;  // The name associated with this node.
    
    // Constructors.

      StringDataNode ( String inString ) 
        { 
          super( ); 
          theString = inString;  // Store this node's name.
          }
    
    // A subset of delegated AbstractTreeModel methods.

        public boolean isLeaf( ) 
          {
            return true;  // It is a leaf.
            }

        public String GetNameString( )
          /* Returns String representing name of this Object.  */
          {
            return theString;  // Simply return the name.
            }
        
        public JComponent GetDataJComponent
          ( TreePath InTreePath, TreeModel InTreeModel )
          /* Returns a JComponent which is appropriate for viewing 
            the current tree node represented specified by InTreePath.  
            This object is the last DataNode in the InTreePath.
            InTreeModel provides context.
            */
          {
            return new TextViewer( 
              InTreePath, InTreeModel, GetNameString( ) 
              );
            }

    } // class StringDataNode
