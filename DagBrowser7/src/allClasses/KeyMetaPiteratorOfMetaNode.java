package allClasses;

import java.util.Collection;



public class KeyMetaPiteratorOfMetaNode
  extends MetaPiteratorOfMetaNode
  
  /* This is a Piterator for searching.
    It iterates over the MetaNode children of 
    the MetaChildren of a MetaNode,
    but only the subset with a particular attribute Key.
    */

  { // class KeyMetaPiteratorOfMetaNode.

		protected String keyString;  // For the desired attribute key.
    
    public KeyMetaPiteratorOfMetaNode
      ( Collection<MetaNode> inCollectionOfMetaNode, String inKeyString)
      /* This constructor creates a KeyMetaPiteratorOfMetaNode for 
        inCollectionOfMetaNode for search for nodes with key inKeyString.  
        */
      {
    	  super( inCollectionOfMetaNode );  // Construct superclass.
    	  keyString= inKeyString;  // Save key for which to search.

        this.lockOnTarget();  // Advance pointer to first search target.
    	  }
 
    public void next()	
      /* This advances the pointer to the next element, if any,
        and sets the pointer window appropriately. */
      {
  	    super.next();  // Advance superclass pointer to next candidate.
        this.lockOnTarget();  // Advance pointer to next search target.
    	  }
 
    private void lockOnTarget()
      /* If the pointer window is not on a node with the desired key
        then the superclass next() method is called until it is.
        */
      {
    	  while (true) {
    	  	MetaNode scanMetaNode= getE();  // Cache present candidate. 
    	    if ( scanMetaNode == null ) break;  // Exit if past end.
    	    if ( scanMetaNode.containsKey( keyString ) ) break;  // Exit if found.
    	    super.next();  // Advance superclass Piterator to next candidate.
      	  }
    	  }

    public void remove()  
      /* Does a remove by passing it to the nested iterator and relocking. */
      {
        super.remove();  // Pass to nested Piterator.
        this.lockOnTarget();  // Lock onto next target.
        }
    
    } // class KeyMetaPiteratorOfMetaNode.
