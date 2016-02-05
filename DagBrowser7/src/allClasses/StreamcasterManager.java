package allClasses;

import java.util.Iterator;


public class StreamcasterManager<
    K, // Key for map entry.
    V extends DataNodeWithKeyAndThreadValue< // Value for map entry. 
    	D, // The DataNode part of value. 
      K  //The key part of value.
      >, 
		D extends DataNodeWithKey< K > // DataNode within map entry value.
    >

	extends MutableListWithMap<K,V,D>

	{
	
	  public StreamcasterManager(   // Constructor.
	      DataTreeModel theDataTreeModel,
	      String nameString,
	      DataNode... inDataNodes 
	      )
	    {
	  		// Superclass injections.
	      super(
		        theDataTreeModel,
		        nameString,
		        inDataNodes
	      		);
	      }

    public void stoppingEntryThreadsV()
      /* This method terminates all of the threads 
        which are associated with the V values in its collection.
        It does this in 2 loops.
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
