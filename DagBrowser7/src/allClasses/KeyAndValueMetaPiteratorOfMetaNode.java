package allClasses;

import java.util.Collection;

public class KeyAndValueMetaPiteratorOfMetaNode
  extends KeyMetaPiteratorOfMetaNode
  
  /* This is a Piterator for iterating over 
    the MetaNode children of the MetaChildren of a MetaNode,
    but only the subset with a particular attribute Key and Value.
    */

  { // class KeyAndValueMetaPiteratorOfMetaNode.

    // Instance variables.

      private Object valueObject;

    // Constructors.
      
      public KeyAndValueMetaPiteratorOfMetaNode
        ( Piterator<MetaNode> inNestedPiteratorOfMetaNode, 
          String inKeyString, Object inValueObject
          )
        /* This constructs from a Piterator,
          a key String and a value Object.
          */
        {
          super( // Construct superclass.
            inNestedPiteratorOfMetaNode, inKeyString 
            );  
          valueObject= inValueObject;  // Save value for which to search.
          this.SearchMetaNode();  // Advance pointer to first search target.
          }
      
      public KeyAndValueMetaPiteratorOfMetaNode
        ( // int DbgI,
          Collection<MetaNode> inCollectionOfMetaNode, 
          String inKeyString, Object inValueObject
          )
        /* This constructs from a Collection,
          a key String and a value Object.
          */
        {
          this(  // Use another constructor to do the work.
            new Piterator<MetaNode>( inCollectionOfMetaNode.iterator() ),
            inKeyString, inValueObject
            );
          //Misc.DbgOut( "KeyAndValueMetaPiteratorOfMetaNode(..) constructor");
          }

    // New Piterator methods.

      public void removeV()  
        /* Does a remove by passing it to the nested iterator and relocking. */
        {
          super.removeV();  // Pass to nested Piterator.
          this.SearchMetaNode();  // Find next search target.
          }

    // Private methods.

      private MetaNode SearchMetaNode()
        /* This advances the pointer to the next element, if any,
          and sets the pointer window appropriately. */
        {
          MetaNode scanMetaNode;
          while (true) {
            scanMetaNode= getE();  // Cache present candidate. 
            if ( scanMetaNode == null )  // Exit if passed end.
              break;
            if  // Exit if attribute key and value both found.
              ( scanMetaNode.get( keyString ).equals( valueObject ) )
              break; 
            super.nextE();  // Advance superclass Piterator to next candidate.
            }
          return scanMetaNode;
          }

    } // class KeyAndValueMetaPiteratorOfMetaNode.
