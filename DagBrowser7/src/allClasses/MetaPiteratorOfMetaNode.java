package allClasses;

import java.util.Collection;

//import java.util.Iterator;

public class MetaPiteratorOfMetaNode
  //extends Piterator<MetaNode>
  
  /* This is a Piterator for iterating over 
    the MetaNode children of the MetaChildren of a MetaNode.
    
    Note, this class and its subclasses might be the basis of 
    some generic classes for iterating over Collections of 
    objects with attributes/maps attached.
    This would have only basic lookup capability.
    Maybe call the interface a MiniMap.  ???
    */

  { // class MetaPiteratorOfMetaNode.

    private Piterator<MetaNode> NestedPiteratorMetaNode;
    
    public MetaPiteratorOfMetaNode
      ( Collection<MetaNode> inCollectionOfMetaNode )
      /* This constructor creates a Piterator for inCollectionOfMetaNode.  
        */
      {
    	  NestedPiteratorMetaNode= 
          new Piterator<MetaNode>(inCollectionOfMetaNode.iterator());
    	  }
 
    public void next()
      /* This advances the pointer to the next element, if any,
        and sets the pointer window appropriately. */
      {
        NestedPiteratorMetaNode.next();
        }

    public MetaNode getE() 
      /* This returns a reference to the element in the pointer window.  
        If the returned value is null then there isn't any in the window,
        and there are no more elements to process.
        */
      {
        return NestedPiteratorMetaNode.getE();
        }

    public void remove()  
      /* Does a remove by passing it to the nested iterator. */
      {
        NestedPiteratorMetaNode.remove();
        // Window set by nested Piterator.
        }
    
    }
