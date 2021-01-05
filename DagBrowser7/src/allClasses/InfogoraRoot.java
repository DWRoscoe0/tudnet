package allClasses;

public class InfogoraRoot  extends NamedList {
  
  /* This class is the root node of the DataNode DAG.
    A NamedList could be used in its place,
    but having a special separate class for it 
    makes debugging easier.
    */
  
  public InfogoraRoot(   // Constructor. 
      DataNode... inDataNodes 
      )
    {
	    initializeV( // Calling superclass NamedList with...
	        "Infogora-Root", // ...the name for this DataNode and...
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
