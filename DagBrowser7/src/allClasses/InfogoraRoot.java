package allClasses;

public class InfogoraRoot  extends NamedList {
  
  /* This class is the root node of the DataNode DAG.
    A NamedList could be used in its place,
    but having a special separate class for it 
    makes debugging easier.
    */
  
  public InfogoraRoot( )  // Constructor.
    {
      super( // Calling superclass NamedList with...
        "NEW-Infogora-Root", // ...the name for this DataNode and...
        new DataNode[] { // ...an array of all child DataNodes.
          new FileRoots(),
          new Outline( 0 ),
          new ConnectionManager.Root(), // Temporary.
          new Infinitree( null, 0 )
          }
        );
      }

  }
