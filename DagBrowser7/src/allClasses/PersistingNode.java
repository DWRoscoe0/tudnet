package allClasses;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NavigableMap;
import java.util.TreeMap;

import static allClasses.AppLog.theAppLog;


public class PersistingNode {

	/* This class is used to store Persistent class data when in memory.  
	  It stores values in two different places.
	  * It stores a single String value.
	    There is no key, or keys, associated with this value,
	    except possibly externally to this node. 
	  * It stores multiple key-value pairs in a NavigableMap.
	    Each key is a String.
	    Each value is another PersistingNode.
      These nodes may be considered named children of the current node.

    Methods on these nodes tend to be fast because all the data
    is in the node or its NavigableMap.
    There is no scanning of long paths.
    The methods in this class understand keys, but do not understand paths.
    Callers of these methods may consider child keys to be path elements. 

		As with all hierarchical structures,
		there can sometimes be confusion between a node and one of its children,
		and between the node's value and the values of its children.
		It is important to distinguish between the two.
		This can be done:
		* using different method names, or
		! additional method key parameters, each one selecting
		  a child in another level of children (preferred).
	  */

	private String valueString; // Stores this node's value string, 
	  // or null if there is no value.
	private NavigableMap<String,PersistingNode> childrenNavigableMap;
	  // This contains the child nodes, if there are any.
	  // Each child entry consists of a String key and a PersistingNode value.
	  // It also makes possible the ability to 
	  // iterate over its entries in sorted order.
		///opt This could be replaced with a SelfReturningNodeOrNodes class.
	    // because many or most of these will be empty because
	    // many nodes will have a value but no children.

	public PersistingNode()
	  // Constructs a node with 0 children and a null value string.
		{
			this(null);
			}

	public PersistingNode(String valueString)
		// Constructs a node with 0 children and a value of valueString.
		{
			this.valueString= valueString;
			this.childrenNavigableMap=  // Create empty Map.
					new TreeMap<String,PersistingNode>();
			}

  public void putChildV(String keyString, String valueString)
	  /* This method stores the value valueString into
	    the child associated with the key keyString.
		  It overwrites any value already stored.
	    If no child node exist then it makes one first.
		 	*/
		{
			PersistingNode childPersistingNode= 
	  			getOrMakeChildPersistingNode(keyString);
			childPersistingNode.putV(valueString);
			}

	public void putV(String valueString)
	  /* This method stores the value valueString into this node.
	    It overwrites any value already stored.
	   	*/
		{
			this.valueString= valueString;
			}

	public String getChildString(String keyString)
	  /* This method returns the value from 
	    the child associated with the key in keyString. 
		  If there is no such child value then null is returned.
		  */
		{
		  String valueString= null; // Set default return value of null.
			PersistingNode childPersistingNode= getChildPersistingNode(keyString);
			if (childPersistingNode != null) // If a child node exists for the key
				valueString= childPersistingNode.getValueString(); // read its value.
			return valueString;
			}

	public String getValueString()
	  /* This method returns the value associated with this node.
		  If there is no value then it returns null.
		 	*/
		{
			return valueString;
			}

	public PersistingNode getOrMakeChildPersistingNode(String keyString)
	  /* This method returns the child PersistingNode 
	    that is associated with the key keyString.  
	    If there is no such child, one is created containing
	    * no valueString, and
	    * a NavigableMap containing no children.
	   */
		{
	    if ( keyString == null || keyString.isEmpty())
	      theAppLog.error("getOrMakePersistingNode() Missing keyString.");

			PersistingNode childPersistingNode= getChildPersistingNode(keyString);
			if (childPersistingNode == null) // No child PersistingNode exists for the key.
				{ // Create an empty child node and store in map.
					childPersistingNode= new PersistingNode(keyString);
					  // Initial value is the node's associated key.
					  // This isn't needed but helps make debugging easier.
					childrenNavigableMap.put(keyString,childPersistingNode);
					}
			return childPersistingNode;
			}

	public PersistingNode getChildPersistingNode(String keyString)
	  /* This method returns the child PersistingNode 
	    that is associated with the key keyString.
	    If there is no such child then null is returned. 
	   */
		{
			PersistingNode childPersistingNode= childrenNavigableMap.get(keyString);
			return childPersistingNode;
			}

	public NavigableMap<String,PersistingNode> getNavigableMap()
	  /* Returns this node's NavigableMap, which contains its children.
	  	///opt Maybe change to private and call from only methods in this class.
	  	  This method is used mostly with NavigableMap get(..) and put(..).
	  	  Maybe create new get(..) and put(..) methods here
	  	  and use those instead?
	  	  Might still need for PersistentCursor?
	  	*/
		{
			return childrenNavigableMap;
			}

  public static MapEpiNode makeMapEpiNode(PersistingNode thePersistingNode)
    /* This method returns a MapEpiNode containing 
      all the data in thePersistingNode.
      It doesn't include a PersistingNode's valueString data if the node has children.   
      */
    {
      LinkedHashMap<EpiNode,EpiNode> resultLinkedHashMap= 
        new LinkedHashMap<EpiNode,EpiNode>(); // Create initially empty output map.
      NavigableMap<String,PersistingNode> childrenNavigableMap= 
          thePersistingNode.getNavigableMap();
      Iterator<String> childrenIterator= // Make iterator for the PersistingNode children. 
          childrenNavigableMap.keySet().iterator();
      while (childrenIterator.hasNext()) // Copy all PersistingNode entries to EpiNode.
        { // Copy data from one PersistingNode map entry to EpiNode map.
          String childKeyString= childrenIterator.next();
          PersistingNode childValuePersistingNode= 
              childrenNavigableMap.get(childKeyString);
          NavigableMap<String,PersistingNode> childValueNavigableMap=
              childValuePersistingNode.getNavigableMap();
          EpiNode childKeyEpiNode= new ScalarEpiNode(childKeyString);
          EpiNode childValueEpiNode;
          if (childValueNavigableMap.isEmpty()) // If there are no children 
            childValueEpiNode= // use value string converted to Scalar as value.
              new ScalarEpiNode(childValuePersistingNode.getValueString()); 
            else // otherwise
            childValueEpiNode= // use node with children recursively converted to EpiNode. 
              makeMapEpiNode(childValuePersistingNode);
          resultLinkedHashMap.put(childKeyEpiNode,childValueEpiNode); // Append new entry to map.
          }
        return new MapEpiNode(resultLinkedHashMap);
      }

	}
