package allClasses;

import java.util.Arrays;
import java.util.List;

public class NamedList 

  extends NamedLeaf  // Will override all leafy-ness.
  
  /* This is a utility class that appears to be 
    simply a List with a name.  
    */
  
  { // class NamedList

    List<DataNode> theListOfDataNodes;

    NamedList ( String nameString, DataNode... inDataNodes )  // Constructor.
      /* This constructor creates a NamedList with
        0 or more DataNodes from the array inDataNodes.
        Theoretically it could be used for 
        many different types of DataNodes.
        */
      {
        super( nameString ); 

        theListOfDataNodes= Arrays.asList( inDataNodes );
        }
    
      public boolean isLeaf( ) 
        {
          return false;  // Overriding the true that NamedLeaf returns.
          }

      public DataNode getChild( int indexI ) 
        /* This returns the child with index indexI.  */
        {
          DataNode resultDataNode;;  // Allocating result space.

          if  // Handling index which is out of range.
            ( (indexI < 0) || (indexI >= theListOfDataNodes.size()) )
            resultDataNode= null;  // Setting result to null.
          else  // Handling index which is in range.
            resultDataNode=   // Setting result to be child from...
              theListOfDataNodes.get(   // ...DataNode List...
                indexI  // ...at this position.
                );

          return resultDataNode;
          }

    } // class NamedList
