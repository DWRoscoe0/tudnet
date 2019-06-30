package allClasses;

public class DataNodeWithKeyAndThreadValue< 
  	D extends KeyedStateList< K >, // DataNode type. 
  	K // Key type in and returned by DataNode.
  	>

  extends Object
  
  /* This class is an Object subclass, not a DataNode subclass,
    but a DataNode subclass is stored in one of its fields.

    This class is used as the Value part of the entries in HashMaps
    used for fast lookup by StreamcasterManager/MutableListWithMap subclasses.
    
    This class combines
    * a subclass of a KeyedStateList, which is
      a DataNode that has an integrated key, and
    * an associated EpiThread.
    This is a rather kludgey way of associating data, in this case, a thread,
    with another object, in this case a DataNode. 
    ??? Why was this done?  Could the thread be stored in the DataNode?
      
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
