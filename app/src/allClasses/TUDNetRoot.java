package allClasses;

public class TUDNetRoot  extends NamedList {
  
  /* This class is the root node of the DataNode DAG.
    A NamedList could be used in its place,
    but having a special separate class for it 
    makes debugging easier.
    */
  
  public TUDNetRoot(   // Constructor. 
      DataNode... inDataNodes 
      )
    {
	    initializeV( // Calling superclass NamedList with...
	        Config.appString + "-Root", // ...the name for this DataNode and...
	        inDataNodes // ...the array of all child DataNodes.
	        );
      }
  
    @Override
    public boolean isRootB()
      /* Returns true, indicating that this node is the root node.
       * This is the only class that does this.
       */
      { 
        return true; 
        }

      
  }
