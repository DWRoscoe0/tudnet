package allClasses;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class NamedList 

  extends NamedNonLeaf  // Will override all remaining leaf behavior.
  
  /* This is a utility class that is simply a List with a name.
    It is meant to be immutable after construction and initialization, 
    but misbehaving subclasses could change it.
    Also, there is nothing preventing changes in
    its children or other descendants.
    */
  
  { // class NamedList

    protected List<DataNode> theListOfDataNodes= // Set to empty,
    		new ArrayList<DataNode>(  // a mutable ArrayList from
	        Arrays.asList(  // an immutable List made from
	        		emptyListOfDataNodes() // an empty DataNode list.
	            )
	        ); // This might be replaced with a SelfReturningListOrNode.

    public void initializeV( String nameString, DataNode... inDataNodes )
      /* This initializes the NamedList with
        0 or more DataNodes from the array inDataNodes.
        Theoretically it could be used for 
        many different types of DataNodes.
        */
      {
    		super.initializeV( nameString );

        theListOfDataNodes= // Creating and storing the DataNode List to be
          new ArrayList<DataNode>(  // a mutable ArrayList from
            Arrays.asList(  // an immutable List made from
              inDataNodes  // the input array.
              )
            );  // the input array.
        }


	  // interface DataNode methods.
 
    public DataNode getChild( int indexI ) 
      /* This returns the child with index indexI or null
        if no such child exists.
        */
      {
        DataNode resultDataNode;  // Allocating result space.

        if  // Handling index which is out of range.
          ( (indexI < 0) || (indexI >= theListOfDataNodes.size()) )
          resultDataNode= null;  // Setting result to null.
        else  // Handling index which is in range.
          resultDataNode=   // Setting result to be child from...
            theListOfDataNodes.get(   // ...DataNode List...
              indexI  // ...at the desired position.
              );

        return resultDataNode;
        }

    } // class NamedList
