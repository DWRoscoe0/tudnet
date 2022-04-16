package allClasses;

import java.util.Collection;

//import java.util.Iterator;

public class PiteratorOfMetaNode
  
  /* This is a Piterator for iterating over the MetaNode children 
    in the MetaChildren instance referenced by a MetaNode.

    Note, this class and its subclasses might be the basis of 
    some generic classes for iterating over Collections of 
    objects with attributes/maps attached.
    This would have only basic lookup capability.

    Maybe call the interface a MiniMap.  ??
    */

  { // class PiteratorOfMetaNode.

    // Instance variables.

      private Piterator<MetaNode> nestedPiteratorOfMetaNodes;

    // Constructors.
      
      public PiteratorOfMetaNode( 
          Piterator<MetaNode> inNestedPiteratorOfMetaNode 
          )
        {
          nestedPiteratorOfMetaNodes= inNestedPiteratorOfMetaNode;
          }

      public PiteratorOfMetaNode( 
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
        /* This returns a reference to the MetaNode in the pointer window.  
          If the returned value is null then there isn't any in the window,
          and there are no more elements to process.
          */
        {
          return nestedPiteratorOfMetaNodes.getE();
          }
 
      public MetaNode nextE()
        /* This advances the pointer to the next element, if any,
          and sets the pointer window appropriately,
          and returns the MetaNode in the window, or null if there is none.
          */
        {
          return nestedPiteratorOfMetaNodes.nextE();
          }

      public void removeV()  
        /* Does a remove by passing it to the nested Piterator. */
        {
          nestedPiteratorOfMetaNodes.removeE();
          }
    
    }
