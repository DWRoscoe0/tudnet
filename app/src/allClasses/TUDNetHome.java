package allClasses;

public class TUDNetHome

  extends NamedList 

  {

    /* This class is the app's Home screen and the root of the DataNode tree.
     * 
     * A NamedList could have been used instead of this class,
     * but having a special separate class for it makes debugging easier,
     * and it allows marking the root node with a 0-storage method isRootB().
     */

    public TUDNetHome(   // Constructor. 
      DataNode... childrenDataNodes // The children of the root DataNode. 
      )
    {
      initializeV( // Call superclass initializer with

          ( // the name for this DataNode which is
            Config.appString // the name of this app
            + "-Home" // concatenated with "-Home"
            ), // and

          childrenDataNodes // the array of all child DataNodes.

          );
      }
  
    @Override
    public boolean isRootB()
      /* This method returns true, indicating that this node is the root node.
       * This is the only class that does this.
       */
      { 
        return true; 
        }

      
  }
