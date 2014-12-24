package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public class Infinitree 
  extends AbDataNode
  /* The purpose of this class is to create a an infinite subtree
    for testing the Infogora browser.
    */
  {
  
    // variables.
       int childNumberI;  // The number of the parent's 0 - (n-1) children.
       Infinitree parentInfinitree= null;  // Parent node, or null if none.
       
    // Constructors.

      public Infinitree( Infinitree inparentInfinitree, int inChildI )
        /* Constructs an Infinitree node.
          inparentInfinitree will be the parent of the new node.
          inChildI is the child number of the node.
          */
        { // Infinitree(.)
          childNumberI= inChildI;  // save child index.
          parentInfinitree= inparentInfinitree;  // save parent.
          } // Infinitree(.)
       
    // Other methods.
  
      public int getChildCount( ) 
        /* This method returns the child cound.
          Because the children are virtual, calculated as needed,
          the child count is calculated from 
          the number of ancestor levels below the root.
          */
        { // getChildCount( )
          Infinitree ScanInfinitree= parentInfinitree;
          int ChildIndexI= 2;  // Initialize child index.
          while  // Increase that by the number of ancestors levels.
            ( ScanInfinitree != null )
            {
              ChildIndexI++;  // increment index.
              ScanInfinitree= ScanInfinitree.parentInfinitree;
              }
          return ChildIndexI;  // Return ending index as count.
          } // getChildCount( )

      public DataNode getChild( int inIndexI ) 
        /* This returns this node's child whose index is inIndexI.  
          The node's children are not cached within the node.
          They are constructe as needed,
          so repeated calls to this method will return 
          different but equal(..) instances.
          */
        { // getChild( int ) 
          Infinitree childInfinitree= null;  // assume inIndexI out of range.
          if  // override result if inIndexI is in range. 
            ( inIndexI >= 0 && inIndexI < getChildCount( ))  // in range?
            childInfinitree= new Infinitree( // yes, calculate child node...
              this,  // ...using this node as parent and...
              inIndexI  // ...its inIndexI as child index.
              );
          return childInfinitree;  // return result whatever.
          } // getChild( int ) 

      public String toString( ) 
        /* This method returns the String representation of
          this tree node.
          */
        {
          String resultString= null;

          if ( parentInfinitree == null ) // This node is the root.
            resultString= "Infinite-Test-Tree";  // Use root's name.
            else  // This node is not root.
            resultString=  // Use parent's name with our chid number appended.
              parentInfinitree.toString() +
              "."+ childNumberI;

          return resultString;  // Return ending String.
          }

      @Override public boolean equals(Object other) 
        /* This is the standard equals() method.  
          This is not a complete implimentation of equals(..).
          It doesn't do null checks.
          */
        {
          boolean resultB = false;  // assume objects are not equal.
          Comparer: {  // Comparer.
            if ( ! ( other instanceof Infinitree ) )  // different class.
              break Comparer;  // exit with false.
            Infinitree that =  // create variable for easy access of fields.
              (Infinitree) other; 
            if ( this.childNumberI != that.childNumberI )  // child numbers not equal.
              break Comparer;  // exit with false.
            if // parents not equal.
              ( ! ( ( parentInfinitree == that.parentInfinitree ) ||
                    ( ( parentInfinitree != null ) &&
                      ( parentInfinitree.equals( that.parentInfinitree ) )
                      )
                    )
                )
              break Comparer;  // exit with false.
            resultB= true;  // everything is equal, so override result.
            }  // Comparer.
          return resultB;
          }

      @Override public int hashCode() 
        /* This method return a hash value for this node.
          It takes into account this node's child number and
          the hash codes of all it ancestors.
          */
        {
          int hash = (2047 * 17) + childNumberI;
          if ( parentInfinitree != null)
            hash = hash * 31 + parentInfinitree.hashCode();
          return hash;
          }

      public JComponent GetDataJComponent( 
          TreePath inTreePath, 
          MetaRoot theMetaRoot,
          DataTreeModel InDataTreeModel
          )
        { // GetDataJComponent()
          JComponent resultJComponent= null;  // For result.

          resultJComponent= // Set result for exploring a List.
            new TitledListViewer( inTreePath, InDataTreeModel );

          return resultJComponent;  // Return the result from above.
          } // GetDataJComponent()
    }
