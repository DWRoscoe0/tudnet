package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

//import javax.swing.JComponent;
//import javax.swing.tree.TreeModel;
//import javax.swing.tree.TreePath;

public abstract class AbDagNode 

  implements DagNode
  
  /* This is an abstract class that implements part of the DagNode interface.
    The default implementations here are 
    ones that are likely to be useful for most subclasses.
    Subclasses can override what they don't like.
    */
    
  {
    // DagNode interface methods.
  
      public boolean isLeaf( ) 
        /* Returns false, because most nodes are not leaves.  */
        { return false; }

      public int getChildCount( ) 
        /* This method actually scans all the children that are
          visible to getChild(.) and counts them.  
          It assumes a functional getChild( IndexI ) method which
          return null if IndexI is out of range.  */
        { // getChildCount( )
          int ChildIndexI= 0;  // Initialize child index.
          while  // Process all children...
            ( getChild( ChildIndexI ) != null )  // ...returned by getChild(.).
            { // process this child.
              ChildIndexI++;  // increment index.
              } // process this child.
          return ChildIndexI;  // Return ending index as count.
          } // getChildCount( )

      public int getIndexOfChild( Object InChildObject ) 
        /* Returns the index of the child ChildObject,
          or -1 if it is not one of this node's children. 
          It assumes a functional getChild(.) method.  */
        { // getIndexOfChild(.)
          int ChildIndexI= 0;  // Initialize child search index.
          while ( true ) // Search for child.
            { // Check one child.
              Object ChildObject= getChild( ChildIndexI );  // get the child.
              if ( ChildObject == null )  // null means no more children.
                { // Exit with failure.
                  ChildIndexI= -1;  // Set index to indicate failure.
                  break;  // Exit while loop.
                  } // Exit with failure.
              if ( InChildObject.equals( ChildObject ) )
                break;  // Found child.  Exit while loop.
              ChildIndexI++;  // increment index for next child.
              } // Check one child.
          return ChildIndexI;  // Return ending index as search result.
          } // getIndexOfChild(.)

      public String GetInfoString()
        /* Returns a String representing information about this object. */
        { 
          return GetNameString( );
          }

      public String GetHeadString()
        /* Returns a String representing this node excluding any children. */
        { 
          return GetNameString( );
          }

      public String GetNameString( )
        /* Returns String representing name of this Object.  */
        {
          return toString();  // Return default String representation.
          }
    
      public JComponent GetDataJComponent
        ( TreePath InTreePath, TreeModel InTreeModel )
        /* Returns a JComponent which is appropriate for viewing 
          the current tree node represented specified by InTreePath,
          using context from InTreeModel.
          */
        { // GetDataJComponent()
          JComponent ResultJComponent= null;  // For result.

          if ( isLeaf( ) )
            {
              ResultJComponent= 
                //new TextViewer( InTreePath, InTreeModel, TextString );
                new TextViewer( InTreePath, InTreeModel, GetHeadString() );
              }
            else
            ResultJComponent= // Calculate a ListViewer.
              new ListViewer( InTreePath, InTreeModel );
          return ResultJComponent;  // return the final result.
          } // GetDataJComponent()

    // other methods.
    
      public int IDCode() { return super.hashCode(); }

    }
