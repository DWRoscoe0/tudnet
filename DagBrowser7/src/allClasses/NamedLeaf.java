package allClasses;

public class NamedLeaf
  
  /* This class is the base class for all named DataNodes that
    are leaves, meaning they are not branches.
    */

  extends NamedDataNode
  
  {

		public static NamedLeaf makeNamedLeaf( String nameString )
			{
				NamedLeaf theNamedLeaf= new NamedLeaf();
				theNamedLeaf.initializeV( nameString );

	  		return theNamedLeaf;
	  		}
	
    public boolean isLeaf( )
      // Make it clear: This is a leaf node, not a branch.
      {
        return true;
        }
      
    }
