package allClasses;

public class DataNodeAndThreadValue< D extends DataNode >

  /* This class exists to combine a DataNode with an associated EpiThread
    for use in the Value part of the HashMap in 
    what became the MutableListWithMap class.
    This combination was first done in what is now one of its subclasses, 
    the Unicaster class.
	  */

	{
		private D theDataNode;
		private EpiThread theEpiThread;

    public DataNodeAndThreadValue(  // Constructor. 
        D theD,
        EpiThread theEpiThread
        )
      {
    		this.theDataNode= theD;
        this.theEpiThread= theEpiThread;
        }
    
		public D getD()
		  { 
		    return theDataNode; 
		    }

    public EpiThread getEpiThread()
      { 
        return theEpiThread; 
        }

		}
