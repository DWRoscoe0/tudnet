package allClasses;

import static allClasses.Globals.appLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MutableListWithMap< 
    K, 
    V extends DataNodeAndThreadValue< ? extends DataNode > 
    >
  extends MutableList
  // This class extends MutableList with a HashMap for fast lookup.
  {

	  protected Map<K,V> childHashMap= // Map for fast child lookup.
		    new ConcurrentHashMap<K,V>(); // Initializing to empty.

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
	
	  protected synchronized void addingV(
	  		K childK,
	  		V childV
	  		)
	    /* This adds childV to both the HashMap and this MutableList.
	      The entry added to the HashMap also has childK.
	      */
	    {
	    	childHashMap.put(  // Adding to HashMap. 
	    			childK,
	      		childV
	      		);
	    	if  // Adding to MutableList and report error if it's there already.
	      	( ! addB( childV.getD() ) )
	      	appLogger.error(
	      			"MutableListWithMap.addingV(..): Already present."
	      			);
	      }

  }
