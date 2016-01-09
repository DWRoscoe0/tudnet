package allClasses;

import static allClasses.Globals.appLogger;




import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MutableListWithMap< 
    K, // Key for map entry.
    V extends DataNodeWithKeyAndThreadValue< // Value for map entry. 
    	D, // The DataNode part of value. 
      K  //The key part of value.
      >, 
		D extends DataNodeWithKey< K > // DataNode within map entry value.
    >

  extends MutableList
  
  // This class extends MutableList with a HashMap for fast child lookup.
  // It also has some support for EpiThreads associated with the child values.
  
  {

	  protected Map<K,V> childHashMap= // Map for fast child lookup.
		    new ConcurrentHashMap<K,V>(); // Initializing map to empty.

    public MutableListWithMap (   // Constructor.
	      DataTreeModel theDataTreeModel,
	      String nameString,
        DataNode... inDataNodes 
	      )
	    {
	  		// Superclass injections.
	      super( // Constructing MutableList superclass.
		        theDataTreeModel,
		        nameString,
		        inDataNodes
	      		);
	      }

    public String getValueString( ) // This is for displaying the List count.
      {
    	  return Integer.toString(getChildCount( ));
        }
    
	  protected synchronized void addingV( K childK, V childV )
	    /* This adds childV to both the HashMap and this MutableList.
	      The entry added to the HashMap has childK as its key.
	      */
	    {
	  		DataNode childDataNode= childV.getDataNodeD();
	  		
	    	childHashMap.put( childK, childV );  // Adding value to HashMap.

	    	if  // Adding DataNode only to MutableList unless it's there already.
	      	( ! addB( childDataNode ) )
	      	appLogger.error( // Logging error if already in list.
	      			"MutableListWithMap.addingV(..): Already present."
	      			);
	      }

	  public synchronized void removingV( D childD )
      // This method removes childV from both the HashMap and this MutableList.
	    {
	    	if  // Removing from this DataNode's List if it's there.
	    		( ! removeB( childD ) )
	      	appLogger.error(
	      			"MutableListWithMap..removingV(..): removeB(..) failed"
	      			);

	    	K childKeyK= childD.getKeyK();
		    childHashMap.remove( childKeyK );  // Removing from Map.
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
    	  // Creating both iterators now before entries have disappeared from Map.
	      Iterator<V> stopIteratorOfVs=
            childHashMap.values().iterator();
	      Iterator<V> joinIteratorOfVs=
            childHashMap.values().iterator();

        while  // Requesting/initiating termination of all entry threads.
          (stopIteratorOfVs.hasNext())
          {
            V theV= stopIteratorOfVs.next();
            theV.getEpiThread().stopV(); // Requesting termination. 
            }

        while  // Waiting for completion of termination of all entry threads.
          (joinIteratorOfVs.hasNext())
          {
            V theV= joinIteratorOfVs.next();
            theV.getEpiThread().joinV(); // Awaiting termination 
            }
        }

  }