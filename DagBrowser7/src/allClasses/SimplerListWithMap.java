package allClasses;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static allClasses.AppLog.theAppLog;


public class SimplerListWithMap< 
    K, // Key for the map entries.
    D extends KeyedStateList< K > // DataNode within map entry value.
    >

  extends MutableList
  
  /* This class was based on MutableListWithMap.
    It is meant to be a more basic version that doesn't include a thread. 
    It extends MutableList with a HashMap for fast child lookup based on key.
    The HashMap and List should be kept synchronized.
    Unlike MutableListWithMap, it has no support for an associated EpiThread.
    */
  
  {

    protected Map<K,D> childHashMap= // Map for fast child lookup.
        new ConcurrentHashMap<K,D>(); // Initializing map to empty.

    public SimplerListWithMap (   // Constructor.
        String nameString,
        DataNode... inDataNodes 
        )
      {
        initializeV(
          nameString,
          inDataNodes
          );
        }

    public String getSummaryString( ) // This simply uses the number of List elements.
      {
        return Integer.toString(getChildCount());
        }
    
    protected synchronized void addingV( K childK, D childD )
      /* This adds childV to the HashMap and the DataNode part
        to this MutableList, keeping them synchronized.
        The entry added to the HashMap has childK as its key.
        */
      {
        childHashMap.put( childK, childD );  // Adding value to HashMap.

        if  // Adding DataNode only to MutableList unless it's there already.
          ( ! addAtEndB( childD ) )
          theAppLog.error( // Logging error if already in list.
              "SimplerListWithMap.addingV(..): Already present."
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
          theAppLog.error(
              "MutableListWithMap..removingV(..): removeB(..) failed"
              );

        K childKeyK= childD.getKeyK();
        childHashMap.remove( childKeyK );  // Removing from Map.
        }

  }
