package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class InfogoraRoot
  //extends Object
  extends AbDagNode
  //implements DagNode
  { // class InfogoraRoot

    // Variables.
      FileRoots TheFileRoots= new FileRoots();  // the only child.
      Outline TheOutline= new Outline( 0 );  // base outline.
      
    // Constructors (none yet).
        
    
    // A subset of delegated AbstractTreeModel methods.

/*      public DagNode[] xGetDagNodes( )
          /* Returns a reference to an an array of IFiles
            which represent all the filesystem roots.
            */
/*        {
            return null;  // eliminate this?
            }
*/  
/*      public boolean isLeaf( ) 
          /* Returns true, because the tree is never empty.  */
/*        {
            return false;  
            }

        public int getChildCount( ) 
          /* Returns the number of filesystem roots.  */
/*        {
            return 2;
            }
*/    
        public DagNode getChild( int IndexI ) 
          /* This returns the child with index IndexI.  */
          {
            switch ( IndexI ) {
              case 0: return TheFileRoots;
              case 1: return TheOutline;
              case 2: return new Infinitree( null, 0 );
              }
            return null;  // anything else returns null, to prevent compiler warning.r
            }

/*      public int getIndexOfChild( Object ChildObject ) 
          /* Returns the index of the filesystem root named by ChildObject 
            in the list of filesystem roots, or -1 if it is not found.
            It does they by comparing Object-s as File-s.
            */
/*        {
            return -1;  // fail.  assume this is not needed.
            }
*/

        public String GetInfoString()
          /* Returns a String representing information about this object. */
          { 
            return GetNameString( );
            }

        public String GetNameString( )
          /* Returns String representing name of this Object.  */
          {
            return "INFOGORA-ROOT";
            }
        
        public JComponent GetDataJComponent
          ( TreePath InTreePath, TreeModel InTreeModel)
          /* Returns a JComponent which is appropriate for viewing 
            the current tree node represented specified by InTreePath.  
            The list element of the InTreePath is assumed to be
            the InfogoraRoot object.
            */
          { // GetDataTreeSelector.
        	  JComponent ResultJComponent=  // Calculate a ListViewer.
              new ListViewer( InTreePath, InTreeModel );
            return ResultJComponent;  // return the final result.
            } // GetDataTreeSelector.

        /* public JComponent GetDataJComponent( TreePath InTreePath )
          /* Returns a JComponent which is appropriate for viewing 
            the current tree node represented specified by InTreePath.  
            The list element of the InTreePath is assumed to be
            the InfogoraRoot object.
            */
          /*
          { // GetDataJComponent.
            System.out.println( "InfogoraRoot.GetDataJComponent(InTreePath)" );
        	  JComponent ResultJComponent=  // Calculate a ListViewer.
              new ListViewer( InTreePath );
            return ResultJComponent;  // return the final result.
            } // GetDataJComponent.
          */

    } // class InfogoraRoot
