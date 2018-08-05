package allClasses;

import java.util.TreeMap;

public class PersistingNode {

	/* This class is used to store Persistent class data when in memory.  
	  It stores two types of data in two different places.
	  * It can store values referenced by this node directly, 
	    without involving child nodes.
	  * It can store values indirectly in child nodes,
	    referenced by a Map and associated with key names.
	  */ 

	private String valueString; // For the value string, or null if there is none.
	private TreeMap<String,PersistingNode> childrenTreeMap; // For child nodes.
		///opt This could be replaced with a SelfReturningNodeOrNodes class.
	    // because many or most of these will be empty.

	public PersistingNode(String valueString)
		// Constructs a node with 0 children and value of valueString.
		{
			this.childrenTreeMap= 
					new TreeMap<String,PersistingNode>(); // Create empty Map.
			this.valueString= valueString;
			}

	public PersistingNode()
	  // Constructs a node with 0 children and a null value string.
		{
			this(null);
			}

	public String getValueString()
	  /* This method returns the value associated with this node.
		  If there is no value then it returns null.
		 	*/
		{
			return valueString;
			}

	public void setValueV(String valueString)
	  /* This method stores the value valueString into this node.
	    It overwrites any value already stored.
	   	*/
		{
			this.valueString= valueString;
			}

	public String getChildValueString(String keyString)
	  /* This method returns the value from 
	    the child associated with key keyString. 
		  If there is no such child value then null is returned.
		  */
		{
		  String valueString= null; // Assume there is no value.
			PersistingNode childPersistingNode= getChildPersistingNode(keyString);
			if (childPersistingNode == null) // a child node exists for the key.
				valueString= getValueString(); // Read value from child node.
			return valueString;
			}

	public void setChildValueV(String keyString, String valueString)
	  /* This method stores the value valueString into
	    the child associated with key keyString.
		  It overwrites any value already stored.
	    If no child node exist then it makes one first.
		 	*/
		{
			PersistingNode childPersistingNode= 
	  			getOrMakeChildPersistingNode(keyString);
			childPersistingNode.setValueV(valueString);
			}

	public TreeMap<String,PersistingNode> getChildrenTreeMap()
	  ///opt Maybe change to private and call other methods from outside instead.
		{
			return childrenTreeMap;
			}

	public PersistingNode getChildPersistingNode(String keyString)
	  /* This method returns the child PersistingNode 
	    that is associated with the key keyString.
	    If there is none then a null is returned. 
	   */
		{
			PersistingNode childPersistingNode= childrenTreeMap.get(keyString);
			return childPersistingNode;
			}

	public PersistingNode getOrMakeChildPersistingNode(String keyString)
	  /* This method returns the child PersistingNode 
	    that is associated with the key keyString.  
	    If there is no such child, one is created with no valueString.
	    This method is used for recursion in a PersistingNode structure.
	   */
		{
			PersistingNode childPersistingNode= getChildPersistingNode(keyString);
			if (childPersistingNode == null) // No value exists for the key.
				{ // Create a null value and store in map.
					childPersistingNode= new PersistingNode();
					childrenTreeMap.put(keyString,childPersistingNode);
					}
			return childPersistingNode;
			}
		
	}
