package allClasses;

import java.util.Iterator;


public class StreamcasterManager<
    K, // Key for map entry.
		D extends KeyedStateList< K >,
    V extends DataNodeWithKeyAndThreadValue<D,K>
    >

	extends MutableListWithMap<K,V,D>

	/* This class manages a set of Streamcasters, either 
	  the app's Unicasters, or a Unicaster's Subcasters.
	  In addition to storing information about them, 
	  it provides methods for:
	  
	  * Testing for their existence, 
	  * Creating them and starting their threads.
	  * Displaying information about them.
	  * Stopping their threads.
	  
	  It is also used to pass packets between 
	  the NetInputStreams and NetOutputStreams of the Streamcasters 
	  and their owners.
	  
	  Most of this classes methods are synchronized.
	  */

	{

		protected final AppFactory theAppFactory;

	  public StreamcasterManager(   // Constructor.
	      String nameString,
	      AppFactory theAppFactory,
	  		DataNode... inDataNodes 
	      )
	    {
	  		// Superclass injections.
	      super(
		        nameString,
		        inDataNodes
	      		);

	      // This class's injections.
	      this.theAppFactory= theAppFactory;
	      }

  	public synchronized D tryingToGetDataNodeWithKeyD( K theKeyK )
	    /* This method returns the KeyedStateList associated with theKeyK,
	      if such a KeyedStateList exists.
		    If it doesn't exist then it returns null.
		    */
      {
    		V theDataNodeWithKeyAndThreadValueV=
    				childHashMap.get( theKeyK );
    		D theDataNodeWithKeyD= // Getting DataNode from value
	        ( theDataNodeWithKeyAndThreadValueV != null )
	        ? theDataNodeWithKeyAndThreadValueV.getDataNodeD() // if it exists,
	        : null; // otherwise getting null.
        return theDataNodeWithKeyD;
        }

    public void stoppingEntryThreadsV()
      /* This method terminates all of the threads which are associated with 
        the V values in this class's collection.  It does this in 2 loops.
        * The first loop requests thread terminations.
	      * The second loop waits for the terminations to complete.
        This is done in anticipation of when termination will require
        protocol handshakes, requiring significant time,
        and therefore would benefit from parallelism.
        */
      {
    	  // Creating both iterators now before entries disappear from Map.
	      Iterator<V> stopIteratorOfVs=
            childHashMap.values().iterator();
	      Iterator<V> joinIteratorOfVs=
            childHashMap.values().iterator();

        while  // Requesting/initiating termination of all entry threads.
          (stopIteratorOfVs.hasNext())
          {
            V theV= stopIteratorOfVs.next();
            theV.getEpiThread().stopV(); // Requesting one termination. 
            }

        while  // Waiting for completion of termination of all entry threads.
          (joinIteratorOfVs.hasNext())
          {
            V theV= joinIteratorOfVs.next();
            theV.getEpiThread().joinV(); // Awaiting one termination 
            }
        }

		}
