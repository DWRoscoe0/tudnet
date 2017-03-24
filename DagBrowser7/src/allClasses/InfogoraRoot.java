package allClasses;

public class InfogoraRoot  extends NamedList {
  
  /* This class is the root node of the DataNode DAG.
    A NamedList could be used in its place,
    but having a special separate class for it 
    makes debugging easier.
    */
  
  public InfogoraRoot( DataNode... inDataNodes )  // Constructor.
    {
	    initializeV( // Calling superclass NamedList with...
	        "Infogora-Root", // ...the name for this DataNode and...
	        inDataNodes // ...the array of all child DataNodes.
	        );
      }

      
  }
