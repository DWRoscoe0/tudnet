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

    // Instance variables.

      protected String keyString;  // For the desired attribute key.

    // Constructors.
      
      public KeyMetaPiteratorOfMetaNode
        ( Piterator<MetaNode> inNestedPiteratorOfMetaNode, String inKeyString )
        /* This constructs from a Piterator and a key String.  */
        {
          super( inNestedPiteratorOfMetaNode );  // Construct superclass.
          keyString= inKeyString;  // Save key for which to search.

          this.SearchMetaNode();  // Advance pointer to first search target.
          }
      
      public KeyMetaPiteratorOfMetaNode
        ( Collection<MetaNode> inCollectionOfMetaNode, String inKeyString)
        /* This constructs from a Collection and a key String.  */
        {
          this(  // Use another constructor to do the work.
            new Piterator<MetaNode>( inCollectionOfMetaNode.iterator() ),
            inKeyString
            );
          }

    // Overridden standard Iterator methods.

    // New Piterator methods.
   
      public MetaNode nextE()	
        /* This advances the pointer to the next element, if any,
          and sets the pointer window appropriately. */
        {
          super.nextE();  // Advance superclass pointer to next candidate.
          return this.SearchMetaNode();  // Search for and return next match.
          }

      public void removeV()  
        /* Does a remove by passing it to the nested iterator and searching. */
        {
          super.removeV();  // Pass to superclass.
          this.SearchMetaNode();  // Search for next match.
          }

    // Private methods.
   
      private MetaNode SearchMetaNode()
        /* If the pointer window is not on a node with the desired key
          then the superclass nextE() method is called until it is.
          */
        {
          MetaNode scanMetaNode;
          while (true) {
            scanMetaNode= getE();  // Cache present candidate. 
            if ( scanMetaNode == null )  // Exit if past end.
              break;
            if ( scanMetaNode.containsKey( keyString ) )  // Exit if found.
              break;
            super.nextE();  // Advance superclass Piterator to next candidate.
            }
          return scanMetaNode;
          }
      
    } // class KeyMetaPiteratorOfMetaNode.
