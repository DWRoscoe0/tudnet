package allClasses;


public class Infinitree 

  extends NamedList

  /* The purpose of this class is to create a an infinite subtree
    for testing the Infogora browser.
    Though this class extends the class NamedList,
    it doesn't actually store references to its children.
    The getChild( int inIndexI ) method 
    constructs a new class instance each time it is called.
    This avoids the complication of managing a lazy-evaluation cache.
    This technique might be called "super-lazy-evaluation".
    */

  {
  
    // variables.

      int childNumberI;  // The number of the parent's 0 - (n-1) children.
      Infinitree parentInfinitree= null; // Parent Infinitree node, or null.
        // This is a copy of the parentNamedList, except a different type.
       
    // Constructors.

      public Infinitree( Infinitree inParentInfinitree, int inChildI )
        /* Constructs an Infinitree node.
          inParentInfinitree will be the parent of the new node.
          inChildI is the number of the new node within the parent's children.
          */
        { // Infinitree(.)
          childNumberI= inChildI;  // save child index.
          parentInfinitree= inParentInfinitree; // Save parent Infinitree.
          this.setParentToV(inParentInfinitree); // Set DataNode parent.
          } // Infinitree(.)
       
    // Other methods.
  
      public int getChildCount( ) 
        /* This method returns the child cound.
          Because the children are virtual, calculated as needed,
          the child count is calculated from the number of 
          ancestor levels between here and the tree root.
          */
        { // getChildCount( )
          Infinitree scanInfinitree= parentInfinitree;
          int childIndexI= 2;  // Initialize child index.
          while  // Increase that by the number of ancestor levels.
            ( scanInfinitree != null )
            {
              childIndexI++;  // increment index.
              scanInfinitree= scanInfinitree.parentInfinitree;
              }
          return childIndexI;  // Return ending index as count.
          } // getChildCount( )

      public DataNode getChild( int inIndexI ) 
        /* This returns this node's child whose index is inIndexI.  
          The node's children are not cached within the node.
          They are constructed when needed,
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

      public String getNameString( )
        /* This method returns the String representation of
          the name of this tree node.
          It is constructed recursively.
          The result is a base String representing the root concatenated with
          all the child numbers between the tree root and this node.
          */
        {
          String resultString= null;

          if ( parentInfinitree == null ) // This node is the root.
            resultString= "Infinite-Test-Tree";  // Result is root's name.
          
            else  // This node is not root.
            resultString=  // Recursively calculate result to be
              parentInfinitree.toString() // the parent's name 
              + "." + childNumberI; // with our child number appended.

          return resultString;
          }

      @Override public boolean equals(Object other) 
        // This is the standard equals() method.  
        {
          boolean resultB = false;  // assume objects are not equal.
          Comparer: {  // Comparer.
            if ( other == null )  // Other is null.
              break Comparer;  // exit with false.
            if ( ! ( other instanceof Infinitree ) )  // different class.
              break Comparer;  // exit with false.
            Infinitree that =  // create variable for easy access of fields.
              (Infinitree) other; 
            if   // child numbers not equal.
              ( this.childNumberI != that.childNumberI )
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
      
    }
