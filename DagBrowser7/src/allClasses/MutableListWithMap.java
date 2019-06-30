package allClasses;

import static allClasses.Globals.appLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MutableListWithMap< 
    K, // Key for the map entries.
    V extends DataNodeWithKeyAndThreadValue< // Value for map entry. 
    	D, // The DataNode part of value. 
      K  //The key stored within DataNode.
      >, 
		D extends KeyedStateList< K > // DataNode within map entry value.
    >

  extends MutableList
  
  /* This class extends MutableList with a HashMap for 
    fast child lookup based on key.
    The HashMap and List should be kept synchronized.
    It also has some support for EpiThreads associated with the child values.
    */
  
  {

	  protected Map<K,V> childHashMap= // Map for fast child lookup.
		    new ConcurrentHashMap<K,V>(); // Initializing map to empty.

    public MutableListWithMap (   // Constructor.
	      String nameString,
        DataNode... inDataNodes 
	      )
	    {
      	initializeV(
	        nameString,
	        inDataNodes
      		);
	      }

    public String getValueString( ) // This is for displaying the List count.
      {
    	  return Integer.toString(getChildCount( ));
        }
    
	  protected synchronized void addingV( K childK, V childV )
	    /* This adds childV to the HashMap and the DataNode part
	      to this MutableList, keeping them synchronized.
	      The entry added to the HashMap has childK as its key.
	      */
	    {
	  		DataNode childDataNode= childV.getDataNodeD();
	  		
	    	childHashMap.put( childK, childV );  // Adding value to HashMap.

	    	if  // Adding DataNode only to MutableList unless it's there already.
	      	( ! addAtEndB( childDataNode ) )
	      	appLogger.error( // Logging error if already in list.
	      			"MutableListWithMap.addingV(..): Already present."
	      			);
	      }

	  public synchronized void removingV( D childD )
      /* This method removes childV and its key from the HashMap 
        and the childD DataNode part from this MutableList, 
        keeping them synchronized.
        */
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
