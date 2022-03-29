package allClasses;

public class DataNodeWithKeyAndThreadValue< 
    D extends KeyedStateList< K >, // DataNode type. 
    K // Key type in and returned by DataNode.
    >

  extends Object
  
  /* 
    ///org This class should probably be eliminated.
    I think I created it in the early days when I was new to Java,
    and I was learning about Maps and Threads, and Java generics,
    and was impatient to get some code working.
    This class is used as the Value part of the entries in HashMaps
    used for fast lookup by StreamcasterManager/MutableListWithMap subclasses.
    I now think that it would have been easier to store the Thread
    in the DataNode itself, along with the key, and not in this separate object.
    
    This class combines
    * a subclass of a KeyedStateList, which is
      a DataNode that has an integrated key, and
    * an associated EpiThread.
      
    This combination was first used in what is now one of its subclasses, 
    the Unicaster class.
    */

  {
    private D theDataNodeD; // extends KeyedStateList<K>
    private EpiThread theEpiThread; // associated thread.

    public DataNodeWithKeyAndThreadValue(  // Constructor. 
        D theDataNodeD,
        EpiThread theEpiThread
        )
      {
        this.theDataNodeD= theDataNodeD;
        this.theEpiThread= theEpiThread;
        }

    public K getKeyK()
      { 
        return getDataNodeD().getKeyK(); 
        }
    
    public D getDataNodeD()
      { 
        return theDataNodeD; 
        }

    public EpiThread getEpiThread()
      { 
        return theEpiThread; 
        }

    }
