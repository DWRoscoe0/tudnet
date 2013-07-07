package allClasses;

import java.util.Collection;

public class KeyAndValueMetaPiteratorOfMetaNode
  extends KeyMetaPiteratorOfMetaNode
  
  /* This is a Piterator for iterating over 
    the MetaNode children of the MetaChildren of a MetaNode,
    but only the subset with a particular attribute Key and Value.
    */

  { // class KeyAndValueMetaPiteratorOfMetaNode.

		private Object valueObject;
    
    public KeyAndValueMetaPiteratorOfMetaNode
      ( Collection<MetaNode> inCollectionOfMetaNode, 
        String inKeyString, Object inValueObject
        )
      /* This constructor creates a KeyAndValueMetaPiteratorOfMetaNode 
        for inCollectionOfMetaNode using inKeyString and inValueObject.  
        */
      {
    	  super( inCollectionOfMetaNode, inKeyString );  // Construct superclass.
    	  valueObject= inValueObject;  // Save value for which to search.

        this.lockOnTarget();  // Advance pointer to first search target.
    	  }

    public void next()
      /* This advances the pointer to the next element, if any,
        and sets the pointer window appropriately. */
      {
  	    super.next();  // Advance superclass pointer to next candidate.
        this.lockOnTarget();  // Advance pointer to next search target.
        }

    public void lockOnTarget()
      /* This advances the pointer to the next element, if any,
        and sets the pointer window appropriately. */
      {
    	  while (true) {
    	  	MetaNode scanMetaNode= getE();  // Cache next candidate. 
    	    if ( scanMetaNode == null ) break;  // Exit if passed end.
    	    if  // Exit if attribute key and value both found.
            ( scanMetaNode.get( keyString ).equals( valueObject ) )
            break; 
    	    super.next();  // Advance superclass Piterator to next candidate.
      	  }
    	  }

    } // class KeyAndValueMetaPiteratorOfMetaNode.
