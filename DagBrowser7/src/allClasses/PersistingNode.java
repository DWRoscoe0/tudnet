package allClasses;

import java.util.NavigableMap;
import java.util.TreeMap;

public class PersistingNode {

	/* This class is used to store Persistent class data when in memory.  
	  It stores values in two different places.
	  * It stores a single value, referenced by this node directly. 
	    This does not involve any of its child nodes.
	    The key, or keys, associated with this value, are not stored in this node.
	  * It stores multiple values indirectly, in its child PersistingNodes.
	    Each child PersistingNode has a key associated with it.
	    These associations are stored together in a NavigableMap
	    which is referenced by this node directly.
	    It can store even more values because the children
	    can have their own children.

    These methods understand keys, but do not understand paths, 
    though these nodes can be used to form paths.

		Operations in these nodes are fast because all the data
		is in the node or its NavigableMap.

		As with all hierarchical structures,
		there can sometimes be confusion between a node and one of its children,
		and between the node's value and the values of its children.
		It is important to distinguish between the two.
		This can be done:
		* using different method names, or
		! additional method key parameters, each one selecting
		  a child in another level of children (preferred).
	  */

	private String valueString; // Stores value string, or null if there is none.
	private NavigableMap<String,PersistingNode> childrenNavigableMap;
	  // This contains the child nodes, if there any.
	  // Each child is associated with a its own key String.
		///opt This could be replaced with a SelfReturningNodeOrNodes class.
	    // because many or most of these will be empty because
	    // many nodes will have a value but no children.

	public PersistingNode()
	  // Constructs a node with 0 children and a null value string.
		{
			this(null);
			}

	public PersistingNode(String valueString)
		// Constructs a node with 0 children and value of valueString.
		{
			this.valueString= valueString;
			this.childrenNavigableMap= 
					new TreeMap<String,PersistingNode>(); // Create empty Map.
			}

  public void putV(String keyString, String valueString)
	  /* This method stores the value in valueString into
	    the child associated with the key in keyString.
		  It overwrites any value already stored.
	    If no child node exist then it makes one first.
		 	*/
		{
			PersistingNode childPersistingNode= 
	  			getOrMakePersistingNode(keyString);
			childPersistingNode.putV(valueString);
			}

	public void putV(String valueString)
	  /* This method stores the value valueString into this node.
	    It overwrites any value already stored.
	   	*/
		{
			this.valueString= valueString;
			}

	public String getString(String keyString)
	  /* This method returns the value from 
	    the child associated with the key in keyString. 
		  If there is no such child value then null is returned.
		  */
		{
		  String valueString= null; // Set default return value of null.
			PersistingNode childPersistingNode= getPersistingNode(keyString);
			if (childPersistingNode != null) // If a child node exists for the key
				valueString= childPersistingNode.getString(); // read its value.
			return valueString;
			}

	public String getString()
	  /* This method returns the value associated with this node.
		  If there is no value then it returns null.
		 	*/
		{
			return valueString;
			}

	public PersistingNode getOrMakePersistingNode(String keyString)
	  /* This method returns the child PersistingNode 
	    that is associated with the key keyString.  
	    If there is no such child, one is created containing
	    * no valueString, and
	    * a NavigableMap containing no children.
	    This method is used for recursion in a PersistingNode structure.
	   */
		{
			PersistingNode childPersistingNode= getPersistingNode(keyString);
			if (childPersistingNode == null) // No value exists for the key.
				{ // Create an empty child node and store in map.
					childPersistingNode= new PersistingNode(keyString);
					  // Initial value is the node's associated key.
					  // This is to help make debugging easier.
					childrenNavigableMap.put(keyString,childPersistingNode);
					}
			return childPersistingNode;
			}

	private PersistingNode getPersistingNode(String keyString)
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

	}
