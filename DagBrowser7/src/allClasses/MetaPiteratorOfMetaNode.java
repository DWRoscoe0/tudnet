package allClasses;

import java.util.Collection;

//import java.util.Iterator;

public class MetaPiteratorOfMetaNode
  //extends Piterator<MetaNode>
  
  /* This is a Piterator for iterating over 
    the MetaNode children in the MetaChildren of a MetaNode.

    Note, this class and its subclasses might be the basis of 
    some generic classes for iterating over Collections of 
    objects with attributes/maps attached.
    This would have only basic lookup capability.

    Maybe call the interface a MiniMap.  ???
    */

  { // class MetaPiteratorOfMetaNode.

    // Instance variables.

      private Piterator<MetaNode> NestedPiteratorMetaNode;

    // Constructors.
      
      public MetaPiteratorOfMetaNode( 
      		Piterator<MetaNode> InNestedPiteratorOfMetaNode 
      		)
        {
          NestedPiteratorMetaNode= InNestedPiteratorOfMetaNode;
          }

      public MetaPiteratorOfMetaNode( 
      		Collection<MetaNode> inCollectionOfMetaNode 
      		)
        {
          this( 
            new Piterator<MetaNode>( inCollectionOfMetaNode.iterator() )
            );
          }

    // Overridden standard Iterator methods.

    // New Piterator methods.

      public MetaNode getE() 
        /* This returns a reference to the element in the pointer window.  
          If the returned value is null then there isn't any in the window,
          and there are no more elements to process.
          */
        {
          return NestedPiteratorMetaNode.getE();
          }
 
      public MetaNode nextE()
        /* This advances the pointer to the next element, if any,
          and sets the pointer window appropriately. */
        {
          return NestedPiteratorMetaNode.nextE();
          }

      public void removeV()  
        /* Does a remove by passing it to the nested Piterator. */
        {
          NestedPiteratorMetaNode.removeV();
          }
    
    }
