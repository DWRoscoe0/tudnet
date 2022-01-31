package allClasses;


public class NamedBranch 

  extends NamedDataNode

  /* This class is the base class for all named DataNodes that
    are not leaves, meaning they are branches.
    Though it is a branch, an instance of this class has no children.
    To have children, this class must be sub-classed.
    */

  {

    public boolean isLeaf()
      /* This method returns false to indicate that
       * an instance of this class is a branch, not a leaf.
       */
      { 
        return false; 
        }

    public String getContentString( )
      /* This method returns an empty string to indicate 
       * that an instance of this class has no content. 
       */
      {
        return ""; // By default, there is no content.
        }

    }
