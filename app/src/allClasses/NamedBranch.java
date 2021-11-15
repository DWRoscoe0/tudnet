package allClasses;


public abstract class NamedBranch 

  extends NamedDataNode

  /* This class is the base class for all named DataNodes that
    are not leaves, meaning they are branches.
    Though it is a branch, not a leaf, it has no children.
    */

  {

    public boolean isLeaf()
      // Be clear: This node is a branch, not a leaf.
      { 
        return false; 
        }

    public String getContentString( ) // DataNode interface method.
      {
        return ""; // By default, there is no content.
        }

    }
