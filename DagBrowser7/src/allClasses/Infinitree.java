package allClasses;

public class Infinitree 
  extends AbDataNode
  /* The purpose of this class is to create a an infinite subtree
    for testing the Infogora browser.
    */
  {
  
    // variables.
       int ChildI;  // the number of child (0 - n-1).
       Infinitree ParentInfinitree= null;  // parent, or null if none.
       
    // Constructors.

      public Infinitree( Infinitree InParentInfinitree, int InChildI )
        /* Constructs an Infinitree node.
          */
        { // Infinitree(.)
          ChildI= InChildI;  // save child index.
          ParentInfinitree= InParentInfinitree;  // save parent.
          } // Infinitree(.)
       
    // other methods.
  
      public int getChildCount( ) 
        { // getChildCount( )
          Infinitree ScanInfinitree= ParentInfinitree;
          int ChildIndexI= 2;  // Initialize child index.
          while  // Increase that by the number of ancestors.
            ( ScanInfinitree != null )
            {
              ChildIndexI++;  // increment index.
              ScanInfinitree= ScanInfinitree.ParentInfinitree;
              }
          return ChildIndexI;  // Return ending index as count.
          } // getChildCount( )

      public DataNode getChild( int InIndexI ) 
        /* This returns the child with index is InIndexI.  */
        { // getChild( int ) 
          Infinitree ChildInfinitree= null;  // assume InIndexI out of range.
          if  // override result if InIndexI is in range. 
            ( InIndexI >= 0 && InIndexI < getChildCount( ))  // in range?
            ChildInfinitree= new Infinitree( // yes, calculate child node...
              this,  // ...using this node as parent and...
              InIndexI  // ...its InIndexI as child index.
              );
          return ChildInfinitree;  // return result whatever.
          } // getChild( int ) 

      public String toString( ) 
        {
          String ResultString= null;
          if ( ParentInfinitree == null ) // this is root.
            ResultString= "Infinite-Test-Tree";
            else
            ResultString= 
              ParentInfinitree.toString() +
              "."+ ChildI;
          return ResultString;  // Return ending String.
          }

      @Override public boolean equals(Object other) 
        /* This is the standard equals() method.  
          This is not a complete implimentation of equals(..).
          It doesn't do null checks.
          */
        {
          boolean ResultB = false;  // assume objects are not equal.
          Comparer: {  // Comparer.
            if ( ! ( other instanceof Infinitree ) )  // different class.
              break Comparer;  // exit with false.
            Infinitree that =  // create variable for easy access of fields.
              (Infinitree) other; 
            if ( this.ChildI != that.ChildI )  // child numbers not equal.
              break Comparer;  // exit with false.
            if // parents not equal.
              ( ! ( ( ParentInfinitree == that.ParentInfinitree ) ||
                    ( ( ParentInfinitree != null ) &&
                      ( ParentInfinitree.equals( that.ParentInfinitree ) )
                      )
                    )
                )
              break Comparer;  // exit with false.
            ResultB= true;  // everything is equal, so override result.
            }  // Comparer.
          return ResultB;
          }

      @Override public int hashCode() 
        {
          int hash = 2047;
          hash = hash * 17 + ChildI;
          if ( ParentInfinitree != null)
            hash = hash * 31 + ParentInfinitree.hashCode();
          return hash;
          }
    
    }
