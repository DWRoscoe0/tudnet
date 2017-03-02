package allClasses;

import static allClasses.Globals.appLogger;

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
  // The HashMap and List are kept synchronized.
  
  {

	  protected Map<K,V> childHashMap= // Map for fast child lookup.
		    new ConcurrentHashMap<K,V>(); // Initializing map to empty.

    public MutableListWithMap (   // Constructor.
	      DataTreeModel theDataTreeModel,
	      String nameString,
        DataNode... inDataNodes 
	      )
	    {
      	/*  //%
	        theDataTreeModel,
	        nameString,
	        inDataNodes
      		);
      	*/  //%
      	initializingV(
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

  }
