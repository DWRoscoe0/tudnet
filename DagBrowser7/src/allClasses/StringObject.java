package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class StringObject // ?? change to StringLeaf. 
  //extends Object
  //implements DataNode
  extends AbDataNode
  /* This is a utility class that appears to be a leaf with a name.  */
  { // class StringObject
  
    // private static final long serialVersionUID = 1L;
    private String TheString;  // The name associated with this node.
    
    // Constructors.

      StringObject ( String StringIn ) 
        { 
          super( ); 
          TheString = StringIn;  // Store this node's name.
          }
    
    // A subset of delegated AbstractTreeModel methods.

        public boolean isLeaf( ) 
          {
            return true;  // It is a leaf.
            }

        public int getChildCount( ) 
          {
            return 0;  // It has 0 children.
            }
      
        public DataNode getChild( int IndexI ) 
          {
            return null;  // Always return nothing.
            }

        public int getIndexOfChild( Object ChildObject ) 
          {
            return -1;  // Never find the child.  ???
            }
        
        public DataNode[] GetDataNodes( )
          {
            return null;
            }

        public String GetInfoString()
          /* Returns a String representing information about this object. */
          { 
            return GetNameString();  // Delegate.
            }

        public String GetNameString( )
          /* Returns String representing name of this Object.  */
          {
            return TheString;  // Simply return the name.
            }
        
        /* public JComponent GetDataJComponent( TreePath InTreePath )
          /* Returns a JComponent which is appropriate for viewing 
            the current tree node represented specified by InTreePath.  
            This object is the last DataNode in the InTreePath.
            */
          /*
          { // GetDataTreeSelector.
            System.out.println( "StringObject.GetDataJComponent(InTreePath)" );
            return new TextViewer( InTreePath, GetNameString( ) );
            }
          */
        
        public JComponent GetDataJComponent
          ( TreePath InTreePath, TreeModel InTreeModel )
          /* Returns a JComponent which is appropriate for viewing 
            the current tree node represented specified by InTreePath.  
            This object is the last DataNode in the InTreePath.
            InTreeModel provides context.
            */
          { // GetDataTreeSelector.
            return new TextViewer( InTreePath, InTreeModel, GetNameString( ) );
            }

    } // class StringObject
