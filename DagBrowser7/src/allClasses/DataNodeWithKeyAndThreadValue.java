package allClasses;

public class DataNodeWithKeyAndThreadValue< 
	//D extends DataNode, // DataNode type. 
	D extends DataNodeWithKey< K >, // DataNode type. 
	K // Key type in and returned by DataNode.
	>
//public class DataNodeWithKeyAndThreadValue< D extends DataNode >

  /* This class exists to combine a DataNode with an associated EpiThread
    for use in the Value part of the HashMap in 
    what became the MutableListWithMap class.
    This combination was first done in what is now one of its subclasses, 
    the Unicaster class.
    
    It also has the capability of returning the nodes key object.
	  */

	{
		private D theDataNodeD;
		private EpiThread theEpiThread;

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
